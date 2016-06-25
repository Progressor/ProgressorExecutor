package ch.bfh.progressor.executor.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.api.Configuration;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;
import ch.bfh.progressor.executor.api.VersionInformation;

/**
 * Base class with helper methods for code execution engines.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public abstract class CodeExecutorBase implements CodeExecutor {

	/**
	 * Character set to use for general operations.
	 */
	public static final Charset CHARSET = Charset.forName("UTF-8");

	/**
	 * The system-dependent newline character.
	 */
	public static final String NEWLINE = String.format("%n");

	/**
	 * The platform the executor runs on.
	 */
	protected static final ExecutorPlatform PLATFORM = ExecutorPlatform.determine();

	/**
	 * The current directory.
	 */
	protected static final File CURRENT_DIRECTORY = new File(".");

	/**
	 * Regular expression pattern for numeric integer literals.
	 */
	protected static final Pattern NUMERIC_INTEGER_PATTERN = Pattern.compile("[-+]?[0-9]+");

	/**
	 * Regular expression pattern for numeric floating-point or decimal literals without exponent.
	 */
	protected static final Pattern NUMERIC_FLOATING_PATTERN = Pattern.compile("[-+]?[0-9]+(\\.[0-9]+)?");

	/**
	 * Regular expression pattern for numeric floating-point or decimal literals. <br>
	 * This pattern does support literals in exponential form (e.g. {@code 1.25e-2}).
	 */
	protected static final Pattern NUMERIC_FLOATING_EXPONENTIAL_PATTERN = Pattern.compile("[-+]?[0-9]+(\\.[0-9]+)?([eE][-+]?[0-9]+)?");

	/**
	 * Placeholder for the custom code fragment in the template file.
	 */
	protected static final String CODE_CUSTOM_FRAGMENT = "$CustomCode$";

	/**
	 * Placeholder for the test case fragment in the template file.
	 */
	protected static final String TEST_CASES_FRAGMENT = "$TestCases$";

	/**
	 * Gets the number of milli(second)s per nano(second).
	 */
	protected static final double MILLIS_IN_NANO = 1e6;

	private static final int BUFFER_SIZE = 1024;
	private static final int BUFFER_FACTOR = 2;
	private static final ByteOrderMark[] BYTE_ORDER_MARKS = CodeExecutorBase.duplicateByteOrderMarks(ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE);

	private static final long MAX_JOIN_TIMEOUT_MILLIS = 150;
	private static final long MAX_BUFFER_TIMEOUT_MILLIS = CodeExecutorBase.MAX_JOIN_TIMEOUT_MILLIS * 10;
	private static final long MAX_INITIAL_TIMEOUT_MILLIS = CodeExecutorBase.MAX_BUFFER_TIMEOUT_MILLIS * 3;
	private static final long MAX_TOTAL_TIMEOUT_MILLIS = CodeExecutorBase.MAX_BUFFER_TIMEOUT_MILLIS * 15;

	private static final Pattern DOUBLE_NEWLINE_PATTERN = Pattern.compile("(\\r\\n|\\r(?!\\n)|(?<!\\r)\\n){2}");
	private static final Pattern RESULT_SUCCESS_PATTERN = Pattern.compile("(OK|ER):", Pattern.CASE_INSENSITIVE);
	private static final Pattern RESULT_EXECUTION_TIME_PATTERN = Pattern.compile(String.format("(%s):", CodeExecutorBase.NUMERIC_FLOATING_EXPONENTIAL_PATTERN.pattern()), Pattern.CASE_INSENSITIVE);

	private Configuration configuration = Configuration.DEFAULT_CONFIGURATION;
	private Set<String> blacklist;
	private StringBuilder template;

	//***************************
	//*** CODE EXECUTOR LOGIC ***
	//***************************

	/**
	 * Gets the configuration to use.
	 *
	 * @return configuration to use
	 */
	public Configuration getConfiguration() {
		return this.configuration;
	}

	@Override
	public void setConfiguration(Configuration configuration) {

		this.configuration = configuration;
	}

	/**
	 * Gets the path to the blacklist file.
	 *
	 * @return default path to the blacklist file
	 */
	protected String getBlackListPath() {
		return String.format("%s/blacklist.json", this.getLanguage());
	}

	@Override
	public synchronized Set<String> getBlacklist() throws ExecutorException {

		if (this.blacklist == null)
			try (InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream(this.getBlackListPath()), CodeExecutorBase.CHARSET)) {
				this.blacklist = new HashSet<>();
				JSONTokener tokener = new JSONTokener(reader);

				if (!tokener.more()) throw new JSONException("No root elements present.");
				JSONArray groups = (JSONArray)tokener.nextValue();
				if (!tokener.more()) throw new JSONException("Multiple root elements present.");

				for (int i = 0; i < groups.length(); i++) {
					JSONObject group = groups.getJSONObject(i);
					JSONArray elements = group.getJSONArray("elements");

					for (int j = 0; j < elements.length(); j++) {
						JSONObject element = elements.getJSONObject(j);
						this.blacklist.add(element.getString("keyword"));
					}
				}

			} catch (IOException | JSONException | ClassCastException ex) {
				throw new ExecutorException("Could not read the blacklist.", ex);
			}

		return Collections.unmodifiableSet(this.blacklist);
	}

	/**
	 * Gets the path to the template file.
	 *
	 * @return default path to the template file
	 */
	protected abstract String getTemplatePath();

	/**
	 * Gets the code template for this language.
	 *
	 * @return code template for this language
	 *
	 * @throws ExecutorException if the code template could not be read
	 */
	protected synchronized StringBuilder getTemplate() throws ExecutorException {

		if (this.template == null)
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(this.getTemplatePath()), CodeExecutorBase.CHARSET))) {
				this.template = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) //read template to StringBuilder
					this.template.append(line).append(CodeExecutorBase.NEWLINE);

			} catch (IOException ex) {
				throw new ExecutorException("Could not read the code template.", ex);
			}

		return new StringBuilder(this.template); //return a new string builder every time
	}

	@Override
	public String getFragment(List<FunctionSignature> functions) throws ExecutorException {
		return this.getFunctionSignatures(functions);
	}

	@Override
	public String getCodeFile(String codeFragment, List<TestCase> testCases) throws ExecutorException {

		try {
			StringBuilder code = this.getTemplate(); //read the template

			int fragStart = code.indexOf(CodeExecutorBase.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + CodeExecutorBase.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			int caseStart = code.indexOf(CodeExecutorBase.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
			code.replace(caseStart, caseStart + CodeExecutorBase.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(testCases));

			return code.toString();

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not generate the code file.", ex);
		}
	}

	/**
	 * Generates the code file with the user's code fragment.
	 *
	 * @param codeFile     file to generate
	 * @param codeFragment code fragment to write into the file
	 * @param testCases    test cases to generate tests for
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected void generateCodeFile(File codeFile, String codeFragment, List<TestCase> testCases) throws ExecutorException {

		try {
			Files.write(codeFile.toPath(), this.getCodeFile(codeFragment, testCases).getBytes(CodeExecutorBase.CHARSET)); //and write the generated code in it

		} catch (IOException ex) {
			throw new ExecutorException("Could not generate the code file.", ex);
		}
	}

	/**
	 * Generates the function signatures.
	 *
	 * @param functions functions to generate signatures for
	 *
	 * @return function signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected abstract String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException;

	/**
	 * Generates the test case signatures.
	 *
	 * @param testCases test cases to generate signatures for
	 *
	 * @return test case signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected abstract String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException;

	/**
	 * Gets the literal for an arbitrary value.
	 *
	 * @param value value to get literal for
	 *
	 * @return literal for value
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected abstract String getValueLiteral(Value value) throws ExecutorException;

	/**
	 * Gets the name of an arbitrary type.
	 *
	 * @param type type to get name of
	 *
	 * @return name of type
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected abstract String getTypeName(ValueType type) throws ExecutorException;

	//**********************
	//*** SYSTEM HELPERS ***
	//**********************

	/**
	 * Tries to recursively delete a file or directory.
	 *
	 * @param file file to directory to delete
	 *
	 * @return whether the file or directory has been deleted
	 */
	protected boolean tryDeleteRecursive(File file) {

		boolean ret = true;

		File[] children; //recursively delete children
		if (file.isDirectory() && (children = file.listFiles()) != null)
			for (File child : children)
				ret &= this.tryDeleteRecursive(child);

		if (this.getConfiguration().shouldCleanUp())
			ret &= file.delete(); //delete file itself
		return ret;
	}

	/**
	 * Concatenates several arrays.
	 *
	 * @param <T>    element type of the array to concatenate
	 * @param arrays the arrays to concatenate
	 *
	 * @return a new array containing all the elements
	 */
	protected <T> T[] concatenateArrays(T[]... arrays) {

		if (arrays.length == 0) return (T[])new Object[0];

		int length = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
		T[] combined = Arrays.copyOf(arrays[0], length);

		for (int i = 1, position = arrays[0].length; i < arrays.length; position += arrays[i++].length)
			System.arraycopy(arrays[i], 0, combined, position, arrays[i].length);

		return combined;
	}

	private static ByteOrderMark[] duplicateByteOrderMarks(ByteOrderMark... byteOrderMarks) {

		ByteOrderMark[] output = new ByteOrderMark[byteOrderMarks.length * 2];
		for (int i = 0; i < byteOrderMarks.length; i++) {

			int[] bytes = new int[byteOrderMarks[i].length() * 2];
			for (int j = 0; j < byteOrderMarks[i].length(); j++) {
				bytes[j] = byteOrderMarks[i].get(j);
				bytes[j + byteOrderMarks[i].length()] = byteOrderMarks[i].get(j);
			}

			output[i] = byteOrderMarks[i];
			output[i + byteOrderMarks.length] = new ByteOrderMark(byteOrderMarks[i].getCharsetName(), bytes);
		}

		return output;
	}

	private String executeSystemCommand(boolean safe, boolean deferred, File directory, String... command) throws ExecutorException {

		Process process = null;
		try {
			process = new ProcessBuilder(command).directory(directory).redirectErrorStream(true).start();

			boolean timeoutException = false;
			String output; //source: http://stackoverflow.com/a/16313762/1325979
			try (BOMInputStream bomInputStream = new BOMInputStream(process.getInputStream(), CodeExecutorBase.BYTE_ORDER_MARKS)) {
				boolean processFinished = false;
				ByteBuffer byteBuffer = ByteBuffer.allocate(CodeExecutorBase.BUFFER_SIZE);

				long maxBufferTimeMillis = System.currentTimeMillis() + CodeExecutorBase.MAX_INITIAL_TIMEOUT_MILLIS;
				final long maxTotalTimeMillis = System.currentTimeMillis() + CodeExecutorBase.MAX_TOTAL_TIMEOUT_MILLIS;
				while (true) {
					if (!byteBuffer.hasArray())
						throw new ExecutorException("Could not properly read process output.");
					if (!safe && (timeoutException = !deferred && System.currentTimeMillis() > maxBufferTimeMillis || System.currentTimeMillis() > maxTotalTimeMillis))
						break;

					int bytesToRead = Math.min(bomInputStream.available(), byteBuffer.remaining());
					int readBytes = bomInputStream.read(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), bytesToRead);
					if (readBytes > 0) {
						byteBuffer.position(byteBuffer.position() + readBytes);
						maxBufferTimeMillis = System.currentTimeMillis() + CodeExecutorBase.MAX_BUFFER_TIMEOUT_MILLIS;
						if (!byteBuffer.hasRemaining()) {
							ByteBuffer newBuffer = ByteBuffer.allocate(byteBuffer.capacity() * CodeExecutorBase.BUFFER_FACTOR);
							byteBuffer.flip();
							newBuffer.put(byteBuffer);
							byteBuffer = newBuffer;
						}

					} else if (!processFinished && process.waitFor(CodeExecutorBase.MAX_JOIN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
						processFinished = true;

					else if (processFinished || readBytes < 0)
						break;
				}

				Charset charset = CodeExecutorBase.CHARSET;
				int bomLength = Arrays.stream(CodeExecutorBase.BYTE_ORDER_MARKS).mapToInt(ByteOrderMark::length).max().orElse(0);
				if (byteBuffer.position() >= bomLength && bomInputStream.hasBOM())
					charset = Charset.forName(bomInputStream.getBOMCharsetName());

				byteBuffer.flip();
				output = charset.newDecoder().decode(byteBuffer.slice()).toString();
			}

			if (timeoutException)
				throw new ExecutorException("Could not execute command in time.", output);
			if (!process.waitFor(CodeExecutorBase.MAX_JOIN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
				throw new ExecutorException("Could not finish command in time.", output);
			if (process.exitValue() != 0)
				throw new ExecutorException("Could not successfully execute command.", output);

			return output;

		} catch (InterruptedException ex) {
			throw new ExecutorException("Could not wait for command to execute in time.", ex);

		} catch (IOException ex) {
			throw new ExecutorException("Could not execute command.", ex);

		} finally {
			if (process != null && process.isAlive())
				//use Java 9 ProcessHandle to destroy children as well
				process.destroyForcibly();
		}
	}

	/**
	 * Executes a standard (unsafe) system command. <br>
	 * Several rules are enforced for unsafe processes:
	 * <ol>
	 * <li>an initial timeout: the processes initially has to react (write on the output stream) before a configured timeout expires</li>
	 * <li>a buffer timeout: the processes then has to continuously react before a configured rolling timeout expires</li>
	 * <li>a total timeout: the processes has to complete before a configured timeout expires</li>
	 * </ol>
	 *
	 * @param directory the working directory for the command
	 * @param command   command to execute
	 *
	 * @return the output of the command
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 */
	protected String executeCommand(File directory, String... command) throws ExecutorException {

		return this.executeSystemCommand(false, false, directory, command);
	}

	/**
	 * Executes an unsafe deferred system command. <br>
	 * Only the total timeout is enforced for deferred processes.
	 * They may bulk output data just before finishing.
	 *
	 * @param directory the working directory for the command
	 * @param command   command to execute
	 *
	 * @return the output of the command
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 * @see #executeCommand(File, String...)
	 */
	protected String executeDeferredCommand(File directory, String... command) throws ExecutorException {

		return this.executeSystemCommand(false, true, directory, command);
	}

	/**
	 * Executes a guaranteed safe system command. <br>
	 * No rules are enforced for safe commands.
	 * They are not being aborted and may run forever.
	 *
	 * @param directory the working directory for the command
	 * @param command   command to execute
	 *
	 * @return the output of the command
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 */
	protected String executeSafeCommand(File directory, String... command) throws ExecutorException {

		return this.executeSystemCommand(true, false, directory, command);
	}

	//*****************************
	//*** MISCELLANEOUS HELPERS ***
	//*****************************

	/**
	 * Constructs a version information object.
	 *
	 * @param languageVersion supported version of the programming language
	 * @param compilerName    name of the used compiler (or interpreter)
	 * @param compilerVersion version of the used compiler (or interpreter)
	 *
	 * @return a version information object
	 */
	protected VersionInformation createVersionInformation(String languageVersion, String compilerName, String compilerVersion) {

		return new VersionInformationImpl(languageVersion, compilerName, compilerVersion);
	}

	/**
	 * Gets a {@link Result} object without performance indicators.
	 *
	 * @param success whether the execution completed successfully
	 * @param fatal   whether the execution ran into a fatal error
	 * @param result  execution's actual result
	 *
	 * @return a {@link Result} object containing the information
	 *
	 * @throws ExecutorException if creation failed
	 */
	protected Result createResult(boolean success, boolean fatal, String result) throws ExecutorException {

		if (success && fatal)
			throw new ExecutorException("Cannot have fatal success.");

		return new ResultImpl(success, fatal, result, null);
	}

	/**
	 * Gets a {@link Result} object including performance indicators.
	 *
	 * @param success                     whether the execution completed successfully
	 * @param fatal                       whether the execution ran into a fatal error
	 * @param result                      execution's actual result
	 * @param totalCompileTimeMillis      total compilation time in milliseconds
	 * @param totalExecutionTimeMillis    total execution time in milliseconds
	 * @param testCaseExecutionTimeMillis current test case's execution time in milliseconds
	 *
	 * @return a {@link Result} object containing the information
	 *
	 * @throws ExecutorException if creation failed
	 */
	protected Result createResult(boolean success, boolean fatal, String result, double totalCompileTimeMillis, double totalExecutionTimeMillis, double testCaseExecutionTimeMillis) throws ExecutorException {

		if (success && fatal)
			throw new ExecutorException("Cannot have fatal success.");

		return new ResultImpl(success, fatal, result,
													new PerformanceIndicatorsImpl(totalCompileTimeMillis, totalExecutionTimeMillis, testCaseExecutionTimeMillis));
	}

	/**
	 * Parses the execution output and returns the results.
	 *
	 * @param output                   raw output of the execution
	 * @param totalCompileTimeMillis   total compilation time in milliseconds
	 * @param totalExecutionTimeMillis total execution time in milliseconds
	 *
	 * @return a {@link List} containing the result objects
	 *
	 * @throws ExecutorException if parsing failed
	 */
	protected List<Result> createResults(String output, double totalCompileTimeMillis, double totalExecutionTimeMillis) throws ExecutorException {

		List<Result> results = new ArrayList<>();
		try (Scanner scanner = new Scanner(output).useDelimiter(CodeExecutorBase.DOUBLE_NEWLINE_PATTERN)) {
			while (scanner.hasNext()) {
				String result = scanner.next();

				Matcher successMatcher = CodeExecutorBase.RESULT_SUCCESS_PATTERN.matcher(result);
				if (!successMatcher.lookingAt())
					throw new ExecutorException("Execution result block did not start properly.");

				boolean success = "OK".equalsIgnoreCase(successMatcher.group(1));
				int resultOffset = successMatcher.end();

				double testCaseExecutionTimeMillis = Double.NaN;
				Matcher executionTimeMatcher = CodeExecutorBase.RESULT_EXECUTION_TIME_PATTERN.matcher(result.substring(resultOffset));
				if (executionTimeMatcher.lookingAt()) {
					testCaseExecutionTimeMillis = Double.parseDouble(executionTimeMatcher.group(1));
					resultOffset += executionTimeMatcher.end();
				}

				results.add(this.createResult(success, false, result.substring(resultOffset),
																			totalCompileTimeMillis, totalExecutionTimeMillis, testCaseExecutionTimeMillis));
			}
		}

		return results;
	}
}
