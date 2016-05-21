package ch.bfh.progressor.executor.api;

/**
 * Represents the execution's Key Performance Indicators.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface PerformanceIndicators {

	/**
	 * Gets the total compilation time in milliseconds.
	 *
	 * @return total compilation time in milliseconds
	 */
	double getTotalCompileTimeMilliseconds();

	/**
	 * Gets the total execution time in milliseconds.
	 *
	 * @return total execution time in milliseconds
	 */
	double getTotalExecutionTimeMilliseconds();

	/**
	 * Gets the current test case's execution time in milliseconds.
	 *
	 * @return current test case's execution time in milliseconds
	 */
	double getTestCaseExecutionTimeMilliseconds();
}
