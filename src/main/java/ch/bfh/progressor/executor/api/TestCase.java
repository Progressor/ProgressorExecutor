package ch.bfh.progressor.executor.api;

import java.util.List;

/**
 * Represents a test case for a specific function.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface TestCase {

	/**
	 * Gets the function this test case refers to.
	 *
	 * @return function this test case refers to
	 */
	FunctionSignature getFunction();

	/**
	 * Gets the values to pass to the function.
	 *
	 * @return values to pass to the function
	 */
	List<Value> getInputValues();

	/**
	 * Gets the values to be expected from the function
	 *
	 * @return values the function is expected to return
	 */
	List<Value> getExpectedOutputValues();
}
