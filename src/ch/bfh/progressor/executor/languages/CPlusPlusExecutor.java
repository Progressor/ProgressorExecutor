//package ch.bfh.progressor.executor.languages;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.UncheckedIOException;
//import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Scanner;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//import ch.bfh.progressor.executor.CodeExecutor;
//import ch.bfh.progressor.executor.ExecutorException;
//import ch.bfh.progressor.executor.thrift.FunctionSignature;
//import ch.bfh.progressor.executor.thrift.PerformanceIndicators;
//import ch.bfh.progressor.executor.thrift.Result;
//import ch.bfh.progressor.executor.thrift.TestCase;
//import ch.bfh.progressor.executor.thrift.executorConstants;
//
///**
// * Created by janick on 19.11.2015.
// */
//public class CPlusPlusExecutor implements CodeExecutor {
//
//	/**
//	 * Character set to use for the custom code.
//	 */
//	public static final Charset CODE_CHARSET = Charset.forName("UTF-8");
//
//	/**
//	 * Unique name of the language this executor supports.
//	 */
//	public static final String CODE_LANGUAGE = "cpp";
//
//	/**
//	 * Path to the file containing the code template to put the fragment into.
//	 */
//	protected static final Path CODE_TEMPLATE = Paths.get("resources", CPlusPlusExecutor.CODE_LANGUAGE, "template.cpp");
//
//	/**
//	 * Path to the file containing the blacklist for this language.
//	 */
//	protected static final Path CODE_BLACKLIST = Paths.get("resources", CPlusPlusExecutor.CODE_LANGUAGE, "blacklist.txt");
//
//	/**
//	 * Name of the class as defined in the template.
//	 */
//	protected static final String CODE_CLASS_NAME = "CustomClass";
//
//	/**
//	 * Placeholder for the custom code fragment as defined in the template.
//	 */
//	protected static final String CODE_CUSTOM_FRAGMENT = "$CustomCode$";
//
//	/**
//	 * Placeholder for the test cases as defined in the template.
//	 */
//	protected static final String TEST_CASES_FRAGMENT = "$TestCases$";
//
//	/**
//	 * Maximum time to use for for the compilation of the user code (in seconds).
//	 */
//	public static final int COMPILE_TIMEOUT_SECONDS = 3;
//
//	/**
//	 * Maximum time to use for the execution of the user code (in seconds).
//	 */
//	public static final int EXECUTION_TIMEOUT_SECONDS = 5;
//
//	private static final Pattern BLACKLIST_COMMENT_PATTERN = Pattern.compile("#.*");
//
//	private static final Pattern PARAMETER_SEPARATOR_PATTERN = Pattern.compile(",\\s*");
//
//	private static final Pattern KEY_VALUE_SEPARATOR_PATTERN = Pattern.compile(":\\s*");
//
//	private List<String> blacklist;
//	private String template;
//
//	@Override
//	public String getLanguage() {
//		return CPlusPlusExecutor.CODE_LANGUAGE;
//	}
//
//	@Override
//	public List<String> getBlacklist() {
//
//		if (this.blacklist == null)
//			try {
//				this.blacklist = Files.readAllLines(CPlusPlusExecutor.CODE_BLACKLIST, CPlusPlusExecutor.CODE_CHARSET).stream() //read blacklist
//															.map(l -> CPlusPlusExecutor.BLACKLIST_COMMENT_PATTERN.matcher(l).replaceAll("")) //remove comments
//															.filter(l -> !l.isEmpty()) //only add lines that actually contain information
//															.collect(Collectors.toList());
//
//			} catch (IOException ex) {
//				throw new UncheckedIOException(ex); //do not handle i/o exception
//			}
//
//		return Collections.unmodifiableList(this.blacklist);
//	}
//
//	private StringBuilder getTemplate() throws IOException {
//
//		if (this.template == null)
//			this.template = new String(Files.readAllBytes(CPlusPlusExecutor.CODE_TEMPLATE), CPlusPlusExecutor.CODE_CHARSET); //read template to StringBuilder
//
//		return new StringBuilder(this.template); //return a new string builder every time
//	}
//
//	@Override
//	public String getFragment(List<FunctionSignature> functions) {
//
//		try {
//			return getTestCaseSignatures(functions);
//
//		} catch (ExecutorException ex) {
//			return String.format("%s:%n%s", "Could not generate the test case code fragment(s).", ex);
//		}
//	}
//
//	@Override
//	public List<Result> execute(String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases) {
//
//		List<Result> results = new ArrayList<>(testCases.size());
//		File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory
//		Runtime rnt = Runtime.getRuntime();
//
//		try {
//			if (!codeDirectory.exists() && !codeDirectory.mkdirs())
//				throw new ExecutorException("Could not create a temporary directory for the user code.");
//
//			//*********************
//			//*** GENERATE CODE ***
//			//*********************
//			this.generateCodeFile(codeDirectory, codeFragment, functions, testCases);
//
//			//********************
//			//*** PARAMETER_SEPARATOR_PATTERN CODE ***
//			//********************
//			long javacStart = System.nanoTime();
//			Process javacProcess = rnt.exec("g++ *.cpp", null, codeDirectory);
//			if (javacProcess.waitFor(CPlusPlusExecutor.COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
//				if (javacProcess.exitValue() != 0)
//					throw new ExecutorException("Could not compile the user code.", this.readConsole(javacProcess));
//
//			} else {
//				javacProcess.destroyForcibly(); //destroy()
//				throw new ExecutorException("Could not compile the user code in time.");
//			}
//			long javacEnd = System.nanoTime();
//
//			//********************
//			//*** EXECUTE CODE ***
//			//********************
//			long javaStart = System.nanoTime();
//			Process javaProcess = rnt.exec(String.format("", CPlusPlusExecutor.CODE_CLASS_NAME), null, codeDirectory);
//			if (javaProcess.waitFor(CPlusPlusExecutor.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
//				if (javaProcess.exitValue() != 0)
//					throw new ExecutorException("Could not execute the user code.", this.readConsole(javaProcess));
//
//			} else {
//				javaProcess.destroyForcibly(); //destroy()
//				throw new ExecutorException("Could not execute the user code in time.");
//			}
//			long javaEnd = System.nanoTime();
//
//			//****************************
//			//*** TEST CASE EVALUATION ***
//			//****************************
//			try (Scanner outStm = new Scanner(javaProcess.getInputStream(), //create a scanner to read the console output case by case
//																				CPlusPlusExecutor.CODE_CHARSET.name()).useDelimiter(String.format("%n%n"))) {
//				while (outStm.hasNext()) {
//					String res = outStm.next(); //get output lines of next test case
//					results.add(new Result(res.startsWith("OK"),
//																 res.substring(3),
//																 new PerformanceIndicators((javaEnd - javaStart) / 1e6)));
//				}
//			}
//
//			//**************************
//			//*** EXCEPTION HANDLING ***
//			//**************************
//		} catch (Exception ex) {
//			Result res = new Result().setSuccess(false);
//			if (ex instanceof ExecutorException && ((ExecutorException)ex).getOutput() != null)
//				res.setResult(String.format("%s:%n%s", ex.getMessage(), ((ExecutorException)ex).getOutput()));
//			else
//				res.setResult(String.format("%s:%n%s", "Could not invoke the user code.", ex));
//
//			while (results.size() < testCases.size())
//				results.add(res);
//
//		} finally {
//			if (codeDirectory.exists())
//				this.deleteRecursive(codeDirectory);
//		}
//
//		return results;
//	}
//
//	private boolean deleteRecursive(File file) {
//
//		boolean ret = true;
//
//		File[] children; //recursively delete children
//		if (file.isDirectory() && (children = file.listFiles()) != null)
//			for (File child : children)
//				ret &= this.deleteRecursive(child);
//
//		ret &= file.delete(); //delete file itself
//		return ret;
//	}
//
//	private String readConsole(Process process) throws ExecutorException {
//
//		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), CPlusPlusExecutor.CODE_CHARSET))) {
//			StringBuilder sb = new StringBuilder();
//
//			String line; //read every line
//			while ((line = reader.readLine()) != null)
//				sb.append(line).append(String.format("%n"));
//
//			return sb.toString(); //create concatenated string
//
//		} catch (IOException ex) {
//			throw new ExecutorException("Could not read the console output.", ex);
//		}
//	}
//
//	private void generateCodeFile(File directory, String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases) throws ExecutorException {
//
//		try {
//			StringBuilder code = this.getTemplate(); //read the template
//
//			int fragStart = code.indexOf(CPlusPlusExecutor.CODE_CUSTOM_FRAGMENT); //place fragment in template
//			code.replace(fragStart, fragStart + CPlusPlusExecutor.CODE_CUSTOM_FRAGMENT.length(), codeFragment);
//
//			int caseStart = code.indexOf(CPlusPlusExecutor.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
//			code.replace(caseStart, caseStart + CPlusPlusExecutor.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(functions, testCases));
//
//			Files.write(Paths.get(directory.getPath(), String.format("%s.java", CPlusPlusExecutor.CODE_CLASS_NAME)), //create a java source file in the temporary directory
//									code.toString().getBytes(CPlusPlusExecutor.CODE_CHARSET)); //and write the generated code in it
//
//		} catch (ExecutorException | IOException ex) {
//			throw new ExecutorException("Could not generate the code file.", ex);
//		}
//	}
//
//
//}
