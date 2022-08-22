package me.szumielxd.proxyserverlist.bungee;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.szumielxd.proxyserverlist.bungee.commands.BungeeCommandWrapper;
import me.szumielxd.proxyserverlist.bungee.listeners.BungeeChannelListener;
import me.szumielxd.proxyserverlist.bungee.listeners.BungeeMotdListener;
import me.szumielxd.proxyserverlist.bungee.objects.BungeeScheduler;
import me.szumielxd.proxyserverlist.bungee.objects.BungeeSenderWrapper;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.ProxyServerListProvider;
import me.szumielxd.proxyserverlist.common.commands.CommonCommand;
import me.szumielxd.proxyserverlist.common.commands.MainCommand;
import me.szumielxd.proxyserverlist.common.commands.ServersCommand;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.ServersConfig;
import me.szumielxd.proxyserverlist.common.gui.ServersGUI;
import me.szumielxd.proxyserverlist.common.managers.ServerPingManager;
import me.szumielxd.proxyserverlist.common.managers.WebManager;
import me.szumielxd.proxyserverlist.common.objects.CommonScheduler;
import me.szumielxd.proxyserverlist.common.objects.SenderWrapper;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class ProxyServerListBungee extends Plugin implements ProxyServerList<CommandSender, ProxiedPlayer> {
	
	
	private BungeeAudiences adventure = null;
	private @Nullable BungeeScheduler scheduler;
	private @Nullable BungeeSenderWrapper senderWrapper;
	private @Nullable ServerPingManager<CommandSender, ProxiedPlayer> pingManager;
	private @Nullable ServersConfig serversConfig;
	private @Nullable ServersGUI<CommandSender, ProxiedPlayer> serversGUI;
	private @Nullable WebManager webmanager;
	
	
	
	public BungeeAudiences adventure() {
		if (this.adventure == null) throw new IllegalStateException("Cannot retrieve audience provider while plugin is not enabled");
		return this.adventure;
	}
	
	
	@Override
	public void onEnable() {
		ProxyServerListProvider.init(this);
		this.scheduler = new BungeeScheduler(this);
		this.adventure = BungeeAudiences.create(this);
		this.senderWrapper = new BungeeSenderWrapper(this);
		this.registerCommand(new MainCommand<>(this));
		this.registerCommand(new ServersCommand<>(this));
		Config.load(new File(this.getDataFolder(), "config.yml"), this);
		this.getLogger().info("Loading server icons...");
		this.serversConfig = new ServersConfig(new File(this.getDataFolder(), "servers.yml")).load(this);
		this.getLogger().info(String.format("Successfully loaded %d server icons!", this.serversConfig.getServerIcons().size()));
		this.pingManager = new ServerPingManager<>(this).start();
		this.serversGUI = new ServersGUI<>(this).start();
		this.webmanager = new WebManager(this).start();
		this.getProxy().registerChannel(SERVERLIST_CHANNEL);
		this.getProxy().getPluginManager().registerListener(this, new BungeeMotdListener(this));
		this.getProxy().getPluginManager().registerListener(this, new BungeeChannelListener(this));
	}
	
	
	private void registerCommand(@NotNull CommonCommand<CommandSender> command) {
		this.getProxy().getPluginManager().registerCommand(this, new BungeeCommandWrapper(this, command));
	}
	
	
	@Override
	public void onDisable() {
		this.getLogger().info("Disabling all modules...");
		this.getProxy().getScheduler().cancel(this);
		this.pingManager.stop();
		this.serversGUI.stop();
		this.webmanager.stop();
		this.getProxy().getPluginManager().unregisterListeners(this);
		if (this.adventure != null) {
			this.adventure.close();
			this.adventure = null;
		}
		try {
			Class<?> bungeeAudiencesImpl = Class.forName("net.kyori.adventure.platform.bungeecord.BungeeAudiencesImpl");
			Field f = bungeeAudiencesImpl.getDeclaredField("INSTANCES");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, BungeeAudiences> instances = (Map<String, BungeeAudiences>) f.get(null);
			instances.remove(this.getDescription().getName());
		} catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		this.getProxy().getPluginManager().unregisterCommands(this);
		this.getLogger().info("Well done. Time to sleep!");
	}


	@Override
	public @NotNull CommonScheduler getScheduler() {
		if (this.scheduler == null) throw new IllegalStateException("Plugin is not initialized");
		return this.scheduler;
	}


	@Override
	public @NotNull SenderWrapper<CommandSender, ProxiedPlayer> getSenderWrapper() {
		if (this.senderWrapper == null) throw new IllegalStateException("Plugin is not initialized");
		return this.senderWrapper;
	}


	@Override
	public @NotNull ServerPingManager<CommandSender, ProxiedPlayer> getServerPingManager() {
		if (this.pingManager == null) throw new IllegalStateException("Plugin is not initialized");
		return this.pingManager;
	}


	@Override
	public @NotNull ServersConfig getServersConfig() {
		if (this.serversConfig == null) throw new IllegalStateException("Plugin is not initialized");
		return this.serversConfig;
	}


	@Override
	public @NotNull ServersGUI<CommandSender, ProxiedPlayer> getServersGUI() {
		if (this.serversGUI == null) throw new IllegalStateException("Plugin is not initialized");
		return this.serversGUI;
	}


	@Override
	public @NotNull String getName() {
		return this.getDescription().getName();
	}


	@Override
	public @NotNull String getVersion() {
		return this.getDescription().getVersion();
	}
	

}
