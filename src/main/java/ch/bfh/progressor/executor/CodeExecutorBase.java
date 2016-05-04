package ch.bfh.progressor.executor;

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
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.io.input.BOMInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Base class with helper methods for code execution engines.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public abstract class CodeExecutorBase implements CodeExecutor {

	/**
	 * Character set to use for general operations.
	 */
	protected static final Charset CHARSET = Charset.forName("UTF-8");

	/**
	 * Placeholder for the custom code fragment as defined in the template.
	 */
	protected static final String CODE_CUSTOM_FRAGMENT = "$CustomCode$";

	/**
	 * Placeholder for the test cases as defined in the template.
	 */
	protected static final String TEST_CASES_FRAGMENT = "$TestCases$";

	/**
	 * Whether or not to use Docker containers to execute the code fragments.
	 */
	protected static boolean USE_DOCKER = false;

	/**
	 * Name of the Docker container (on LINUX only).
	 */
	protected static final String DOCKER_CONTAINER_NAME = String.format("progressor%sexecutor", File.separator);

	/**
	 * Regular expression pattern for parameter separation.
	 */
	protected static final Pattern PARAMETER_SEPARATOR_PATTERN = Pattern.compile(",\\s*");

	/**
	 * Regular expression pattern for key-value pair separation.
	 */
	protected static final Pattern KEY_VALUE_SEPARATOR_PATTERN = Pattern.compile(":\\s*");

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

	/**
	 * Gets the combined command line to run a specific command.
	 *
	 * @param prepend the first few arguments
	 * @param append  the last few arguments
	 *
	 * @return the combined command line arguments
	 */
	protected String[] getCombinedCommandLine(String[] prepend, String... append) {

		String[] combined = Arrays.copyOf(prepend, prepend.length + append.length);
		System.arraycopy(append, 0, combined, prepend.length, append.length);
		return combined;
	}

	/**
	 * Gets the cmd.exe command line to run a specific command.
	 *
	 * @param arguments additional arguments to pass to Docker
	 *
	 * @return the combined command line arguments
	 */
	protected String[] getCmdCommandLine(String... arguments) {

		return this.getCombinedCommandLine(new String[] { "cmd", "/C" }, arguments);
	}

	/**
	 * Gets the Docker command line to run a specific command.
	 *
	 * @param workingDirectory the working directory for Docker to run in
	 * @param arguments        additional arguments to pass to Docker
	 *
	 * @return the combined command line arguments
	 */
	protected String[] getDockerCommandLine(File workingDirectory, String... arguments) {

		return this.getCombinedCommandLine(new String[] { "docker", "run", "-v", String.format("%s:%sopt", workingDirectory.getAbsolutePath(), File.separator), CodeExecutorBase.DOCKER_CONTAINER_NAME }, arguments);
	}

	/**
	 * Recursively deletes a directory and all its sub-directories and files.
	 *
	 * @param file directory (or file) to delete
	 *
	 * @return whether or not the directory was successfully deleted
	 */
	protected boolean deleteRecursive(File file) {

		boolean ret = true;

		File[] children; //recursively delete children
		if (file.isDirectory() && (children = file.listFiles()) != null)
			for (File child : children)
				ret &= this.deleteRecursive(child);

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
}
