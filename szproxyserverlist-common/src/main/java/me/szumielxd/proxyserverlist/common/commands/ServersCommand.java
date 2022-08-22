package me.szumielxd.proxyserverlist.common.commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerIcon;
import me.szumielxd.proxyserverlist.common.managers.ServerPingManager;
import me.szumielxd.proxyserverlist.common.objects.SenderWrapper;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;
import net.kyori.adventure.text.Component;

public class ServersCommand<T, U extends T> extends CommonCommand<T> {
	
	private final @NotNull ProxyServerList<T, U> plugin;
	private final @NotNull Component invalidServerMessage = MiscUtil.parseComponent(Config.MESSAGES_INVALID_SERVER.getString(), true, false);

	public ServersCommand(@NotNull ProxyServerList<T, U> plugin) {
		super(Config.GUI_COMMAND_NAME.getString(), "proxyserverlist.command.servers", Config.GUI_COMMAND_ALIASES.getStringList().toArray(new String[0]));
		this.plugin = plugin;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(@NotNull T sender, String[] args) {
		SenderWrapper<T, U> swrapper = this.plugin.getSenderWrapper();
		if (swrapper.isPlayer(sender)) {
			if (args.length == 0) {
				UUID uuid = this.plugin.getSenderWrapper().getUniqueId((U) sender);
				this.plugin.getServersGUI().open(uuid);
				return;
			}
			if (args.length > 0) {
				ServerPingManager<T, U> mgr = this.plugin.getServerPingManager();
				SerializableServerIcon srv = this.plugin.getServersConfig().getServerIcons().get(String.join(" ", args).toLowerCase());
				if (srv != null && srv.isHidingOffline() && srv.getNames().stream().noneMatch(mgr::isCachedOnline)) srv = null;
				if (srv != null) {
					List<String> names = srv.getNames().stream().filter(mgr::isCachedOnline).collect(Collectors.toList());
					this.plugin.getSenderWrapper().connectToServer((U) sender, MiscUtil.random(names.isEmpty() ? srv.getNames() : names));
				} else {
					this.plugin.getSenderWrapper().sendMessage(sender, this.invalidServerMessage);
				}
			}
		}
	}

	@Override
	public @NotNull List<String> onTabComplete(@NotNull T sender, @NotNull String[] args) {
		if (args.length > 0) {
			String arg = String.join(" ", args).toLowerCase();
			ServerPingManager<T, U> mgr = this.plugin.getServerPingManager();
			return this.plugin.getServersConfig().getServerIcons().entrySet().stream()
					.filter(e -> e.getKey().startsWith(arg)).map(Map.Entry::getValue)
					.filter(s -> !s.isHidingOffline() || s.getNames().stream().anyMatch(mgr::isCachedOnline))
					.map(SerializableServerIcon::getFriendlyName).map(s -> this.skipSpaces(s, args.length-1)).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
	
	private String skipSpaces(String text, int spaces) {
		int skipped = 0;
		int index = 0;
		for (; index < text.length() && skipped < spaces; index++) {
			if (text.charAt(index) == ' ') skipped++;
		}
		return text.substring(index);
	}

}
