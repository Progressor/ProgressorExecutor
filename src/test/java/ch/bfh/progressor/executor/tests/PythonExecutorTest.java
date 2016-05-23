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
