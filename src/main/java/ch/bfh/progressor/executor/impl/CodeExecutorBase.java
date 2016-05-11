package ch.bfh.progressor.executor.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.io.input.BOMInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.Result;

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
	 * Maximum time to use for for the Dockercontainer of the user to start (in seconds).
	 */
	public static final int CONTAINER_START_TIMEOUT = 3;

	/**
	 * Maximum time to use for the Dockercontainer of the user to stop (in seconds).
	 */
	public static final int CONTAINER_STOP_TIMEOUT = 3;

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

	private List<String> blacklist;
	private StringBuilder template;

	//********************************
	//*** INTERFACE IMPLEMENTATION ***
	//********************************

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
				throw new ExecutorException(true, "Could not read the blacklist.", ex);
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
				throw new ExecutorException(true, "Could not read the code template.", ex);
			}

		return new StringBuilder(this.template); //return a new string builder every time
	}

	//**********************
	//*** HELPER METHODS ***
	//**********************

	/**
	 * Concatenate several arrays.
	 *
	 * @param arrays arrays to concatenate
	 *
	 * @return a concatenated array
	 */
	protected <T> T[] concat(T[]... arrays) {

		if (arrays.length == 0) return (T[])new Object[0];

		int length = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
		T[] combined = Arrays.copyOf(arrays[0], length);

		for (int i = 1, position = arrays[0].length; i < arrays.length; position += arrays[i++].length)
			System.arraycopy(arrays[i], 0, combined, position, arrays[i].length);

		return combined;
	}

	/**
	 * @param dockerContainerName the ID of the started dockercontainer.
	 * @param arguments           additional arguments to pass to Docker
	 *
	 * @return the combined command line argument
	 */
	protected String[] getDockerCommandLine(String dockerContainerName, String... arguments) {

		return this.concat(new String[] { "docker", "exec", dockerContainerName }, arguments);

	}

	/**
	 * Gets the Docker command line to run a specific command for scriptlanguages.
	 * For example Kotlinscript, Python, etc.
	 *
	 * @param workingDirectory the working directory for Docker to run in
	 * @param arguments        additional arguments to pass to Docker
	 *
	 * @return the combined command line arguments
	 */
	protected String[] getDockerCommandLine(File workingDirectory, String... arguments) {
		return this.concat(new String[] { "docker", "run", "-v", String.format("%s:%sopt", workingDirectory.getAbsolutePath(), File.separator), CodeExecutorBase.DOCKER_IMAGE_NAME }, arguments);
	}

	private String[] startDockerCommandLine(File workingDirectory) {
		return this.concat(new String[] { "docker", "run", "-td", "-v", String.format("%s:%sopt", workingDirectory.getAbsolutePath(), File.separator), CodeExecutorBase.DOCKER_IMAGE_NAME });
	}

	private String[] dockerContainerStop(String containerID) {
		return this.concat(new String[] { "docker", "stop", containerID });
	}

	protected Process startDockerProcess(File codeDirectory) throws IOException {
		return new ProcessBuilder(startDockerCommandLine(codeDirectory)).redirectErrorStream(true).start();
	}

	protected Process stopDockerProcess(String containerID) throws IOException {
		return new ProcessBuilder(this.dockerContainerStop(containerID)).redirectErrorStream(true).start();
	}

	protected String getContainerID(Process process) {
		String containerID = null;
		try (Scanner out = new Scanner(this.getSafeReader(process.getInputStream())).useDelimiter(String.format("%n"))) {
			while (out.hasNext()) {
				containerID = out.next();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return containerID;
	}

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
	protected InputStreamReader getSafeReader(InputStream stream) throws IOException {

		BOMInputStream bom = new BOMInputStream(stream);
		return new InputStreamReader(bom, (bom.getBOM() != null ? Charset.forName(bom.getBOMCharsetName()) : CodeExecutorBase.CHARSET).newDecoder());
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
	protected String readConsole(Process process) throws ExecutorException {

		final String newLine = String.format("%n");

		try (BufferedReader reader = new BufferedReader(this.getSafeReader(process.getInputStream()))) {
			StringBuilder sb = new StringBuilder();

			String line; //read every line
			while ((line = reader.readLine()) != null)
				sb.append(line).append(newLine);

			return sb.toString(); //create concatenated string

		} catch (IOException ex) {
			throw new ExecutorException(false, "Could not read the console output.", ex);
		}
	}

	/**
	 * Constructs a result object.
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

		return new ResultImpl(success, fatal, result, new PerformanceIndicatorsImpl(runtimeMilliseconds));
	}

}


