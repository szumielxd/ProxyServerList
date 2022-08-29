package me.szumielxd.proxyserverlist.common.objects;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import net.kyori.adventure.text.Component;

public interface SenderWrapper<T, U extends T> {
	
	
	public @NotNull UUID getUniqueId(@NotNull U player);
	
	public @NotNull String getName(@NotNull T sender);
	
	public int getProtocolVersion(@NotNull U player);
	
	public boolean hasPermission(@NotNull T sender, @NotNull String permission);
	
	public void sendMessage(@NotNull T sender, @NotNull Component message);

	public default void connectToServer(@NotNull U player, @NotNull String server) {
		this.connectToServer(player, server, null);
	}
	
	public void connectToServer(@NotNull U player, @NotNull String server, @Nullable BiConsumer<U, Boolean> resultConsumer);
	
	public @NotNull T getConsole();
	
	public boolean isPlayer(@Nullable T sender);
	
	public boolean isOnline(@NotNull U sender);

	public @NotNull Optional<U> getPlayer(@NotNull UUID uuid);
	
	public @NotNull Optional<U> getPlayer(@NotNull String playerName);
	
	public @NotNull Collection<U> getPlayers();
	
	public int getPlayerCount();
	
	public @NotNull Optional<Collection<U>> getPlayers(@NotNull String serverName);
	
	public @Nullable Object componentToBase(@Nullable Component component);
	
	public @Nullable Component baseToComponent(@Nullable Object component);
	
	public @NotNull Optional<SocketAddress> getServerAddress(@NotNull String serverName);
	
	public @NotNull ProxyServerList<T, U> getPlugin();
	
	public @NotNull Optional<String> getFavicon();
	
	public int getMaxPlayers();
	

}
