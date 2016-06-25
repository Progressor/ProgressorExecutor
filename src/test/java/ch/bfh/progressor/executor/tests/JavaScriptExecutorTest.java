package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.languages.JavaScriptExecutor;

public class JavaScriptExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("function helloWorld() { return 'Hello, World!'; }").append(CodeExecutorBase.NEWLINE)
																															.append("function concatStrings(a, b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function minChar(a, b) { return a < b ? a : b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function exor(a, b) { return a !== b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt8(a, b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt16(a, b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt32(a, b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt64(a, b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumFloat32(a, b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumFloat64(a, b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumDecimal(a, b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt32Array(a, l) { return a.reduce((b, c) => b + c, 0); }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt32List(l) { return l.reduce((b, c) => b + c, 0); }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt32Set(s) { return [...s].reduce((b, c) => b + c, 0); }").append(CodeExecutorBase.NEWLINE)
																															.append("function getMapEntry(m, k) { return m.get(k); }").append(CodeExecutorBase.NEWLINE)
																															.append("function getMapListEntry(m, k, i) { return m.get(k)[i]; }").append(CodeExecutorBase.NEWLINE)
																															.append("function infiniteLoop() { while(true); }").append(CodeExecutorBase.NEWLINE)
																															.append("function recursion() { return recursion(); }").append(CodeExecutorBase.NEWLINE)
																															.append("function error() { throw 'error'; }").toString();

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new JavaScriptExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return JavaScriptExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return JavaScriptExecutorTest.FRAGMENT;
	}

	@Override
	protected boolean hasTotalCompilationTime() {
		return false;
	}
}
