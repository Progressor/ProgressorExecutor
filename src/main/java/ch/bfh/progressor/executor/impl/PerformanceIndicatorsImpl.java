package ch.bfh.progressor.executor.impl;

import java.util.ArrayList;
import java.util.List;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.PerformanceIndicators;

/**
 * Read-only implementation of a {@link PerformanceIndicators}.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class PerformanceIndicatorsImpl implements PerformanceIndicators {

	private final double totalCompileTimeMilliseconds;
	private final double totalExecutionTimeMilliseconds;
	private final double testCaseExecutionTimeMilliseconds;

	/**
	 * Construct a new {@link PerformanceIndicators}.
	 *
	 * @param totalCompileTimeMilliseconds      total compilation time in milliseconds
	 * @param totalExecutionTimeMilliseconds    total execution time in milliseconds
	 * @param testCaseExecutionTimeMilliseconds current test case's execution time in milliseconds
	 */
	public PerformanceIndicatorsImpl(double totalCompileTimeMilliseconds, double totalExecutionTimeMilliseconds, double testCaseExecutionTimeMilliseconds) {

		this.totalCompileTimeMilliseconds = totalCompileTimeMilliseconds;
		this.totalExecutionTimeMilliseconds = totalExecutionTimeMilliseconds;
		this.testCaseExecutionTimeMilliseconds = testCaseExecutionTimeMilliseconds;
	}

	@Override
	public double getTotalCompileTimeMilliseconds() {
		return this.totalExecutionTimeMilliseconds;
	}

	@Override
	public double getTotalExecutionTimeMilliseconds() {
		return this.totalExecutionTimeMilliseconds;
	}

	@Override
	public double getTestCaseExecutionTimeMilliseconds() {
		return this.testCaseExecutionTimeMilliseconds;
	}

	/**
	 * Converts custom {@link PerformanceIndicators}s to thrift {@link ch.bfh.progressor.executor.thrift.PerformanceIndicators} instances.
	 *
	 * @param indicators custom instances to convert
	 *
	 * @return thrift {@link ch.bfh.progressor.executor.thrift.PerformanceIndicators} instances
	 *
	 * @throws ExecutorException if conversation failed
	 */
	public static List<ch.bfh.progressor.executor.thrift.PerformanceIndicators> convertToThrift(List<PerformanceIndicators> indicators) throws ExecutorException {

		List<ch.bfh.progressor.executor.thrift.PerformanceIndicators> output = new ArrayList<>(indicators.size());
		for (PerformanceIndicators indicator : indicators)
			output.add(new ch.bfh.progressor.executor.thrift.PerformanceIndicators(indicator.getTotalCompileTimeMilliseconds(),
																																						 indicator.getTotalExecutionTimeMilliseconds(),
																																						 indicator.getTestCaseExecutionTimeMilliseconds()));

		return output;
	}
}
