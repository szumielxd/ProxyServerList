package me.szumielxd.proxyserverlist.common.managers;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import io.netty.channel.unix.DomainSocketAddress;
import lombok.Getter;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerIcon;
import me.szumielxd.proxyserverlist.common.configuration.ServersConfig;
import me.szumielxd.proxyserverlist.common.objects.CachedServerInfo;
import me.szumielxd.proxyserverlist.common.objects.CommonScheduler.ExecutedTask;
import me.szumielxd.proxyserverlist.common.objects.PingResult;
import me.szumielxd.proxyserverlist.common.objects.PingResult.Players;
import me.szumielxd.proxyserverlist.common.objects.SenderWrapper;
import me.szumielxd.proxyserverlist.common.utils.CompatibleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class ServerPingManager<T, U extends T> {
	
	
	private static final Pattern fieldPattern = Pattern.compile("MINECRAFT_(1_\\d+(_\\d+)?)");
	private static final Map<Integer, String> VERSIONS_MAP = Stream.of(ProtocolVersions.class.getDeclaredFields()).map(f -> {
			try {
				Matcher match = fieldPattern.matcher(f.getName());
				if (match.matches()) {
					f.setAccessible(true);
					int id = f.getInt(null);
					return new AbstractMap.SimpleEntry<>(id, match.group(1).replace('_', '.'));
				}
				return null;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
	
	
	private final @NotNull ProxyServerList<T, U> plugin;
	private final @NotNull Map<String, CachedServerInfo> cache;
	private final @NotNull CompatibleComponent notification;
	private final int notifyAfter = Config.SERVERSTATUS_NOTIFY_AFTER.getInt() * 1000;
	private final int notifyInterval = Config.SERVERSTATUS_NOTIFY_INTERVAL.getInt() * 1000;
	private @Nullable ExecutedTask updateTask;
	private long lastNotification = 0;
	private final @Getter @NotNull String globalVersion;
	
	
	public ServerPingManager(@NotNull ProxyServerList<T, U> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		this.cache = this.plugin.getServersConfig().getServerIcons().values().stream().map(SerializableServerIcon::getNames)
				.flatMap(List::stream).filter(srv -> this.plugin.getSenderWrapper().getPlayers(srv).isPresent())
				.collect(Collectors.toMap(String::toLowerCase, i -> new CachedServerInfo(i), (a, b) -> a));
		this.notification = new CompatibleComponent(Config.SERVERSTATUS_NOTIFY_MESSAGE.getString());
		List<Entry<Integer, String>> versions = ServerPingManager.VERSIONS_MAP.entrySet().stream().filter(e -> e.getKey() > 0).sorted(Entry.comparingByKey()).collect(Collectors.toList());
		this.globalVersion = versions.size() == 1 ? versions.get(0).getValue() : versions.get(0).getValue() + "-" + versions.get(versions.size() -1).getValue();
	}
	
	
	public @NotNull JsonElement serializeServersToJson() {
		JsonObject json = new JsonObject();
		this.cache.forEach((name, server) -> json.add(name, server.serializeToJson()));
		return json;
	}
	
	
	public ServerPingManager<T, U> start() {
		if (Config.SERVERSTATUS_REFRESH_TIME.getInt() > 0) {
			this.plugin.getScheduler().runTaskTimer(this::update, 1L, Config.SERVERSTATUS_REFRESH_TIME.getInt(), TimeUnit.SECONDS);
		}
		return this;
	}
	
	
	public boolean stop() {
		if (this.updateTask == null) return false;
		this.updateTask.cancel();
		return true;
	}
	
	
	public void update() {
		final Map<String, CachedServerInfo> dead = this.pingServers();
		if (!dead.isEmpty() && System.currentTimeMillis() >= this.lastNotification + this.notifyInterval && this.notifyDown(dead)) {
			this.lastNotification = System.currentTimeMillis();
		}
	}
	
	
	private Map<String, CachedServerInfo> pingServers() {
		ServersConfig cfg = this.plugin.getServersConfig();
		return this.cache.entrySet().parallelStream().map(entry -> {
			CompletableFuture<Entry<String, CachedServerInfo>> future = new CompletableFuture<>();
			Optional<SocketAddress> address = this.plugin.getSenderWrapper().getServerAddress(entry.getKey());
			if (!address.isPresent()) {
				future.complete(null);
			} else {
				ping(address.get(), result -> {
					Entry<String, CachedServerInfo> down = null;
					try {
						CachedServerInfo info = entry.getValue();
						if (result.getPing() > -1) {
							if (info.getPing() <= -1) info.setLastStateToggle(System.currentTimeMillis());
							info.setDescription(result.getDescription());
							info.setVersionId(result.getVersion().getProtocol());
							info.setVersionFriendlyName(VERSIONS_MAP.getOrDefault(result.getVersion().getProtocol(), "?"));
							info.setVersionName(result.getVersion().getName());
						} else {
							if (info.getPing() > -1) info.setLastStateToggle(System.currentTimeMillis());
							else if (System.currentTimeMillis() >= info.getLastStateToggle() + this.notifyAfter) down = entry;
						}
						Optional<Players> players = result.getPlayers();
						info.setOnline(players.map(Players::getOnline).orElse(0));
						info.setMaxOnline(players.map(Players::getMax).orElse(0));
						info.setPing(result.getPing());
					} finally {
						future.complete(down);
					}
				});
			}
			return future;
		}).map(CompletableFuture::join).filter(Objects::nonNull)
				.filter(e -> Optional.ofNullable(cfg.getServerIcons().get(e.getValue().getFriendlyName().toLowerCase()))
						.filter(s -> !s.isHidingOffline()).isPresent())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}
	
	
	private boolean notifyDown(@NotNull Map<String, CachedServerInfo> dead) {
		SenderWrapper<T, U> swrapper = this.plugin.getSenderWrapper();
		return swrapper.getPlayers().parallelStream().filter(p -> swrapper.hasPermission(p, "proxyserverlist.notify.serverdown")).filter(p -> {
			ProtocolizePlayer prot = Protocolize.playerProvider().player(swrapper.getUniqueId(p));
			if (prot != null) {
				Component base = this.notification.get(prot.protocolVersion());
				Iterator<Component> iter = dead.entrySet().stream().map(entry -> {
					int diff = (int) (System.currentTimeMillis() - entry.getValue().getLastStateToggle())/1000;
					int sec = diff % 60;
					diff /= 60;
					int min = diff % 60;
					diff /= 60;
					StringBuilder time = new StringBuilder(sec + "s");
					if (min > 0) time.insert(0, min + "m ");
					if (diff > 0) time.insert(0, diff + "h ");
					return base.replaceText(b -> b.matchLiteral("{time}").replacement(time.toString())).replaceText(b -> b.matchLiteral("{name}").replacement(entry.getKey()));
				}).iterator();
				Component comp = iter.next();
				while (iter.hasNext()) comp = comp.append(Component.newline()).append(iter.next());
				swrapper.sendMessage(p, comp);
			}
			return prot != null;
		}).count() > 0;
	}
	
	
	public Optional<CachedServerInfo> getCachedServer(@NotNull String server) {
		Objects.requireNonNull(server, "server cannot be null");
		return Optional.ofNullable(this.cache.get(server.toLowerCase()));
	}
	
	
	public boolean isCachedOnline(@NotNull String server) {
		Objects.requireNonNull(server, "server cannot be null");
		return this.getCachedServer(server).filter(srv -> srv.getPing() >= 0).isPresent();
	}
	
	
	public int readVarInt(DataInputStream in) throws IOException {
		int i = 0;
		int j = 0;
		while (true) {
			int k = in.readByte();
			i |= (k & 0x7F) << j++ * 7;
			if (j > 5) throw new RuntimeException("VarInt too big");
			if ((k & 0x80) != 128) break;
		}
		return i;
	}
 
	public void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
		while (true) {
			if ((paramInt & 0xFFFFFF80) == 0) {
			  out.writeByte(paramInt);
			  return;
			}

			out.writeByte(paramInt & 0x7F | 0x80);
			paramInt >>>= 7;
		}
	}
	
	
	private void ping(SocketAddress address, Consumer<PingResult> resultConsumer) {
		new Thread(() -> {
			PingResult result = new PingResult(new PingResult.Version(-1, null), Optional.empty(), Component.empty(), (int) -1);
			
			String hostname = address instanceof InetSocketAddress ? ((InetSocketAddress) address).getHostString() : ((DomainSocketAddress) address).path();
			
			try (Socket socket = new Socket()) {
				socket.setSoTimeout(5000);
				long time = System.currentTimeMillis();
				socket.connect(address, 5000);
				try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream handshake = new DataOutputStream(baos);
					handshake.writeByte(0x00); // handshake packet id
					this.writeVarInt(handshake, -1); // protocol version
					this.writeVarInt(handshake, hostname.length()); // hostname length
					handshake.writeBytes(hostname); // hostname
					handshake.writeShort(address instanceof InetSocketAddress ? ((InetSocketAddress) address).getPort() : 1);
					this.writeVarInt(handshake, 1); // state (1 for handshake)
					
					this.writeVarInt(out, baos.size()); // handshake size
					out.write(baos.toByteArray()); // handshake
					
					out.writeByte(0x01);
					out.writeByte(0x00);
					
					try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
						this.readVarInt(in); // packet size
						int id = this.readVarInt(in); // packet id
						if (id == 0x00) { // handshake packet id
							int length = this.readVarInt(in); // string length
							if (length > 0) {
								byte[] bytes = new byte[length];
								in.readFully(bytes);
								time = System.currentTimeMillis() - time;
								JsonObject json = new Gson().fromJson(new String(bytes), JsonObject.class);
								
								JsonObject version = json.get("version").getAsJsonObject();
								String versionName = version.get("name").getAsString();
								int versionId = version.get("protocol").getAsInt();
								
								JsonObject players = json.get("players").getAsJsonObject();
								int online = players.get("online").getAsInt();
								int max = players.get("max").getAsInt();
								
								Component description = GsonComponentSerializer.gson().deserializeFromTree(json.get("description"));
								
								result = new PingResult(new PingResult.Version(versionId, versionName), Optional.of(new PingResult.Players(online, max, Collections.emptyList())), description, (int) time);
								
							}
						}
					}
					
				}
			} catch (IOException e) {
				// empty catch
			} finally {
				resultConsumer.accept(result);
			}
		},"proxyserverlist-pinger").start();
	}
	

}
