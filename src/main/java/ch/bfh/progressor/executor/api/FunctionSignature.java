package ch.bfh.progressor.executor.api;

import java.util.List;

/**
 * Represents the signature of a function to test.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface FunctionSignature {

	/**
	 * Gets the name of the function.
	 *
	 * @return name of the function
	 */
	String getName();

	/**
	 * Gets the names of the function's input parameters.
	 *
	 * @return a {@link List} containing the names of the function's input parameters
	 */
	List<String> getInputNames();

	/**
	 * Gets the types of the function's input parameters.
	 *
	 * @return a {@link List} containing the types of the function's input parameters
	 */
	List<ValueType> getInputTypes();

	/**
	 * Gets the names of the function's output parameters.
	 *
	 * @return a {@link List} containing the names of the function's output parameters
	 */
	List<String> getOutputNames();

	/**
	 * Gets the types of the function's output parameters.
	 *
	 * @return a {@link List} containing the types of the function's output parameters
	 */
	List<ValueType> getOutputTypes();
}
