package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;
import ch.bfh.progressor.executor.api.VersionInformation;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.impl.CodeExecutorDockerBase;

/**
 * Code execution engine for Kotlin code. <br>
 * Compiles and executes the Kotlin code in two steps.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class KotlinExecutor extends CodeExecutorDockerBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "kotlin";

	/**
	 * Name of the Kotlin (Java) main class.
	 */
	protected static final String CODE_CLASS_NAME = "Program";

	/**
	 * Regular expression pattern for extracting the language/compiler version.
	 */
	protected static final Pattern VERSION_PATTERN = Pattern.compile("\\d[\\d\\.-]*");

	@Override
	public String getLanguage() {
		return KotlinExecutor.CODE_LANGUAGE;
	}

	@Override
	public VersionInformation fetchVersionInformation() throws ExecutorException {

		String version = null;

		String versionOutput = this.executeSafeCommand(CodeExecutorBase.CURRENT_DIRECTORY, CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlinc.bat" : "kotlinc", "-version");
		Matcher versionMatcher = KotlinExecutor.VERSION_PATTERN.matcher(versionOutput);
		if (versionMatcher.find())
			version = versionMatcher.group();

		return this.createVersionInformation(version, "kotlinc", version);
	}

	@Override
	protected String getTemplatePath() {
		return String.format("%s/template.kt", this.getLanguage());
	}

	@Override
	protected List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException {

		final File codeFile = new File(codeDirectory, String.format("%s.kt", KotlinExecutor.CODE_CLASS_NAME));

		//*********************
		//*** GENERATE CODE ***
		//*********************
		this.generateCodeFile(codeFile, codeFragment, testCases);

		//********************
		//*** COMPILE CODE ***
		//********************
		final long compilationStart = System.nanoTime();

		try {
			//this.executeSafeCommand(codeDirectory, CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlinc.bat" : "kotlinc", codeFile.getName());
			this.simulateKotlinCompilerScript(true, codeDirectory, codeFile.getName());
		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not compile the user code.", ex);
		}

		final long compilationEnd = System.nanoTime();

		//********************
		//*** EXECUTE CODE ***
		//********************
		final long executionStart = System.nanoTime();

		String executionOutput;
		try {
			//executionOutput = this.executeCommand(codeDirectory, CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlin.bat" : "kotlin", KotlinExecutor.CODE_CLASS_NAME);
			executionOutput = this.simulateKotlinScript(false, codeDirectory, KotlinExecutor.CODE_CLASS_NAME);

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not execute the user code.", ex);
		}

		final long executionEnd = System.nanoTime();

		//****************************
		//*** TEST CASE EVALUATION ***
		//****************************
		return this.createResults(executionOutput,
															(compilationEnd - compilationStart) / CodeExecutorBase.MILLIS_IN_NANO,
															(executionEnd - executionStart) / CodeExecutorBase.MILLIS_IN_NANO);
	}

	/////////////////////////////////////////////////////////////////
	//              Kotlin script (bat/sh) simulations             //
	/////////////////////////////////////////////////////////////////
	// these simulations are only needed until Java 9 is published //
	//      the scripts start Java in a separate child process     //
	//       Java 8 cannot determine and kill child processes      //
	/////////////////////////////////////////////////////////////////
	//            TODO: remove when upgrading to Java 9            //
	/////////////////////////////////////////////////////////////////

	/**
	 * Simulates the {@code kotlin}/{@code kotlin.bat} scripts. <br>
	 * Needed because a Java process started by one of these scripts cannot be aborted in case of an infinite loop.
	 *
	 * @param safe      whether the command is safe to execute
	 * @param directory the working directory for the command
	 * @param arguments arguments to simulate
	 *
	 * @return the output of the command
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 * @see #executeCommand(File, String...)
	 * @see #executeSafeCommand(File, String...)
	 */
	protected String simulateKotlinScript(boolean safe, File directory, String... arguments) throws ExecutorException {

		return this.simulateKotlinCompilerScript(safe, true, directory, arguments);
	}

	/**
	 * Simulates the {@code kotlinc}/{@code kotlinc.bat} scripts. <br>
	 * Needed because a Java process started by one of these scripts cannot be aborted in case of an infinite loop.
	 *
	 * @param safe      whether the command is safe to execute
	 * @param directory the working directory for the command
	 * @param arguments arguments to simulate
	 *
	 * @return the output of the command
	 *
	 * @throws ExecutorException if the command cannot be executed successfully
	 * @see #executeCommand(File, String...)
	 * @see #executeSafeCommand(File, String...)
	 */
	protected String simulateKotlinCompilerScript(boolean safe, File directory, String... arguments) throws ExecutorException {

		return this.simulateKotlinCompilerScript(safe, false, directory, arguments);
	}

	//all the variable names and values have been taken from the kotlin[c][.bat] scripts
	@SuppressWarnings({ "LocalVariableNamingConvention", "MethodParameterNamingConvention" })
	private String simulateKotlinCompilerScript(boolean safe, boolean KOTLIN_RUNNER, File directory, String... arguments) throws ExecutorException {

		final String KOTLIN_COMPILER = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler";

		final String JAVACMD = "java"; //ignore JAVA_HOME

		final String[] JAVA_OPTS = { "-Xmx256M", "-Xms32M" };

		final String KOTLIN_HOME = System.getenv("KOTLIN_HOME"); //ignored fallback: find script and extract path; ignored workaround for cygwin
		if (KOTLIN_HOME == null)
			throw new ExecutorException("Cannot find Kotlin libraries.");

		//ignored: extract java arguments (-D*, -J*)

		String[] command;
		if (KOTLIN_RUNNER)
			command = this.concatenateArrays(new String[] { JAVACMD }, JAVA_OPTS,
																			 new String[] { String.format("-Dkotlin.home=%s", KOTLIN_HOME),
																											"-cp", Paths.get(KOTLIN_HOME, "lib", "kotlin-runner.jar").toString(), "org.jetbrains.kotlin.runner.Main" },
																			 arguments);
		else
			command = this.concatenateArrays(new String[] { JAVACMD }, JAVA_OPTS,
																			 new String[] { "-noverify",
																											"-cp", Paths.get(KOTLIN_HOME, "lib", "kotlin-preloader.jar").toString(), "org.jetbrains.kotlin.preloading.Preloader",
																											"-cp", Paths.get(KOTLIN_HOME, "lib", "kotlin-compiler.jar").toString(), KOTLIN_COMPILER },
																			 arguments);

		return safe ? this.executeSafeCommand(directory, command)
								: this.executeDeferredCommand(directory, command);
	}

	// end of script simulations //

	@Override
	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {

			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException("Exactly one output type has to be defined for a Kotlin sample.");

			sb.append("fun ").append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(function.getInputNames().get(i)).append(": ").append(this.getTypeName(function.getInputTypes().get(i)));
			}

			sb.append(") : ").append(this.getTypeName(function.getOutputTypes().get(0))).append(" {").append(CodeExecutorBase.NEWLINE).append('\t').append(CodeExecutorBase.NEWLINE).append('}').append(CodeExecutorBase.NEWLINE);
		}

		return sb.toString();
	}

	@Override
	protected String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			if (testCase.getExpectedOutputValues().size() != 1)
				throw new ExecutorException("Exactly one output value has to be defined for a Kotlin example.");

			sb.append(CodeExecutorBase.NEWLINE).append("try {").append(CodeExecutorBase.NEWLINE); //begin test case block

			sb.append("val start = System.nanoTime()").append(CodeExecutorBase.NEWLINE);
			sb.append("val result: ").append(this.getTypeName(testCase.getFunction().getOutputTypes().get(0))).append("? = ").append(testCase.getFunction().getName()).append('('); //test case invocation
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(')').append(CodeExecutorBase.NEWLINE);
			sb.append("val end = System.nanoTime()").append(CodeExecutorBase.NEWLINE);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (testCase.getFunction().getOutputTypes().get(0).getBaseType()) {
				case ARRAY:
					comparisonPrefix = "Arrays.equals("; //use helper method to compare arrays
					comparisonSeparator = ", ";
					comparisonSuffix = ")";
					break;

				case FLOAT32:
				case FLOAT64:
					comparisonSeparator = ".hasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSuffix = ")";
					break;

				default:
					comparisonSeparator = " == "; //compare objects using equality operator
					break;
			}

			sb.append("val success = ").append(comparisonPrefix).append("result").append(comparisonSeparator); //result evaluation
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(CodeExecutorBase.NEWLINE);

			sb.append("out.write(\"%s:%f:%s%n%n\".format(if (success) \"OK\" else \"ER\", (end - start) * 1e-6, result))").append(CodeExecutorBase.NEWLINE); //print result to the console

			sb.append("} catch (ex: Exception) {").append(CodeExecutorBase.NEWLINE); //finish test case block / begin exception handling
			sb.append("out.write(\"ER:\"); out.flush()").append(CodeExecutorBase.NEWLINE);
			sb.append("ex.printStackTrace(System.out)").append(CodeExecutorBase.NEWLINE);

			sb.append("} finally {").append(CodeExecutorBase.NEWLINE);
			sb.append("out.flush()").append(CodeExecutorBase.NEWLINE);
			sb.append('}');
		}

		return sb.toString();
	}

	@Override
	protected String getValueLiteral(Value value) throws ExecutorException {

		switch (value.getType().getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				StringBuilder sb = new StringBuilder();

				if (value.getType().getBaseType() == ValueType.BaseType.ARRAY)
					switch (value.getType().getGenericParameters().get(0).getBaseType()) {
						case CHARACTER:
						case BOOLEAN:
						case INT8:
						case INT16:
						case INT32:
						case INT64:
						case FLOAT32:
						case FLOAT64:
							sb.append(String.format("%sArrayOf(", this.getTypeName(value.getType().getGenericParameters().get(0)).toLowerCase())); //use 'primitive' array helper
							break;

						default:
							sb.append("arrayOf("); //use standard array helper
					}

				else
					sb.append(value.getType().getBaseType() == ValueType.BaseType.LIST ? "listOf(" : "setOf(");

				boolean first = true; //generate collection elements
				for (Value element : value.getCollection()) {
					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(element));
				}

				return sb.append(')').toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder("mapOf("); //begin map initialisation

				first = true; //generate key/value pairs
				for (List<Value> element : value.get2DCollection()) {
					if (element.size() != 2) //validate key/value pair
						throw new ExecutorException("Map entries always need a key and a value.");

					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(element.get(0))).append(" to ").append(this.getValueLiteral(element.get(1)));
				}

				return sb.append(")").toString(); //finish initialisation and return literal

			case STRING:
			case CHARACTER:
				String valueSafe = IntStream.range(0, value.getSingle().length()).map(value.getSingle()::charAt).mapToObj(i -> String.format("\\u%04X", i))
																		.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

				char separator = value.getType().getBaseType() == ValueType.BaseType.CHARACTER ? '\'' : '"';
				return String.format("%1$c%2$s%1$c", separator, valueSafe);

			case BOOLEAN:
				return Boolean.toString("true".equalsIgnoreCase(value.getSingle()));

			case INT8:
			case INT16:
			case INT32:
			case INT64:
				if (!CodeExecutorBase.NUMERIC_INTEGER_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric integer literal.", value));

				switch (value.getType().getBaseType()) {
					case INT8:
					case INT16:
						return String.format("(%s).to%s()", value.getSingle(), this.getTypeName(value.getType()));

					case INT32:
						return value.getSingle();

					case INT64:
					default:
						return String.format("%sL", value.getSingle());
				}

			case FLOAT32:
			case FLOAT64:
			case DECIMAL:
				if (!CodeExecutorBase.NUMERIC_FLOATING_EXPONENTIAL_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric literal.", value));

				switch (value.getType().getBaseType()) {
					case FLOAT32:
						return String.format("%sF", value.getSingle());

					case FLOAT64:
						return value.getSingle();

					case DECIMAL:
					default:
						return String.format("BigDecimal(\"%s\")", value.getSingle());
				}

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", value.getType()));
		}
	}

	@Override
	protected String getTypeName(ValueType type) throws ExecutorException {

		switch (type.getBaseType()) {
			case ARRAY:
				switch (type.getGenericParameters().get(0).getBaseType()) {
					case CHARACTER:
					case BOOLEAN:
					case INT8:
					case INT16:
					case INT32:
					case INT64:
					case FLOAT32:
					case FLOAT64:
						return String.format("%sArray", this.getTypeName(type.getGenericParameters().get(0))); //return 'primitive' array type

					default:
						//fallthrough
				}

			case LIST:
			case SET:
				return String.format("%s<%s>", type.getBaseType() == ValueType.BaseType.ARRAY ? "Array" : type.getBaseType() == ValueType.BaseType.LIST ? "List" : "Set",
														 this.getTypeName(type.getGenericParameters().get(0))); //return standard type

			case MAP:
				return String.format("Map<%s, %s>", this.getTypeName(type.getGenericParameters().get(0)), this.getTypeName(type.getGenericParameters().get(1)));

			case STRING:
				return "String";

			case CHARACTER:
				return "Char";

			case BOOLEAN:
				return "Boolean";

			case INT8:
				return "Byte";

			case INT16:
				return "Short";

			case INT32:
				return "Int";

			case INT64:
				return "Long";

			case FLOAT32:
				return "Float";

			case FLOAT64:
				return "Double";

			case DECIMAL:
				return "BigDecimal";

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", type));
		}
	}
}
