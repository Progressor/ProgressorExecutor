package ch.bfh.progressor.executor.impl;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.VersionInformation;

/**
 * Base class for Docker-enabled code execution engines.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public abstract class CodeExecutorDockerBase extends CodeExecutorBase {

	private static final Logger LOGGER = Logger.getLogger(CodeExecutorDockerBase.class.getName());

	private static final String DOCKER_IMAGE_NAME = String.format("progressor%sexecutor", File.separator);
	private static final ThreadLocal<String> DOCKER_CONTAINER_ID = new ThreadLocal<>();

	//***************************
	//*** CODE EXECUTOR LOGIC ***
	//***************************

	@Override
	public final VersionInformation getVersionInformation() throws ExecutorException {

		try {
			if (this.shouldUseDocker())
				try {
					this.startDocker(CodeExecutorBase.CURRENT_DIRECTORY);
				} catch (Exception ex) {
					CodeExecutorDockerBase.LOGGER.log(Level.SEVERE, "Could not start Docker (for version information).", ex);
				}

			return this.fetchVersionInformation();

		} catch (Exception ex) {
			throw new ExecutorException("Could not fetch version information.", ex);

		} finally {
			if (this.willUseDocker())
				try {
					this.stopDocker(CodeExecutorBase.CURRENT_DIRECTORY);
				} catch (Exception ex) {
					CodeExecutorDockerBase.LOGGER.log(Level.SEVERE, "Could not stop Docker (for version information).", ex);
				}
		}
	}

	/**
	 * Fetches the version information for the language the executor supports.
	 *
	 * @return version information for the supported language
	 */
	protected abstract VersionInformation fetchVersionInformation() throws ExecutorException;

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
					CodeExecutorDockerBase.LOGGER.log(Level.SEVERE, "Could not start Docker.", ex);
				}

			return this.executeTestCases(codeFragment, testCases, codeDirectory);

		} catch (Exception ex) {
			StringBuilder sb = new StringBuilder("Could not invoke the user code.").append(CodeExecutorBase.NEWLINE);
			Throwable throwable = ex;
			do sb.append(throwable).append(CodeExecutorBase.NEWLINE);
			while ((throwable = throwable.getCause()) != null);

			try {
				return Collections.nCopies(testCases.size(), this.createResult(false, true, sb.toString()));

			} catch (ExecutorException ex2) {
				throw new RuntimeException("Could not invoke the user code.", ex);
			}

		} finally {
			if (this.willUseDocker())
				try {
					this.stopDocker(codeDirectory);
				} catch (Exception ex) {
					CodeExecutorDockerBase.LOGGER.log(Level.SEVERE, "Could not stop Docker.", ex);
				}

			if (codeDirectory.exists())
				if (!this.tryDeleteRecursive(codeDirectory))
					CodeExecutorDockerBase.LOGGER.warning("Could not delete temporary folder.");
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

	//**********************
	//*** SYSTEM HELPERS ***
	//**********************

	private boolean shouldUseDocker() {
		return CodeExecutorBase.PLATFORM.hasDockerSupport() && this.getConfiguration().shouldUseDocker();
	}

	/**
	 * Gets whether or not to use Docker.
	 *
	 * @return whether or not to use Docker
	 */
	protected final boolean willUseDocker() {
		return this.shouldUseDocker() && CodeExecutorDockerBase.DOCKER_CONTAINER_ID.get() != null;
	}

	private void startDocker(File directory) throws ExecutorException {

		String output = super.executeSafeCommand(directory, "docker", "run", "-td", "-v", String.format("%s:%sopt", directory.getAbsolutePath(), File.separator), CodeExecutorDockerBase.DOCKER_IMAGE_NAME);

		try (Scanner scanner = new Scanner(output)) {
			if (scanner.hasNextLine())
				CodeExecutorDockerBase.DOCKER_CONTAINER_ID.set(scanner.nextLine());
			else
				throw new ExecutorException("Could not read identifier of created docker container.");
		}
	}

	private String[] getDockerCommand(String... command) {

		return this.willUseDocker() ? this.concatenateArrays(new String[] { "docker", "exec", CodeExecutorDockerBase.DOCKER_CONTAINER_ID.get() }, command)
																: command;
	}

	@Override
	protected String executeCommand(File directory, String... command) throws ExecutorException {

		return super.executeCommand(directory, this.getDockerCommand(command));
	}

	@Override
	protected String executeDeferredCommand(File directory, String... command) throws ExecutorException {

		return super.executeDeferredCommand(directory, this.getDockerCommand(command));
	}

	@Override
	protected String executeSafeCommand(File directory, String... command) throws ExecutorException {

		return super.executeSafeCommand(directory, this.getDockerCommand(command));
	}

	private void stopDocker(File directory) throws ExecutorException {

		super.executeSafeCommand(directory, "docker", "stop", CodeExecutorDockerBase.DOCKER_CONTAINER_ID.get());
		super.executeSafeCommand(directory, "docker", "rm", CodeExecutorDockerBase.DOCKER_CONTAINER_ID.get());
		CodeExecutorDockerBase.DOCKER_CONTAINER_ID.set(null);
	}
}
