package me.szumielxd.proxyserverlist.common.objects.enums;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import lombok.AllArgsConstructor;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerIcon;

@AllArgsConstructor
public enum ServerSortType implements Comparator<Entry<SerializableServerIcon, Integer>> {
	
	
	NATURAL((a, b) -> 0),
	ALPHABETICALLY((a, b) -> a.getKey().getFriendlyName().compareTo(b.getKey().getFriendlyName())),
	ALPHABETICALLY_REVERSE(ALPHABETICALLY.comparator.reversed()),
	ALPHABETICALLY_IGNORECASE((a, b) -> a.getKey().getFriendlyName().compareToIgnoreCase(b.getKey().getFriendlyName())),
	ALPHABETICALLY_IGNORECASE_REVERSE(ALPHABETICALLY_IGNORECASE.comparator.reversed()),
	ONLINE((a, b) -> Integer.compare(a.getValue(), b.getValue())),
	ONLINE_REVERSE(ONLINE.comparator.reversed()),
	;
	
	
	private final @NotNull Comparator<Entry<SerializableServerIcon, Integer>> comparator;

	@Override
	public int compare(Entry<SerializableServerIcon, Integer> o1, Entry<SerializableServerIcon, Integer> o2) {
		return this.comparator.compare(o1, o2);
	}
	
	public static Optional<ServerSortType> tryParse(@NotNull String name) {
		return Stream.of(ServerSortType.values()).filter(t -> t.name().equalsIgnoreCase(name)).findAny();
	}
	

}
