package ch.bfh.progressor.executor.impl;

import ch.bfh.progressor.executor.api.Configuration;

/**
 * Read-only implementation of a {@link Configuration} object.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class ConfigurationImpl implements Configuration {

	private final boolean useDocker;

	/**
	 * Constructs a new {@link Configuration} object.
	 *
	 * @param useDocker whether or not to use Docker containers
	 */
	public ConfigurationImpl(boolean useDocker) {

		this.useDocker = useDocker;
	}

	@Override
	public boolean shouldUseDocker() {
		return this.useDocker;
	}
}
