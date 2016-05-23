package ch.bfh.progressor.executor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.thrift.TException;
import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.api.Configuration;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.impl.FunctionSignatureImpl;
import ch.bfh.progressor.executor.impl.ResultImpl;
import ch.bfh.progressor.executor.impl.TestCaseImpl;
import ch.bfh.progressor.executor.impl.VersionInformationImpl;

/**
 * Implementation of the {@link ch.bfh.progressor.executor.thrift.ExecutorService}.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class ExecutorService implements ch.bfh.progressor.executor.thrift.ExecutorService.Iface {

	private static final Logger LOGGER = Logger.getLogger(ExecutorService.class.getName());

	private int logId;
	private final Map<String, CodeExecutor> codeExecutors = new HashMap<>();
	private final Configuration configuration;

	public ExecutorService(Configuration configuration) {

		this.configuration = configuration;
	}

	private synchronized int getLogId() {

		return this.logId++;
	}

	private void addCodeExecutor(CodeExecutor codeExecutor) {

		codeExecutor.setConfiguration(configuration);
		this.codeExecutors.put(codeExecutor.getLanguage(), codeExecutor);
	}

	private void loadCodeExecutors() {

		ServiceLoader<CodeExecutor> executors = ServiceLoader.load(CodeExecutor.class); //fetch the executor classes
		for (CodeExecutor executor : executors)
			if (!this.codeExecutors.containsKey(executor.getLanguage())) { //store the unloaded executor instances
				ExecutorService.LOGGER.fine(String.format("Bulk loaded code executor '%s'.", executor.getClass().getName()));
				this.addCodeExecutor(executor);
			}
	}

	private boolean tryLoadCodeExecutor(String language) {

		ServiceLoader<CodeExecutor> executors = ServiceLoader.load(CodeExecutor.class); //fetch the executor classes
		for (CodeExecutor executor : executors)
			if (language.equals(executor.getLanguage())) { //store the executor instance for the chosen language
				ExecutorService.LOGGER.fine(String.format("Loaded code executor '%s'.", executor.getClass().getName()));
				this.addCodeExecutor(executor);
				return true;
			}

		return false;
	}

	private CodeExecutor getCodeExecutor(String language) throws ExecutorException {

		if (!this.codeExecutors.containsKey(language) && !this.tryLoadCodeExecutor(language)) //try to load code executor; if unable, throw exception
			throw new ExecutorException(String.format("Could not find an executor for language '%s'.", language));

		return this.codeExecutors.get(language); //return the instance
	}

	/**
	 * Empty implementation. <br>
	 * Only used to test if Executor is available.
	 */
	@Override
	public void ping() {
		//do nothing
	}

	/**
	 * Fetches all currently supported languages. <br>
	 * The Executor uses a {@link ServiceLoader} to dynamically load {@link CodeExecutor}s.
	 * New code executors may become available later on.
	 *
	 * @return a {@link Set} containing all currently supported languages
	 */
	@Override
	public Set<String> getSupportedLanguages() {

		this.loadCodeExecutors();
		return this.codeExecutors.keySet();
	}

	/**
	 * Fetches the language, compiler and platform version information for a specific language.
	 *
	 * @param language language to get version information for
	 *
	 * @return version information for specified language
	 *
	 * @throws TException if anything goes wrong (check cause for exception type)
	 */
	@Override
	public ch.bfh.progressor.executor.thrift.VersionInformation getVersionInformation(String language) throws TException {

		final int logId = this.getLogId();
		ExecutorService.LOGGER.info(String.format("%-6d: getVersionInformation(language=%s)", logId, language));

		try {
			return VersionInformationImpl.convertToThrift(this.getCodeExecutor(language).getVersionInformation(), //delegate call
																										ExecutorPlatform.OPERATING_SYSTEM_NAME,
																										ExecutorPlatform.OPERATING_SYSTEM_VERSION,
																										ExecutorPlatform.OPERATING_SYSTEM_ARCHITECTURE);

		} catch (Exception ex) { //wrap exception
			String msg = String.format("Could not fetch version information for language '%s'.", language);
			ExecutorService.LOGGER.log(Level.WARNING, msg, ex);
			throw new TException(msg, ex);

		} finally {
			ExecutorService.LOGGER.finer(String.format("%-6d: finished", logId));
		}
	}

	/**
	 * Fetches the blacklist for a specific language.
	 *
	 * @param language language to get blacklist for
	 *
	 * @return a {@link Set} containing the blacklisted keywords for specified language
	 *
	 * @throws TException if anything goes wrong (check cause for exception type)
	 */
	@Override
	public Set<String> getBlacklist(String language) throws TException {

		final int logId = this.getLogId();
		ExecutorService.LOGGER.info(String.format("%-6d: getBlacklist(language=%s)", logId, language));

		try {
			return this.getCodeExecutor(language).getBlacklist(); //delegate call

		} catch (Exception ex) { //wrap exception
			String msg = String.format("Could not fetch the blacklist for language '%s'.", language);
			ExecutorService.LOGGER.log(Level.WARNING, msg, ex);
			throw new TException(msg, ex);

		} finally {
			ExecutorService.LOGGER.finer(String.format("%-6d: finished", logId));
		}
	}

	/**
	 * Generates empty function signatures for specific functions.
	 *
	 * @param language  language to generate code fragment in
	 * @param functions functions to generate signatures for
	 *
	 * @return empty function signatures for specified functions
	 *
	 * @throws TException if anything goes wrong (check cause for exception type)
	 */
	@Override
	public String getFragment(String language, List<ch.bfh.progressor.executor.thrift.FunctionSignature> functions) throws TException {

		final int logId = this.getLogId();
		ExecutorService.LOGGER.info(String.format("%-6d: getFragment(language=%s)", logId, language));

		try {
			return this.getCodeExecutor(language).getFragment(FunctionSignatureImpl.convertFromThrift(functions)); //delegate call

		} catch (Exception ex) { //wrap exception
			String msg = String.format("Could not generate the fragment for language '%s'.", language);
			ExecutorService.LOGGER.log(Level.WARNING, msg, ex);
			throw new TException(msg, ex);

		} finally {
			ExecutorService.LOGGER.finer(String.format("%-6d: finished", logId));
		}
	}

	/**
	 * Executes test cases on a specific code fragment.
	 *
	 * @param language  language to code fragment is written in
	 * @param fragment  code fragment to execute test cases on
	 * @param functions functions the fragment implements
	 * @param testCases test cases to execute on the code fragment
	 *
	 * @return a {@link List} containing the {@link Result}s of the test cases, in the same order
	 *
	 * @throws TException if anything goes wrong (check cause for exception type)
	 */
	@Override
	public List<ch.bfh.progressor.executor.thrift.Result> execute(String language, String fragment, List<ch.bfh.progressor.executor.thrift.FunctionSignature> functions, List<ch.bfh.progressor.executor.thrift.TestCase> testCases) throws TException {

		final int logId = this.getLogId();
		ExecutorService.LOGGER.info(String.format("%-6d: execute(language=%s, fragment=..., %d testCases: %s...)", logId, language, testCases.size(), !testCases.isEmpty() ? testCases.get(0) : null));

		try {
			CodeExecutor codeExecutor = this.getCodeExecutor(language);
			List<Result> results;

			List<String> blacklist = codeExecutor.getBlacklist().stream().filter(fragment::contains).collect(Collectors.toList());
			if (!blacklist.isEmpty()) { //validate fragment against blacklist
				results = Collections.nCopies(testCases.size(),
																			new ResultImpl(false, true, String.format("Validation against blacklist failed (illegal: %s).", String.join(", ", blacklist))));

			} else {
				results = codeExecutor.execute(fragment, TestCaseImpl.convertFromThrift(functions, testCases)); //delegate execution call

				if (results.size() < testCases.size())
					results.addAll(Collections.nCopies(testCases.size() - results.size(),
																						 new ResultImpl(false, true, "Could not read execution result for test case.")));
			}

			return ResultImpl.convertToThrift(results);

		} catch (Exception ex) { //wrap exception
			String msg = String.format("Could not execute the code fragment in language '%s'.", language);
			ExecutorService.LOGGER.log(Level.WARNING, msg, ex);
			throw new TException(msg, ex);

		} finally {
			ExecutorService.LOGGER.finer(String.format("%-6d: finished", logId));
		}
	}
}
