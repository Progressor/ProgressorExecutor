package ch.bfh.progressor.executor.api;

/**
 * Represents the version information of a supported programming language.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface VersionInformation {

	/**
	 * Gets the supported version of the programming language.
	 *
	 * @return supported version of the programming language
	 */
	String getLanguageVersion();

	/**
	 * Gets the name of the programming language's used compiler (or interpreter).
	 *
	 * @return name of the used compiler (or interpreter)
	 */
	String getCompilerName();

	/**
	 * Gets the version of the programming language's used compiler (or interpreter).
	 *
	 * @return version of the used compiler (or interpreter)
	 */
	String getCompilerVersion();
}
