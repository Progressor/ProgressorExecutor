package ch.bfh.progressor.executor.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ValueType;

/**
 * Represents a read-only implementation of {@link ValueType}s. <br>
 * Also contains parsing logic.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class ValueTypeImpl implements ValueType {

	private static final Pattern DELIMITER_OPEN_PATTERN = Pattern.compile("\\s*<\\s*");
	private static final Pattern DELIMITER_CLOSE_PATTERN = Pattern.compile("\\s*>\\s*");
	private static final Pattern ELEMENT_SEPARATOR_PATTERN = Pattern.compile("\\s*,\\s*");

	private final ValueType.BaseType baseType;
	private final List<ValueType> genericParameters;

	/**
	 * Parses a value type.
	 *
	 * @param type string to parse
	 *
	 * @return parsed {@link ValueType}
	 *
	 * @throws ExecutorException if the parsing failed
	 */
	public static ValueType parse(String type) throws ExecutorException {

		AtomicInteger index = new AtomicInteger(0);
		ValueType result = ValueTypeImpl.parse(type, index);

		if (index.get() != type.length())
			throw new ExecutorException(String.format("Expected end of type '%s' at %d.", type, index.get()));

		return result;
	}

	private static ValueType parse(String input, AtomicInteger index) throws ExecutorException {

		ValueType.BaseType type = Arrays.stream(ValueType.BaseType.values()).filter(t -> input.length() >= index.get() + t.getName().length() && input.substring(index.get(), index.get() + t.getName().length()).equals(t.getName())).findAny().orElse(null);
		if (type == null)
			throw new ExecutorException(String.format("Unknown type in '%s' at %d.", input, index.get()));
		index.addAndGet(type.getName().length());

		List<ValueType> genericTypes = new ArrayList<>();
		Matcher matcher = ValueTypeImpl.DELIMITER_OPEN_PATTERN.matcher(input.substring(index.get()));
		if (matcher.lookingAt()) {
			do {
				index.addAndGet(matcher.group().length());
				genericTypes.add(ValueTypeImpl.parse(input, index));
			}
			while ((matcher = ValueTypeImpl.ELEMENT_SEPARATOR_PATTERN.matcher(input.substring(index.get()))).lookingAt());

			if (!(matcher = ValueTypeImpl.DELIMITER_CLOSE_PATTERN.matcher(input.substring(index.get()))).lookingAt())
				throw new ExecutorException(String.format("Missing closing pointy bracket in '%s' at %d.", input, index.get()));
			index.addAndGet(matcher.group().length());
		}

		if (genericTypes.size() != type.getDimensions())
			throw new ExecutorException(String.format("Mismatch in number of generic types for %s. Found: %d, expected: %d.", type, genericTypes.size(), type.getDimensions()));

		return new ValueTypeImpl(type, genericTypes);
	}

	private ValueTypeImpl(ValueType.BaseType baseType, List<ValueType> genericParameters) {

		this.baseType = baseType;
		this.genericParameters = Collections.unmodifiableList(genericParameters);
	}

	@Override
	public ValueType.BaseType getBaseType() {
		return this.baseType;
	}

	@Override
	public List<ValueType> getGenericParameters() {
		return this.genericParameters; //is unmodifiable
	}

	/**
	 * Gets a unique hash code for the {@link ValueType}.
	 *
	 * @return (pseudo)-unique hash code
	 */
	@Override
	public int hashCode() {

		int hash = 17;
		hash = (hash + this.baseType.hashCode()) * 31;
		for (ValueType genericParameter : this.genericParameters)
			hash = (hash + genericParameter.hashCode()) * 31;
		return hash;
	}

	/**
	 * Compares two {@link ValueType} instances.
	 *
	 * @param obj other object to compare
	 *
	 * @return whether the two objects are equal
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == null || this.getClass() != obj.getClass()) return false;

		ValueTypeImpl other = (ValueTypeImpl)obj;
		return this.baseType.equals(other.baseType) && this.genericParameters.equals(other.genericParameters);
	}

	/**
	 * Gets the generic type name.
	 *
	 * @return generic type name
	 */
	@Override
	public String toString() {
		return String.format(this.genericParameters.isEmpty() ? "%s" : "%s<%s>", this.baseType, String.join(", ", this.genericParameters.stream().map(ValueType::toString).collect(Collectors.toList())));
	}
}
