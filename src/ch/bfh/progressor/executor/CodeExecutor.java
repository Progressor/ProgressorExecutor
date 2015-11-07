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
}
