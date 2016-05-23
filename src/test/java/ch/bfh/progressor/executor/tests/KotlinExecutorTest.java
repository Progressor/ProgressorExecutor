package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.languages.KotlinExecutor;

public class KotlinExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("fun helloWorld() = \"Hello, World!\"").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun concatStrings(a: String, b: String) = a + b").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun minChar(a: Char, b: Char) = if (a < b) a else b").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun exor(a: Boolean, b: Boolean) = a.xor(b)").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumInt8(a: Byte, b: Byte) = (a + b).toByte()").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumInt16(a: Short, b: Short) = (a + b).toShort()").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumInt32(a: Int, b: Int) = a + b").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumInt64(a: Long, b: Long) = a + b").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumFloat32(a: Float, b: Float) = a + b").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumFloat64(a: Double, b: Double) = a + b").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumDecimal(a: BigDecimal, b: BigDecimal) = a + b").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumInt32Array(a: Array<Int>, l: Int) = a.sum()").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumInt32List(l: List<Int>) = l.sum()").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun sumInt32Set(s: Set<Int>) = s.sum()").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun getMapEntry(m: Map<Int, String>, k: Int) = m[k]").append(CodeExecutorTestBase.NEWLINE)
																															.append("fun getMapListEntry(m: Map<Int, List<String>>, k: Int, i: Int) = m[k]!![i]").toString();

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new KotlinExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return KotlinExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return KotlinExecutorTest.FRAGMENT;
	}
}
