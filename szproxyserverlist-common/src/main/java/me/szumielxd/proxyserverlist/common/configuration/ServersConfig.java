package me.szumielxd.proxyserverlist.common.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.YamlConfiguration;

import dev.simplix.protocolize.data.ItemType;
import lombok.Getter;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerIcon.ServerDisplayParseException;
import me.szumielxd.proxyserverlist.common.utils.CompatibleComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class ServersConfig {
	
	
	private final @NotNull File file;
	private @Getter @NotNull Map<String, SerializableServerIcon> serverIcons = Collections.emptyMap();
	
	
	public ServersConfig(@NotNull File file) {
		this.file = Objects.requireNonNull(file, "file cannot be null");
	}
	
	
	public <T, U extends T> ServersConfig load(@NotNull ProxyServerList<T, U> plugin) {
		Objects.requireNonNull(plugin, "plugin cannot be null");
		if (this.file.getParentFile().exists() || this.file.getParentFile().mkdirs()) {
			try {
				if (this.file.exists() || this.generateConfig()) {
					YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
					Map<String, SerializableServerIcon> icons = new HashMap<>();
					config.getKeys(false).stream().filter(config::isConfigurationSection).map(config::getConfigurationSection).forEach(sec -> {
						try {
							SerializableServerIcon icon = SerializableServerIcon.serialize(sec);
							boolean empty = true;
							if (!icon.getNames().isEmpty()) for (String srv : icon.getNames()) {
								if (!plugin.getSenderWrapper().getPlayers(srv).isPresent())
									plugin.getLogger().warning(String.format("Warning while parsing section `%s` in file `%s`: %s", sec.getName(), file.getName(), String.format(ServerDisplayParseException.Cause.SERVER.getMessage(), srv)));
								else
									empty = false;
							}
							if (empty) throw new ServerDisplayParseException(null, ServerDisplayParseException.Cause.EMPTY_SERVERS);
							icons.put(icon.getFriendlyName().toLowerCase(), icon);
						} catch (ServerDisplayParseException e) {
							plugin.getLogger().warning(String.format("Error while parsing section `%s` in file `%s`: %s", sec.getName(), file.getName(), e.getMessage()));
						}
					});
					this.serverIcons = Collections.unmodifiableMap(icons);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		return this;
	}
	
	
	private boolean generateConfig() throws IOException {
		if (!this.file.createNewFile()) return false;
		YamlConfiguration config = new YamlConfiguration();
		config.set("creative", new SerializableServerIcon(Arrays.asList("creative1", "creative2"), "Creative", Arrays.asList(new CompatibleComponent("Build!")), ItemType.WOODEN_AXE, NamedTextColor.LIGHT_PURPLE, false, false, 4, null, null).deserialize());
		config.save(this.file);
		return true;
	}
	
	
	
	

}
