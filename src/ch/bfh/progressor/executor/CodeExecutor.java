package ch.bfh.progressor.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Interface to be implemented by all code execution engines.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface CodeExecutor {

	/**
	 * Gets the unique name of the language the executor supports.
	 *
	 * @return unique name of the supported language
	 */
	String getLanguage();

	/**
	 * Gets the blacklist containing the strings not allowed in the code fragment.
	 *
	 * @return a {@link Collection} containing the strings not allowed in the code fragment
	 */
	Collection<String> getBlacklist();

	/**
	 * Executes a provided code fragment.
	 *
	 * @param codeFragment code fragment to execute
	 * @param testCases    test cases to execute
	 *
	 * @return a {@link List} containing the {@link Result} for each test case
	 */
	List<Result> execute(String codeFragment, List<TestCase> testCases);

	/**
	 * Executes a provided code fragment.
	 *
	 * @param codeFragment code fragment to execute
	 * @param testCases    test cases to execute
	 *
	 * @return a {@link List} containing the {@link Result} for each test case
	 */
	default List<Result> execute(String codeFragment, TestCase... testCases) {

		return this.execute(codeFragment, Arrays.asList(testCases));
	}
}
