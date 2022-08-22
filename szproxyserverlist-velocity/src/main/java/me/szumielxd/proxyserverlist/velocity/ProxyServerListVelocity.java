package me.szumielxd.proxyserverlist.velocity;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

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
import me.szumielxd.proxyserverlist.velocity.commands.VelocityCommandWrapper;
import me.szumielxd.proxyserverlist.velocity.listeners.VelocityChannelListener;
import me.szumielxd.proxyserverlist.velocity.listeners.VelocityMotdListener;
import me.szumielxd.proxyserverlist.velocity.objects.VelocityScheduler;
import me.szumielxd.proxyserverlist.velocity.objects.VelocitySenderWrapper;

@Plugin(
		id = "id----",
		name = "@pluginName@",
		version = "@version@",
		authors = { "@author@" },
		description = "@description@",
		url = "https://github.com/szumielxd/ProxyServerList/",
		dependencies = { 
				@Dependency( id="protocolize", optional=false )
		}
)
public class ProxyServerListVelocity implements ProxyServerList<CommandSource, Player> {
	
	
	private final ProxyServer server;
	private final Logger logger;
	private final File dataFolder;
	
	
	private @Nullable VelocityScheduler scheduler;
	private @Nullable VelocitySenderWrapper senderWrapper;
	private @Nullable ServerPingManager<CommandSource, Player> pingManager;
	private @Nullable ServersConfig serversConfig;
	private @Nullable ServersGUI<CommandSource, Player> serversGUI;
	private @Nullable WebManager webmanager;
	
	
	@Inject
	public ProxyServerListVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		this.server = server;
		this.logger = logger;
		this.dataFolder = dataDirectory.toFile();
	}
	
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
	    this.onEnable();
	}
	
	
	@Override
	public void onEnable() {
		ProxyServerListProvider.init(this);
		this.scheduler = new VelocityScheduler(this);
		this.senderWrapper = new VelocitySenderWrapper(this);
		this.registerCommand(new MainCommand<>(this));
		this.registerCommand(new ServersCommand<>(this));
		Config.load(new File(this.dataFolder, "config.yml"), this);
		this.getLogger().info("Loading server icons...");
		this.serversConfig = new ServersConfig(new File(this.dataFolder, "servers.yml")).load(this);
		this.getLogger().info(String.format("Successfully loaded %d server icons!", this.serversConfig.getServerIcons().size()));
		this.pingManager = new ServerPingManager<>(this).start();
		this.serversGUI = new ServersGUI<>(this).start();
		this.webmanager = new WebManager(this).start();
		this.getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.from(SERVERLIST_CHANNEL));
		this.getProxy().getEventManager().register(this, new VelocityMotdListener(this));
		this.getProxy().getEventManager().register(this, new VelocityChannelListener(this));
	}
	
	
	private void registerCommand(@NotNull CommonCommand<CommandSource> command) {
		CommandManager mgr = this.getProxy().getCommandManager();
		CommandMeta meta = mgr.metaBuilder(command.getName()).aliases(command.getAliases()).build();
		mgr.register(meta, new VelocityCommandWrapper(this, command));
	}
	
	
	@Override
	public void onDisable() {
		this.getLogger().info("Disabling all modules...");
		this.pingManager.stop();
		this.serversGUI.stop();
		this.webmanager.stop();
		this.getProxy().getEventManager().unregisterListeners(this);
		this.getScheduler().cancelAll();
		this.getLogger().info("Well done. Time to sleep!");
	}
	
	
	@Override
	public @NotNull Logger getLogger() {
		return this.logger;
	}
	
	
	public @NotNull ProxyServer getProxy() {
		return this.server;
	}


	@Override
	public @NotNull CommonScheduler getScheduler() {
		if (this.scheduler == null) throw new IllegalStateException("Plugin is not initialized");
		return this.scheduler;
	}


	@Override
	public @NotNull VelocitySenderWrapper getSenderWrapper() {
		if (this.senderWrapper == null) throw new IllegalStateException("Plugin is not initialized");
		return this.senderWrapper;
	}


	@Override
	public @NotNull ServerPingManager<CommandSource, Player> getServerPingManager() {
		if (this.pingManager == null) throw new IllegalStateException("Plugin is not initialized");
		return this.pingManager;
	}


	@Override
	public @NotNull ServersConfig getServersConfig() {
		if (this.serversConfig == null) throw new IllegalStateException("Plugin is not initialized");
		return this.serversConfig;
	}


	@Override
	public @NotNull ServersGUI<CommandSource, Player> getServersGUI() {
		if (this.serversGUI == null) throw new IllegalStateException("Plugin is not initialized");
		return this.serversGUI;
	}


	@Override
	public @NotNull String getName() {
		return this.getProxy().getPluginManager().ensurePluginContainer(this).getDescription().getName().orElse("");
	}


	@Override
	public @NotNull String getVersion() {
		return this.getProxy().getPluginManager().ensurePluginContainer(this).getDescription().getVersion().orElse("");
	}
	

}
