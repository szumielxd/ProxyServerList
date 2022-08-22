package me.szumielxd.proxyserverlist.common;

import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.proxyserverlist.common.configuration.ServersConfig;
import me.szumielxd.proxyserverlist.common.gui.ServersGUI;
import me.szumielxd.proxyserverlist.common.managers.ServerPingManager;
import me.szumielxd.proxyserverlist.common.objects.CommonScheduler;
import me.szumielxd.proxyserverlist.common.objects.SenderWrapper;

public interface ProxyServerList<T, U extends T> {
	
	
	public static final String SERVERLIST_CHANNEL = "psl:main";
	
	
	public @NotNull Logger getLogger();
	
	public @NotNull CommonScheduler getScheduler();
	
	public @NotNull SenderWrapper<T, U> getSenderWrapper();
	
	public @NotNull ServerPingManager<T, U> getServerPingManager();
	
	public @NotNull ServersConfig getServersConfig();
	
	public @NotNull ServersGUI<T, U> getServersGUI();
	
	public @NotNull String getName();
	
	public @NotNull String getVersion();
	
	
	public void onEnable();
	
	public void onDisable();
	

}
