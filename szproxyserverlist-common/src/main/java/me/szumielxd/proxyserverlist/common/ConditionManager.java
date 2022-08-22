package me.szumielxd.proxyserverlist.common;

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConditionManager {
	
	
	private static final Map<String, Class<? extends AbstractCondition>> CONDITIONS = Collections.unmodifiableMap(Stream.of(
				new AbstractMap.SimpleEntry<>(">", BiggerCondition.class),
				new AbstractMap.SimpleEntry<>(">=", BiggerEqualCondition.class),
				new AbstractMap.SimpleEntry<>("<", SmallerCondition.class),
				new AbstractMap.SimpleEntry<>("<=", SmallerEqualCondition.class),
				new AbstractMap.SimpleEntry<>("==", EqualCondition.class),
				new AbstractMap.SimpleEntry<>("!=", NotEqualCondition.class),
				new AbstractMap.SimpleEntry<>("===", EqualIgnoreCaseCondition.class),
				new AbstractMap.SimpleEntry<>("!==", NotEqualIgnoreCaseCondition.class),
				new AbstractMap.SimpleEntry<>("~", ContainsCondition.class),
				new AbstractMap.SimpleEntry<>("!~", NotContainsCondition.class)
		).collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	
	private static final Pattern CONDITION_PATTERN = Pattern.compile("(.+?|\".+?\") ?(" + CONDITIONS.keySet().stream().map(Pattern::quote).collect(Collectors.joining("|")) + ") ?(.+|\".+\")");
	
	
	public static Optional<AbstractCondition> tryParse(String condition) {
		Matcher match = CONDITION_PATTERN.matcher(condition);
		if (match.matches()) {
			String prefix = match.group(1);
			if (prefix.startsWith("\"") && prefix.endsWith("\"")) prefix = prefix.substring(1, prefix.length()-1);
			String suffix = match.group(3);
			if (suffix.startsWith("\"") && suffix.endsWith("\"")) suffix = suffix.substring(1, suffix.length()-1);
			try {
				return Optional.of(CONDITIONS.get(match.group(2)).getConstructor(String.class, String.class).newInstance(prefix, suffix));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		} return Optional.empty();
	}
	
	
	public static String toString(AbstractCondition condition) {
		return CONDITIONS.entrySet().stream().filter(e -> condition.getClass().equals(e.getValue()))
				.map(Entry::getKey).findAny()
				.map(str -> "\"" + condition.leftSide + "\" " + str + " \"" + condition.rightSide + "\"").orElseThrow(NullPointerException::new);
	}
	
	
	
	@AllArgsConstructor
	public abstract static class AbstractCondition implements Predicate<UnaryOperator<String>> {
		
		protected @Getter String leftSide;
		protected @Getter String rightSide;

	}
	
	private static class BiggerCondition extends AbstractCondition {

		public BiggerCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			try {
				return Integer.parseInt(replacer.apply(this.leftSide)) > Integer.parseInt(replacer.apply(this.rightSide));
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
	
	private static class BiggerEqualCondition extends AbstractCondition {

		public BiggerEqualCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			try {
				return Integer.parseInt(replacer.apply(this.leftSide)) >= Integer.parseInt(replacer.apply(this.rightSide));
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
	
	private static class SmallerCondition extends AbstractCondition {

		public SmallerCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			try {
				return Integer.parseInt(replacer.apply(this.leftSide)) < Integer.parseInt(replacer.apply(this.rightSide));
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
	
	private static class SmallerEqualCondition extends AbstractCondition {

		public SmallerEqualCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			try {
				return Integer.parseInt(replacer.apply(this.leftSide)) < Integer.parseInt(replacer.apply(this.rightSide));
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
	
	private static class EqualCondition extends AbstractCondition {

		public EqualCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return replacer.apply(this.leftSide).equals(replacer.apply(this.rightSide));
		}
	}
	
	private static class NotEqualCondition extends AbstractCondition {

		public NotEqualCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return !replacer.apply(this.leftSide).equals(replacer.apply(this.rightSide));
		}
	}
	
	private static class EqualIgnoreCaseCondition extends AbstractCondition {

		public EqualIgnoreCaseCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return replacer.apply(this.leftSide).equalsIgnoreCase(replacer.apply(this.rightSide));
		}
	}
	
	private static class NotEqualIgnoreCaseCondition extends AbstractCondition {

		public NotEqualIgnoreCaseCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return !replacer.apply(this.leftSide).equalsIgnoreCase(replacer.apply(this.rightSide));
		}
	}
	
	private static class ContainsCondition extends AbstractCondition {

		public ContainsCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return replacer.apply(this.leftSide).contains(replacer.apply(this.rightSide));
		}
	}
	
	private static class NotContainsCondition extends AbstractCondition {

		public NotContainsCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return !replacer.apply(this.leftSide).contains(replacer.apply(this.rightSide));
		}
	}
	

}
