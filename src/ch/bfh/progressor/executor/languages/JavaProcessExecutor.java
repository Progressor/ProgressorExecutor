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
import ch.bfh.progressor.executor.CodeExecutor;
import ch.bfh.progressor.executor.ExecutorException;
import ch.bfh.progressor.executor.PerformanceIndicators;
import ch.bfh.progressor.executor.Result;
import ch.bfh.progressor.executor.TestCase;
import ch.bfh.progressor.executor.executorConstants;

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
	 * Path to the file containing the code template to put the fragment into.
	 */
	protected static final Path CODE_TEMPLATE = Paths.get("resources", "java", "template-process.java");

	/**
	 * Path to the file containing the blacklist for this language.
	 */
	protected static final Path CODE_BLACKLIST = Paths.get("resources", "java", "blacklist.txt");

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

	private List<String> blacklist;
	private StringBuilder template;

	@Override
	public String getLanguage() {
		return "java";
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

			this.generateCodeFile(codeDirectory, codeFragment, testCases);

			long javacStart = System.nanoTime();
			Process javacProcess = rnt.exec("javac *.java", null, codeDirectory);
			if (javacProcess.waitFor(JavaProcessExecutor.COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (javacProcess.exitValue() != 0)
					throw new ExecutorException("Could not compile the user code.", this.readConsole(javacProcess));

			} else {
				javacProcess.destroyForcibly(); //destroy()
				throw new ExecutorException("Could not compile the user code in time.");
			}
			long javacEnd = System.nanoTime();

			long javaStart = System.nanoTime();
			Process javaProcess = rnt.exec(String.format("java %s", JavaProcessExecutor.CODE_CLASS_NAME), null, codeDirectory);
			if (javaProcess.waitFor(JavaProcessExecutor.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				if (javaProcess.exitValue() != 0)
					throw new ExecutorException("Could not execute the user code.", this.readConsole(javaProcess));

			} else {
				javaProcess.destroyForcibly(); //destroy()
				throw new ExecutorException("Could not execute the user code in time.");
			}
			long javaEnd = System.nanoTime();

			try (Scanner outStm = new Scanner(javaProcess.getInputStream(), //create a scanner to read the console output case by case
																				JavaProcessExecutor.CODE_CHARSET.name()).useDelimiter(String.format("%n%n"))) {
				while (outStm.hasNext()) {
					String res = outStm.next(); //get output lines of next test case
					results.add(new Result().setSuccess(res.startsWith("OK")).setResult(res.substring(3))
																	.setPerformance(new PerformanceIndicators().setRuntimeMilliSeconds((javaEnd - javaStart) / 1000)));
				}
			}

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

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {

			if (testCase.getInputValuesSize() != testCase.getInputTypesSize())
				throw new ExecutorException("The same number of input values & types have to be defined..");

			if (testCase.getExpectedOutputValuesSize() != 1 || testCase.getOutputTypesSize() != 1)
				throw new ExecutorException("Exactly one output value has to be defined for a java sample.");

			String oType = testCase.getOutputTypes().get(0);
			sb.append("{ ").append(this.getJavaClass(oType)).append(" res = ").append("inst.").append(testCase.getFunctionName()).append('(');
			for (int j = 0; j < testCase.getInputValuesSize(); j++) {
				if (j > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(j), testCase.getInputTypes().get(j)));
			}
			sb.append("); ");

			sb.append("System.out.printf(\"%s:%s%n%n\", res ");
			switch (oType) {
				case executorConstants.TypeCharacter:
				case executorConstants.TypeBoolean:
				case executorConstants.TypeByte:
				case executorConstants.TypeShort:
				case executorConstants.TypeInteger:
				case executorConstants.TypeLong:
				case executorConstants.TypeSingle:
				case executorConstants.TypeDouble:
					sb.append(" == ");
					break;

				case executorConstants.TypeString:
				case executorConstants.TypeDecimal:
					sb.append(".equals(");
					break;

				default:
					throw new ExecutorException(String.format("Value type %s is not supported.", oType));
			}

			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0), oType));

			if (oType.equals(executorConstants.TypeString) || oType.equals(executorConstants.TypeDecimal))
				sb.append(')');

			sb.append(" ? \"OK\" : \"ER\", res); }").append(String.format("%n"));
		}

		return sb.toString();
	}

	private String getValueLiteral(String value, String type) throws ExecutorException {

		if ("null".equals(value))
			return "null";

		boolean isArr = type.startsWith(String.format("%s<", executorConstants.TypeContainerArray));
		boolean isLst = type.startsWith(String.format("%s<", executorConstants.TypeContainerList));
		boolean isSet = type.startsWith(String.format("%s<", executorConstants.TypeContainerSet));

		if (isArr || isLst || isSet) {
			int cntTypLen = (isArr ? executorConstants.TypeContainerArray : isLst ? executorConstants.TypeContainerList : executorConstants.TypeContainerSet).length();
			String elmTyp = type.substring(cntTypLen + 1, type.length() - cntTypLen - 2);

			if (elmTyp.split(",\\s*").length != 1)
				throw new ExecutorException("Array, List & Set types need 1 type parameter.");

			StringBuilder sb = new StringBuilder();
			if (isArr)
				sb.append("new ").append(this.getJavaClass(elmTyp)).append("[] { ");
			else if (isLst)
				sb.append(String.format("Arrays.<%s>asList(", this.getJavaClass(elmTyp)));
			else
				sb.append(String.format("new HashSet<%1$s>(Arrays.<%1$s>asList(", this.getJavaClass(elmTyp)));

			boolean first = true;
			for (String elm : value.split(",\\s*")) {
				if (first) first = false;
				else sb.append(", ");
				sb.append(this.getValueLiteral(elm, elmTyp));
			}

			return sb.append(isArr ? " }" : isLst ? ')' : "))").toString();

		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String elmTyp = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - executorConstants.TypeContainerMap.length() - 2);
			String[] kvTyps = elmTyp.split(",\\s*");

			if (kvTyps.length != 2)
				throw new ExecutorException("Map type needs 2 type parameters.");

			StringBuilder sb = new StringBuilder();
			sb.append(String.format("new HashMap<%s, %s>() {{ ", this.getJavaClass(kvTyps[0]), this.getJavaClass(kvTyps[1])));

			for (String ety : value.split(",\\s*")) {
				String[] kv = ety.split(":\\s*");

				if (kv.length != 2)
					throw new ExecutorException("Map entries always need a key and a value.");

				sb.append("put(").append(this.getValueLiteral(kv[0], kvTyps[0])).append(", ").append(this.getValueLiteral(kv[0], kvTyps[0])).append("); ");
			}
			return sb.append("}};").toString();
		}

		switch (type) {
			case executorConstants.TypeString:
				return String.format("\"%s\"", value);

			case executorConstants.TypeCharacter:
				return String.format("'%s'", value);

			case executorConstants.TypeBoolean:
				return Boolean.toString("true".equalsIgnoreCase(value));

			case executorConstants.TypeByte:
			case executorConstants.TypeShort:
			case executorConstants.TypeInteger:
				return String.format("%d", Integer.parseInt(value, 10));

			case executorConstants.TypeLong:
				return String.format("%dL", Long.parseLong(value, 10));

			case executorConstants.TypeSingle:
				return String.format("%ff", Float.parseFloat(value));

			case executorConstants.TypeDouble:
				return String.format("%f", Double.parseDouble(value));

			case executorConstants.TypeDecimal:
				return String.format("new BigDecimal(\"%s\")", value);

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", type));
		}
	}

	private String getJavaClass(String type) throws ExecutorException {

		if (type.startsWith(String.format("%s<", executorConstants.TypeContainerArray))) {
			String typeParam = type.substring(executorConstants.TypeContainerArray.length() + 1, type.length() - executorConstants.TypeContainerArray.length() - 2);
			return String.format("%s[]", this.getJavaClass(typeParam));

		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerList))) {
			String typeParam = type.substring(executorConstants.TypeContainerList.length() + 1, type.length() - executorConstants.TypeContainerList.length() - 2);
			return String.format("List<%s>", this.getJavaClass(typeParam));

		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerSet))) {
			String typeParam = type.substring(executorConstants.TypeContainerSet.length() + 1, type.length() - executorConstants.TypeContainerSet.length() - 2);
			return String.format("Set<%s>", this.getJavaClass(typeParam));

		} else if (type.startsWith(String.format("%s<", executorConstants.TypeContainerMap))) {
			String typeParams = type.substring(executorConstants.TypeContainerMap.length() + 1, type.length() - executorConstants.TypeContainerMap.length() - 2);
			String[] typeParamsArray = typeParams.split(",\\s*");
			return String.format("Map<%s, %s>", this.getJavaClass(typeParamsArray[0]), this.getJavaClass(typeParamsArray[1]));
		}

		switch (type) {
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
