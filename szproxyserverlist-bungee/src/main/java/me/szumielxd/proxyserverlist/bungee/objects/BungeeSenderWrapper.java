package me.szumielxd.proxyserverlist.bungee.objects;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import me.szumielxd.proxyserverlist.bungee.ProxyServerListBungee;
import me.szumielxd.proxyserverlist.common.objects.SenderWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeSenderWrapper implements SenderWrapper<CommandSender, ProxiedPlayer> {
	
	
	private final @Getter @NotNull ProxyServerListBungee plugin;
	
	
	public BungeeSenderWrapper(@NotNull ProxyServerListBungee plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
	}
	

	@Override
	public @NotNull UUID getUniqueId(@NotNull ProxiedPlayer player) {
		return player.getUniqueId();
	}

	@Override
	public @NotNull String getName(@NotNull CommandSender sender) {
		return sender.getName();
	}

	@Override
	public int getProtocolVersion(@NotNull ProxiedPlayer player) {
		if (player.getPendingConnection() != null) return player.getPendingConnection().getVersion();
		return -1;
	}
	
	@Override
	public void sendMessage(@NotNull CommandSender sender, @NotNull Component message) {
		Objects.requireNonNull(sender, "sender cannot be null");
		Objects.requireNonNull(message, "message cannot be null");
		this.plugin.adventure().sender(sender).sendMessage(message);
	}
	
	@Override
	public boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission) {
		return Objects.requireNonNull(sender, "sender cannot be null").hasPermission(permission);
	}

	@Override
	public void connectToServer(@NotNull ProxiedPlayer player, @NotNull String server, @Nullable BiConsumer<ProxiedPlayer, Boolean> resultConsumer) {
		Optional.ofNullable(this.plugin.getProxy().getServerInfo(server)).ifPresent(srv -> {
			if (resultConsumer != null) player.connect(srv, (result, throwable) -> resultConsumer.accept(player, result));
			else player.connect(srv);
		});
	}

	@Override
	public @NotNull CommandSender getConsole() {
		return this.plugin.getProxy().getConsole();
	}

	@Override
	public boolean isPlayer(@Nullable CommandSender sender) {
		return sender instanceof ProxiedPlayer;
	}

	@Override
	public boolean isOnline(@NotNull ProxiedPlayer player) {
		return player.isConnected();
	}

	@Override
	public @NotNull Optional<ProxiedPlayer> getPlayer(@NotNull UUID uuid) {
		return Optional.ofNullable(this.plugin.getProxy().getPlayer(uuid));
	}

	@Override
	public @NotNull Optional<ProxiedPlayer> getPlayer(@NotNull String playerName) {
		return Optional.ofNullable(this.plugin.getProxy().getPlayer(playerName));
	}

	@Override
	public @NotNull Collection<ProxiedPlayer> getPlayers() {
		return this.plugin.getProxy().getPlayers();
	}
	
	@Override
	public int getPlayerCount() {
		return this.plugin.getProxy().getOnlineCount();
	}

	@Override
	public @NotNull Optional<Collection<ProxiedPlayer>> getPlayers(@NotNull String serverName) {
		return Optional.ofNullable(this.plugin.getProxy().getServerInfo(serverName)).map(ServerInfo::getPlayers);
	}

	@Override
	public @Nullable BaseComponent[] componentToBase(@Nullable Component component) {
		return BungeeComponentSerializer.get().serializeOrNull(component);
	}

	@Override
	public @Nullable Component baseToComponent(@Nullable Object component) {
		return BungeeComponentSerializer.get().deserializeOrNull(null);
	}

	@Override
	public @NotNull Optional<SocketAddress> getServerAddress(@NotNull String serverName) {
		return Optional.ofNullable(this.plugin.getProxy().getServerInfo(serverName)).map(ServerInfo::getSocketAddress);
	}
	
	@Override
	public @NotNull Optional<String> getFavicon() {
		return Optional.ofNullable(this.plugin.getProxy().getConfig().getFaviconObject()).map(Favicon::getEncoded);
	}
	
	@Override
	public int getMaxPlayers() {
		return this.plugin.getProxy().getConfig().getPlayerLimit();
	}

}
