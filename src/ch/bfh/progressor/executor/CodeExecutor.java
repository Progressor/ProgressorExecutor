package ch.bfh.progressor.executor;

import java.util.Arrays;
import java.util.List;

/**
 * Interface to be implemented by all code execution engines.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
@FunctionalInterface
public interface CodeExecutor {

	/**
	 * String to indicate the end of a line.
	 */
	String END_OF_LINE = String.format("%n");

	/**
	 * Execute a provided code fragment.
	 *
	 * @param codeFragment code fragment to execute
	 * @param testCases    test cases to execute
	 *
	 * @return outcomes for each test case
	 */
	List<Result> execute(String codeFragment, List<TestCase> testCases);

	/**
	 * Execute a provided code fragment.
	 *
	 * @param codeFragment code fragment to execute
	 * @param testCases    test cases to execute
	 *
	 * @return outcomes for each test case
	 */
	default List<Result> execute(String codeFragment, TestCase... testCases) {

		return this.execute(codeFragment, Arrays.asList(testCases));
	}

	/**
	 * Get a human-readable exception message.
	 *
	 * @param message top-level message
	 * @param ex      thrown exception to get message from
	 *
	 * @return human-readable exception message
	 */
	static String getExceptionMessage(String message, Throwable ex) {

		StringBuilder sb = new StringBuilder(message);
		do sb.append(CodeExecutor.END_OF_LINE).append('>').append(ex);
		while ((ex = ex.getCause()) != null);

		return sb.toString();
	}
}
