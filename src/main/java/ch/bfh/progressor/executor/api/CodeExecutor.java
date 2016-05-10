package ch.bfh.progressor.executor.api;

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
	 * @throws ExecutorException if the blacklist could not be read
	 */
	Collection<String> getBlacklist() throws ExecutorException;

	/**
	 * Executes a provided code fragment.
	 *
	 * @param codeFragment code fragment to execute
	 * @param testCases    test cases to execute
	 *
	 * @return a {@link List} containing the {@link Result} for each test case
	 */
	List<Result> execute(String codeFragment, List<TestCase> testCases);
}
