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
 * Code execution engine for C++ code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class CPlusPlusExecutor extends CodeExecutor {

	/**
	 * Character set to use for the custom code.
	 */
	public static final Charset CODE_CHARSET = Charset.forName("UTF-8");

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "cpp";

	/**
	 * Name of the executable.
	 */
	public static final String EXECUTABLE_NAME = "main";

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
		return CPlusPlusExecutor.CODE_LANGUAGE;
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
			long gccStart = System.nanoTime();
			Process gccProcess = new ProcessBuilder("g++", "*.cpp", "-std=c++11", "-o", CPlusPlusExecutor.EXECUTABLE_NAME).directory(codeDirectory).redirectErrorStream(true).start();
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
			long cppStart = System.nanoTime();
			Process cppProcess = new ProcessBuilder(CPlusPlusExecutor.EXECUTABLE_NAME).directory(codeDirectory).redirectErrorStream(true).start();
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
			try (Scanner outStm = new Scanner(cppProcess.getInputStream(), //create a scanner to read the console output case by case
																				CPlusPlusExecutor.CODE_CHARSET.name()).useDelimiter(String.format("%n%n"))) {
				while (outStm.hasNext()) {
					String res = outStm.next(); //get output lines of next test case
					results.add(new Result(res.startsWith("OK"), false,
																 res.substring(3),
																 new PerformanceIndicators((gccEnd - gccStart) / 1e6)));
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

			int fragStart = code.indexOf(CodeExecutor.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + CodeExecutor.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			int caseStart = code.indexOf(CodeExecutor.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
			code.replace(caseStart, caseStart + CodeExecutor.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(functions, testCases));

			Files.write(Paths.get(directory.getPath(), "main.cpp"), //create a c++ source file in the temporary directory
									code.toString().getBytes(CPlusPlusExecutor.CODE_CHARSET)); //and write the generated code in it

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
				throw new ExecutorException(true, "Exactly one output type has to be defined for a C++ sample.");

			sb.append("public ").append(this.getCppType(function.getOutputTypes().get(0))).append(' ');
			sb.append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getCppType(function.getInputTypes().get(i))).append(' ').append(function.getInputNames().get(i));
			}

			sb.append(") {").append(newLine).append('\t').append(newLine).append('}').append(newLine);
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
				throw new ExecutorException(true, "Exactly one output value has to be defined for a C++ sample.");

			sb.append("try {").append(newLine); //begin test case block

			String oType = function.getOutputTypes().get(0); //test case invocation and return value storage
			sb.append(this.getCppType(oType)).append(" ret = ").append(testCase.getFunctionName()).append('(');
			for (int i = 0; i < testCase.getInputValuesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i), function.getInputTypes().get(i)));
			}
			sb.append(");").append(newLine);

			sb.append("bool suc = ret == "); //validate return value
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0), oType)).append(';').append(newLine);

			sb.append("cout << (suc ? \"OK\" : \"ER\") << \":\" << ret << endl << endl;").append(newLine); //print result to the console
			sb.append("} catch (const exception &ex) {").append(newLine); //finish test case block / begin exception handling (standard exception class)
			sb.append("cout << \"ER:\" << ex.what() << endl << endl;").append(newLine);
			sb.append("} catch (const string &ex) {").append(newLine); //secondary exception handling (exception string)
			sb.append("cout << \"ER:\" << ex << endl << endl;").append(newLine);
			sb.append("} catch (...) {").append(newLine); //last resort (handling all unknown exceptions)
			sb.append("cout << \"ER:unknown exception\" << endl << endl;").append(newLine);
			sb.append('}').append(newLine); //finish exception handling
		}

		return sb.toString();
	}

	private String getValueLiteral(String value, String type) throws ExecutorException {

		if ("null".equals(value))
			return "nullptr";

		//check for collection container types
		boolean isArr = type.startsWith(String.format("%s<", executorConstants.TypeContainerArray));
		boolean isLst = type.startsWith(String.format("%s<", executorConstants.TypeContainerList));
		boolean isSet = type.startsWith(String.format("%s<", executorConstants.TypeContainerSet));
		if (isArr || isLst || isSet) {
			int cntTypLen = (isArr ? executorConstants.TypeContainerArray : isLst ? executorConstants.TypeContainerList : executorConstants.TypeContainerSet).length();
			String elmTyp = type.substring(cntTypLen + 1, type.length() - cntTypLen - 2);

			if (CodeExecutor.PARAMETER_SEPARATOR_PATTERN.split(elmTyp).length != 1) //validate type parameters
				throw new ExecutorException(true, "Array, List & Set types need 1 type parameter.");

			String[] elms = CodeExecutor.PARAMETER_SEPARATOR_PATTERN.split(value);

			StringBuilder sb = new StringBuilder();
			if (isArr) //begin array initialisation syntax
				sb.append("new ").append(this.getCppType(elmTyp)).append('[').append(elms.length).append("] { ");
			else if (isLst) //begin list initialisation using helper method
				sb.append(String.format("list<%s>(", this.getCppType(elmTyp)));
			else //begin set initialisation using constructor and helper method
				sb.append(String.format("set<%s>{", this.getCppType(elmTyp)));

			boolean first = true; //generate collection elements
			for (String elm : elms) {
				if (first) first = false;
				else sb.append(", ");
				sb.append(this.getValueLiteral(elm, elmTyp));
			}

			return sb.append(" }").toString(); //finish collection initialisation and return literal

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String elmTyp = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - 1);
			String[] kvTyps = CodeExecutor.PARAMETER_SEPARATOR_PATTERN.split(elmTyp);

			if (kvTyps.length != 2) // validate type parameters
				throw new ExecutorException(true, "Map type needs 2 type parameters.");

			StringBuilder sb = new StringBuilder(); //begin map initialisation
			sb.append(String.format("map<%s, %s> { ", this.getCppType(kvTyps[0]), this.getCppType(kvTyps[1])));

			for (String ety : CodeExecutor.PARAMETER_SEPARATOR_PATTERN.split(value)) { //generate key/value pairs
				String[] kv = CodeExecutor.KEY_VALUE_SEPARATOR_PATTERN.split(ety);

				if (kv.length != 2) //validate key/value pair
					throw new ExecutorException(true, "Map entries always need a key and a value.");

				sb.append('{').append(this.getValueLiteral(kv[0], kvTyps[0])).append(", ").append(this.getValueLiteral(kv[1], kvTyps[1])).append("}, ");
			}

			return sb.append('}').toString(); //finish initialisation and return literal
		}

		switch (type) { //switch over basic types
			case executorConstants.TypeString:
			case executorConstants.TypeCharacter:
				ByteBuffer valueChars = CPlusPlusExecutor.CODE_CHARSET.encode(value);
				String valueSafe = IntStream.range(0, valueChars.remaining()).map(i -> valueChars.get()).mapToObj(i -> String.format("\\u%04X", i))
																		.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

				char separator = type.equals(executorConstants.TypeCharacter) ? '\'' : '"';
				return String.format("%1$c%2$s%1$c", separator, valueSafe);

			case executorConstants.TypeBoolean:
				return Boolean.toString("true".equalsIgnoreCase(value));

			case executorConstants.TypeByte:
				return Byte.toString(Byte.parseByte(value));

			case executorConstants.TypeShort:
				return Short.toString(Short.parseShort(value));

			case executorConstants.TypeInteger:
				return String.format("%dL", Integer.parseInt(value));

			case executorConstants.TypeLong:
				return String.format("%dLL", Long.parseLong(value));

			case executorConstants.TypeSingle:
				return String.format("%fF", Float.parseFloat(value));

			case executorConstants.TypeDouble:
				return Double.toString(Double.parseDouble(value));

			case executorConstants.TypeDecimal:
				return String.format("%sL", new BigDecimal(value).toPlainString());

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}

	private String getCppType(String type) throws ExecutorException {

		//check for collection container types
		boolean isArr = type.startsWith(String.format("%s<", executorConstants.TypeContainerArray));
		boolean isLst = type.startsWith(String.format("%s<", executorConstants.TypeContainerList));
		boolean isSet = type.startsWith(String.format("%s<", executorConstants.TypeContainerSet));
		if (isArr || isLst || isSet) {
			String typeParam = type.substring(executorConstants.TypeContainerArray.length() + 1, type.length() - executorConstants.TypeContainerArray.length() - 2);

			if (CodeExecutor.PARAMETER_SEPARATOR_PATTERN.split(typeParam).length != 1) //validate type parameters
				throw new ExecutorException(true, "Array, List & Set types need 1 type parameter.");

			return String.format(isArr ? "%s[]" : isLst ? "List<%s>" : "Set<%s>", this.getCppType(typeParam)); //return class name

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String typeParams = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - 1);
			String[] typeParamsArray = CodeExecutor.PARAMETER_SEPARATOR_PATTERN.split(typeParams);

			if (typeParamsArray.length != 2) // validate type parameters
				throw new ExecutorException(true, "Map type needs 2 type parameters.");

			return String.format("map<%s, %s>", this.getCppType(typeParamsArray[0]), this.getCppType(typeParamsArray[1])); //return class name
		}

		switch (type) { //switch over primitive types
			case executorConstants.TypeString:
				return "string";

			case executorConstants.TypeCharacter:
				return "char";

			case executorConstants.TypeBoolean:
				return "bool";

			case executorConstants.TypeByte:
				return "signed char";

			case executorConstants.TypeShort:
				return "int";

			case executorConstants.TypeInteger:
				return "long int";

			case executorConstants.TypeLong:
				return "long long int";

			case executorConstants.TypeSingle:
				return "float";

			case executorConstants.TypeDouble:
				return "double";

			case executorConstants.TypeDecimal:
				return "long double";

			default:
				throw new ExecutorException(true, String.format("Value type %s is not supported.", type));
		}
	}
}
