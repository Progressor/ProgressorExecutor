package ch.bfh.progressor.executor.impl;

import ch.bfh.progressor.executor.api.VersionInformation;

/**
 * Read-only implementation of a {@link VersionInformation}.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class VersionInformationImpl implements VersionInformation {

	private final String languageVersion, compilerName, compilerVersion;

	/**
	 * Construct a new {@link VersionInformation}.
	 *
	 * @param languageVersion supported version of the programming language
	 * @param compilerName    name of the used compiler (or interpreter)
	 * @param compilerVersion version of the used compiler (or interpreter)
	 */
	public VersionInformationImpl(String languageVersion, String compilerName, String compilerVersion) {

		this.languageVersion = languageVersion;
		this.compilerName = compilerName;
		this.compilerVersion = compilerVersion;
	}

	@Override
	public String getLanguageVersion() {
		return this.languageVersion;
	}

	@Override
	public String getCompilerName() {
		return this.compilerName;
	}

	@Override
	public String getCompilerVersion() {
		return this.compilerVersion;
	}

	/**
	 * Converts a custom {@link VersionInformation} to a thrift {@link ch.bfh.progressor.executor.thrift.VersionInformation} instance.
	 *
	 * @param versionInformation   custom version information to convert
	 * @param platformName         name of the platform the executor runs on
	 * @param platformVersion      version of the platform the executor runs on
	 * @param platformArchitecture architecture of the platform the executor runs on
	 *
	 * @return thrift {@link ch.bfh.progressor.executor.thrift.VersionInformation} instance
	 */
	public static ch.bfh.progressor.executor.thrift.VersionInformation convertToThrift(VersionInformation versionInformation, String platformName, String platformVersion, String platformArchitecture) {

		return new ch.bfh.progressor.executor.thrift.VersionInformation(versionInformation.getLanguageVersion(),
																																		versionInformation.getCompilerName(), versionInformation.getCompilerVersion(),
																																		platformName, platformVersion, platformArchitecture);
	}
}
