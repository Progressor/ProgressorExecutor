package ch.bfh.progressor.executor.api;

import java.util.List;
import java.util.Set;

/**
 * Interface to be implemented by all code execution engines.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface CodeExecutor {

	/**
	 * Sets the configuration to use.
	 *
	 * @param configuration configuration to use
	 */
	void setConfiguration(Configuration configuration);

	/**
	 * Gets the unique name of the language the executor supports.
	 *
	 * @return unique name of the supported language
	 */
	String getLanguage();

	/**
	 * Gets the version information for the language the executor supports.
	 *
	 * @return version information for the supported language
	 *
	 * @throws ExecutorException if fetching the version information failed
	 */
	VersionInformation getVersionInformation() throws ExecutorException;

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
	 * @return a {@link Set} containing the strings not allowed in the code fragment
	 *
	 * @throws ExecutorException if the blacklist could not be read
	 */
	Set<String> getBlacklist() throws ExecutorException;

	/**
	 * Gets the whole code generated in order to execute the provided code fragment.
	 *
	 * @param codeFragment code fragment to include in the generated code
	 * @param testCases    test cases to include in the generated code
	 *
	 * @return the whole code containing the provided code fragment and generated code for each test case
	 *
	 * @throws ExecutorException if the blacklist could not be read
	 */
	String getCodeFile(String codeFragment, List<TestCase> testCases) throws ExecutorException;

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
