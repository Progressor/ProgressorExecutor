package ch.bfh.progressor.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import ch.bfh.progressor.executor.thrift.FunctionSignature;
import ch.bfh.progressor.executor.thrift.Result;
import ch.bfh.progressor.executor.thrift.TestCase;

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
	 * Gets the fragment(s) for the function signatures in the language the executor supports.
	 *
	 * @param functions function signatures to get fragment(s) for
	 *
	 * @return fragment(s) for the function signatures in the supported language
	 *
	 * @throws ExecutorException if the fragment could not be generated
	 */
	String getFragment(List<FunctionSignature> functions) throws ExecutorException;

	/**
	 * Gets the blacklist containing the strings not allowed in the code fragment.
	 *
	 * @return a {@link Collection} containing the strings not allowed in the code fragment
	 *
	 * @throws ExecutorException if the fragment could not be read
	 */
	Collection<String> getBlacklist() throws ExecutorException;

	/**
	 * Executes a provided code fragment.
	 *
	 * @param codeFragment code fragment to execute
	 * @param functions    function signatures to execute tests on
	 * @param testCases    test cases to execute
	 *
	 * @return a {@link List} containing the {@link Result} for each test case
	 */
	List<Result> execute(String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases);

	/**
	 * Executes a provided code fragment.
	 *
	 * @param codeFragment code fragment to execute
	 * @param functions    function signatures to execute tests on
	 * @param testCases    test cases to execute
	 *
	 * @return a {@link List} containing the {@link Result} for each test case
	 */
	default List<Result> execute(String codeFragment, List<FunctionSignature> functions, TestCase... testCases) {

		return this.execute(codeFragment, functions, Arrays.asList(testCases));
	}
}
