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
 * Code execution engine for C# code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class CSharpExecutor extends CodeExecutorBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "csharp";

	/**
	 * Name of the C# executable.
	 */
	protected static final String EXECUTABLE_NAME = "main";

	/**
	 * Maximum time to use for for the compilation of the user code (in seconds).
	 */
	public static final int COMPILE_TIMEOUT_SECONDS = 5;

	/**
	 * Maximum time to use for the execution of the user code (in seconds).
	 */
	public static final int EXECUTION_TIMEOUT_SECONDS = 10;

	@Override
	public String getLanguage() {
		return CSharpExecutor.CODE_LANGUAGE;
	}

	@Override
	public String getFragment(List<FunctionSignature> functions) throws ExecutorException {
		return this.getFunctionSignatures(functions);
	}

	@Override
	public List<Result> execute(String codeFragment, List<TestCase> testCases) {

		final File localDirectory = new File(".");
		final File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory
		final File codeFile = new File(codeDirectory, String.format("%s.cs", CSharpExecutor.EXECUTABLE_NAME));
		final File executableFile = new File(codeDirectory, String.format("%s.exe", CSharpExecutor.EXECUTABLE_NAME));

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
			String[] cscArguments;
			switch (CodeExecutorBase.PLATFORM) {
				case WINDOWS:
					cscArguments = new String[] { "csc", codeFile.getName(), "/debug" };
					break;
				case UNIX_LINUX:
					cscArguments = new String[] { "mcs", codeFile.getName(), "-debug" };
					break;
				default:
					throw new ExecutorException(true, "Unsupported platform detected.");
			}
			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER) {
				Process dockerStartProcess = this.startDockerProcess(codeDirectory);
				if (dockerStartProcess.waitFor(CSharpExecutor.CONTAINER_START_TIMEOUT, TimeUnit.SECONDS)) {
					if (dockerStartProcess.exitValue() != 0)
						throw new ExecutorException(true, "Could not start dockercontainer.", this.readConsole(dockerStartProcess));

				} else {
					dockerStartProcess.destroyForcibly(); //destroy()
					throw new ExecutorException(true, "Could not start dockercontainer in time.");
				}
				containerID = this.getContainerID(dockerStartProcess);
				dockerStartProcess.destroy();
				cscArguments = this.getDockerCommandLine(containerID, cscArguments);
			}

			long cscStart = System.nanoTime();
			Process cscProcess = new ProcessBuilder(cscArguments).directory(codeDirectory).redirectErrorStream(true).start();
			if (cscProcess.waitFor(CSharpExecutor.COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (cscProcess.exitValue() != 0)
					throw new ExecutorException(true, "Could not compile the user code.", this.readConsole(cscProcess));

			} else {
				cscProcess.destroyForcibly(); //destroy()
				throw new ExecutorException(true, "Could not compile the user code in time.");
			}
			long cscEnd = System.nanoTime();

			//********************
			//*** EXECUTE CODE ***
			//********************
			String[] csArguments;
			switch (CodeExecutorBase.PLATFORM) {
				case WINDOWS:
					csArguments = new String[] { executableFile.getAbsolutePath() };
					break;
				case UNIX_LINUX:
					csArguments = new String[] { "mono", CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER ? new File(localDirectory, executableFile.getName()).getPath() : executableFile.getAbsolutePath(), "--debug" };
					break;
				default:
					throw new ExecutorException(true, "Unsupported platform detected.");
			}
			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER)
				csArguments = this.getDockerCommandLine(containerID, csArguments);

			long csStart = System.nanoTime();
			Process csProcess = new ProcessBuilder(csArguments).directory(codeDirectory).redirectErrorStream(true).start();
			if (csProcess.waitFor(CSharpExecutor.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (csProcess.exitValue() != 0)
					throw new ExecutorException(true, "Could not execute the user code.", this.readConsole(csProcess));

			} else {
				csProcess.destroyForcibly(); //destroy()
				throw new ExecutorException(true, "Could not execute the user code in time.");
			}
			long csEnd = System.nanoTime();
			if (CodeExecutorBase.PLATFORM.hasDockerSupport() && CodeExecutorBase.USE_DOCKER) {
				Process dockerStopProcess = this.stopDockerProcess(containerID);
				if (dockerStopProcess.waitFor(CSharpExecutor.CONTAINER_STOP_TIMEOUT, TimeUnit.SECONDS)) {
					if (dockerStopProcess.exitValue() != 0)
						throw new ExecutorException(true, "Could not stop dockercontainer.", this.readConsole(dockerStopProcess));

				} else {
					dockerStopProcess.destroyForcibly(); //destroy()
					throw new ExecutorException(true, "Could not stop dockercontainer in time.");
				}
			}


			//this.checkProcess(dockerStopProcess,CSharpExecutor.CONTAINER_STOP_TIMEOUT,"Could not stop dockercontainer.","Could not stop dockercontainer in time.");
			//****************************
			//*** TEST CASE EVALUATION ***
			//****************************
			try (Scanner outStm = new Scanner(this.getSafeReader(csProcess.getInputStream())).useDelimiter(String.format("%n%n"))) {
				while (outStm.hasNext()) { //create a scanner to read the console output case by case
					String res = outStm.next(); //get output lines of next test case
					results.add(new ResultImpl(res.startsWith("OK"), false,
																		 res.substring(3),
																		 new PerformanceIndicatorsImpl((csEnd - csStart) / 1e6)));
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
	 * Generates the C# code file with the user's code fragment.
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

			Files.write(Paths.get(directory.getPath(), String.format("%s.cs", CSharpExecutor.EXECUTABLE_NAME)), //create a c++ source file in the temporary directory
									code.toString().getBytes(CodeExecutorBase.CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
			throw new ExecutorException(true, "Could not generate the code file.", ex);
		}
	}

	/**
	 * Generates the C# function signatures.
	 *
	 * @param functions functions to generate signatures for
	 *
	 * @return C# function signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {

			//validate input / output types & names
			if (function.getInputTypes().size() != function.getInputNames().size())
				throw new ExecutorException(true, "The same number of input types & names have to be defined.");
			if (function.getOutputTypes().size() != 1 || function.getOutputTypes().size() != function.getOutputNames().size())
				throw new ExecutorException(true, "Exactly one output type has to be defined for a C# sample.");

			sb.append("public ").append(this.getTypeName(function.getOutputTypes().get(0))).append(' ').append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getTypeName(function.getInputTypes().get(i))).append(' ').append(function.getInputNames().get(i));
			}

			sb.append(") {").append(newLine).append('\t').append(newLine).append('}').append(newLine);
		}

		return sb.toString();
	}

	/**
	 * Generates the C# test case signatures.
	 *
	 * @param testCases test cases to generate signatures for
	 *
	 * @return C# test case signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {

			//validate input / output types & values
			if (testCase.getInputValues().size() != testCase.getFunction().getInputTypes().size())
				throw new ExecutorException(true, "The same number of input values & types have to be defined.");
			if (testCase.getExpectedOutputValues().size() != 1 || testCase.getExpectedOutputValues().size() != testCase.getFunction().getOutputTypes().size())
				throw new ExecutorException(true, "Exactly one output value has to be defined for a C# sample.");

			sb.append(newLine).append("try {").append(newLine); //begin test case block

			ValueType oType = testCase.getFunction().getOutputTypes().get(0); //test case invocation and return value storage
			sb.append(this.getTypeName(oType)).append(" ret = ").append("inst.").append(testCase.getFunction().getName()).append('(');
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(");").append(newLine);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (oType.getBaseType()) {
				case STRING:
				case CHARACTER:
				case BOOLEAN:
				case INT8:
				case INT16:
				case INT32:
				case INT64:
				case DECIMAL:
					comparisonSeparator = " == "; //compare primitive types using equality operator
					break;

				case FLOAT32:
				case FLOAT64:
					comparisonPrefix = "HasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSeparator = ", ";
					comparisonSuffix = ", 1)";
					break;

				default:
					comparisonSeparator = ".Equals("; //compare objects using equality method
					comparisonSuffix = ")";
					break;
			}

			sb.append("bool suc = ").append(comparisonPrefix).append("ret").append(comparisonSeparator);
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(";").append(newLine);
			sb.append("Console.WriteLine(\"{0}:{1}\", suc ? \"OK\" : \"ER\", ret);").append(newLine).append("Console.WriteLine();").append(newLine); //print result to the console

			sb.append("} catch (Exception ex) {").append(newLine); //finish test case block / begin exception handling
			sb.append("Console.WriteLine(\"ER:{0}\", ex);").append(newLine).append("Console.WriteLine();").append(newLine);
			sb.append('}'); //finish exception handling
		}

		return sb.toString();
	}

	/**
	 * Gets the C# literal for an arbitrary value.
	 *
	 * @param value value to get literal for
	 *
	 * @return C# literal for value
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getValueLiteral(Value value) throws ExecutorException {

		switch (value.getType().getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				StringBuilder sb = new StringBuilder();
				sb.append("new ").append(this.getTypeName(value.getType())).append(" { "); //begin initialisation

				boolean first = true; //generate collection elements
				if (!value.getCollection().isEmpty())
					for (Value element : value.getCollection()) {
						if (first) first = false;
						else sb.append(", ");
						sb.append(this.getValueLiteral(element));
					}

				return sb.append(" }").toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder(); //begin initialisation
				sb.append("new ").append(this.getTypeName(value.getType())).append(" { "); //begin initialisation

				first = true; //generate collection elements
				if (!value.get2DCollection().isEmpty())
					for (List<Value> element : value.get2DCollection()) { //generate key/value pairs
						if (element.size() != 2) //validate key/value pair
							throw new ExecutorException(true, "Map entries always need a key and a value.");

						if (first) first = false;
						else sb.append(", ");
						sb.append('{').append(this.getValueLiteral(element.get(0))).append(", ").append(this.getValueLiteral(element.get(1))).append('}');
					}

				return sb.append(" }").toString(); //finish initialisation and return literal

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

				return value.getType().getBaseType() == ValueType.BaseType.INT64 ? String.format("%sL", value.getSingle()) : value.getSingle();

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
						return String.format("%sM", value.getSingle());
				}

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", value.getType()));
		}
	}

	/**
	 * Gets the C# name of an arbitrary type.
	 *
	 * @param type type to get name of
	 *
	 * @return C# name of type
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getTypeName(ValueType type) throws ExecutorException {

		switch (type.getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				return String.format(type.getBaseType() == ValueType.BaseType.ARRAY ? "%s[]" : type.getBaseType() == ValueType.BaseType.LIST ? "List<%s>" : "HashSet<%s>",
														 this.getTypeName(type.getGenericParameters().get(0))); //return class name

			case MAP:
				return String.format("Dictionary<%s, %s>", this.getTypeName(type.getGenericParameters().get(0)), this.getTypeName(type.getGenericParameters().get(1))); //return class name

			case STRING:
				return "string";

			case CHARACTER:
				return "char";

			case BOOLEAN:
				return "bool";

			case INT8:
				return "sbyte";

			case INT16:
				return "short";

			case INT32:
				return "int";

			case INT64:
				return "long";

			case FLOAT32:
				return "float";

			case FLOAT64:
				return "double";

			case DECIMAL:
				return "decimal";

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}
}
