package ch.bfh.progressor.executor.api;

/**
 * Represents the configuration of an Executor.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface Configuration {

	/**
	 * The default configuration to use.
	 */
	Configuration DEFAULT_CONFIGURATION = new Configuration() {

		@Override
		public boolean shouldUseDocker() {
			return ExecutorPlatform.determine().hasDockerSupport();
		}

		@Override
		public boolean shouldCleanUp() {
			return true;
		}
	};

	/**
	 * Gets whether to use Docker containers.
	 *
	 * @return whether to use Docker containers
	 */
	boolean shouldUseDocker();

	/**
	 * Gets whether to clean up temporary files.
	 *
	 * @return whether to clean up temporary files
	 */
	boolean shouldCleanUp();
}
