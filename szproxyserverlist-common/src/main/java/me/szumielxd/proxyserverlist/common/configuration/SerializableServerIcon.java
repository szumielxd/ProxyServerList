package me.szumielxd.proxyserverlist.common.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simpleyaml.configuration.ConfigurationSection;

import dev.simplix.protocolize.data.ItemType;
import lombok.Getter;
import me.szumielxd.proxyserverlist.common.ConditionManager;
import me.szumielxd.proxyserverlist.common.ConditionManager.AbstractCondition;
import me.szumielxd.proxyserverlist.common.utils.CompatibleComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class SerializableServerIcon {
	
	
	private final @Getter @NotNull List<String> names;
	private final @Getter @NotNull String friendlyName;
	private final @Getter @NotNull List<CompatibleComponent> description;
	private final @Getter @NotNull ItemType type;
	private final @Getter @Nullable NamedTextColor accent;
	private final @Getter boolean hidingOffline;
	private final @Getter boolean usePingedPlayers;
	private final @Getter int slot;
	private final @Getter @Nullable AbstractCondition showCondition;
	private final @Getter @Nullable String geyserImage;
	
	
	public SerializableServerIcon(@NotNull List<String> names, @NotNull String friendlyName, @NotNull List<CompatibleComponent> description, @NotNull ItemType type, @Nullable NamedTextColor accent, boolean hidingOffline, boolean usePingedPlayers, int slot, @Nullable AbstractCondition showCondition, @Nullable String geyserImage) {
		this.names = Collections.unmodifiableList(Objects.requireNonNull(names, "names cannot be null"));
		this.friendlyName = Objects.requireNonNull(friendlyName, "friendlyName cannot be null");
		this.description = Collections.unmodifiableList(Objects.requireNonNull(description, "description cannot be null"));
		this.type = Objects.requireNonNull(type, "type cannot be null");
		this.accent = accent;
		this.hidingOffline = hidingOffline;
		this.usePingedPlayers = usePingedPlayers;
		this.slot = slot;
		this.showCondition = showCondition;
		this.geyserImage = geyserImage;
	}
	
	
	public static @NotNull SerializableServerIcon serialize(@NotNull ConfigurationSection section) throws ServerDisplayParseException {
		List<String> name = Optional.ofNullable(section.getStringList("servers")).orElseThrow(() -> new IllegalArgumentException("unknown section `servers`"));
		String friendlyName = Optional.ofNullable(section.getString("server-name")).orElseThrow(() -> new IllegalArgumentException("unknown section `server-name`"));
		List<CompatibleComponent> description = Optional.ofNullable(section.getStringList("description"))
				.orElseThrow(() -> new IllegalArgumentException("unknown section `description`"))
				.stream().map(CompatibleComponent::new).collect(Collectors.toList());
		ItemType type = Optional.ofNullable(section.getString("item")).map(item -> { 
			try {
				return ItemType.valueOf(item.toUpperCase());
			} catch (Exception e) {
				throw new ServerDisplayParseException(item, ServerDisplayParseException.Cause.ITEM_TYPE);
			}
		}).orElseThrow(() -> new IllegalArgumentException("unknown section `item`"));
		NamedTextColor accent = Optional.ofNullable(section.getString("accent")).map(NamedTextColor.NAMES::value).orElse(null);
		boolean hidingOffline = section.getBoolean("hide-offline");
		boolean usePingedPlayers = section.getBoolean("use-pinged-players");
		int slot = Optional.of(section.getString("slot")).map(i -> {
			Integer s = null;
			try {
				s = Integer.parseInt(i);
			} catch (NumberFormatException e) {
				// empty catch
			}
			if (s == null || s < 0 || s >= Config.GUI_COMMAND_ROWS.getInt()*9) throw new ServerDisplayParseException(i, ServerDisplayParseException.Cause.SLOT);
			return s;
		}).orElse(0);
		AbstractCondition showCondition = Optional.ofNullable(section.getString("show-condition"))
				.flatMap(ConditionManager::tryParse).orElse(null);
		String geyserImage = section.getString("geyser-image");
		
		return new SerializableServerIcon(name, friendlyName, description, type, accent, hidingOffline, usePingedPlayers, slot, showCondition, geyserImage);
	}
	
	
	public Map<String, Object> deserialize() {
		Map<String, Object> map = new HashMap<>();
		map.put("servers", this.names);
		map.put("server-name", this.friendlyName);
		map.put("description", this.description.stream().map(Object::toString).toArray());
		map.put("item", this.type.name());
		if (this.accent != null) map.put("accent", this.accent.toString());
		map.put("hide-offline", this.hidingOffline);
		map.put("use-pinged-players", this.usePingedPlayers);
		map.put("slot", this.slot);
		if (this.showCondition != null) map.put("show-condition", ConditionManager.toString(this.showCondition));
		if (this.geyserImage != null) map.put("geyser-image", this.geyserImage);
		return Collections.unmodifiableMap(map);
	}
	
	
	public static class ServerDisplayParseException extends IllegalArgumentException {

		private static final long serialVersionUID = 4661204448620543869L;
		
		private final transient @Getter @Nullable Object value;
		private final transient @Getter @NotNull Cause target;
		
		public ServerDisplayParseException(@Nullable Object value, Cause target) {
			super(String.format(target.getMessage(), value));
			this.value = value;
			this.target = Objects.requireNonNull(target, "`target` cannot be null");
		}
		
		
		public enum Cause {
			
			ITEM_TYPE("`%s` is not valid item-type"),
			SERVER("`%s` is not valid server"),
			EMPTY_SERVERS("icon must be binded to one server at least"),
			SLOT("`%s` slot is outside inventory")
			;
			
			private final @Getter String message;
			
			Cause(String display) {
				this.message = display;
			}
		}
		
		
	}
	

}
