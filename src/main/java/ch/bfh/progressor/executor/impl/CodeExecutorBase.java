package ch.bfh.progressor.executor.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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

	private static final Logger LOGGER = Logger.getLogger(CodeExecutorBase.class.getName());

	private static final String CODE_CUSTOM_FRAGMENT = "$CustomCode$";
	private static final String TEST_CASES_FRAGMENT = "$TestCases$";

	private static final String DOCKER_IMAGE_NAME = String.format("progressor%sexecutor", File.separator);
	private static final ThreadLocal<String> DOCKER_CONTAINER_ID = new ThreadLocal<>();

	private static final int BUFFER_SIZE = 1024;
	private static final ByteOrderMark[] BYTE_ORDER_MARKS = { ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE };

	private static final long MAX_JOIN_TIMEOUT_MILLIS = 125;
	private static final long MAX_BUFFER_TIMEOUT_MILLIS = 1500;
	private static final long MAX_TOTAL_TIMEOUT_MILLIS = CodeExecutorBase.MAX_JOIN_TIMEOUT_MILLIS * 15;

	private Configuration configuration = Configuration.DEFAULT_CONFIGURATION;
	private Set<String> blacklist;
	private StringBuilder template;

	//***************************
	//*** CODE EXECUTOR LOGIC ***
	//***************************

	@Override
	public void setConfiguration(Configuration configuration) {

		this.configuration = configuration;
	}

	@Override
	public final VersionInformation getVersionInformation() throws ExecutorException {

		try {
			if (this.shouldUseDocker())
				try {
					this.startDocker(CodeExecutorBase.CURRENT_DIRECTORY);
				} catch (Exception ex) {
					CodeExecutorBase.LOGGER.log(Level.SEVERE, "Could not start Docker (for version information).", ex);
				}

			return this.fetchVersionInformation();

		} catch (Exception ex) {
			throw new ExecutorException("Could not fetch version information.", ex);

		} finally {
			if (this.willUseDocker())
				try {
					this.stopDocker(CodeExecutorBase.CURRENT_DIRECTORY);
				} catch (Exception ex) {
					CodeExecutorBase.LOGGER.log(Level.SEVERE, "Could not stop Docker (for version information).", ex);
				}
		}
	}

	/**
	 * Fetches the version information for the language the executor supports.
	 *
	 * @return version information for the supported language
	 */
	protected abstract VersionInformation fetchVersionInformation() throws ExecutorException;

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
	protected String getTemplatePath() {
		return String.format("%s/template.txt", this.getLanguage());
	}

	/**
	 * Gets the code template for this language.
	 *
	 * @return code template for this language
	 *
	 * @throws ExecutorException if the code template could not be read
	 */
	protected synchronized StringBuilder getTemplate() throws ExecutorException {

		final String newLine = String.format("%n");

		if (this.template == null)
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(this.getTemplatePath()), CodeExecutorBase.CHARSET))) {
				this.template = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) //read template to StringBuilder
					this.template.append(line).append(newLine);

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
	public final List<Result> execute(String codeFragment, List<TestCase> testCases) {

		final File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory

		try {
			if (!codeDirectory.exists() && !codeDirectory.mkdirs())
				throw new ExecutorException("Could not create a temporary directory for the user code.");

			if (this.shouldUseDocker())
				try {
					this.startDocker(codeDirectory);
				} catch (Exception ex) {
					CodeExecutorBase.LOGGER.log(Level.SEVERE, "Could not start Docker.", ex);
				}

			return this.executeTestCases(codeFragment, testCases, codeDirectory);

		} catch (Exception ex) {
			String newLine = String.format("%n");

			StringBuilder sb = new StringBuilder("Could not invoke the user code.").append(newLine);
			Throwable throwable = ex;
			do sb.append(throwable).append(newLine);
			while ((throwable = throwable.getCause()) != null);

			return Collections.nCopies(testCases.size(), new ResultImpl(false, true, sb.toString()));

		} finally {
			if (this.willUseDocker())
				try {
					this.stopDocker(codeDirectory);
				} catch (Exception ex) {
					CodeExecutorBase.LOGGER.log(Level.SEVERE, "Could not stop Docker.", ex);
				}

			if (codeDirectory.exists())
				if (!this.tryDeleteRecursive(codeDirectory))
					CodeExecutorBase.LOGGER.warning("Could not delete temporary folder.");
		}
	}

	/**
	 * Executes a provided code fragment.
	 *
	 * @param codeFragment  code fragment to execute
	 * @param testCases     test cases to execute
	 * @param codeDirectory directory to place code file in
	 *
	 * @return a {@link List} containing the {@link Result} for each test case
	 *
	 * @throws ExecutorException if the execution failed
	 */
	protected abstract List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException;

	/**
	 * Generates the code file with the user's code fragment.
	 *
	 * @param codeFile     file to generate
	 * @param codeFragment code fragment to write into the file
	 * @param testCases    test cases to generate tests for
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected final void generateCodeFile(File codeFile, String codeFragment, List<TestCase> testCases) throws ExecutorException {

		try {
			StringBuilder code = this.getTemplate(); //read the template

			int fragStart = code.indexOf(CodeExecutorBase.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + CodeExecutorBase.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			int caseStart = code.indexOf(CodeExecutorBase.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
			code.replace(caseStart, caseStart + CodeExecutorBase.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(testCases));

			Files.write(codeFile.toPath(), code.toString().getBytes(CodeExecutorBase.CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
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

	private boolean tryDeleteRecursive(File file) {

		boolean ret = true;

		File[] children; //recursively delete children
		if (file.isDirectory() && (children = file.listFiles()) != null)
			for (File child : children)
				ret &= this.tryDeleteRecursive(child);

		ret &= file.delete(); //delete file itself
		return ret;
	}

	private <T> T[] concat(T[]... arrays) {

		if (arrays.length == 0) return (T[])new Object[0];

		int length = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
		T[] combined = Arrays.copyOf(arrays[0], length);

		for (int i = 1, position = arrays[0].length; i < arrays.length; position += arrays[i++].length)
			System.arraycopy(arrays[i], 0, combined, position, arrays[i].length);

		return combined;
	}

	private String executeSystemCommand(File directory, String... command) throws ExecutorException {

		Process process = null;
		try {
			process = new ProcessBuilder(command).directory(directory).redirectErrorStream(true).start();
			StringBuilder stringBuilder = new StringBuilder();

			long start = System.currentTimeMillis();

			try (BOMInputStream inputStream = new BOMInputStream(process.getInputStream(), CodeExecutorBase.BYTE_ORDER_MARKS);
					 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, (inputStream.hasBOM() ? Charset.forName(inputStream.getBOMCharsetName()) : CodeExecutorBase.CHARSET).newDecoder()))) {
				char[] charBuffer = new char[CodeExecutorBase.BUFFER_SIZE];

				long currentTimeMillis = System.currentTimeMillis();
				long maxBufferTimeMillis = currentTimeMillis + CodeExecutorBase.MAX_BUFFER_TIMEOUT_MILLIS;
				long maxTotalTimeMillis = currentTimeMillis + CodeExecutorBase.MAX_TOTAL_TIMEOUT_MILLIS;
				while ((currentTimeMillis = System.currentTimeMillis()) < maxBufferTimeMillis && currentTimeMillis < maxTotalTimeMillis) {

					int readResult = bufferedReader.ready() ? bufferedReader.read(charBuffer, 0, charBuffer.length) : 0;
					if (readResult > 0) {
						stringBuilder.append(charBuffer, 0, readResult);
						maxBufferTimeMillis = currentTimeMillis + CodeExecutorBase.MAX_BUFFER_TIMEOUT_MILLIS;

					} else if (readResult < 0)
						break;
				}
			}

			long duration = System.currentTimeMillis() - start;

			if (process.waitFor(CodeExecutorBase.MAX_JOIN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
				if (process.exitValue() == 0)
					return stringBuilder.toString();
				else
					throw new ExecutorException("Could not successfully execute command.", stringBuilder.toString());

			else
				throw new ExecutorException("Could not execute command in time.");

		} catch (IOException ex) {
			throw new ExecutorException("Could not execute command.", ex);

		} catch (InterruptedException ex) {
			throw new ExecutorException("Could not wait for command to execute in time.", ex);

		} finally {
			if (process != null && process.isAlive())
				process.destroyForcibly();
		}
	}

	private boolean shouldUseDocker() {
		return CodeExecutorBase.PLATFORM.hasDockerSupport() && this.configuration.shouldUseDocker();
	}

	/**
	 * Gets whether or not to use Docker.
	 *
	 * @return whether or not to use Docker
	 */
	protected final boolean willUseDocker() {
		return this.shouldUseDocker() && CodeExecutorBase.DOCKER_CONTAINER_ID.get() != null;
	}

	private void startDocker(File directory) throws ExecutorException {

		final String newLine = String.format("%n");

		String output = this.executeSystemCommand(directory, "docker", "run", "-td", "-v", String.format("%s:%sopt", directory.getAbsolutePath(), File.separator), CodeExecutorBase.DOCKER_IMAGE_NAME);

		try (Scanner scanner = new Scanner(output)) {
			if (scanner.hasNextLine())
				CodeExecutorBase.DOCKER_CONTAINER_ID.set(scanner.nextLine());
			else
				throw new ExecutorException("Could not read identifier of created docker container.");
		}
	}

	/**
	 * Executes a system command.
	 *
	 * @param directory the working directory for the command
	 * @param command   command to execute
	 *
	 * @return the output of the command
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 */
	protected String executeCommand(File directory, String... command) throws ExecutorException {

		return this.executeSystemCommand(directory, this.willUseDocker() ? this.concat(new String[] { "docker", "exec", CodeExecutorBase.DOCKER_CONTAINER_ID.get() }, command) : command);
	}

	private void stopDocker(File directory) throws ExecutorException {

		this.executeSystemCommand(directory, "docker", "stop", CodeExecutorBase.DOCKER_CONTAINER_ID.get());
		this.executeSystemCommand(directory, "docker", "rm", CodeExecutorBase.DOCKER_CONTAINER_ID.get());
		CodeExecutorBase.DOCKER_CONTAINER_ID.set(null);
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
	 * Parses the execution output and returns the results.
	 *
	 * @param output             raw output of the execution
	 * @param totalCompileTime   total compilation time
	 * @param totalExecutionTime total execution time
	 * @param timeUnit           the unit of the compilation and execution times
	 *
	 * @return a {@link List} containing the result objects
	 */
	protected List<Result> createResults(String output, long totalCompileTime, long totalExecutionTime, TimeUnit timeUnit) {

		final Pattern doubleNewlinePattern = Pattern.compile("(\\r\\n|\\r|\\n){2}");
		final Pattern resultSuccessPattern = Pattern.compile("(OK|ER):", Pattern.CASE_INSENSITIVE);
		final Pattern resultExecutionTimePattern = Pattern.compile("(\\d+(\\.\\d+|)):", Pattern.CASE_INSENSITIVE);

		List<Result> results = new ArrayList<>();
		try (Scanner scanner = new Scanner(output).useDelimiter(doubleNewlinePattern)) {
			while (scanner.hasNext()) {
				String result = scanner.next();
				int resultOffset = 0;

				boolean success = false;
				Matcher successMatcher = resultSuccessPattern.matcher(result);
				if (successMatcher.lookingAt()) {
					success = "OK".equalsIgnoreCase(successMatcher.group(1));
					resultOffset = successMatcher.end();
				}

				double executionTime = Double.NaN;
				Matcher executionTimeMatcher = resultExecutionTimePattern.matcher(result);
				if (executionTimeMatcher.lookingAt()) {
					executionTime = Double.parseDouble(executionTimeMatcher.group(1));
					resultOffset = executionTimeMatcher.end();
				}

				results.add(new ResultImpl(success, false, result.substring(resultOffset),
																	 new PerformanceIndicatorsImpl(totalCompileTime > 0 ? timeUnit.toMillis(totalCompileTime) : Double.NaN,
																																 timeUnit.toMillis(totalExecutionTime),
																																 executionTime)));
			}
		}

		return results;
	}
}
