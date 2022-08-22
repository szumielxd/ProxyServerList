package me.szumielxd.proxyserverlist.common.objects;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import net.kyori.adventure.text.Component;

@ToString
@AllArgsConstructor
public class PingResult {
	
	private @Getter @Setter @NonNull Version version;
	private @Getter @Setter @NonNull Optional<Players> players;
	private @Getter @Setter @NonNull Component description;
	private @Getter @Setter int ping;
	
	public PingResult copy() {
		return new PingResult(this.version.copy(), this.players.map(Players::copy), this.description, this.ping);
	}
	
	public PingResult unmodifiable() {
		return new UnmodifiablePingResult(this.version.unmodifiable(), this.players.map(Players::unmodifiable), this.description, this.ping);
	}
	
	public static class UnmodifiablePingResult extends PingResult {

		public UnmodifiablePingResult(@NonNull Version version, @NonNull Optional<Players> players, @NonNull Component description, int ping) {
			super(version, players, description, ping);
		}
		@Override
		public void setVersion(@NonNull Version version) {
			throw new UnsupportedOperationException("cannot modify this object");
		}
		@Override
		public void setPlayers(@NonNull Optional<Players> players) {
			throw new UnsupportedOperationException("cannot modify this object");
		}
		@Override
		public void setDescription(@NonNull Component description) {
			throw new UnsupportedOperationException("cannot modify this object");
		}
		@Override
		public void setPing(int ping) {
			throw new UnsupportedOperationException("cannot modify this object");
		}
	}
	
	
	@ToString
	@AllArgsConstructor
	public static class Version {
		
		private @Getter @Setter int protocol;
		private @Getter @Setter @Nullable String name;
		
		public Version copy() {
			return new Version(this.protocol, this.name);
		}
		
		public Version unmodifiable() {
			return new UnmodifiableVersion(this.protocol, this.name);
		}
		
		private static class UnmodifiableVersion extends Version {

			public UnmodifiableVersion(int protocol, @Nullable String name) {
				super(protocol, name);
			}
			@Override
			public void setProtocol(int protocol) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
			@Override
			public void setName(@Nullable String name ) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
		}
		
	}
	
	
	@ToString
	@AllArgsConstructor
	public static class Players {
		
		private @Getter @Setter int max;
		private @Getter @Setter int online;
		private @Getter @Setter List<SamplePlayer> players;
		
		public Players copy() {
			return new Players(this.max, this.online, this.players.stream().map(SamplePlayer::copy).collect(Collectors.toList()));
		}
		
		public Players unmodifiable() {
			return new UnmodifiablePlayers(this.max, this.online, this.players.stream().map(SamplePlayer::unmodifiable).collect(Collectors.toList()));
		}
		
		private static class UnmodifiablePlayers extends Players {

			public UnmodifiablePlayers(int max, int online, List<SamplePlayer> players) {
				super(max, online, players);
			}
			@Override
			public void setMax(int max) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
			@Override
			public void setOnline(int online) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
			@Override
			public void setPlayers(List<SamplePlayer> players) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
		}
		
		
		@ToString
		@AllArgsConstructor
		public static class SamplePlayer {
			
			private @Getter @Setter @Nullable UUID uniqueId;
			private @Getter @Setter @Nullable String name;
			
			public SamplePlayer copy() {
				return new SamplePlayer(this.uniqueId, this.name);
			}
			
			public SamplePlayer unmodifiable() {
				return new UnmodifiableSamplePlayer(this.uniqueId, this.name);
			}
			
			private static class UnmodifiableSamplePlayer extends SamplePlayer {

				public UnmodifiableSamplePlayer(@Nullable UUID uniqueId, @Nullable String name) {
					super(uniqueId, name);
				}
				@Override
				public void setUniqueId(@Nullable UUID uniqueId) {
					throw new UnsupportedOperationException("cannot modify this object");
				}
				@Override
				public void setName(@Nullable String name) {
					throw new UnsupportedOperationException("cannot modify this object");
				}
			}
			
		}
		
	}
	

}
