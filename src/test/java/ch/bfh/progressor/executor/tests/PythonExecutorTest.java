package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.languages.PythonExecutor;

public class PythonExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("def helloWorld(): return 'Hello, World!'").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def concatStrings(a, b): return a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def minChar(a, b): return a if a < b else b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def exor(a, b): return a != b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumInt8(a, b): return a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumInt16(a, b): return a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumInt32(a, b): return a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumInt64(a, b): return a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumFloat32(a, b): return a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumFloat64(a, b): return a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumDecimal(a, b): return a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumInt32Array(a, l): return sum(a)").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumInt32List(l): return sum(l)").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def sumInt32Set(s): return sum(s)").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def getMapEntry(m, k): return m[k]").append(CodeExecutorTestBase.NEW_LINE)
																															.append("def getMapListEntry(m, k, i): return m[k][i]").toString();

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
}
