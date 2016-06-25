package ch.bfh.progressor.executor.api;

/**
 * Represents an execution result.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface Result {

	/**
	 * Whether the execution completed successfully.
	 *
	 * @return whether the execution completed successfully
	 */
	boolean isSuccess();

	/**
	 * Whether the execution ran into a fatal error.
	 *
	 * @return whether the execution ran into a fatal error
	 */
	boolean isFatal();

	/**
	 * Gets the execution's actual result.
	 *
	 * @return execution's actual result
	 */
	String getResult();

	/**
	 * Gets the execution's Key Performance Indicators.
	 *
	 * @return execution's Key Performance Indicators
	 */
	PerformanceIndicators getPerformance();
}
