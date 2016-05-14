package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;

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
	protected List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException {

		final File codeFile = new File(codeDirectory, String.format("%s.kts", KotlinExecutor.CODE_CLASS_NAME));

		//*********************
		//*** GENERATE CODE ***
		//*********************
		this.generateCodeFile(codeFile, codeFragment, testCases);

		//********************
		//*** EXECUTE CODE ***
		//********************
		long executionStart = System.nanoTime();

		Process executionProcess;
		try {
			executionProcess = this.executeCommand(codeDirectory, KotlinExecutor.COMPILE_TIMEOUT_SECONDS + KotlinExecutor.EXECUTION_TIMEOUT_SECONDS,
																						 CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "kotlinc.bat" : "kotlinc", "-script", "-nowarn", codeFile.getName());

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not execute the user code.", ex);
		}

		long executionEnd = System.nanoTime();

		//****************************
		//*** TEST CASE EVALUATION ***
		//****************************
		List<Result> results = new ArrayList<>(testCases.size());
		for (String result : this.readDelimited(executionProcess, String.format("%n%n")))
			results.add(this.getResult(result.startsWith("OK"), false, result.substring(3), (executionEnd - executionStart) / 1e6));
		return results;
	}
}
