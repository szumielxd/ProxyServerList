package me.szumielxd.proxyserverlist.bungee.listeners;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.listeners.MotdListener;
import me.szumielxd.proxyserverlist.common.objects.PingResult;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeMotdListener extends MotdListener implements Listener {

	public BungeeMotdListener(@NotNull ProxyServerList<?, ?> plugin) {
		super(plugin);
	}
	
	
	@EventHandler
	public void onPing(ProxyPingEvent event) {
		ServerPing ping = event.getResponse();
		PingResult wrapper = new PingResult(new PingResult.Version(ping.getVersion().getProtocol(), ping.getVersion().getName()),
				Optional.ofNullable(ping.getPlayers()).map(p -> new PingResult.Players(p.getMax(), p.getOnline(), 
						Optional.ofNullable(p.getSample()).map(arr -> Stream.of(arr).map(s -> new PingResult.Players.SamplePlayer(s.getUniqueId(), s.getName())).collect(Collectors.toList())).orElse(Collections.emptyList()))),
				BungeeComponentSerializer.get().deserialize(new BaseComponent[] { ping.getDescriptionComponent() }), -1);
		PingResult result = this.applyMotd(wrapper, event.getConnection().getVersion());
		
		
		
		event.setResponse(new ServerPing(new ServerPing.Protocol(result.getVersion().getName(), result.getVersion().getProtocol()),
				result.getPlayers().map(p -> new ServerPing.Players(p.getMax(), p.getOnline(), p.getPlayers().stream().map(s -> new ServerPing.PlayerInfo(s.getName(), s.getUniqueId())).toArray(ServerPing.PlayerInfo[]::new))).orElse(null),
				new TextComponent(BungeeComponentSerializer.get().serialize(result.getDescription())), ping.getFaviconObject()));
	}
	

}
