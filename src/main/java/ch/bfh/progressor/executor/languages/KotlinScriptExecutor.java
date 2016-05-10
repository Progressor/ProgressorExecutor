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
import ch.bfh.progressor.executor.CodeExecutorBase;
import ch.bfh.progressor.executor.Executor;
import ch.bfh.progressor.executor.ExecutorException;
import ch.bfh.progressor.executor.ExecutorPlatform;
import ch.bfh.progressor.executor.thrift.FunctionSignature;
import ch.bfh.progressor.executor.thrift.PerformanceIndicators;
import ch.bfh.progressor.executor.thrift.Result;
import ch.bfh.progressor.executor.thrift.TestCase;

/**
 * Code execution engine for Kotlin code. <br>
 * Uses the script engine to execute the Kotlin code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class KotlinScriptExecutor extends KotlinExecutor {

	@Override
	protected String getTemplatePath() {
		return String.format("%s/template-script.txt", this.getLanguage());
	}

	@Override
	public List<Result> execute(String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases) {

		final File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory
		final File codeFile = new File(codeDirectory, String.format("%s.kts", KotlinExecutor.CODE_CLASS_NAME));

		List<Result> results = new ArrayList<>(testCases.size());
		try {
			if (!codeDirectory.exists() && !codeDirectory.mkdirs())
				throw new ExecutorException(true, "Could not create a temporary directory for the user code.");

			//*********************
			//*** GENERATE CODE ***
			//*********************
			this.generateCodeFile(codeDirectory, codeFragment, functions, testCases);

			//********************
			//*** EXECUTE CODE ***
			//********************
			String[] kotlinArguments = { Executor.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlinc.bat" : "kotlinc", "-script", "-nowarn", codeFile.getName() };
			if (CodeExecutorBase.shouldUseDocker())
				kotlinArguments = this.getDockerCommandLine(codeDirectory, kotlinArguments);

			long kotlinStart = System.nanoTime();
			Process kotlinProcess = new ProcessBuilder(kotlinArguments).directory(codeDirectory).redirectErrorStream(true).start();
			if (kotlinProcess.waitFor(KotlinExecutor.COMPILE_TIMEOUT_SECONDS + KotlinExecutor.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
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
				this.tryDeleteRecursive(codeDirectory);
		}

		return results;
	}

	@Override
	protected void generateCodeFile(File directory, String codeFragment, List<FunctionSignature> functions, List<TestCase> testCases) throws ExecutorException {

		try {
			StringBuilder code = this.getTemplate(); //read the template

			int fragStart = code.indexOf(CodeExecutorBase.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + CodeExecutorBase.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			int caseStart = code.indexOf(CodeExecutorBase.TEST_CASES_FRAGMENT); //generate test cases and place them in fragment
			code.replace(caseStart, caseStart + CodeExecutorBase.TEST_CASES_FRAGMENT.length(), this.getTestCaseSignatures(functions, testCases));

			Files.write(Paths.get(directory.getPath(), String.format("%s.kts", KotlinExecutor.CODE_CLASS_NAME)), //create a Kotlin source file in the temporary directory
									code.toString().getBytes(CodeExecutorBase.CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
			throw new ExecutorException(true, "Could not generate the code file.", ex);
		}
	}
}
