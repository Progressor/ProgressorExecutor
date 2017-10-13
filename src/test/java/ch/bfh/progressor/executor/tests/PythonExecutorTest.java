package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.languages.PythonExecutor;

public class PythonExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("def helloWorld(): return 'Hello, World!'").append(CodeExecutorBase.NEWLINE)
																															.append("def concatStrings(a, b): return a + b").append(CodeExecutorBase.NEWLINE)
																															.append("def minChar(a, b): return a if a < b else b").append(CodeExecutorBase.NEWLINE)
																															.append("def exor(a, b): return a != b").append(CodeExecutorBase.NEWLINE)
																															.append("def sumInt8(a, b): return a + b").append(CodeExecutorBase.NEWLINE)
																															.append("def sumInt16(a, b): return a + b").append(CodeExecutorBase.NEWLINE)
																															.append("def sumInt32(a, b): return a + b").append(CodeExecutorBase.NEWLINE)
																															.append("def sumInt64(a, b): return a + b").append(CodeExecutorBase.NEWLINE)
																															.append("def sumFloat32(a, b): return a + b").append(CodeExecutorBase.NEWLINE)
																															.append("def sumFloat64(a, b): return a + b").append(CodeExecutorBase.NEWLINE)
																															.append("def sumDecimal(a, b): return a + b").append(CodeExecutorBase.NEWLINE)
																															.append("def sumInt32Array(a, l): return sum(a)").append(CodeExecutorBase.NEWLINE)
																															.append("def sumInt32List(l): return sum(l)").append(CodeExecutorBase.NEWLINE)
																															.append("def sumInt32Set(s): return sum(s)").append(CodeExecutorBase.NEWLINE)
																															.append("def getMapEntry(m, k): return m[k]").append(CodeExecutorBase.NEWLINE)
																															.append("def getMapListEntry(m, k, i): return m[k][i]").append(CodeExecutorBase.NEWLINE)
																															.append("def intersectArray(a1, l1, a2, l2): return list(filter(a1.__contains__, a2))").append(CodeExecutorBase.NEWLINE)
																															.append("def intersectList(l1, l2): return list(filter(l1.__contains__, l2))").append(CodeExecutorBase.NEWLINE)
																															.append("def intersectSet(s1, s2): return set(filter(s1.__contains__, s2))").append(CodeExecutorBase.NEWLINE)
																															.append("def intersectMap(m1, m2): return { i: m1[i] for i in filter(lambda i: i in m2 and m1[i] == m2[i], m1.keys())}").append(CodeExecutorBase.NEWLINE)
																															.append("def infiniteLoop():").append(CodeExecutorBase.NEWLINE).append(" while True: 0").append(CodeExecutorBase.NEWLINE)
																															.append("def recursion(): return recursion()").append(CodeExecutorBase.NEWLINE)
																															.append("def error(): raise Exception()").toString();

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new PythonExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return PythonExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return PythonExecutorTest.FRAGMENT;
	}

	@Override
	protected boolean hasTotalCompilationTime() {
		return false;
	}
}
