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
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.impl.PerformanceIndicatorsImpl;
import ch.bfh.progressor.executor.impl.ResultImpl;

/**
 * Code execution engine for C/C++ code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class CPlusPlusExecutor extends CodeExecutorBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "cpp";

	/**
	 * Name of the C/C++ executable.
	 */
	protected static final String EXECUTABLE_NAME = "main";

	/**
	 * Maximum time to use for the compilation of the user code (in seconds).
	 */
	public static final int COMPILE_TIMEOUT_SECONDS = 3;

	/**
	 * Maximum time to use for the execution of the user code (in seconds).
	 */
	public static final int EXECUTION_TIMEOUT_SECONDS = 5;

	@Override
	public String getLanguage() {
		return CPlusPlusExecutor.CODE_LANGUAGE;
	}

	@Override
	public String getFragment(List<FunctionSignature> functions) throws ExecutorException {
		return this.getFunctionSignatures(functions);
	}

	@Override
	public List<Result> execute(String codeFragment, List<TestCase> testCases) {

		final File localDirectory = new File(".");
		final File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory
		final File codeFile = new File(codeDirectory, String.format("%s.cpp", CPlusPlusExecutor.EXECUTABLE_NAME));
		final File executableFile = new File(codeDirectory, CPlusPlusExecutor.EXECUTABLE_NAME);

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
			String[] gccArguments;
			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER) {
				Process dockerStartProcess = this.startDockerProcess(codeDirectory);
				if (dockerStartProcess.waitFor(CPlusPlusExecutor.CONTAINER_START_TIMEOUT, TimeUnit.SECONDS)) {
					if (dockerStartProcess.exitValue() != 0)
						throw new ExecutorException(true, "Could not compile the user code.", this.readConsole(dockerStartProcess));
				} else {
					dockerStartProcess.destroyForcibly(); //destroy()
					throw new ExecutorException(true, "Could not compile the user code in time.");
				}
				containerID = this.getContainerID(dockerStartProcess);
				dockerStartProcess.destroy();
				gccArguments = this.getDockerCommandLine(containerID, "g++", codeFile.getName(), "-std=c++11", "-o", CPlusPlusExecutor.EXECUTABLE_NAME);
			} else
				gccArguments = new String[] { "g++", codeFile.getAbsolutePath(), "-std=c++11", "-o", CPlusPlusExecutor.EXECUTABLE_NAME };

			long gccStart = System.nanoTime();
			Process gccProcess = new ProcessBuilder(gccArguments).directory(codeDirectory).redirectErrorStream(true).start();
			if (gccProcess.waitFor(CPlusPlusExecutor.COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (gccProcess.exitValue() != 0)
					throw new ExecutorException(true, "Could not compile the user code.", this.readConsole(gccProcess));

			} else {
				gccProcess.destroyForcibly(); //destroy()
				throw new ExecutorException(true, "Could not compile the user code in time.");
			}
			long gccEnd = System.nanoTime();

			//********************
			//*** EXECUTE CODE ***
			//********************
			String[] cppArguments;
			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER)
				cppArguments = this.getDockerCommandLine(containerID, new File(localDirectory, executableFile.getName()).getPath());
			else
				cppArguments = new String[] { executableFile.getAbsolutePath() };

			long cppStart = System.nanoTime();
			Process cppProcess = new ProcessBuilder(cppArguments).directory(codeDirectory).redirectErrorStream(true).start();
			if (cppProcess.waitFor(CPlusPlusExecutor.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (cppProcess.exitValue() != 0)
					throw new ExecutorException(true, "Could not execute the user code.", this.readConsole(cppProcess));

			} else {
				cppProcess.destroyForcibly(); //destroy()
				throw new ExecutorException(true, "Could not execute the user code in time.");
			}
			long cppEnd = System.nanoTime();

			//****************************
			//*** TEST CASE EVALUATION ***
			//****************************
			try (Scanner outStm = new Scanner(this.getSafeReader(cppProcess.getInputStream())).useDelimiter(String.format("%n%n"))) {
				while (outStm.hasNext()) { //create a scanner to read the console output case by case
					String res = outStm.next(); //get output lines of next test case
					results.add(new ResultImpl(res.startsWith("OK"), false,
																		 res.substring(3),
																		 new PerformanceIndicatorsImpl((cppEnd - cppStart) / 1e6)));
				}
			}

			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER) {
				Process dockerStopProcess = new ProcessBuilder(this.dockerContainerStop(containerID)).redirectErrorStream(true).start();
				if (dockerStopProcess.waitFor(CPlusPlusExecutor.CONTAINER_STOP_TIMEOUT, TimeUnit.SECONDS)) {
					if (cppProcess.exitValue() != 0)
						throw new ExecutorException(true, "Could not stop dockercontainer.", this.readConsole(dockerStopProcess));

				} else {
					cppProcess.destroyForcibly(); //destroy()
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
	 * Generates the C/C++ code file with the user's code fragment.
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

			Files.write(Paths.get(directory.getPath(), String.format("%s.cpp", CPlusPlusExecutor.EXECUTABLE_NAME)), //create a C/C++ source file in the temporary directory
									code.toString().getBytes(CodeExecutorBase.CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
			throw new ExecutorException(true, "Could not generate the code file.", ex);
		}
	}

	/**
	 * Generates the C/C++ function signatures.
	 *
	 * @param functions functions to generate signatures for
	 *
	 * @return C/C++ function signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {
			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException(true, "Exactly one output type has to be defined for a C/C++ sample.");

			sb.append(this.getTypeName(function.getOutputTypes().get(0), true)).append(' ').append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getTypeName(function.getInputTypes().get(i), true)).append(' ').append(function.getInputNames().get(i));
			}

			sb.append(") {").append(newLine).append('\t').append(newLine).append('}').append(newLine);
		}

		return sb.toString();
	}

	/**
	 * Generates the C/C++ test case signatures.
	 *
	 * @param testCases test cases to generate signatures for
	 *
	 * @return C/C++ test case signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			if (testCase.getExpectedOutputValues().size() != 1)
				throw new ExecutorException(true, "Exactly one output value has to be defined for a C/C++ sample.");

			sb.append(newLine).append("try {").append(newLine); //begin test case block

			ValueType oType = testCase.getFunction().getOutputTypes().get(0); //test case invocation and return value storage
			sb.append(this.getTypeName(oType, true)).append(" ret = ").append(testCase.getFunction().getName()).append('(');
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(");").append(newLine);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (oType.getBaseType()) {
				case FLOAT32:
				case FLOAT64:
				case DECIMAL:
					comparisonPrefix = "hasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSeparator = ", ";
					comparisonSuffix = ")";
					break;

				default:
					comparisonSeparator = " == "; //compare objects using equality operator
					break;
			}

			sb.append("bool suc = ").append(comparisonPrefix).append("ret").append(comparisonSeparator);
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(";").append(newLine);

			String returnPrefix = "";
			switch (oType.getBaseType()) {
				case INT8: //force numeric types to be printed as numbers (not chars or the like)
				case INT16:
				case INT32:
				case INT64:
				case FLOAT32:
				case FLOAT64:
				case DECIMAL:
					returnPrefix = "+";
			}

			sb.append("cout << (suc ? \"OK\" : \"ER\") << \":\" << ").append(returnPrefix).append("ret << endl << endl;").append(newLine); //print result to the console
			sb.append("} catch (const exception &ex) {").append(newLine); //finish test case block / begin exception handling (standard exception class)
			sb.append("cout << \"ER:\" << ex.what() << endl << endl;").append(newLine);
			sb.append("} catch (const string &ex) {").append(newLine); //secondary exception handling (exception C++-string)
			sb.append("cout << \"ER:\" << ex << endl << endl;").append(newLine);
			sb.append("} catch (char const* const &ex) {").append(newLine); //tertiary exception handling (exception C-string)
			sb.append("cout << \"ER:\" << ex << endl << endl;").append(newLine);
			sb.append("} catch (...) {").append(newLine); //last resort (handling all unknown exceptions)
			sb.append("cout << \"ER:unknown exception\" << endl << endl;").append(newLine);
			sb.append('}'); //finish exception handling
		}

		return sb.toString();
	}

	/**
	 * Gets the C/C++ literal for an arbitrary value.
	 *
	 * @param value value to get literal for
	 *
	 * @return C/C++ literal for value
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getValueLiteral(Value value) throws ExecutorException {

		switch (value.getType().getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				StringBuilder sb = new StringBuilder();
				if (value.getType().getBaseType() == ValueType.BaseType.ARRAY) //begin array initialisation syntax
					sb.append("new ").append(this.getTypeName(value.getType().getGenericParameters().get(0), false)).append('[').append(value.getCollection().size()).append("] { ");
				else
					sb.append(this.getTypeName(value.getType(), false)).append(" { ");

				boolean first = true; //generate collection elements
				for (Value element : value.getCollection()) {
					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(element));
				}

				return sb.append(" }").toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder(); //begin map initialisation
				sb.append(this.getTypeName(value.getType(), false)).append(" { ");

				first = true; //generate collection elements
				if (!value.get2DCollection().isEmpty())
					for (List<Value> element : value.get2DCollection()) { //generate key/value pairs
						if (element.size() != 2) //validate key/value pair
							throw new ExecutorException(true, "Map entries always need a key and a value.");

						if (first) first = false;
						else sb.append(", ");
						sb.append('{').append(this.getValueLiteral(element.get(0))).append(", ").append(this.getValueLiteral(element.get(1))).append("}");
					}

				return sb.append('}').toString(); //finish initialisation and return literal

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
					case INT32:
						return value.getSingle();

					default:
						return String.format("%sLL", value.getSingle());
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

					default:
						return String.format("%sL", value.getSingle());
				}

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", value.getType()));
		}
	}

	/**
	 * Gets the C/C++ name of an arbitrary type.
	 *
	 * @param type type to get name of
	 *
	 * @return C/C++ name of type
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getTypeName(ValueType type, boolean isDeclaration) throws ExecutorException {

		switch (type.getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				if (type.getBaseType() == ValueType.BaseType.ARRAY) //alternative: array<%s>
					return String.format(isDeclaration ? "%s*" : "%s[]", this.getTypeName(type.getGenericParameters().get(0), true)); //return class name
				else
					return String.format(type.getBaseType() == ValueType.BaseType.LIST ? "vector<%s>" : "set<%s>", this.getTypeName(type.getGenericParameters().get(0), true)); //return class name

			case MAP:
				return String.format("map<%s, %s>", this.getTypeName(type.getGenericParameters().get(0), true), this.getTypeName(type.getGenericParameters().get(1), true)); //return class name

			case STRING:
				return "string";

			case CHARACTER:
				return "char";

			case BOOLEAN:
				return "bool";

			case INT8:
				return "int8_t";

			case INT16:
				return "int16_t";

			case INT32:
				return "int32_t";

			case INT64:
				return "int64_t";

			case FLOAT32:
				return "float";

			case FLOAT64:
				return "double";

			case DECIMAL:
				return "long double";

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}
}
