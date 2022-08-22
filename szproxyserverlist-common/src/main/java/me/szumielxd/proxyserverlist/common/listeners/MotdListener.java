package me.szumielxd.proxyserverlist.common.listeners;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerIcon;
import me.szumielxd.proxyserverlist.common.objects.CachedServerInfo;
import me.szumielxd.proxyserverlist.common.objects.PingResult;
import me.szumielxd.proxyserverlist.common.objects.enums.ServerSortType;
import me.szumielxd.proxyserverlist.common.utils.CompatibleComponent.CompatibleRawComponent;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public abstract class MotdListener {
	
	
	private final @NotNull ProxyServerList<?, ?> plugin;
	private final int cacheTime;
	private final boolean modifyPlayers;
	private final int modifyMaxPlayers;
	private final @NotNull List<String> playersDisplay;
	private final @NotNull List<CompatibleRawComponent> motd;
	private final @NotNull List<String> versionNames;
	
	private final @NotNull List<SerializableServerIcon> shownServers;
	
	private final @NotNull List<ServerSortType> sortOrder;
	private final @NotNull Comparator<Entry<SerializableServerIcon, Integer>> serverComparator;
	
	private long lastUpdate;
	private PingResult cachedResult;
	
	
	protected MotdListener(@NotNull ProxyServerList<?, ?> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		this.cacheTime = Config.MOTD_CACHE.getInt() * 1000;
		this.modifyPlayers = Config.MOTD_PLAYERS_MODIFY.getBoolean();
		this.modifyMaxPlayers = Config.MOTD_PLAYERS_SHOWMOREMAX.getInt();
		
		List<String> filteredServers = Config.MOTD_SERVERS.getStringList().stream().map(String::toLowerCase).collect(Collectors.toList());
		this.shownServers = this.plugin.getServersConfig().getServerIcons().values().stream()
				.filter(srv -> Config.MOTD_SERVERS_HIDELISTED.getBoolean() != filteredServers.contains(srv.getFriendlyName().toLowerCase()))
				.collect(Collectors.toList());
		
		this.playersDisplay = Config.MOTD_PLAYERS_DISPLAY.getStringList().stream().map(str -> MiscUtil.translateAlternateColorCodes('&', str)).collect(Collectors.toList());
		
		this.sortOrder = Config.MOTD_SERVERS_ORDER.getStringList().stream().map(ServerSortType::tryParse).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
		this.serverComparator = (e1, e2) -> {
			Iterator<ServerSortType> iter = this.sortOrder.iterator();
			int result = 0;
			while (result == 0 && iter.hasNext()) {
				result = iter.next().compare(e1, e2);
			}
			return result;
		};
		this.motd = Config.MOTD_DISPLAY.getStringList().stream().map(CompatibleRawComponent::new).collect(Collectors.toList());
		this.versionNames = Config.MOTD_VERSION_DISPLAY.getStringList().stream().map(str -> MiscUtil.translateAlternateColorCodes('&', str)).collect(Collectors.toList());
	}
	
	
	protected @NotNull PingResult applyMotd(@NotNull PingResult ping, int protocolId) {
		
		if (this.lastUpdate + this.cacheTime <= System.currentTimeMillis() || this.cachedResult != null) {
			
			// set max players
			if (this.modifyPlayers) {
				PingResult.Players players = ping.getPlayers().orElse(new PingResult.Players(0, 0, Collections.emptyList()));
				players.setMax(Math.min(0, players.getMax() + this.modifyMaxPlayers));
				ping.setPlayers(Optional.of(players));
			}
			
			if (!this.playersDisplay.isEmpty()) {
				PingResult.Players players = ping.getPlayers().orElse(new PingResult.Players(0, 0, Collections.emptyList()));
				int gonline = players.getOnline();
				int gmaxOnline = players.getMax();
				
				List<PingResult.Players.SamplePlayer> samples = new ArrayList<>();
				for (String line : this.playersDisplay) {
					line = this.parseStaticPlaceholders(line, gonline, gmaxOnline);
					if (line.contains("{servers}")) {
						
						List<Entry<SerializableServerIcon, Integer>> servers = new ArrayList<>();
						
						if (!this.shownServers.isEmpty()) for (SerializableServerIcon icon : this.shownServers) {
							List<CachedServerInfo> cachedInfo = icon.getNames().stream().map(this.plugin.getServerPingManager()::getCachedServer).filter(Optional::isPresent).map(Optional::get).filter(s -> !icon.isHidingOffline() || s.getPing() > -1).collect(Collectors.toList());
							if (!cachedInfo.isEmpty()) servers.add(new AbstractMap.SimpleEntry<>(icon, icon.isUsePingedPlayers() ? cachedInfo.stream().mapToInt(CachedServerInfo::getOnline).sum()
									: icon.getNames().stream().map(this.plugin.getSenderWrapper()::getPlayers).filter(Optional::isPresent).map(Optional::get).mapToInt(Collection::size).sum()));
						}
						
						Collections.sort(servers, this.serverComparator);
						
						if (!servers.isEmpty()) for (Entry<SerializableServerIcon, Integer> entry : servers) {
							List<CachedServerInfo> cachedInfo = entry.getKey().getNames().stream().map(this.plugin.getServerPingManager()::getCachedServer).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
							int maxOnline = cachedInfo.stream().mapToInt(CachedServerInfo::getMaxOnline).sum();
							String accent = LegacyComponentSerializer.legacySection().serialize(Component.text(".", entry.getKey().getAccent()));
							accent = accent.substring(0, accent.length()-1);
							String version = cachedInfo.isEmpty() ? "?" : cachedInfo.get(0).getVersionFriendlyName();
							samples.add(new PingResult.Players.SamplePlayer(UUID.randomUUID(), this.parseServerPlaceholders(line, entry.getValue(), maxOnline, entry.getKey().getFriendlyName(), accent, version == null ? "?" : version)));
						}
						
					} else {
						samples.add(new PingResult.Players.SamplePlayer(UUID.randomUUID(), line));
					}
				}
				players.setPlayers(samples);
			}
			
			if (!this.motd.isEmpty()) {
				String text = this.parseStaticPlaceholders(MiscUtil.random(this.motd).get(protocolId == -1 ? Integer.MAX_VALUE : protocolId),
						ping.getPlayers().map(PingResult.Players::getOnline).orElse(0),
						ping.getPlayers().map(PingResult.Players::getMax).orElse(0));
				ping.setDescription(MiscUtil.parseComponent(text, protocolId == -1 || protocolId >= 735, false));			
			}
			
			if (!this.versionNames.isEmpty()) {
				String text = this.parseStaticPlaceholders(MiscUtil.random(this.versionNames), 
						ping.getPlayers().map(PingResult.Players::getOnline).orElse(0),
						ping.getPlayers().map(PingResult.Players::getMax).orElse(0));
				ping.setVersion(new PingResult.Version(-1, text));
			}
			
			this.cachedResult = ping.unmodifiable();
			this.lastUpdate = System.currentTimeMillis();
		}
		
		return this.cachedResult;
		
	}
	
	
	private String parseStaticPlaceholders(String str, int online, int maxOnline) {
		return str.replace("{gonline}", String.valueOf(online))
				.replace("{gmaxonline}", String.valueOf(maxOnline))
				.replace("{gversion}", this.plugin.getServerPingManager().getGlobalVersion());
	}
	
	private String parseServerPlaceholders(String str, int online, int maxOnline, String name, String accent, String version) {
		return str.replace("{online}", String.valueOf(online))
				.replace("{maxonline}", String.valueOf(maxOnline))
				.replace("{servers}", name)
				.replace("{accent}", accent)
				.replace("{version}", version);
	}
	

}
