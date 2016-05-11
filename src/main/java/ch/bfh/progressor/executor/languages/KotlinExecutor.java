package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.impl.PerformanceIndicatorsImpl;
import ch.bfh.progressor.executor.impl.ResultImpl;

/**
 * Code execution engine for Kotlin code. <br>
 * Compiles and executes the Kotlin code in two steps.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class KotlinExecutor extends CodeExecutorBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "kotlin";

	/**
	 * Name of the Kotlin (Java) main class.
	 */
	protected static final String CODE_CLASS_NAME = "Program";

	/**
	 * Maximum time to use for for the compilation of the user code (in seconds).
	 */
	public static final int COMPILE_TIMEOUT_SECONDS = 10;

	/**
	 * Maximum time to use for the execution of the user code (in seconds).
	 */
	public static final int EXECUTION_TIMEOUT_SECONDS = 10;

	@Override
	public String getLanguage() {
		return KotlinExecutor.CODE_LANGUAGE;
	}

	@Override
	public String getFragment(List<FunctionSignature> functions) throws ExecutorException {
		return this.getFunctionSignatures(functions);
	}

	@Override
	public List<Result> execute(String codeFragment, List<TestCase> testCases) {

		final File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory
		final File codeFile = new File(codeDirectory, String.format("%s.kt", KotlinExecutor.CODE_CLASS_NAME));

		String containerID = null;

		List<Result> results = new ArrayList<>(testCases.size());
		try {
			if (!codeDirectory.exists() && !codeDirectory.mkdirs())
				throw new ExecutorException(true, "Could not create a temporary directory for the user code.");

			//*********************
			//*** GENERATE CODE ***
			//*********************
			this.generateCodeFile(codeDirectory, codeFragment, testCases);

			//********************
			//*** COMPILE CODE ***
			//********************
			String[] kotlincArguments = { CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlinc.bat" : "kotlinc", codeFile.getName() };
			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER) {
				Process dockerProcess = this.startDockerProcess(codeDirectory);
				containerID = this.getContainerID(dockerProcess);
				dockerProcess.destroy();
				kotlincArguments = this.getDockerCommandLine(containerID, kotlincArguments);
			}

			long kotlincStart = System.nanoTime();
			Process kotlincProcess = new ProcessBuilder(kotlincArguments).directory(codeDirectory).redirectErrorStream(true).start();
			if (kotlincProcess.waitFor(KotlinExecutor.COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (kotlincProcess.exitValue() != 0)
					throw new ExecutorException(true, "Could not compile the user code.", this.readConsole(kotlincProcess));

			} else {
				kotlincProcess.destroyForcibly(); //destroy()
				throw new ExecutorException(true, "Could not compile the user code in time.");
			}
			long kotlincEnd = System.nanoTime();

			//********************
			//*** EXECUTE CODE ***
			//********************
			String[] kotlinArguments = { CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlin.bat" : "kotlin", KotlinExecutor.CODE_CLASS_NAME };
			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER)
				kotlinArguments = this.getDockerCommandLine(containerID, kotlinArguments);

			long kotlinStart = System.nanoTime();
			Process kotlinProcess = new ProcessBuilder(kotlinArguments).directory(codeDirectory).redirectErrorStream(true).start();
			if (kotlinProcess.waitFor(KotlinExecutor.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (kotlinProcess.exitValue() != 0)
					throw new ExecutorException(true, "Could not execute the user code.", this.readConsole(kotlinProcess));

			} else {
				kotlinProcess.destroyForcibly(); //destroy()
				throw new ExecutorException(true, "Could not execute the user code in time.");
			}
			long kotlinEnd = System.nanoTime();

			//****************************
			//*** TEST CASE EVALUATION ***
			//****************************
			try (Scanner outStm = new Scanner(this.getSafeReader(kotlinProcess.getInputStream())).useDelimiter(String.format("%n%n"))) {
				while (outStm.hasNext()) { //create a scanner to read the console output case by case
					String res = outStm.next(); //get output lines of next test case
					results.add(new ResultImpl(res.startsWith("OK"), false,
																		 res.substring(3),
																		 new PerformanceIndicatorsImpl((kotlinEnd - kotlinStart) / 1e6)));
				}
			}

			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER) {
				Process dockerStopProcess = new ProcessBuilder(this.dockerContainerStop(containerID)).redirectErrorStream(true).start();
				if (dockerStopProcess.waitFor(KotlinExecutor.CONTAINER_STOP_TIMEOUT, TimeUnit.SECONDS)) {
					if (dockerStopProcess.exitValue() != 0)
						throw new ExecutorException(true, "Could not stop dockercontainer.", this.readConsole(dockerStopProcess));

				} else {
					dockerStopProcess.destroyForcibly(); //destroy()
					throw new ExecutorException(true, "Could not stop dockercontainer in time.");
				}
			}

			//**************************
			//*** EXCEPTION HANDLING ***
			//**************************
		} catch (Exception ex) {
			ExecutorException exEx;
			Result result;
			if (ex instanceof ExecutorException && (exEx = (ExecutorException)ex).getOutput() != null)
				result = new ResultImpl(false, exEx.isFatal(), String.format("%s:%n%s", ex.getMessage(), exEx.getOutput()), null);
			else
				result = new ResultImpl(false, false, String.format("%s:%n%s", "Could not invoke the user code.", ex), null);

			while (results.size() < testCases.size())
				results.add(result);

		} finally {
			if (codeDirectory.exists())
				this.tryDeleteRecursive(codeDirectory);
		}

		return results;
	}

	/**
	 * Generates the Kotlin code file with the user's code fragment.
	 *
	 * @param directory    directory to create code file in
	 * @param codeFragment code fragment to write into the file
	 * @param testCases    test cases to generate tests for
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected void generateCodeFile(File directory, String codeFragment, List<TestCase> testCases) throws ExecutorException {

		try {
			StringBuilder code = this.getTemplate(); //read the template

			int fragStart = code.indexOf(CodeExecutorBase.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + CodeExecutorBase.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			int caseStart = code.indexOf(CodeExecutorBase.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
			code.replace(caseStart, caseStart + CodeExecutorBase.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(testCases));

			Files.write(Paths.get(directory.getPath(), String.format("%s.kt", KotlinExecutor.CODE_CLASS_NAME)), //create a Kotlin source file in the temporary directory
									code.toString().getBytes(CodeExecutorBase.CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
			throw new ExecutorException(true, "Could not generate the code file.", ex);
		}
	}

	/**
	 * Generates the Kotlin function signatures.
	 *
	 * @param functions functions to generate signatures for
	 *
	 * @return Kotlin function signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {

			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException(true, "Exactly one output type has to be defined for a Kotlin sample.");

			sb.append("fun ").append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(function.getInputNames().get(i)).append(": ").append(this.getTypeName(function.getInputTypes().get(i)));
			}

			sb.append(") : ").append(this.getTypeName(function.getOutputTypes().get(0))).append(" {").append(newLine).append('\t').append(newLine).append('}').append(newLine);
		}

		return sb.toString();
	}

	/**
	 * Generates the Kotlin test case signatures.
	 *
	 * @param testCases test cases to generate signatures for
	 *
	 * @return Kotlin test case signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			sb.append(newLine).append("try {").append(newLine); //begin test case block

			ValueType oType = testCase.getFunction().getOutputTypes().get(0); //test case invocation and return value storage
			sb.append("val ret = ").append(testCase.getFunction().getName()).append('(');
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(')').append(newLine);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (oType.getBaseType()) {
				case FLOAT32:
				case FLOAT64:
					comparisonSeparator = ".hasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSuffix = ")";
					break;

				default:
					comparisonSeparator = " == "; //compare objects using equality operator
					break;
			}

			sb.append("val suc = ").append(comparisonPrefix).append("ret").append(comparisonSeparator);
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(newLine);
			sb.append("out.write(\"%s:%s%n%n\".format(if (suc) \"OK\" else \"ER\", ret))").append(newLine); //print result to the console

			sb.append("} catch (ex: Exception) {").append(newLine); //finish test case block / begin exception handling
			sb.append("out.write(\"ER:\");").append(newLine);
			sb.append("ex.printStackTrace(System.out);").append(newLine);
			sb.append('}');
		}

		return sb.toString();
	}

	/**
	 * Gets the Kotlin literal for an arbitrary value.
	 *
	 * @param value value to get literal for
	 *
	 * @return Kotlin literal for value
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getValueLiteral(Value value) throws ExecutorException {

		switch (value.getType().getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				StringBuilder sb = new StringBuilder();
				sb.append(value.getType().getBaseType() == ValueType.BaseType.ARRAY ? "arrayOf(" :
									value.getType().getBaseType() == ValueType.BaseType.LIST ? "listOf(" : "setOf(");

				boolean first = true; //generate collection elements
				if (!value.getCollection().isEmpty())
					for (Value element : value.getCollection()) {
						if (first) first = false;
						else sb.append(", ");
						sb.append(this.getValueLiteral(element));
					}

				return sb.append(')').toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder("mapOf("); //begin map initialisation

				first = true; //generate key/value pairs
				if (!value.get2DCollection().isEmpty())
					for (List<Value> element : value.get2DCollection()) {
						if (element.size() != 2) //validate key/value pair
							throw new ExecutorException(true, "Map entries always need a key and a value.");

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
					throw new ExecutorException(true, String.format("Value %s is not a valid numeric integer literal.", value));

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
					throw new ExecutorException(true, String.format("Value %s is not a valid numeric literal.", value));

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
				throw new ExecutorException(true, String.format("Value type %s is not supported.", value.getType()));
		}
	}

	/**
	 * Gets the Kotlin name of an arbitrary type.
	 *
	 * @param type type to get name of
	 *
	 * @return Kotlin name of type
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getTypeName(ValueType type) throws ExecutorException {

		switch (type.getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				return String.format("%s<%s>", type.getBaseType() == ValueType.BaseType.ARRAY ? "Array" : type.getBaseType() == ValueType.BaseType.LIST ? "List" : "Set",
														 this.getTypeName(type.getGenericParameters().get(0))); //return class name

			case MAP:
				return String.format("Map<%s, %s>", this.getTypeName(type.getGenericParameters().get(0)), this.getTypeName(type.getGenericParameters().get(1))); //return class name

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
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}
}
