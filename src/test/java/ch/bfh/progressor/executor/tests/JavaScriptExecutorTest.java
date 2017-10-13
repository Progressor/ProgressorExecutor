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
																															.append("function intersectArray(a1, l1, a2, l2) { return a1.filter(i => a2.includes(i)); }").append(CodeExecutorBase.NEWLINE)
																															.append("function intersectList( l1, l2) { return l1.filter(i => l2.includes(i)); }").append(CodeExecutorBase.NEWLINE)
																															.append("function intersectSet(s1, s2) { var a2 = [...s2]; return new Set(Array.from(s1).filter(i => a2.includes(i))); }").append(CodeExecutorBase.NEWLINE)
																															.append("function intersectMap(m1, m2) { var a2 = [...m2.keys()]; return new Map(Array.from(m1).filter(([k, v]) => a2.includes(k) && v === m2.get(k))); }").append(CodeExecutorBase.NEWLINE)
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
