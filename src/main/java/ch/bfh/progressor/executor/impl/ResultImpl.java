package ch.bfh.progressor.executor.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.PerformanceIndicators;
import ch.bfh.progressor.executor.api.Result;

/**
 * Read-only implementation of a {@link Result}.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class ResultImpl implements Result {

	private final boolean success;
	private final boolean fatal;
	private final String result;
	private final PerformanceIndicators performance;

	/**
	 * Construct a new {@link Result}.
	 *
	 * @param success     whether or not the execution completed successfully
	 * @param fatal       whether or not the execution ran into a fatal error
	 * @param result      execution's actual result
	 * @param performance execution's Key Performance Indicators
	 */
	public ResultImpl(boolean success, boolean fatal, String result, PerformanceIndicators performance) {

		this.success = success;
		this.fatal = fatal;
		this.result = result;
		this.performance = performance;
	}

	@Override
	public boolean isSuccess() {
		return this.success;
	}

	@Override
	public boolean isFatal() {
		return this.fatal;
	}

	@Override
	public String getResult() {
		return this.result;
	}

	@Override
	public PerformanceIndicators getPerformance() {
		return this.performance;
	}

	/**
	 * Converts custom {@link Result}s to thrift {@link ch.bfh.progressor.executor.thrift.Result} instances.
	 *
	 * @param results custom results to convert
	 *
	 * @return thrift {@link ch.bfh.progressor.executor.thrift.Result} instances
	 *
	 * @throws ExecutorException if conversation failed
	 */
	public static List<ch.bfh.progressor.executor.thrift.Result> convertToThrift(List<Result> results) throws ExecutorException {

		List<ch.bfh.progressor.executor.thrift.Result> output = new ArrayList<>(results.size());
		for (Result result : results) {
			ch.bfh.progressor.executor.thrift.PerformanceIndicators performance = result.getPerformance() != null ? PerformanceIndicatorsImpl.convertToThrift(Collections.singletonList(result.getPerformance())).get(0) : null;
			output.add(new ch.bfh.progressor.executor.thrift.Result(result.isSuccess(), result.isFatal(), result.getResult(), performance));
		}

		return output;
	}
}
