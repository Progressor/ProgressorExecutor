package ch.bfh.progressor.executor.languages;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import ch.bfh.progressor.executor.CodeExecutor;
import ch.bfh.progressor.executor.ExecutorException;
import ch.bfh.progressor.executor.thrift.PerformanceIndicators;
import ch.bfh.progressor.executor.thrift.Result;
import ch.bfh.progressor.executor.thrift.TestCase;
import ch.bfh.progressor.executor.thrift.executorConstants;

/**
 * Code execution engine for java code.
 * Uses a new process to execute the custom java code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class JavaProcessExecutor implements CodeExecutor {

	/**
	 * Character set to use for the custom code.
	 */
	public static final Charset CODE_CHARSET = Charset.forName("UTF-8");

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "java";

	/**
	 * Path to the file containing the code template to put the fragment into.
	 */
	protected static final Path CODE_TEMPLATE = Paths.get("resources", JavaProcessExecutor.CODE_LANGUAGE, "template-process.java");

	/**
	 * Path to the file containing the blacklist for this language.
	 */
	protected static final Path CODE_BLACKLIST = Paths.get("resources", JavaProcessExecutor.CODE_LANGUAGE, "blacklist.txt");

	/**
	 * Name of the class as defined in the template.
	 */
	protected static final String CODE_CLASS_NAME = "CustomClass";

	/**
	 * Placeholder for the custom code fragment as defined in the template.
	 */
	protected static final String CODE_CUSTOM_FRAGMENT = "$CustomCode$";

	/**
	 * Placeholder for the test cases as defined in the template.
	 */
	protected static final String TEST_CASES_FRAGMENT = "$TestCases$";

	/**
	 * Maximum time to use for for the compilation of the user code (in seconds).
	 */
	public static final int COMPILE_TIMEOUT_SECONDS = 3;

	/**
	 * Maximum time to use for the execution of the user code (in seconds).
	 */
	public static final int EXECUTION_TIMEOUT_SECONDS = 5;

	private static final Pattern PARAMETER_SEPARATOR_PATTERN = Pattern.compile(",\\s*");

	private static final Pattern KEY_VALUE_SEPARATOR_PATTERN = Pattern.compile(":\\s*");

	private List<String> blacklist;
	private StringBuilder template;

	@Override
	public String getLanguage() {
		return JavaProcessExecutor.CODE_LANGUAGE;
	}

	@Override
	public List<String> getBlacklist() {

		if (this.blacklist == null)
			try {
				this.blacklist = Collections.unmodifiableList(Files.readAllLines(JavaProcessExecutor.CODE_BLACKLIST, JavaProcessExecutor.CODE_CHARSET));

			} catch (IOException ex) {
				throw new UncheckedIOException(ex); //do not handle i/o exception
			}

		return this.blacklist; //return the same unmodifiable list every time
	}

	private StringBuilder getTemplate() throws IOException {

		if (this.template == null)
			this.template = new StringBuilder(new String(Files.readAllBytes(JavaProcessExecutor.CODE_TEMPLATE), JavaProcessExecutor.CODE_CHARSET)); //read template to StringBuilder

		return new StringBuilder(this.template); //return a new string builder every time
	}

	@Override
	public List<Result> execute(String codeFragment, List<TestCase> testCases) {

		List<Result> results = new ArrayList<>(testCases.size());
		File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory
		Runtime rnt = Runtime.getRuntime();

		try {
			if (!codeDirectory.exists() && !codeDirectory.mkdirs())
				throw new ExecutorException("Could not create a temporary directory for the user code.");

			//*********************
			//*** GENERATE CODE ***
			//*********************
			this.generateCodeFile(codeDirectory, codeFragment, testCases);

			//********************
			//*** PARAMETER_SEPARATOR_PATTERN CODE ***
			//********************
			long javacStart = System.currentTimeMillis();
			Process javacProcess = rnt.exec("javac *.java", null, codeDirectory);
			if (javacProcess.waitFor(JavaProcessExecutor.COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (javacProcess.exitValue() != 0)
					throw new ExecutorException("Could not compile the user code.", this.readConsole(javacProcess));

			} else {
				javacProcess.destroyForcibly(); //destroy()
				throw new ExecutorException("Could not compile the user code in time.");
			}
			long javacEnd = System.currentTimeMillis();

			//********************
			//*** EXECUTE CODE ***
			//********************
			long javaStart = System.currentTimeMillis();
			Process javaProcess = rnt.exec(String.format("java %s", JavaProcessExecutor.CODE_CLASS_NAME), null, codeDirectory);
			if (javaProcess.waitFor(JavaProcessExecutor.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (javaProcess.exitValue() != 0)
					throw new ExecutorException("Could not execute the user code.", this.readConsole(javaProcess));

			} else {
				javaProcess.destroyForcibly(); //destroy()
				throw new ExecutorException("Could not execute the user code in time.");
			}
			long javaEnd = System.currentTimeMillis();

			//****************************
			//*** TEST CASE EVALUATION ***
			//****************************
			try (Scanner outStm = new Scanner(javaProcess.getInputStream(), //create a scanner to read the console output case by case
																				JavaProcessExecutor.CODE_CHARSET.name()).useDelimiter(String.format("%n%n"))) {
				while (outStm.hasNext()) {
					String res = outStm.next(); //get output lines of next test case
					results.add(new Result(res.startsWith("OK"),
																 res.substring(3),
																 new PerformanceIndicators(Math.toIntExact(javaEnd - javaStart))));
				}
			}

			//**************************
			//*** EXCEPTION HANDLING ***
			//**************************
		} catch (Exception ex) {
			Result res = new Result().setSuccess(false);
			if (ex instanceof ExecutorException && ((ExecutorException)ex).getOutput() != null)
				res.setResult(String.format("%s:%n%s", ex.getMessage(), ((ExecutorException)ex).getOutput()));
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

	private boolean deleteRecursive(File file) {

		boolean ret = true;

		File[] children; //recursively delete children
		if (file.isDirectory() && (children = file.listFiles()) != null)
			for (File child : children)
				ret &= this.deleteRecursive(child);

		ret &= file.delete(); //delete file itself
		return ret;
	}

	private String readConsole(Process process) throws ExecutorException {

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), JavaProcessExecutor.CODE_CHARSET))) {
			StringBuilder sb = new StringBuilder();

			String line; //read every line
			while ((line = reader.readLine()) != null)
				sb.append(line).append(String.format("%n"));

			return sb.toString(); //create concatenated string

		} catch (IOException ex) {
			throw new ExecutorException("Could not read the console output.", ex);
		}
	}

	private void generateCodeFile(File directory, String codeFragment, List<TestCase> testCases) throws ExecutorException {

		try {
			StringBuilder code = this.getTemplate(); //read the template

			int fragStart = code.indexOf(JavaProcessExecutor.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + JavaProcessExecutor.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			int caseStart = code.indexOf(JavaProcessExecutor.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
			code.replace(caseStart, caseStart + JavaProcessExecutor.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(testCases));

			Files.write(Paths.get(directory.getPath(), String.format("%s.java", JavaProcessExecutor.CODE_CLASS_NAME)), //create a java source file in the temporary directory
									code.toString().getBytes(JavaProcessExecutor.CODE_CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
			throw new ExecutorException("Could not generate the code file.", ex);
		}
	}

	private String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		final String NEWLINE = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {

			//validate input / output types & values
			if (testCase.getInputValuesSize() != testCase.getInputTypesSize())
				throw new ExecutorException("The same number of input values & types have to be defined..");
			if (testCase.getExpectedOutputValuesSize() != 1 || testCase.getExpectedOutputValuesSize() != testCase.getOutputTypesSize())
				throw new ExecutorException("Exactly one output value has to be defined for a java sample.");

			sb.append("try {").append(NEWLINE); //begin test case block

			String oType = testCase.getOutputTypes().get(0); //test case invocation and return value storage
			sb.append(this.getJavaClass(oType)).append(" ret = ").append("inst.").append(testCase.getFunctionName()).append('(');
			for (int i = 0; i < testCase.getInputValuesSize(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i), testCase.getInputTypes().get(i)));
			}
			sb.append(");").append(NEWLINE);

			sb.append("boolean suc = ret "); //begin validation of return value

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
					sb.append(".equals("); //compare objects using equality method
					break;

				//default:
				//throw new ExecutorException(String.format("Value type %s is not supported.", oType));
			}

			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0), oType)); //expected output

			if (oType.equals(executorConstants.TypeString) || oType.equals(executorConstants.TypeDecimal))
				sb.append(')'); //close equality method parentheses

			sb.append(';').append(NEWLINE); //finish validation of return value

			sb.append("System.out.printf(\"%s:%s%n%n\", suc ? \"OK\" : \"ER\", ret);").append(NEWLINE); //print result to the console

			sb.append("} catch (Exception ex) {").append(NEWLINE); //finish test case block / begin exception handling
			sb.append("System.out.print(\"ER:\");");
			sb.append("ex.printStackTrace(System.out);");
			sb.append('}').append(NEWLINE); //finish exception handling
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
				throw new ExecutorException("Array, List & Set types need 1 type parameter.");

			StringBuilder sb = new StringBuilder();
			if (isArr) //begin array initialisation syntax
				sb.append("new ").append(this.getJavaClass(elmTyp)).append("[] { ");
			else if (isLst) //begin list initialisation using helper method
				sb.append(String.format("Arrays.<%s>asList(", this.getJavaClass(elmTyp)));
			else //begin set initialisation using constructor and helper method
				sb.append(String.format("new HashSet<%1$s>(Arrays.<%1$s>asList(", this.getJavaClass(elmTyp)));

			boolean first = true; //generate collection elements
			for (String elm : JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(value)) {
				if (first) first = false;
				else sb.append(", ");
				sb.append(this.getValueLiteral(elm, elmTyp));
			}

			return sb.append(isArr ? " }" : isLst ? ')' : "))").toString(); //finish collection initialisation and return literal

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String elmTyp = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - executorConstants.TypeContainerMap.length() - 2);
			String[] kvTyps = JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(elmTyp);

			if (kvTyps.length != 2) // validate type parameters
				throw new ExecutorException("Map type needs 2 type parameters.");

			StringBuilder sb = new StringBuilder(); //begin map initialisation using anonymous class with initialisation block
			sb.append(String.format("new HashMap<%s, %s>() {{ ", this.getJavaClass(kvTyps[0]), this.getJavaClass(kvTyps[1])));

			for (String ety : JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(value)) { //generate key/value pairs
				String[] kv = JavaProcessExecutor.KEY_VALUE_SEPARATOR_PATTERN.split(ety);

				if (kv.length != 2) //validate key/value pair
					throw new ExecutorException("Map entries always need a key and a value.");

				sb.append("put(").append(this.getValueLiteral(kv[0], kvTyps[0])).append(", ").append(this.getValueLiteral(kv[0], kvTyps[0])).append("); ");
			}

			return sb.append("}};").toString(); //finish initialisation and return literal
		}

		switch (type) { //switch over basic types
			case executorConstants.TypeString:
				return String.format("\"%s\"", value);

			case executorConstants.TypeCharacter:
				return String.format("'%s'", value);

			case executorConstants.TypeBoolean:
				return Boolean.toString("true".equalsIgnoreCase(value));

			case executorConstants.TypeByte:
			case executorConstants.TypeShort:
			case executorConstants.TypeInteger:
			case executorConstants.TypeDouble:
				return String.format("%s", value);

			case executorConstants.TypeLong:
				return String.format("%sL", value);

			case executorConstants.TypeSingle:
				return String.format("%sf", value);

			case executorConstants.TypeDecimal:
				return String.format("new BigDecimal(\"%s\")", value);

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", type));
		}
	}

	private String getJavaClass(String type) throws ExecutorException {

		//check for collection container types
		boolean isArr = type.startsWith(String.format("%s<", executorConstants.TypeContainerArray));
		boolean isLst = type.startsWith(String.format("%s<", executorConstants.TypeContainerList));
		boolean isSet = type.startsWith(String.format("%s<", executorConstants.TypeContainerSet));
		if (isArr || isLst || isSet) {
			String typeParam = type.substring(executorConstants.TypeContainerArray.length() + 1, type.length() - executorConstants.TypeContainerArray.length() - 2);

			if (JavaProcessExecutor.PARAMETER_SEPARATOR_PATTERN.split(typeParam).length != 1) //validate type parameters
				throw new ExecutorException("Array, List & Set types need 1 type parameter.");

			return String.format(isArr ? "%s[]" : isLst ? "List<%s>" : "Set<%s>", this.getJavaClass(typeParam)); //return class name

			//check for map container type
		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String typeParams = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - executorConstants.TypeContainerMap.length() - 2);
			String[] typeParamsArray = typeParams.split(",\\s*");

			if (typeParamsArray.length != 2) // validate type parameters
				throw new ExecutorException("Map type needs 2 type parameters.");

			return String.format("Map<%s, %s>", this.getJavaClass(typeParamsArray[0]), this.getJavaClass(typeParamsArray[1])); //return class name
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
				return "BigDecimal";

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", type));
		}
	}
}
