package me.szumielxd.proxyserverlist.common;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProxyServerListProvider {
	
	
	private static @Nullable ProxyServerList<?, ?> instance = null;
	
	
	public static void init(@NotNull ProxyServerList<?, ?> instance) {
		ProxyServerListProvider.instance = Objects.requireNonNull(instance, "instance cannot be null");
	}
	
	
	public static @NotNull ProxyServerList<?, ?> get() {
		if (instance == null) throw new IllegalArgumentException("ProxyAnnouncements is not initialized");
		return instance;
	}
	

}
