package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.languages.KotlinScriptExecutor;

public class KotlinScriptExecutorTest extends KotlinExecutorTest {

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new KotlinScriptExecutor();
	}

	@Override
	protected boolean hasTotalCompilationTime() {
		return false;
	}
}
