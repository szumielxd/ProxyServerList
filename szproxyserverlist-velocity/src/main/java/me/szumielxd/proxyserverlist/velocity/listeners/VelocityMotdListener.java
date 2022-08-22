package me.szumielxd.proxyserverlist.velocity.listeners;

import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;

import me.szumielxd.proxyserverlist.common.listeners.MotdListener;
import me.szumielxd.proxyserverlist.common.objects.PingResult;
import me.szumielxd.proxyserverlist.velocity.ProxyServerListVelocity;

public class VelocityMotdListener extends MotdListener {

	public VelocityMotdListener(@NotNull ProxyServerListVelocity plugin) {
		super(plugin);
	}
	
	
	@Subscribe
	public void onPing(ProxyPingEvent event) {
		
		ServerPing ping = event.getPing();
		PingResult wrapper = new PingResult(new PingResult.Version(ping.getVersion().getProtocol(), ping.getVersion().getName()),
				ping.getPlayers().map(p -> new PingResult.Players(p.getOnline(), p.getMax(), p.getSample().stream()
						.map(s -> new PingResult.Players.SamplePlayer(s.getId(), s.getName())).collect(Collectors.toList()))),
				ping.getDescriptionComponent(), -1);
		PingResult result = this.applyMotd(wrapper, event.getConnection().getProtocolVersion().getProtocol());
		
		event.setPing(new ServerPing(new ServerPing.Version(result.getVersion().getProtocol(), result.getVersion().getName()),
				result.getPlayers().map(p -> new ServerPing.Players(p.getOnline(), p.getMax(), p.getPlayers().stream().map(s -> new ServerPing.SamplePlayer(s.getName(), s.getUniqueId())).collect(Collectors.toList()))).orElse(null),
				result.getDescription(), ping.getFavicon().orElse(null), ping.getModinfo().orElse(null)));
		
	}
	

}
