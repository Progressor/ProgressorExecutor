package ch.bfh.progressor.executor.api;

import java.util.List;

/**
 * Represents an arbitrary value.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface Value {

	/**
	 * Gets the type of the value.
	 *
	 * @return type of the value
	 */
	ValueType getType();

	/**
	 * Gets the number of dimensions of the value.
	 *
	 * @return number of dimensions of the value
	 */
	int getDimensions();

	/**
	 * Tries to get a single (0-D) value.
	 *
	 * @return a single {@link String}
	 *
	 * @throws UnsupportedOperationException if there is a dimension mismatch
	 */
	String getSingle();

	/**
	 * Tries to get a (1-D) collection of values.
	 *
	 * @return a {@link List} of {@link Value}s
	 *
	 * @throws UnsupportedOperationException if there is a dimension mismatch
	 */
	List<Value> getCollection();

	/**
	 * Tries to get a 2-dimensional collection of value.
	 *
	 * @return a {@link List} of {@link List}s of {@link Value}s
	 *
	 * @throws UnsupportedOperationException if there is a dimension mismatch
	 */
	List<List<Value>> get2DCollection();
}
