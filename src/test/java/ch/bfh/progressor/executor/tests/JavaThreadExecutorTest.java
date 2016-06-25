package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.languages.JavaProcessExecutor;
import ch.bfh.progressor.executor.languages.JavaThreadExecutor;

public class JavaThreadExecutorTest extends JavaProcessExecutorTest {

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new JavaThreadExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return JavaProcessExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return JavaProcessExecutorTest.FRAGMENT;
	}

	@Override
	protected boolean hasTotalExecutionTime() {
		return false;
	}
}
