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
	};

	/**
	 * Gets whether or not to use Docker containers.
	 *
	 * @return whether or not to use Docker containers
	 */
	boolean shouldUseDocker();
}
