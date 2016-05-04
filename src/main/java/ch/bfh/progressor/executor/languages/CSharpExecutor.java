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
import ch.bfh.progressor.executor.BomProofInputStream;
import ch.bfh.progressor.executor.CodeExecutorBase;
import ch.bfh.progressor.executor.Executor;
import ch.bfh.progressor.executor.ExecutorException;
import ch.bfh.progressor.executor.thrift.FunctionSignature;
import ch.bfh.progressor.executor.thrift.PerformanceIndicators;
import ch.bfh.progressor.executor.thrift.Result;
import ch.bfh.progressor.executor.thrift.TestCase;
import ch.bfh.progressor.executor.thrift.executorConstants;

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
	public static final String EXECUTABLE_NAME = "main";

	/**
	 * Maximum time to use for for the compilation of the user code (in seconds).
	 */
	public static final int COMPILE_TIMEOUT_SECONDS = 5;

	/**
	 * Maximum time to use for the execution of the user code (in seconds).
	 */
	public static final int EXECUTION_TIMEOUT_SECONDS = 5;

	@Override
	public String getLanguage() {
		return CSharpExecutor.CODE_LANGUAGE;
	}

	@Override
	public String getFragment(List<FunctionSignature> functions) throws ExecutorException {
		return this.getFunctionSignatures(functions);
	}

	@Override
	public List<Result> execute(String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases) {

		List<Result> results = new ArrayList<>(testCases.size());
		File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory

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
			long cscStart = System.nanoTime();
			Process cscProcess = null;
			if(Executor.useDocker)cscProcess = new ProcessBuilder("docker", "run", "-v",codeDirectory.getAbsolutePath()+"/:/opt", DOCKERCONTAINER, "mcs", CSharpExecutor.EXECUTABLE_NAME+".cs").redirectErrorStream(true).start();
			else cscProcess = new ProcessBuilder(System.getProperty("os.name").substring(0, 3).equals("Win") ? "csc" : "mcs", codeDirectory.getAbsolutePath()+"/"+ CSharpExecutor.EXECUTABLE_NAME+".cs", "/debug").redirectErrorStream(true).start();
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
			if (System.getProperty("os.name").substring(0, 3).equals("Win"))
				csArguments = new String[] { "cmd", "/C", CSharpExecutor.EXECUTABLE_NAME };
			else{
				if(Executor.useDocker)csArguments = new String[] {"docker", "run", "-v",codeDirectory.getAbsolutePath()+":/opt", DOCKERCONTAINER, "mono", CSharpExecutor.EXECUTABLE_NAME+".exe"};
				else csArguments = new String[] {"mono", CSharpExecutor.EXECUTABLE_NAME+".exe"};
			}

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

			//****************************
			//*** TEST CASE EVALUATION ***
			//****************************
			try (Scanner outStm = new Scanner(new BomProofInputStream(cscProcess.getInputStream()), CodeExecutorBase.CHARSET.name())
				.useDelimiter(String.format("%n%n"))) {
				while (outStm.hasNext()) { //create a scanner to read the console output case by case
					String res = outStm.next(); //get output lines of next test case
					results.add(new Result(res.startsWith("OK"), false,
																 res.substring(3),
																 new PerformanceIndicators((cscEnd - cscStart) / 1e6)));
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

			Files.write(Paths.get(directory.getPath(), String.format("%s.cs", CSharpExecutor.EXECUTABLE_NAME)), //create a c++ source file in the temporary directory
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
				throw new ExecutorException(true, "Exactly one output type has to be defined for a C# sample.");

			sb.append("public ").append(this.getTypeName(function.getOutputTypes().get(0))).append(' ').append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getTypeName(function.getInputTypes().get(i))).append(' ').append(function.getInputNames().get(i));
			}

			sb.append(") {").append(newLine).append('\t').append(newLine).append('}').append(newLine);
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
				throw new ExecutorException(true, "Exactly one output value has to be defined for a C# sample.");

			sb.append(newLine).append("try {").append(newLine); //begin test case block

			String oType = function.getOutputTypes().get(0); //test case invocation and return value storage
			sb.append(this.getTypeName(oType)).append(" ret = ").append("inst.").append(testCase.getFunctionName()).append('(');
			for (int i = 0; i < testCase.getInputValuesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i), function.getInputTypes().get(i)));
			}
			sb.append(");").append(newLine);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (oType) {
				case executorConstants.TypeString:
				case executorConstants.TypeCharacter:
				case executorConstants.TypeBoolean:
				case executorConstants.TypeInt8:
				case executorConstants.TypeInt16:
				case executorConstants.TypeInt32:
				case executorConstants.TypeInt64:
				case executorConstants.TypeDecimal:
					comparisonSeparator = " == "; //compare primitive types using equality operator
					break;

				case executorConstants.TypeFloat32:
				case executorConstants.TypeFloat64:
					comparisonPrefix = "HasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSeparator = ", ";
					comparisonSuffix = ", 1)";
					break;

				default:
					comparisonSeparator = ".Equals("; //compare objects using equality method
					comparisonSuffix = ")";
					break;

				//default:
				//throw new ExecutorException(String.format("Value type %s is not supported.", oType));
			}

			sb.append("bool suc = ").append(comparisonPrefix).append("ret").append(comparisonSeparator);
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0), oType)).append(comparisonSuffix).append(";").append(newLine);
			sb.append("Console.WriteLine(\"{0}:{1}\", suc ? \"OK\" : \"ER\", ret);").append(newLine).append("Console.WriteLine();").append(newLine); //print result to the console

			sb.append("} catch (Exception ex) {").append(newLine); //finish test case block / begin exception handling
			sb.append("Console.WriteLine(\"ER:{0}\", ex);").append(newLine).append("Console.WriteLine();").append(newLine);
			sb.append('}'); //finish exception handling
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
			sb.append("new ").append(this.getTypeName(type)).append(" { "); //begin initialisation

			boolean first = true; //generate collection elements
			if (!value.isEmpty())
				for (String elm : CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(value)) {
					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(elm, elmTyp));
				}

			return sb.append(" }").toString(); //finish collection initialisation and return literal

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String elmTyp = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - 1);
			String[] kvTyps = CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(elmTyp);

			if (kvTyps.length != 2) // validate type parameters
				throw new ExecutorException(true, "Map type needs 2 type parameters.");

			StringBuilder sb = new StringBuilder(); //begin initialisation
			sb.append("new ").append(this.getTypeName(type)).append(" { "); //begin initialisation

			boolean first = true; //generate collection elements
			if (!value.isEmpty())
				for (String ety : CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(value)) { //generate key/value pairs
					String[] kv = CodeExecutorBase.KEY_VALUE_SEPARATOR_PATTERN.split(ety);

					if (kv.length != 2) //validate key/value pair
						throw new ExecutorException(true, "Map entries always need a key and a value.");

					if (first) first = false;
					else sb.append(", ");
					sb.append('{').append(this.getValueLiteral(kv[0], kvTyps[0])).append(", ").append(this.getValueLiteral(kv[1], kvTyps[1])).append('}');
				}

			return sb.append(" }").toString(); //finish initialisation and return literal
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

				return type.equals(executorConstants.TypeInt64) ? String.format("%sL", value) : value;

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
						return String.format("%sM", value);
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

			return String.format(isArr ? "%s[]" : isLst ? "List<%s>" : "HashSet<%s>", this.getTypeName(typeParam)); //return class name

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String typeParams = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - 1);
			String[] typeParamsArray = CodeExecutorBase.PARAMETER_SEPARATOR_PATTERN.split(typeParams);

			if (typeParamsArray.length != 2) // validate type parameters
				throw new ExecutorException(true, "Map type needs 2 type parameters.");

			return String.format("Dictionary<%s, %s>", this.getTypeName(typeParamsArray[0]), this.getTypeName(typeParamsArray[1])); //return class name
		}

		switch (type) { //switch over basic types
			case executorConstants.TypeString:
				return "string";

			case executorConstants.TypeCharacter:
				return "char";

			case executorConstants.TypeBoolean:
				return "bool";

			case executorConstants.TypeInt8:
				return "sbyte";

			case executorConstants.TypeInt16:
				return "short";

			case executorConstants.TypeInt32:
				return "int";

			case executorConstants.TypeInt64:
				return "long";

			case executorConstants.TypeFloat32:
				return "float";

			case executorConstants.TypeFloat64:
				return "double";

			case executorConstants.TypeDecimal:
				return "decimal";

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}
}
