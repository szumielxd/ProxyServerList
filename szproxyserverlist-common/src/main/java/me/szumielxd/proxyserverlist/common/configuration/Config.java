package me.szumielxd.proxyserverlist.common.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.Configuration;
import org.simpleyaml.configuration.MemorySection;
import org.simpleyaml.configuration.file.YamlConfiguration;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;

public enum Config {
	
	PREFIX("common.prefix", "&5&lPde&lS&b&lL&r &8&l»&r &7", true),
	DEBUG("common.debug", false),
	MESSAGES_PERM_ERROR("message.perm-error", "&cNo, you can't", true),
	MESSAGES_COMMAND_ERROR("message.command-error", "&4An error occured while attempting to perform this command. Please report this to admin.", true),
	MESSAGES_CONSOLE_ERROR("message.console-error", "&cNot for console ;c", true),
	MESSAGES_INVALID_SERVER("message.invalid-server", "&cInvalid server", true),
	COMMAND_NAME("command.name", "proxyserverlist"),
	COMMAND_ALIASES("command.aliases", Arrays.asList("proxysrvlist", "psl")),
	COMMAND_SUB_RELOAD_EXECUTE("command.sub.reload.execute", "Reloading...", true),
	COMMAND_SUB_RELOAD_ERROR("command.sub.reload.error", "&cAn error occured while reloading plugin. See console for more info.", true),
	COMMAND_SUB_RELOAD_SUCCESS("command.sub.reload.success", "&aSuccessfully reloaded {plugin} v{version}", true),
	GUI_REFRESH_TIME("gui.refresh-time", 5),
	GUI_COMMAND_NAME("gui.command.name", "servers"),
	GUI_COMMAND_ALIASES("gui.command.aliases", Arrays.asList("servergui", "hub")),
	GUI_COMMAND_ROWS("gui.rows", 6),
	GUI_COMMAND_PLAYERSASAMOUNT("gui.players-as-amount", true),
	GUI_COMMAND_TITLE("gui.title", "&5&lAvailable Servers"),
	GUI_COMMAND_BACKGROUND("gui.background", "BLACK_STAINED_GLASS_PANE[0-54]|RED_STAINED_GLASS_PANE[4,13,22,31,40,49]"),
	GUI_COMMAND_FORMAT("gui.format", Arrays.asList("&6&m---&k[]&6&m-------------------------&k[]&6&m---", "&8» &7Server: {accent}{name}", "&8» &7Base version: {accent}{version}", "&8» &7Online: {accent}{online}", "&8» &7Ping: {accent}{ping}", "", "&8» &7Description:", "  {accent}{description}", "&6&m---&k[]&6&m-------------------------&k[]&6&m---")),
	SERVERSTATUS_REFRESH_TIME("server-status.refresh-time", 5),
	SERVERSTATUS_NOTIFY_AFTER("server-status.notify-after", 90),
	SERVERSTATUS_NOTIFY_INTERVAL("server-status.notify-interval", 180),
	SERVERSTATUS_NOTIFY_MESSAGE("server-status.notify-message", "&4&l[&c&l&k$&r&4&l]&r &4Server &b{name} &4is offline since &b{time}&4."),
	MOTD_CACHE("motd.cache", 30),
	MOTD_PLAYERS_MODIFY("motd.players.modify", true),
	MOTD_PLAYERS_SHOWMOREMAX("motd.players.show-more-max", 1),
	MOTD_PLAYERS_DISPLAY("motd.players.display", Arrays.asList("&a", "  &7Version: &a{gversion}", "  &7Facebook: &9fb.example.com", "  &7Forum: &bforum.example.com", "  &7TeamSpeak: &cts.example.com", "  &7Discord: &4dc.example.com", "  &7Shop: &dshop.example.com", "&a", "  &7Available servers:", "    &8- {accent}{servers} &8[&6{online}/{maxonline}&8] (&7{version}&8)", "&a", "  &7Catch 'em all!", "&a")),
	MOTD_SERVERS_HIDELISTED("motd.servers-hide-listed", true),
	MOTD_SERVERS_ORDER("motd.servers-order", Arrays.asList("NATURAL", "ALPHABETICALLY", "ALPHABETICALLY_IGNORECASE", "ALPHABETICALLY_REVERSE", "ALPHABETICALLY_IGNORECASE_REVERSE", "ONLINE", "ONLINE_REVERSE")),
	MOTD_SERVERS("motd.servers", Arrays.asList("hidden-server", "another_hidden_server")),
	MOTD_VERSION_DISPLAY("motd.versions.display", Arrays.asList("&b{gversion} &8»&r                                                             &7Players: &6{gonline}")),
	MOTD_DISPLAY("motd.display", Arrays.asList("&8[ &4* &8] &l«&7&m-----&r &8[ <gradient:#AA00AA:#55FFFF>&lExample.com</gradient> <gradient:#FF55FF:#FF0000>{gversion}</gradient> &8] &7&m-----&r&8&l» &8[ &4* &8]\\n&8» &6&lPvP &8- &eFight for The Queen!", "&8[ &4* &8] &l«&7&m-----&r &8[ <gradient:#AA00AA:#55FFFF>&lExample.com</gradient> <gradient:#FF55FF:#FF0000>{gversion}</gradient> &8] &7&m-----&r&8&l» &8[ &4* &8]\\n&8» <#FF69B4|&d>Supports</#FFB6C1> &4&lR&a&lG&1&lB")),
	WEB_PORT("web.port", 4420),
	;
	
	
	
	//////////////////////////////////////////////////////////////////////
	
	private final String path;
	private List<String> texts;
	private String text;
	private int number;
	private boolean bool;
	private Map<String, Object> map;
	private boolean colored = false;
	private Class<?> type;
	
	
	private Config(String path, String text) {
		this(path, text, false);
	}
	private Config(String path, String text, boolean colored) {
		this.path = path;
		this.colored = colored;
		setValue(text);
	}
	private Config(String path, List<String> texts) {
		this(path, texts, false);
	}
	private Config(String path, List<String> texts, boolean colored) {
		this.path = path;
		this.colored = colored;
		setValue(texts);
	}
	private Config(String path, int number) {
		this.path = path;
		setValue(number);
	}
	private Config(String path, boolean bool) {
		this.path = path;
		setValue(bool);
	}
	private Config(String path, Map<String, Object> valueMap) {
		this.path = path;
		setValue(valueMap);
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	
	private void setValue(String text) {
		this.type = String.class;
		this.text = text;
		this.texts = new ArrayList<>(Arrays.asList(this.text));
		this.number = text.length();
		this.bool = !text.isEmpty();
		this.map = new HashMap<>();
	}
	private void setValue(List<String> texts) {
		this.type = String[].class;
		this.text = String.join(", ", texts);
		this.texts = texts;
		this.number = texts.size();
		this.bool = !texts.isEmpty();
		AtomicInteger index = new AtomicInteger(0);
		this.map = texts.stream().collect(Collectors.toMap(v -> String.valueOf(index.getAndAdd(1)), v -> v));
	}
	private void setValue(int number) {
		this.type = Integer.class;
		this.text = Integer.toString(number);
		this.texts = new ArrayList<>(Arrays.asList(this.text));
		this.number = number;
		this.bool = number > 0;
		this.map = new HashMap<>();
	}
	private void setValue(boolean bool) {
		this.type = Boolean.class;
		this.text = Boolean.toString(bool);
		this.texts = new ArrayList<>(Arrays.asList(this.text));
		this.number = bool? 1 : 0;
		this.bool = bool;
		this.map = new HashMap<>();
	}
	private void setValue(Map<String, Object> valueMap) {
		this.type = Map.class;
		this.text = valueMap.toString();
		this.texts = valueMap.values().stream().map(Object::toString).collect(Collectors.toList());
		this.number = valueMap.size();
		this.bool = !valueMap.isEmpty();
		this.map = valueMap;
	}
	
	
	public String getString() {
		return this.text;
	}
	@Override
	public String toString() {
		return this.text;
	}
	public List<String> getStringList() {
		return new ArrayList<>(this.texts);
	}
	public int getInt() {
		return this.number;
	}
	public boolean getBoolean() {
		return this.bool;
	}
	public Map<String, Object> getValueMap() {
		return this.map;
	}
	public boolean isColored() {
		return this.colored;
	}
	public Class<?> getType() {
		return this.type;
	}
	public String getPath() {
		return this.path;
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	
	public static void load(@NotNull File file, @NotNull ProxyServerList<?, ?> plugin) {
		Objects.requireNonNull(plugin, "plugin cannot be null").getLogger().info("Loading configuration from '" + Objects.requireNonNull(file, "file cannot be null").getName() + "'");
		if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
		try {
			if(!file.exists()) file.createNewFile();
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			if(loadConfig(config) > 0) config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int loadConfig(Configuration config) {
		int modify = 0;
		for (Config val : Config.values()) {
			if(!config.contains(val.getPath())) modify++;
			if (val.getType().equals(String.class)) {
				if (val.isColored())val.setValue(getColoredStringOrSetDefault(config, val.getPath(), val.getString()));
				else val.setValue(getStringOrSetDefault(config, val.getPath(), val.getString()));
			} else if (val.getType().equals(String[].class)) {
				if (val.isColored())val.setValue(getColoredStringListOrSetDefault(config, val.getPath(), val.getStringList()));
				else val.setValue(getStringListOrSetDefault(config, val.getPath(), val.getStringList()));
			} else if (val.getType().equals(Integer.class)) val.setValue(getIntOrSetDefault(config, val.getPath(), val.getInt()));
			else if (val.getType().equals(Boolean.class)) val.setValue(getBooleanOrSetDefault(config, val.getPath(), val.getBoolean()));
			else if (val.getType().equals(Map.class)) val.setValue(getMapOrSetDefault(config, val.getPath(), val.getValueMap()));
		}
		return modify;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> Map<String,T> getMapOrSetDefault(Configuration config, String path, Map<String,T> def) {
		if (config.contains(path)) {
			return (Map<String, T>) ((MemorySection) config.getConfigurationSection(path)).getMapValues(false);
		}
		config.set(path, def);
		return def;
	}
	
	private static int getIntOrSetDefault(Configuration config, String path, int def) {
		if (config.contains(path)) return config.getInt(path);
		config.set(path, def);
		return def;
	}
	
	private static boolean getBooleanOrSetDefault(Configuration config, String path, boolean def) {
		if (config.contains(path)) return config.getBoolean(path);
		config.set(path, def);
		return def;
	}
	
	private static String getStringOrSetDefault(Configuration config, String path, String def) {
		if (config.contains(path)) return config.getString(path);
		config.set(path, def);
		return def;
	}
	
	private static String getColoredStringOrSetDefault(Configuration config, String path, String def) {
		return MiscUtil.translateAlternateColorCodes('&', getStringOrSetDefault(config, path, def.replace('§', '&')));
	}
	
	private static ArrayList<String> getStringListOrSetDefault(Configuration config, String path, List<String> def) {
		if(config.contains(path)) return new ArrayList<>(config.getStringList(path));
		config.set(path, def);
		return new ArrayList<>(def);
	}
	
	private static ArrayList<String> getColoredStringListOrSetDefault(Configuration config, String path, List<String> def) {
		ArrayList<String> list = getStringListOrSetDefault(config, path, def.stream().map(str -> str.replace('§', '&')).collect(Collectors.toCollection(ArrayList::new)));
		return list.stream().map((str) -> MiscUtil.translateAlternateColorCodes('&', str))
				.collect(Collectors.toCollection(ArrayList::new));
	}

}
