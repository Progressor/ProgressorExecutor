package ch.bfh.progressor.executor.api;

/**
 * Represents the supported platforms (operating systems) of the executor.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public enum ExecutorPlatform {

	/**
	 * Windows platform.
	 * Supports Java, C++, C# and Kotlin.
	 */
	WINDOWS(false),

	/**
	 * Unix platform.
	 * Supports Java, C++, C# (Mono), Kotlin and Docker (containerisation).
	 */
	UNIX_LINUX(true),

	/**
	 * Unrecognised or unsupported platform.
	 */
	UNSUPPORTED(false);

	/**
	 * Name of the operating system.
	 */
	public static final String OPERATING_SYSTEM_NAME = System.getProperty("os.name");

	/**
	 * Version of the operating system.
	 */
	public static final String OPERATING_SYSTEM_VERSION = System.getProperty("os.version");

	/**
	 * Architecture of the operating system.
	 */
	public static final String OPERATING_SYSTEM_ARCHITECTURE = System.getProperty("os.arch");

	private final boolean dockerSupported;

	ExecutorPlatform(boolean dockerSupported) {

		this.dockerSupported = dockerSupported;
	}

	/**
	 * Determines the platform the executor runs on.
	 *
	 * @return platform the executor runs on
	 */
	public static ExecutorPlatform determine() {

		//source: http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/

		String os = ExecutorPlatform.OPERATING_SYSTEM_NAME.toLowerCase();

		if (os.contains("win"))
			return ExecutorPlatform.WINDOWS;

		else if (os.contains("nix") || os.contains("nux") || os.contains("aix"))
			return ExecutorPlatform.UNIX_LINUX;

			//else if (os.contains("sunos"))
			//	return ExecutorPlatform.UNSUPPORTED;

			//else if (os.contains("mac"))
			//	return ExecutorPlatform.UNSUPPORTED;

		else
			return ExecutorPlatform.UNSUPPORTED;
	}

	/**
	 * Whether the platform supports Docker containers.
	 *
	 * @return whether the platform supports Docker containers
	 */
	public boolean hasDockerSupport() {
		return this.dockerSupported;
	}
}
