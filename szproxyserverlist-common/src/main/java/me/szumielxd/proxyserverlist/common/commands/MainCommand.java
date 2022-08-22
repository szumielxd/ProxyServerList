package me.szumielxd.proxyserverlist.common.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.objects.SenderWrapper;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MainCommand<T> extends CommonCommand<T> {
	
	private final @NotNull ProxyServerList<T, ?> plugin;

	public MainCommand(@NotNull ProxyServerList<T, ?> plugin) {
		super(Config.COMMAND_NAME.getString(), "proxyserverlist.command.main", Config.COMMAND_ALIASES.getStringList().toArray(new String[0]));
		this.plugin = plugin;
	}

	@Override
	public void execute(@NotNull T sender, String[] args) {
		SenderWrapper<T, ?> swrapper = this.plugin.getSenderWrapper();
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
				if (swrapper.hasPermission(sender, "proxyserverlist.command.reload")) {
					boolean failed = false;
					UnaryOperator<String> replacer = (str) -> {
						return str.replace("{plugin}", this.plugin.getName()).replace("{version}", this.plugin.getVersion());
					};
					swrapper.sendMessage(sender, LegacyComponentSerializer.legacySection().deserialize(replacer.apply(Config.PREFIX.getString()+Config.COMMAND_SUB_RELOAD_EXECUTE.getString())));
					try {
						this.plugin.onDisable();
					} catch (Exception e) {
						e.printStackTrace();
						failed = true;
					}
					try {
						this.plugin.onEnable();
					} catch (Exception e) {
						e.printStackTrace();
						failed = true;
					}
					if (failed) swrapper.sendMessage(sender, LegacyComponentSerializer.legacySection().deserialize(replacer.apply(Config.PREFIX.getString()+Config.COMMAND_SUB_RELOAD_ERROR.getString())));
					else swrapper.sendMessage(sender, LegacyComponentSerializer.legacySection().deserialize(replacer.apply(Config.PREFIX.getString()+Config.COMMAND_SUB_RELOAD_SUCCESS.getString())));
				} else {
					swrapper.sendMessage(sender, LegacyComponentSerializer.legacySection().deserialize(Config.PREFIX.getString()+Config.MESSAGES_PERM_ERROR.getString()));
				}
				return;
			}
		}
		swrapper.sendMessage(sender, LegacyComponentSerializer.legacySection().deserialize(Config.PREFIX.getString()+"/"+Config.COMMAND_NAME.getString()+" reload|rl"));
	}

	@Override
	public @NotNull List<String> onTabComplete(@NotNull T sender, @NotNull String[] args) {
		SenderWrapper<T, ?> swrapper = this.plugin.getSenderWrapper();
		ArrayList<String> list = new ArrayList<>();
		if (swrapper.hasPermission(sender, "proxyserverlist.command.main")) {
			if (args.length == 1) {
				if ("reload".startsWith(args[0].toLowerCase()) && swrapper.hasPermission(sender, "proxyserverlist.command.main.reload")) list.add("reload");
				if ("rl".startsWith(args[0].toLowerCase()) && swrapper.hasPermission(sender, "proxyserverlist.command.main.reload")) list.add("rl");
				return list;
			}
		}
		return list;
	}

}
