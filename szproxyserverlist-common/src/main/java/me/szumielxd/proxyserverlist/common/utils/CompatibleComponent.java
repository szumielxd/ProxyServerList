package me.szumielxd.proxyserverlist.common.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CompatibleComponent {
	
	private static final Pattern SEPARATOR = Pattern.compile("(?<!\\\\)(\\\\\\\\){0,}\\|");
	private static final Pattern ESCAPE_REVERTER = Pattern.compile("\\\\([\\\\\\|])");
	
	
	private final @NotNull Component legacy;
	private final @NotNull Component text;
	
	public CompatibleComponent(@NotNull String text) {
		CompatibleRawComponent raw = new CompatibleRawComponent(text);
		this.text = MiscUtil.parseComponent(raw.getNew(), false, false);
		this.legacy = MiscUtil.parseComponent(raw.getLegacy(), true, false);
	}
	
	public Component get(int protocolVersion) {
		if (protocolVersion >= 735) return this.text; // >= 1.16
		return this.legacy; // < 1.16
	}
	
	public Component getLegacy() {
		return this.legacy;
	}
	
	public Component getNew() {
		return this.text;
	}
	
	@Override
	public @NotNull String toString() {
		String result = MiniMessage.get().serialize(this.text).replace("\\", "\\\\").replace("|", "\\|");
		String old = MiniMessage.get().serialize(this.legacy).replace("\\", "\\\\").replace("|", "\\|");
		if (!result.equals(old)) result += "|" + old;
		return result;
	}
	
	
	public static class CompatibleRawComponent {
		
		private final @NotNull String legacy;
		private final @NotNull String text;
		
		public CompatibleRawComponent(@NotNull String text) {
			Objects.requireNonNull(text, "text cannot be null");
			Matcher match = SEPARATOR.matcher(text);
			String newStr;
			String oldStr;
			if (match.find()) {
				newStr = text.substring(0, match.start()) + Optional.ofNullable(match.group(1)).orElse("");
				oldStr = text.substring(match.end(), text.length());
			} else {
				oldStr = newStr = text;
			}
			this.text = ESCAPE_REVERTER.matcher(newStr).replaceAll("$1");
			this.legacy = ESCAPE_REVERTER.matcher(oldStr).replaceAll("$1");
		}
		
		public String get(int protocolVersion) {
			if (protocolVersion >= 735) return this.text; // >= 1.16
			return this.legacy; // < 1.16
		}
		
		public String getLegacy() {
			return this.legacy;
		}
		
		public String getNew() {
			return this.text;
		}
		
	}
	
	
	

}
