package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ch.bfh.progressor.executor.CodeExecutorBase;
import ch.bfh.progressor.executor.Executor;
import ch.bfh.progressor.executor.ExecutorException;
import ch.bfh.progressor.executor.ExecutorPlatform;
import ch.bfh.progressor.executor.thrift.FunctionSignature;
import ch.bfh.progressor.executor.thrift.PerformanceIndicators;
import ch.bfh.progressor.executor.thrift.Result;
import ch.bfh.progressor.executor.thrift.TestCase;
import ch.bfh.progressor.executor.thrift.executorConstants;

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
	public List<Result> execute(String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases) {

		final File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory
		final File codeFile = new File(codeDirectory, String.format("%s.kt", KotlinExecutor.CODE_CLASS_NAME));

		List<Result> results = new ArrayList<>(testCases.size());
		try {
			if (!codeDirectory.exists() && !codeDirectory.mkdirs())
				throw new ExecutorException(true, "Could not create a temporary directory for the user code.");

			//*********************
			//*** GENERATE CODE ***
			//*********************
			this.generateCodeFile(codeDirectory, codeFragment, functions, testCases);

			//********************
			//*** COMPILE CODE ***
			//********************
			String[] kotlincArguments = { Executor.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlinc.bat" : "kotlinc", codeFile.getName() };
			if (CodeExecutorBase.USE_DOCKER)
				kotlincArguments = this.getDockerCommandLine(codeDirectory, kotlincArguments);

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
			String[] kotlinArguments = { Executor.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlin.bat" : "kotlin", KotlinExecutor.CODE_CLASS_NAME };
			if (CodeExecutorBase.USE_DOCKER)
				kotlinArguments = this.getDockerCommandLine(codeDirectory, kotlinArguments);

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
					results.add(new Result(res.startsWith("OK"), false,
																 res.substring(3),
																 new PerformanceIndicators((kotlinEnd - kotlinStart) / 1e6)));
				}
			}

			//**************************
			//*** EXCEPTION HANDLING ***
			//**************************
		} catch (Exception ex) {
			ExecutorException exEx;
			Result res = new Result().setSuccess(false).setFatal(false);
			if (ex instanceof ExecutorException && (exEx = (ExecutorException)ex).getOutput() != null)
				res.setFatal(exEx.isFatal()).setResult(String.format("%s:%n%s", ex.getMessage(), exEx.getOutput()));
			else
				res.setResult(String.format("%s:%n%s", "Could not invoke the user code.", ex));

			while (results.size() < testCases.size())
				results.add(res);

		} finally {
			if (codeDirectory.exists())
				this.deleteRecursive(codeDirectory);
		}

		return results;
	}

	protected void generateCodeFile(File directory, String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases) throws ExecutorException {

		try {
			StringBuilder code = this.getTemplate(); //read the template

			int fragStart = code.indexOf(CodeExecutorBase.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + CodeExecutorBase.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			int caseStart = code.indexOf(CodeExecutorBase.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
			code.replace(caseStart, caseStart + CodeExecutorBase.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(functions, testCases));

			Files.write(Paths.get(directory.getPath(), String.format("%s.kt", KotlinExecutor.CODE_CLASS_NAME)), //create a Kotlin source file in the temporary directory
									code.toString().getBytes(CodeExecutorBase.CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
			throw new ExecutorException(true, "Could not generate the code file.", ex);
		}
	}

	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {

			//validate input / output types & names
			if (function.getInputTypesSize() != function.getInputNamesSize())
				throw new ExecutorException(true, "The same number of input types & names have to be defined.");
			if (function.getOutputTypesSize() != 1 || function.getOutputTypesSize() != function.getOutputNamesSize())
				throw new ExecutorException(true, "Exactly one output type has to be defined for a Kotlin sample.");

			sb.append("fun ").append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(function.getInputNames().get(i)).append(": ").append(this.getTypeName(function.getInputTypes().get(i)));
			}

			sb.append(") : ").append(this.getTypeName(function.getOutputTypes().get(0))).append(" {").append(newLine).append('\t').append(newLine).append('}').append(newLine);
		}

		return sb.toString();
	}

	protected String getTestCaseSignatures(List<FunctionSignature> functions, List<TestCase> testCases) throws ExecutorException {

		final String newLine = String.format("%n");

		Map<String, FunctionSignature> functionsMap = functions.stream().collect(Collectors.toMap(FunctionSignature::getName, Function.identity()));

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			FunctionSignature function = functionsMap.get(testCase.getFunctionName());

			//validate input / output types & values
			if (testCase.getInputValuesSize() != function.getInputTypesSize())
				throw new ExecutorException(true, "The same number of input values & types have to be defined.");
			if (testCase.getExpectedOutputValuesSize() != 1 || testCase.getExpectedOutputValuesSize() != function.getOutputTypesSize())
				throw new ExecutorException(true, "Exactly one output value has to be defined for a Kotlin sample.");

			sb.append(newLine).append("try {").append(newLine); //begin test case block

			String oType = function.getOutputTypes().get(0); //test case invocation and return value storage
			sb.append("val ret = ").append(testCase.getFunctionName()).append('(');
			for (int i = 0; i < testCase.getInputValuesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i), function.getInputTypes().get(i)));
			}
			sb.append(')').append(newLine);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (oType) {
				case executorConstants.TypeFloat32:
				case executorConstants.TypeFloat64:
					comparisonSeparator = ".hasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSuffix = ")";
					break;

				//case executorConstants.TypeString:
				//case executorConstants.TypeCharacter:
				//case executorConstants.TypeBoolean:
				//case executorConstants.TypeInt8:
				//case executorConstants.TypeInt16:
				//case executorConstants.TypeInt32:
				//case executorConstants.TypeInt64:
				//case executorConstants.TypeDecimal:
				default:
					comparisonSeparator = " == "; //compare objects using equality operator
					break;

				//default:
				//throw new ExecutorException(String.format("Value type %s is not supported.", oType));
			}

			sb.append("val suc = ").append(comparisonPrefix).append("ret").append(comparisonSeparator);
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0), oType)).append(comparisonSuffix).append(newLine);
			sb.append("out.write(\"%s:%s%n%n\".format(if (suc) \"OK\" else \"ER\", ret))").append(newLine); //print result to the console

			sb.append("} catch (ex: Exception) {").append(newLine); //finish test case block / begin exception handling
			sb.append("out.write(\"ER:\");").append(newLine);
			sb.append("ex.printStackTrace(System.out);").append(newLine);
			sb.append('}');
		}

		return sb.toString();
	}

	protected String getValueLiteral(String value, String type) throws ExecutorException {

		if ("null".equals(value))
			return "null";

		//check for collection container types
		boolean isArr = type.startsWith(String.format("%s<", executorConstants.TypeContainerArray));
		boolean isLst = type.startsWith(String.format("%s<", executorConstants.TypeContainerList));
		boolean isSet = type.startsWith(String.format("%s<", executorConstants.TypeContainerSet));
		if (isArr || isLst || isSet) {
			int cntTypLen = (isArr ? executorConstants.TypeContainerArray : isLst ? executorConstants.TypeContainerList : executorConstants.TypeContainerSet).length();
			String elmTyp = type.substring(cntTypLen + 1, type.length() - 1);

			if (CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(elmTyp).length != 1) //validate type parameters
				throw new ExecutorException(true, "Array, List & Set types need 1 type parameter.");

			StringBuilder sb = new StringBuilder();
			if (isArr) sb.append("arrayOf("); //begin array initialisation
			else if (isLst) sb.append("listOf("); //begin list initialisation
			else sb.append("setOf("); //begin set initialisation

			boolean first = true; //generate collection elements
			if (!value.isEmpty())
				for (String elm : CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(value)) {
					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(elm, elmTyp));
				}

			return sb.append(')').toString(); //finish collection initialisation and return literal

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String elmTyp = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - 1);
			String[] kvTyps = CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(elmTyp);

			if (kvTyps.length != 2) // validate type parameters
				throw new ExecutorException(true, "Map type needs 2 type parameters.");

			StringBuilder sb = new StringBuilder("mapOf("); //begin map initialisation

			boolean first = true; //generate key/value pairs
			if (!value.isEmpty())
				for (String ety : CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(value)) {
					String[] kv = CodeExecutorBase.KEY_VALUE_SEPARATOR_PATTERN.split(ety);

					if (kv.length != 2) //validate key/value pair
						throw new ExecutorException(true, "Map entries always need a key and a value.");

					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(kv[0], kvTyps[0])).append(" to ").append(this.getValueLiteral(kv[1], kvTyps[1]));
				}

			return sb.append(")").toString(); //finish initialisation and return literal
		}

		switch (type) { //switch over basic types
			case executorConstants.TypeString:
			case executorConstants.TypeCharacter:
				String valueSafe = IntStream.range(0, value.length()).map(value::charAt).mapToObj(i -> String.format("\\u%04X", i))
																		.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

				char separator = type.equals(executorConstants.TypeCharacter) ? '\'' : '"';
				return String.format("%1$c%2$s%1$c", separator, valueSafe);

			case executorConstants.TypeBoolean:
				return Boolean.toString("true".equalsIgnoreCase(value));

			case executorConstants.TypeInt8:
			case executorConstants.TypeInt16:
			case executorConstants.TypeInt32:
			case executorConstants.TypeInt64:
				if (!CodeExecutorBase.NUMERIC_INTEGER_PATTERN.matcher(value).matches())
					throw new ExecutorException(true, String.format("Value %s is not a valid numeric integer literal.", value));

				switch (type) {
					case executorConstants.TypeInt8:
					case executorConstants.TypeInt16:
						return String.format("(%s).to%s()", value, this.getTypeName(type));

					case executorConstants.TypeInt32:
						return value;

					case executorConstants.TypeInt64:
					default:
						return String.format("%sL", value);
				}

			case executorConstants.TypeFloat32:
			case executorConstants.TypeFloat64:
			case executorConstants.TypeDecimal:
				if (!CodeExecutorBase.NUMERIC_FLOATING_EXPONENTIAL_PATTERN.matcher(value).matches())
					throw new ExecutorException(true, String.format("Value %s is not a valid numeric literal.", value));

				switch (type) {
					case executorConstants.TypeFloat32:
						return String.format("%sF", value);

					case executorConstants.TypeFloat64:
						return value;

					case executorConstants.TypeDecimal:
					default:
						return String.format("BigDecimal(\"%s\")", value);
				}

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}

	protected String getTypeName(String type) throws ExecutorException {

		//check for collection container types
		boolean isArr = type.startsWith(String.format("%s<", executorConstants.TypeContainerArray));
		boolean isLst = type.startsWith(String.format("%s<", executorConstants.TypeContainerList));
		boolean isSet = type.startsWith(String.format("%s<", executorConstants.TypeContainerSet));
		if (isArr || isLst || isSet) {
			int typLen = (isArr ? executorConstants.TypeContainerArray : isLst ? executorConstants.TypeContainerList : executorConstants.TypeContainerSet).length();
			String typeParam = type.substring(typLen + 1, type.length() - 1);

			if (CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(typeParam).length != 1) //validate type parameters
				throw new ExecutorException(true, "Array, List & Set types need 1 type parameter.");

			return String.format("%s<%s>", isArr ? "Array" : isLst ? "List" : "Set", this.getTypeName(typeParam)); //return class name

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String typeParams = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - 1);
			String[] typeParamsArray = CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(typeParams);

			if (typeParamsArray.length != 2) // validate type parameters
				throw new ExecutorException(true, "Map type needs 2 type parameters.");

			return String.format("Map<%s, %s>", this.getTypeName(typeParamsArray[0]), this.getTypeName(typeParamsArray[1])); //return class name
		}

		switch (type) { //switch over basic types
			case executorConstants.TypeString:
				return "String";

			case executorConstants.TypeCharacter:
				return "Char";

			case executorConstants.TypeBoolean:
				return "Boolean";

			case executorConstants.TypeInt8:
				return "Byte";

			case executorConstants.TypeInt16:
				return "Short";

			case executorConstants.TypeInt32:
				return "Int";

			case executorConstants.TypeInt64:
				return "Long";

			case executorConstants.TypeFloat32:
				return "Float";

			case executorConstants.TypeFloat64:
				return "Double";

			case executorConstants.TypeDecimal:
				return "BigDecimal";

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}
}
