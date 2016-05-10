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

	private final double runtimeMilliseconds;

	/**
	 * Construct a new {@link PerformanceIndicators}.
	 *
	 * @param runtimeMilliseconds runtime in milliseconds
	 */
	public PerformanceIndicatorsImpl(double runtimeMilliseconds) {

		this.runtimeMilliseconds = runtimeMilliseconds;
	}

	@Override
	public double getRuntimeMilliseconds() {
		return this.runtimeMilliseconds;
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
			output.add(new ch.bfh.progressor.executor.thrift.PerformanceIndicators(indicator.getRuntimeMilliseconds()));

		return output;
	}
}
