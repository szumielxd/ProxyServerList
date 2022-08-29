package me.szumielxd.proxyserverlist.common.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerIcon;
import me.szumielxd.proxyserverlist.common.managers.ServerPingManager;
import me.szumielxd.proxyserverlist.common.objects.CachedServerInfo;
import me.szumielxd.proxyserverlist.common.objects.CommonScheduler.ExecutedTask;
import me.szumielxd.proxyserverlist.common.objects.SenderWrapper;
import me.szumielxd.proxyserverlist.common.utils.CompatibleComponent;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ServersGUI<T, U extends T> {
	
	
	private static final Pattern BACKGROUND_PATTERN = Pattern.compile("([a-zA-Z_]+)\\[(\\d+(-\\d+|(,\\d+){0,100}))\\]");
	
	
	private final @NotNull ProxyServerList<T, U> plugin;
	private final @NotNull Optional<GeyserServerForm<T, U>> geyserForm;
	private final @NotNull Map<Integer, ItemStack> background = new HashMap<>();
	private final @NotNull CompatibleComponent title;
	private final @NotNull List<CompatibleComponent> format;
	private final @NotNull ConcurrentMap<UUID, Inventory> openedGUIs = new ConcurrentHashMap<>();
	private @Nullable ExecutedTask updateTask = null;
	
	
	public ServersGUI(@NotNull ProxyServerList<T, U> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		
		Optional<GeyserServerForm<T, U>> form;
		try {
			Class.forName("org.geysermc.geyser.GeyserImpl");
			form = Optional.of(new GeyserServerForm<>(plugin));
		} catch (ClassNotFoundException e) {
			form = Optional.empty();
		}
		this.geyserForm = form;
		
		String[] backgroundTexts = Config.GUI_COMMAND_BACKGROUND.getString().split("\\|");
		Stream.of(backgroundTexts).forEach(str -> {
			Matcher match = BACKGROUND_PATTERN.matcher(str);
			if (match.matches()) {
				ItemType type = ItemType.valueOf(match.group(1));
				if (type != null) {
					String slots = match.group(2);
					int index = slots.indexOf('-');
					(index > -1 ? IntStream.rangeClosed(Integer.parseInt(slots.substring(0, index)), Integer.parseInt(slots.substring(index+1, slots.length())))
							: Stream.of(slots.split(",")).mapToInt(Integer::parseInt)
					).filter(i -> Config.GUI_COMMAND_ROWS.getInt()*9 > i).forEach(i -> {
						ItemStack item = new ItemStack(type);
						item.displayName("");
						this.background.put(i, item);
					});
				}
			}
		});
		
		this.format = Config.GUI_COMMAND_FORMAT.getStringList().stream().map(CompatibleComponent::new).collect(Collectors.toList());
		this.title = new CompatibleComponent(Config.GUI_COMMAND_TITLE.getString());
		
	}
	
	
	public ServersGUI<T, U> start() {
		if (Config.GUI_REFRESH_TIME.getInt() > 0)
			this.updateTask = this.plugin.getScheduler().runTaskTimer(this::update, 1L, Config.GUI_REFRESH_TIME.getInt(), TimeUnit.SECONDS);
		return this;
	}
	
	public boolean stop() {
		if (this.updateTask == null) return false;
		this.updateTask.cancel();
		this.openedGUIs.keySet().parallelStream().map(Protocolize.playerProvider()::player)
				.filter(Objects::nonNull).forEach(ProtocolizePlayer::closeInventory);
		this.openedGUIs.clear();
		return true;
	}
	
	
	public void open(@NotNull UUID playerUniqueId) {
		
		if (this.geyserForm.filter(form -> form.open(playerUniqueId)).isPresent()) return;
		
		
		ProtocolizePlayer player = Protocolize.playerProvider().player(playerUniqueId);
		if (player != null) {
			SenderWrapper<T, U> swrapper = this.plugin.getSenderWrapper();
			Inventory inv = new Inventory(InventoryType.chestInventoryWithRows(Config.GUI_COMMAND_ROWS.getInt()));
			inv.title(swrapper.componentToBase(this.title.get(player.protocolVersion())));
			this.setupIcons(inv, player);
			inv.onClose(close -> this.openedGUIs.remove(close.player().uniqueId(), inv));
			inv.onClick(click -> {
				if (click.clickedItem() instanceof ServerItemStack) {
					ServerItemStack srvItem = (ServerItemStack) click.clickedItem();
					switch (click.clickType()) {
					case RIGHT_CLICK:
					case LEFT_CLICK:
						Optional<U> proxyPlayer = this.plugin.getSenderWrapper().getPlayer(click.player().uniqueId());
						if (proxyPlayer.isPresent()) {
							this.openedGUIs.remove(click.player().uniqueId(), inv);
							click.player().closeInventory();
							swrapper.connectToServer(proxyPlayer.get(), MiscUtil.random(srvItem.getServerNames()));
						}
						break;
					default:
						break;
					}
				}
				click.cancelled(true);
			});
			player.openInventory(inv);
			openedGUIs.put(playerUniqueId, inv);
		}
		
	}
	
	
	private Inventory setupIcons(@NotNull Inventory inv, @NotNull ProtocolizePlayer player) {
		// background
		this.background.forEach(inv::item);
		
		// icons
		this.getAvailableServerIcons(player.uniqueId(), player.protocolVersion())
				.forEach(icon -> inv.item(icon.getSlot(), new ServerItemStack(player.protocolVersion(), format, icon, this.plugin.getSenderWrapper(), (short) 0)));
		return inv;
	}
	
	
	public Collection<SerializableServerIcon> getAvailableServerIcons(UUID playerId, int protocol) {
		// icons
		ServerPingManager<T, U> ping = this.plugin.getServerPingManager();
		return this.plugin.getServersConfig().getServerIcons().values().stream()
				.filter(i -> !i.isHidingOffline() || i.getNames().stream().anyMatch(ping::isCachedOnline)).filter(i -> this.canSee(i, playerId, protocol))
				.collect(Collectors.toList());
	}
	
	
	public boolean canSee(SerializableServerIcon icon, UUID playerId, int protocol) {
		if (icon.getShowCondition() == null) return true;
		ServerPingManager<T, U> pingManager = this.plugin.getServerPingManager();
		SenderWrapper<T, U> senderWrapper = this.plugin.getSenderWrapper();
		List<CachedServerInfo> pings = icon.getNames().parallelStream()
				.map(pingManager::getCachedServer)
				.filter(Optional::isPresent).map(Optional::get)
				.collect(Collectors.toList());
		int amount = icon.isUsePingedPlayers() ? pings.stream().mapToInt(CachedServerInfo::getOnline).sum()
				: icon.getNames().parallelStream().map(senderWrapper::getPlayers).filter(Optional::isPresent).map(Optional::get).mapToInt(Collection::size).sum();
		int maxOnline = pings.stream().mapToInt(CachedServerInfo::getMaxOnline).sum();
		int ping = pings.stream().mapToInt(CachedServerInfo::getPing).filter(i -> i > -1).max().orElse(-1);
		String versions = pings.stream().map(CachedServerInfo::getVersionFriendlyName).distinct().collect(Collectors.joining(", "));
		return pings.stream().map(info -> ((UnaryOperator<String>) str -> str.replace("{online}", Integer.toString(amount))
					.replace("{maxonline}", Integer.toString(maxOnline))
					.replace("{ping}", Integer.toString(ping))
					.replace("{playerid}", playerId.toString())
					.replace("{playerversion}", Integer.toString(protocol))
					.replace("{version}", versions).replace("{motd}", Optional.ofNullable(LegacyComponentSerializer.legacySection().serializeOrNull(info.getDescription())).orElse(""))
		)).anyMatch(icon.getShowCondition());
	}
	
	
	public void update() {
		this.openedGUIs.entrySet().parallelStream().forEach(entry -> {
			ProtocolizePlayer pl = Protocolize.playerProvider().player(entry.getKey());
			if (pl != null) pl.openInventory(this.setupIcons(entry.getValue(), pl));
		});
	}
	

}
