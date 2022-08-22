package me.szumielxd.proxyserverlist.common.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

import dev.simplix.protocolize.api.item.ItemStack;
import lombok.Getter;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerIcon;
import me.szumielxd.proxyserverlist.common.objects.CachedServerInfo;
import me.szumielxd.proxyserverlist.common.objects.SenderWrapper;
import me.szumielxd.proxyserverlist.common.utils.CompatibleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ServerItemStack extends ItemStack {

	private final @Getter @NotNull List<String> serverNames;
	
	public ServerItemStack(int protocolVersion, @NotNull List<CompatibleComponent> format, @NotNull SerializableServerIcon icon, @NotNull SenderWrapper<?, ?> senderWrapper, short durability) {
		super(Objects.requireNonNull(icon, "icon cannot be null").getType(), 1, durability);
		Objects.requireNonNull(format, "format cannot be null");
		Objects.requireNonNull(senderWrapper, "senderWrapper cannot be null");
		this.serverNames = icon.getNames();
		List<Component> formatComp = new ArrayList<>();
		Objects.requireNonNull(format, "format cannot be null").stream().map(c -> c.get(protocolVersion))
				.map(c -> c.replaceText(b -> b.match("\\{accent\\}(.*)").replacement((match, i) -> Component.text(match.group(1), icon.getAccent())))
						.replaceText(b -> b.matchLiteral("{name}").replacement(icon.getFriendlyName())))
				.forEachOrdered(line -> {
					AtomicBoolean matched = new AtomicBoolean(false);
					// very stupid containment check
					line.replaceText(builder -> builder.matchLiteral("{description}").replacement(b -> {
						matched.set(true);
							return Component.empty();
					}));
					if (matched.get()) {
						// append description
						icon.getDescription().forEach(desc -> formatComp.add(line.replaceText(builder -> builder.matchLiteral("{description}").replacement(desc.get(protocolVersion)))));
					} else {
						// append format line
						formatComp.add(line);
					}
				});
		
		List<CachedServerInfo> pings = this.serverNames.stream().map(senderWrapper.getPlugin().getServerPingManager()::getCachedServer)
				.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
		int amount = icon.isUsePingedPlayers() ? pings.stream().mapToInt(CachedServerInfo::getOnline).sum()
				: this.serverNames.parallelStream().map(senderWrapper::getPlayers).filter(Optional::isPresent).map(Optional::get).mapToInt(Collection::size).sum();
		Component online = Component.text(amount);
		Component maxOnline = Component.text(pings.stream().mapToInt(CachedServerInfo::getMaxOnline).sum());
		String versions = pings.stream().map(CachedServerInfo::getVersionFriendlyName).distinct().collect(Collectors.joining(", "));
		int ping = pings.stream().mapToInt(CachedServerInfo::getPing).filter(i -> i > -1).max().orElse(-1);
		List<Object> description = formatComp.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).map(line -> parse(line, online, maxOnline, versions, ping)).map(senderWrapper::componentToBase).collect(Collectors.toList());
		if (description.isEmpty()) {
			this.displayName("");
		} else {
			this.displayName(description.get(0));
			this.lore(description.subList(1, description.size()), false);
		}
		
		if (Config.GUI_COMMAND_PLAYERSASAMOUNT.getBoolean()) {
			this.amount((byte) Math.max(1, amount));
		}
		
	}
	
	
	private Component parse(Component comp, Component online, Component maxOnline, @NotNull String version, int ping) {
		Component pingComp = ping > -1 ? Component.text(ping + "ms") : Component.text("Offline", NamedTextColor.RED);
		return comp.replaceText(b -> b.matchLiteral("{online}").replacement(online))
				.replaceText(b -> b.matchLiteral("{maxonline}").replacement(maxOnline))
				.replaceText(b -> b.matchLiteral("{ping}").replacement(pingComp))
				.replaceText(b -> b.matchLiteral("{version}").replacement(version));
	}

}
