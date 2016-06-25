package ch.bfh.progressor.executor.impl;

import ch.bfh.progressor.executor.api.Configuration;

/**
 * Read-only implementation of a {@link Configuration} object.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class ConfigurationImpl implements Configuration {

	private final boolean useDocker, cleanUp;

	/**
	 * Constructs a new {@link Configuration} object.
	 *
	 * @param useDocker whether to use Docker containers
	 * @param cleanUp   whether to clean up temporary files
	 */
	public ConfigurationImpl(boolean useDocker, boolean cleanUp) {

		this.useDocker = useDocker;
		this.cleanUp = cleanUp;
	}

	@Override
	public boolean shouldUseDocker() {
		return this.useDocker;
	}

	@Override
	public boolean shouldCleanUp() {
		return this.cleanUp;
	}
}
