package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ch.bfh.progressor.executor.CodeExecutor;
import ch.bfh.progressor.executor.ExecutorException;
import ch.bfh.progressor.executor.thrift.FunctionSignature;
import ch.bfh.progressor.executor.thrift.PerformanceIndicators;
import ch.bfh.progressor.executor.thrift.Result;
import ch.bfh.progressor.executor.thrift.TestCase;
import ch.bfh.progressor.executor.thrift.executorConstants;

/**
 * Created by janick Weidmann on 14.03.2016.
 */
public class CSharpExecutor extends CodeExecutor {

	/**
	 * Character set to use for the custom code.
	 */
	public static final Charset CODE_CHARSET = Charset.forName("UTF-8");

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "csharp";

	/**
	 * Name the .exe file for compiled code
	 */
	public static final String EXECUTE_NAME = "ExecuteCode";

	/**
	 * Maximum time to use for for the compilation of the user code (in seconds).
	 */
	public static final int COMPILE_TIMEOUT_SECONDS = 3;

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
			//*** PARAMETER_SEPARATOR_PATTERN CODE ***
			//********************
			long cSharpc = System.nanoTime();
			Process cSharpProcess = new ProcessBuilder("csc","/out:" + EXECUTE_NAME + ".exe","*.cs").directory(codeDirectory).redirectErrorStream(true).start();
			if (cSharpProcess.waitFor(CSharpExecutor.COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (cSharpProcess.exitValue() != 0)
					throw new ExecutorException(true, "Could not compile the user code.", this.readConsole(cSharpProcess));

			} else {
				cSharpProcess.destroyForcibly(); //destroy()
				throw new ExecutorException(true, "Could not compile the user code in time.");
			}
			long cppcEnd = System.nanoTime();

			//********************
			//*** EXECUTE CODE ***
			//********************
			long cSharpStart = System.nanoTime();
			Process cSharpExecuteProcess = new ProcessBuilder(codeDirectory.getAbsolutePath() + "\\" + EXECUTE_NAME + ".exe").redirectErrorStream(true).start();
			if (cSharpExecuteProcess.waitFor(CSharpExecutor.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (cSharpExecuteProcess.exitValue() != 0)
					throw new ExecutorException(true, "Could not execute the user code.", this.readConsole(cSharpExecuteProcess));

			} else {
				cSharpExecuteProcess.destroyForcibly(); //destroy()
				throw new ExecutorException(true, "Could not execute the user code in time.");
			}
			long cSharpEnd = System.nanoTime();

			//****************************
			//*** TEST CASE EVALUATION ***
			//****************************
			try (Scanner outStm = new Scanner(cSharpExecuteProcess.getInputStream(), //create a scanner to read the console output case by case
																				CSharpExecutor.CODE_CHARSET.name()).useDelimiter(String.format("%n%n"))) {
				while (outStm.hasNext()) {
					String res = outStm.next(); //get output lines of next test case
					results.add(new Result(res.startsWith("OK"), false,
																 res.substring(3),
																 new PerformanceIndicators((cppcEnd - cSharpc) / 1e6)));
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

	private void generateCodeFile(File directory, String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases) throws ExecutorException {

		try {
			StringBuilder code = this.getTemplate(); //read the template

			int fragStart = code.indexOf(CSharpExecutor.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + CSharpExecutor.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			int caseStart = code.indexOf(CSharpExecutor.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
			code.replace(caseStart, caseStart + CSharpExecutor.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(functions, testCases));

			Files.write(Paths.get(directory.getPath(), "main.cs"), //create a c++ source file in the temporary directory
									code.toString().getBytes(CSharpExecutor.CODE_CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
			throw new ExecutorException(true, "Could not generate the code file.", ex);
		}
	}
	private String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {

			//validate input / output types & names
			if (function.getInputTypesSize() != function.getInputNamesSize())
				throw new ExecutorException(true, "The same number of input types & names have to be defined.");
			if (function.getOutputTypesSize() != 1 || function.getOutputTypesSize() != function.getOutputNamesSize())
				throw new ExecutorException(true, "Exactly one output type has to be defined for a java sample.");

			sb.append("public static").append(this.getCSharpType(function.getOutputTypes().get(0))).append(' ');
			sb.append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getCSharpType(function.getInputTypes().get(i))).append(' ').append(function.getInputNames().get(i));
			}

			sb.append(") {").append(newLine).append("\t").append(newLine).append('}').append(newLine);
		}

		return sb.toString();
	}

	private String getTestCaseSignatures(List<FunctionSignature> functions, List<TestCase> testCases) throws ExecutorException {

		final String newLine = String.format("%n");

		Map<String, FunctionSignature> functionsMap = functions.stream().collect(Collectors.toMap(FunctionSignature::getName, f -> f));

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			FunctionSignature function = functionsMap.get(testCase.getFunctionName());

			//validate input / output types & values
			if (testCase.getInputValuesSize() != function.getInputTypesSize())
				throw new ExecutorException(true, "The same number of input values & types have to be defined.");
			if (testCase.getExpectedOutputValuesSize() != 1 || testCase.getExpectedOutputValuesSize() != function.getOutputTypesSize())
				throw new ExecutorException(true, "Exactly one output value has to be defined for a java sample.");

			sb.append("try {").append(newLine); //begin test case block

			String oType = function.getOutputTypes().get(0); //test case invocation and return value storage
			sb.append(this.getCSharpType(oType)).append(" ret = ").append("inst.").append(testCase.getFunctionName()).append('(');
			for (int i = 0; i < testCase.getInputValuesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i), function.getInputTypes().get(i)));
			}
			sb.append(");").append(newLine);

			sb.append("Boolean suc = ret"); //begin validation of return value

			boolean useEquals = false;
			switch (oType) {
				case executorConstants.TypeCharacter:
				case executorConstants.TypeBoolean:
				case executorConstants.TypeByte:
				case executorConstants.TypeShort:
				case executorConstants.TypeInteger:
				case executorConstants.TypeLong:
				case executorConstants.TypeSingle:
				case executorConstants.TypeDouble:
					sb.append(" == "); //compare primitive types using equality operator
					break;

				case executorConstants.TypeString:
				case executorConstants.TypeDecimal:
				default:
					useEquals = true;
					sb.append(".Equals("); //compare objects using equality method
					break;

				//default:
				//throw new ExecutorException(String.format("Value type %s is not supported.", oType));
			}

			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0), oType)); //expected output

			if (useEquals)
				sb.append(')'); //close equality method parentheses

			sb.append(';').append(newLine); //finish validation of return value

			sb.append("Console.Write(\"{0}:{1}%n%n\", suc ? \"OK\" : \"ER\", ret);").append(newLine); //print result to the console

			sb.append("} catch (Exception ex) {").append(newLine); //finish test case block / begin exception handling
			sb.append("Console.Write(\"ER:\");");
			sb.append("Console.Write(ex.StackTrace);").append(newLine);
			sb.append('}').append(newLine); //finish exception handling
		}

		return sb.toString();
	}

	private String getValueLiteral(String value, String type) throws ExecutorException {

		if ("null".equals(value))
			return "null";

		//check for collection container types
		boolean isArr = type.startsWith(String.format("%s<", executorConstants.TypeContainerArray));
		boolean isLst = type.startsWith(String.format("%s<", executorConstants.TypeContainerList));
		boolean isSet = type.startsWith(String.format("%s<", executorConstants.TypeContainerSet));
		if (isArr || isLst || isSet) {
			int cntTypLen = (isArr ? executorConstants.TypeContainerArray : isLst ? executorConstants.TypeContainerList : executorConstants.TypeContainerSet).length();
			String elmTyp = type.substring(cntTypLen + 1, type.length() - cntTypLen - 2);

			if (JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(elmTyp).length != 1) //validate type parameters
				throw new ExecutorException(true, "Array, List & Set types need 1 type parameter.");

			StringBuilder sb = new StringBuilder();
			if (isArr) //begin array initialisation syntax
				sb.append("new ").append(this.getCSharpClass(elmTyp)).append("[] { ");
			else if (isLst) //begin list initialisation using helper method
				sb.append(String.format("List<%s> {", this.getCSharpClass(elmTyp)));
			else //begin set initialisation using constructor and helper method
				sb.append(String.format("new HashSet<%1$s>(new List<%1$s>{", this.getCSharpClass(elmTyp)));

			boolean first = true; //generate collection elements
			for (String elm : JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(value)) {
				if (first) first = false;
				else sb.append(", ");
				sb.append(this.getValueLiteral(elm, elmTyp));
			}

			return sb.append(isArr ? " }" : isLst ? '}' : "})").toString(); //finish collection initialisation and return literal

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String elmTyp = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - 1);
			String[] kvTyps = JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(elmTyp);

			if (kvTyps.length != 2) // validate type parameters
				throw new ExecutorException(true, "Map type needs 2 type parameters.");

			StringBuilder sb = new StringBuilder(); //begin map initialisation using anonymous class with initialisation block
			sb.append(String.format("new HashMap<%s, %s>() {{ ", this.getCSharpClass(kvTyps[0]), this.getCSharpClass(kvTyps[1])));

			for (String ety : JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(value)) { //generate key/value pairs
				String[] kv = JavaProcessExecutor.KEY_VALUE_SEPARATOR_PATTERN.split(ety);

				if (kv.length != 2) //validate key/value pair
					throw new ExecutorException(true, "Map entries always need a key and a value.");

				sb.append("put(").append(this.getValueLiteral(kv[0], kvTyps[0])).append(", ").append(this.getValueLiteral(kv[1], kvTyps[1])).append("); ");
			}

			return sb.append("}}").toString(); //finish initialisation and return literal
		}

		switch (type) { //switch over basic types
			case executorConstants.TypeString:
			case executorConstants.TypeCharacter:
				ByteBuffer valueChars = JavaProcessExecutor.CODE_CHARSET.encode(value);
				String valueSafe = IntStream.range(0, valueChars.remaining()).map(i -> valueChars.get()).mapToObj(i -> String.format("\\u%04X", i))
																		.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

				char separator = type == executorConstants.TypeCharacter ? '\'' : '"';
				return String.format("%1$c%2$s%1$c", separator, valueSafe);

			case executorConstants.TypeBoolean:
				return Boolean.toString("true".equalsIgnoreCase(value));

			case executorConstants.TypeByte:
				return Byte.toString(Byte.parseByte(value));

			case executorConstants.TypeShort:
				return Short.toString(Short.parseShort(value));

			case executorConstants.TypeInteger:
				return Integer.toString(Integer.parseInt(value));

			case executorConstants.TypeLong:
				return String.format("%dL", Long.parseLong(value));

			case executorConstants.TypeSingle:
				return String.format("%ff", Float.parseFloat(value));

			case executorConstants.TypeDouble:
				return Double.toString(Double.parseDouble(value));

			case executorConstants.TypeDecimal:
				return String.format("new BigDecimal(\"%s\")", new BigDecimal(value).toPlainString());

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}

	private String getCSharpType(String type) throws ExecutorException {

		switch (type) { //switch over primitive types
			case executorConstants.TypeCharacter:
				return "char";

			case executorConstants.TypeBoolean:
				return "Boolean";

			case executorConstants.TypeByte:
				return "byte";

			case executorConstants.TypeShort:
				return "short";

			case executorConstants.TypeInteger:
				return "int";

			case executorConstants.TypeLong:
				return "long";

			case executorConstants.TypeSingle:
				return "float";

			case executorConstants.TypeDouble:
				return "double";

			default:
				return getCSharpClass(type);
		}
	}

	private String getCSharpClass(String type) throws ExecutorException {

		//check for collection container types
		boolean isArr = type.startsWith(String.format("%s<", executorConstants.TypeContainerArray));
		boolean isLst = type.startsWith(String.format("%s<", executorConstants.TypeContainerList));
		boolean isSet = type.startsWith(String.format("%s<", executorConstants.TypeContainerSet));
		if (isArr || isLst || isSet) {
			String typeParam = type.substring(executorConstants.TypeContainerArray.length() + 1, type.length() - executorConstants.TypeContainerArray.length() - 2);

			if (JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(typeParam).length != 1) //validate type parameters
				throw new ExecutorException(true, "Array, List & Set types need 1 type parameter.");

			return String.format(isArr ? "%s[]" : isLst ? "List<%s>" : "Set<%s>", this.getCSharpClass(typeParam)); //return class name

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String typeParams = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - 1);
			String[] typeParamsArray = JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(typeParams);

			if (typeParamsArray.length != 2) // validate type parameters
				throw new ExecutorException(true, "Map type needs 2 type parameters.");

			return String.format("Map<%s, %s>", this.getCSharpClass(typeParamsArray[0]), this.getCSharpClass(typeParamsArray[1])); //return class name
		}

		switch (type) { //switch over basic types
			case executorConstants.TypeString:
				return "String";

			case executorConstants.TypeCharacter:
				return "Character";

			case executorConstants.TypeBoolean:
				return "Boolean";

			case executorConstants.TypeByte:
				return "Byte";

			case executorConstants.TypeShort:
				return "Short";

			case executorConstants.TypeInteger:
				return "Integer";

			case executorConstants.TypeLong:
				return "Long";

			case executorConstants.TypeSingle:
				return "Float";

			case executorConstants.TypeDouble:
				return "Double";

			case executorConstants.TypeDecimal:
				return "BigInteger";

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}
}
