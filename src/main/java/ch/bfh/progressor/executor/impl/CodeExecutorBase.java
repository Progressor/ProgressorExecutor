package ch.bfh.progressor.executor.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.io.input.BOMInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;

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
	 * Placeholder for the custom code fragment as defined in the template.
	 */
	protected static final String CODE_CUSTOM_FRAGMENT = "$CustomCode$";

	/**
	 * Placeholder for the test cases as defined in the template.
	 */
	protected static final String TEST_CASES_FRAGMENT = "$TestCases$";

	/**
	 * The platform the executor runs on.
	 */
	protected static final ExecutorPlatform PLATFORM = ExecutorPlatform.determine();

	/**
	 * Whether or not to use Docker containers.
	 */
	protected static final boolean USE_DOCKER = true;

	/**
	 * Name of the Docker container to use.
	 */
	protected static final String DOCKER_IMAGE_NAME = String.format("progressor%sexecutor", File.separator);

	/**
	 * Maximum time to use for for the Docker container to start (in seconds).
	 */
	public static final int DOCKER_CONTAINER_START_TIMEOUT = 3;

	/**
	 * Maximum time to use for the Docker container to stop (in seconds).
	 */
	public static final int DOCKER_CONTAINER_STOP_TIMEOUT = 3;

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

	private List<String> blacklist;
	private StringBuilder template;
	private String dockerContainerId;

	//***************************
	//*** CODE EXECUTOR LOGIC ***
	//***************************

	/**
	 * Gets the path to the blacklist file.
	 *
	 * @return default path to the blacklist file
	 */
	protected String getBlackListPath() {
		return String.format("%s%sblacklist.json", this.getLanguage(), File.separator);
	}

	@Override
	public Collection<String> getBlacklist() throws ExecutorException {

		if (this.blacklist == null)
			try (InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream(this.getBlackListPath()), CodeExecutorBase.CHARSET)) {
				this.blacklist = new ArrayList<>();
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

		return Collections.unmodifiableList(this.blacklist);
	}

	/**
	 * Gets the path to the template file.
	 *
	 * @return default path to the template file
	 */
	protected String getTemplatePath() {
		return String.format("%s%stemplate.txt", this.getLanguage(), File.separator);
	}

	/**
	 * Gets the code template for this language.
	 *
	 * @return code template for this language
	 *
	 * @throws ExecutorException if the code template could not be read
	 */
	protected StringBuilder getTemplate() throws ExecutorException {

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
	public List<Result> execute(String codeFragment, List<TestCase> testCases) {

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

			return Collections.nCopies(testCases.size(), this.getResult(false, true, sb.toString()));

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
	protected void generateCodeFile(File codeFile, String codeFragment, List<TestCase> testCases) throws ExecutorException {

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

	private <T> T[] concat(T[]... arrays) {

		if (arrays.length == 0) return (T[])new Object[0];

		int length = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
		T[] combined = Arrays.copyOf(arrays[0], length);

		for (int i = 1, position = arrays[0].length; i < arrays.length; position += arrays[i++].length)
			System.arraycopy(arrays[i], 0, combined, position, arrays[i].length);

		return combined;
	}

	private Process executeSystemCommand(File directory, long timeoutSeconds, String... command) throws ExecutorException {

		try {
			Process process = new ProcessBuilder(command).directory(directory).redirectErrorStream(true).start();
			if (process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
				if (process.exitValue() == 0)
					return process;
				else
					throw new ExecutorException("Could not successfully execute command.", this.readAll(process));

			} else {
				process.destroyForcibly(); //destroy()
				throw new ExecutorException("Could not execute command in time.");
			}

		} catch (IOException ex) {
			throw new ExecutorException("Could not execute command.", ex);

		} catch (InterruptedException ex) {
			throw new ExecutorException("Could not wait for command to execute in time.", ex);
		}
	}

	/**
	 * Gets whether or not to use Docker.
	 *
	 * @return whether or not to use Docker
	 */
	private boolean shouldUseDocker() {
		return CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER;
	}

	/**
	 * Gets whether or not to use Docker.
	 *
	 * @return whether or not to use Docker
	 */
	protected boolean willUseDocker() {
		return this.shouldUseDocker() && this.dockerContainerId != null;
	}

	/**
	 * Starts a Docker container and remembers its identifier for futher use.
	 *
	 * @param directory the working directory for Docker to run in
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 */
	private void startDocker(File directory) throws ExecutorException {

		this.dockerContainerId = this.readFirstLine(this.executeSystemCommand(directory, CodeExecutorBase.DOCKER_CONTAINER_START_TIMEOUT,
																																					"docker", "run", "-td", "-v", String.format("%s:%sopt", directory.getAbsolutePath(), File.separator), CodeExecutorBase.DOCKER_IMAGE_NAME));
	}

	/**
	 * Executes a system command.
	 *
	 * @param directory      the working directory for the command
	 * @param timeoutSeconds time ot wait for the command to execute (in seconds)
	 * @param command        command to execute
	 *
	 * @return the {@link Process} the command was executed in
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 */
	protected Process executeCommand(File directory, long timeoutSeconds, String... command) throws ExecutorException {

		return this.executeSystemCommand(directory, timeoutSeconds, this.willUseDocker()
																																? this.concat(new String[] { "docker", "exec", this.dockerContainerId }, command)
																																: command);
	}

	/**
	 * Stops and deletes the Docker container.
	 *
	 * @param directory the working directory for Docker to run in
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 */
	private void stopDocker(File directory) throws ExecutorException {

		this.executeSystemCommand(directory, CodeExecutorBase.DOCKER_CONTAINER_STOP_TIMEOUT, "docker", "stop", this.dockerContainerId);
		this.executeSystemCommand(directory, CodeExecutorBase.DOCKER_CONTAINER_STOP_TIMEOUT, "docker", "rm", this.dockerContainerId);
		this.dockerContainerId = null;
	}

	//******************************
	//*** INPUT / OUTPUT HELPERS ***
	//******************************

	/**
	 * Recursively deletes a directory and all its sub-directories and files.
	 *
	 * @param file directory (or file) to delete
	 *
	 * @return whether or not the directory was successfully deleted
	 */
	protected boolean tryDeleteRecursive(File file) {

		boolean ret = true;

		File[] children; //recursively delete children
		if (file.isDirectory() && (children = file.listFiles()) != null)
			for (File child : children)
				ret &= this.tryDeleteRecursive(child);

		ret &= file.delete(); //delete file itself
		return ret;
	}

	/**
	 * Creates a safe {@link InputStreamReader} for a specific {@link InputStream}.
	 *
	 * @param stream input stream to create reader for
	 *
	 * @return a safe reader for a specific input stream
	 *
	 * @throws IOException if no safe reader could be created for the input stream
	 */
	private InputStreamReader getSafeReader(InputStream stream) throws IOException {

		BOMInputStream bom = new BOMInputStream(stream);
		return new InputStreamReader(bom, (bom.getBOM() != null ? Charset.forName(bom.getBOMCharsetName()) : CodeExecutorBase.CHARSET).newDecoder());
	}

	/**
	 * Reads the first line of the console output of a specified process. <br>
	 * Note that the process' error stream needs to be redirected to read error output as well
	 * (e.g. using {@link ProcessBuilder#redirectErrorStream(boolean)}).
	 *
	 * @param process process to read console output of
	 *
	 * @return first line of the console output of a specified process
	 *
	 * @throws ExecutorException if the console output could not be read
	 */
	protected String readFirstLine(Process process) throws ExecutorException {

		try (BufferedReader reader = new BufferedReader(this.getSafeReader(process.getInputStream()))) {
			return reader.readLine();

		} catch (IOException ex) {
			throw new ExecutorException("Could not read console output.", ex);
		}
	}

	/**
	 * Reads the complete console output of a specified process. <br>
	 * Note that the process' error stream needs to be redirected to read error output as well
	 * (e.g. using {@link ProcessBuilder#redirectErrorStream(boolean)}).
	 *
	 * @param process process to read console output of
	 *
	 * @return complete console output of a specified process
	 *
	 * @throws ExecutorException if the console output could not be read
	 */
	protected String readAll(Process process) throws ExecutorException {

		try (Scanner scanner = new Scanner(this.getSafeReader(process.getInputStream())).useDelimiter("\\Z")) {
			if (scanner.hasNext())
				return scanner.next();

		} catch (IOException ex) {
			throw new ExecutorException("Could not read console output.", ex);
		}

		return null;
	}

	/**
	 * Reads the complete console output of a specified process. <br>
	 * Note that the process' error stream needs to be redirected to read error output as well
	 * (e.g. using {@link ProcessBuilder#redirectErrorStream(boolean)}).
	 *
	 * @param process   process to read console output of
	 * @param delimiter delimiter to use to split console output
	 *
	 * @return {@link List} of delimited parts of the console output read from the specified process
	 *
	 * @throws ExecutorException if the console output could not be read
	 */
	protected List<String> readDelimited(Process process, String delimiter) throws ExecutorException {

		List<String> parts = new ArrayList<>();
		try (Scanner scanner = new Scanner(this.getSafeReader(process.getInputStream())).useDelimiter(delimiter)) {
			while (scanner.hasNext())
				parts.add(scanner.next());

		} catch (IOException ex) {
			throw new ExecutorException("Could not read console output.", ex);
		}

		return parts;
	}

	//*****************************
	//*** MISCELLANEOUS HELPERS ***
	//*****************************

	/**
	 * Constructs a result object.
	 *
	 * @param success whether or not the execution was a success
	 * @param fatal   whether or not a fatal error occurred
	 * @param result  the actual result
	 *
	 * @return a result object with the specified information
	 */
	protected Result getResult(boolean success, boolean fatal, String result) {

		if (success && fatal)
			throw new IllegalArgumentException("Cannot be a fatal success.");

		return new ResultImpl(success, fatal, result, null);
	}

	/**
	 * Constructs a result object including performance indicators.
	 *
	 * @param success             whether or not the execution was a success
	 * @param fatal               whether or not a fatal error occurred
	 * @param result              the actual result
	 * @param runtimeMilliseconds the runtime of the execution in milliseconds
	 *
	 * @return a result object with the specified information
	 */
	protected Result getResult(boolean success, boolean fatal, String result, double runtimeMilliseconds) {

		if (success && fatal)
			throw new IllegalArgumentException("Cannot be a fatal success.");

		return new ResultImpl(success, fatal, result,
													new PerformanceIndicatorsImpl(runtimeMilliseconds));
	}
}
