package ch.bfh.progressor.executor.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;

/**
 * Represents an abstract base class for read-only implementations of arbitrary {@link Value}s. <br>
 * Also contains parsing logic.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public abstract class ValueImpl implements Value {

	private static final Pattern DELIMITER_OPEN_PATTERN = Pattern.compile("\\s*\\{\\s*");
	private static final Pattern DELIMITER_CLOSE_PATTERN = Pattern.compile("\\s*\\}\\s*");
	private static final List<Pattern> SEPARATOR_PATTERNS = Arrays.asList(Pattern.compile("\\s*,\\s*"), Pattern.compile("\\s*:\\s*"));

	/**
	 * Parses an arbitrary string.
	 *
	 * @param type  type of the value
	 * @param value string to parse
	 *
	 * @return parsed {@link Value}
	 *
	 * @throws ExecutorException if the parsing failed
	 */
	public static Value parse(ValueType type, String value) throws ExecutorException {

		AtomicInteger index = new AtomicInteger(0);
		Value result = ValueImpl.parse(type, value, index);

		if (index.get() != value.length())
			throw new ExecutorException(true, String.format("Expected end of value '%s' at %d.", value, index.get()));

		return result;
	}

	private static Value parse(ValueType type, String input, AtomicInteger index, Pattern... delimiters) throws ExecutorException {

		switch (type.getBaseType().getDimensions()) {
			case 0:
				Matcher matcher = Pattern.compile(String.format(delimiters.length > 0 ? ".*?(?=(%s))" : ".*", String.join(")|(", Arrays.stream(delimiters).map(Pattern::pattern).collect(Collectors.toList())))).matcher(input.substring(index.get()));
				if (!matcher.lookingAt())
					throw new ExecutorException(true, String.format("Error in value '%s' at %d.", input, index.get()));
				index.addAndGet(matcher.group().length());
				return new ValueImpl.Single(type, matcher.group());

			case 1:
				if (!(matcher = ValueImpl.DELIMITER_OPEN_PATTERN.matcher(input.substring(index.get()))).lookingAt())
					throw new ExecutorException(true, String.format("Missing opening curly bracket in '%s' at %d.", input, index.get()));
				index.addAndGet(matcher.group().length());

				List<Value> collection = new ArrayList<>();
				for (int i = 0; true; i++) {
					if ((matcher = ValueImpl.DELIMITER_CLOSE_PATTERN.matcher(input.substring(index.get()))).lookingAt()) {
						index.addAndGet(matcher.group().length());
						break;
					} else if (i > 0 && (matcher = ValueImpl.SEPARATOR_PATTERNS.get(0).matcher(input.substring(index.get()))).lookingAt())
						index.addAndGet(matcher.group().length());
					else if (i > 0)
						throw new ExecutorException(true, String.format("Missing element separator in '%s' at %d.", input, index.get()));

					collection.add(ValueImpl.parse(type.getGenericParameters().get(0), input, index, ValueImpl.DELIMITER_CLOSE_PATTERN, ValueImpl.SEPARATOR_PATTERNS.get(0)));
				}
				return new ValueImpl.Collection1D(type, collection);

			case 2:
				if (!(matcher = ValueImpl.DELIMITER_OPEN_PATTERN.matcher(input.substring(index.get()))).lookingAt())
					throw new ExecutorException(true, String.format("Missing opening curly bracket in '%s' at %d.", input, index.get()));
				index.addAndGet(matcher.group().length());

				List<List<Value>> collection2D = new ArrayList<>();
				for (int i = 0; true; i++) {
					if ((matcher = ValueImpl.DELIMITER_CLOSE_PATTERN.matcher(input.substring(index.get()))).lookingAt()) {
						index.addAndGet(matcher.group().length());
						break;
					} else if (i > 0 && (matcher = ValueImpl.SEPARATOR_PATTERNS.get(0).matcher(input.substring(index.get()))).lookingAt())
						index.addAndGet(matcher.group().length());
					else if (i > 0)
						throw new ExecutorException(true, String.format("Missing element separator in '%s' at %d.", input, index.get()));

					collection = new ArrayList<>();
					for (int j = 0; true; j++) {
						if ((matcher = ValueImpl.DELIMITER_CLOSE_PATTERN.matcher(input.substring(index.get()))).lookingAt()
								|| (matcher = ValueImpl.SEPARATOR_PATTERNS.get(0).matcher(input.substring(index.get()))).lookingAt()) {
							break;
						} else if (j > 0 && (matcher = ValueImpl.SEPARATOR_PATTERNS.get(1).matcher(input.substring(index.get()))).lookingAt())
							index.addAndGet(matcher.group().length());
						else if (j > 0)
							throw new ExecutorException(true, String.format("Missing sub-element separator in '%s' at %d.", input, index.get()));

						collection.add(ValueImpl.parse(type.getGenericParameters().get(j), input, index, ValueImpl.DELIMITER_CLOSE_PATTERN, ValueImpl.SEPARATOR_PATTERNS.get(0), ValueImpl.SEPARATOR_PATTERNS.get(1)));
					}
					collection2D.add(collection);
				}
				return new ValueImpl.Collection2D(type, collection2D);

			default:
				throw new ExecutorException(true, String.format("Unsupported number of dimensions: %d.", type.getBaseType().getDimensions()));
		}
	}

	private final ValueType type;

	private ValueImpl(ValueType type) {

		this.type = type;
	}

	@Override
	public ValueType getType() {
		return this.type;
	}

	private RuntimeException defaultGetter(int dimensions) {

		if (dimensions != this.getDimensions())
			return new UnsupportedOperationException(String.format("Mismatch in dimensions. Requested: %d, actual: %d.", dimensions, this.getDimensions()));
		else
			return new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getSingle() {
		throw this.defaultGetter(0);
	}

	@Override
	public List<Value> getCollection() {
		throw this.defaultGetter(1);
	}

	@Override
	public List<List<Value>> get2DCollection() {
		throw this.defaultGetter(2);
	}

	/**
	 * Gets a unique hash code for the {@link Value}.
	 *
	 * @return (pseudo)-unique hash code
	 */
	@Override
	public abstract int hashCode();

	/**
	 * Compares two {@link Value} instances.
	 *
	 * @param obj other object to compare
	 *
	 * @return whether the two objects are equal
	 */
	@Override
	public abstract boolean equals(Object obj);

	/**
	 * Gets a string representation of the {@link Value}.
	 *
	 * @return {@link String} representation of the {@link Value}
	 */
	@Override
	public abstract String toString();

	private static class Single extends ValueImpl {

		private final String value;

		private Single(ValueType type, String value) {
			super(type);

			this.value = value;
		}

		@Override
		public int getDimensions() {
			return 0;
		}

		@Override
		public String getSingle() {
			return this.value;
		}

		@Override
		public int hashCode() {
			return (17 + this.value.hashCode()) * 31;
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && this.getClass() == obj.getClass() && this.value.equals(((ValueImpl.Single)obj).value);
		}

		@Override
		public String toString() {
			return this.value;
		}
	}

	private static class Collection1D extends ValueImpl {

		private final List<Value> collection;

		private Collection1D(ValueType type, List<Value> collection) {
			super(type);

			this.collection = collection;
		}

		@Override
		public int getDimensions() {
			return 1;
		}

		@Override
		public List<Value> getCollection() {
			return this.collection;
		}

		@Override
		public int hashCode() {
			return (17 + this.collection.hashCode()) * 31;
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && this.getClass() == obj.getClass() && this.collection.equals(((ValueImpl.Collection1D)obj).collection);
		}

		@Override
		public String toString() {
			return String.format("{%s}", String.join("|", this.collection.stream().map(Value::toString).collect(Collectors.toList())));
		}
	}

	private static class Collection2D extends ValueImpl {

		private final List<List<Value>> collection;

		private Collection2D(ValueType type, List<List<Value>> collection) {
			super(type);

			this.collection = collection;
		}

		@Override
		public int getDimensions() {
			return 2;
		}

		@Override
		public List<List<Value>> get2DCollection() {
			return this.collection;
		}

		@Override
		public int hashCode() {
			return (17 + this.collection.hashCode()) * 31;
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && this.getClass() == obj.getClass() && this.collection.equals(((ValueImpl.Collection2D)obj).collection);
		}

		@Override
		public String toString() {
			return String.format("{%s}", String.join("|", this.collection.stream().map(c -> String.format("{%s}", String.join("|", c.stream().map(Value::toString).collect(Collectors.toList())))).collect(Collectors.toList())));
		}
	}
}
