package ch.bfh.progressor.executor;

/**
 * Represents the supported platforms (operating systems) of the executor.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public enum ExecutorPlatform {

	/**
	 * WINDOWS platform.
	 * Supports Java, C/C++, C# and Kotlin.
	 */
	WINDOWS,

	/**
	 * LINUX platform.
	 * Supports Java, C/C++, C# (Mono), Kotlin and Docker (containerisation).
	 */
	LINUX;

	/**
	 * Determines the platform the executor runs on.
	 *
	 * @return platform the executor runs on
	 */
	public static ExecutorPlatform determine() {

		return System.getProperty("os.name").substring(0, 3).equalsIgnoreCase("Win") ? ExecutorPlatform.WINDOWS : ExecutorPlatform.LINUX;
	}
}
