package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.CodeExecutor;
import ch.bfh.progressor.executor.languages.KotlinExecutor;

public class KotlinExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("fun helloWorld() = \"Hello, World!\"").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun concatStrings(a: String, b: String) = a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun minChar(a: Char, b: Char) = if (a < b) a else b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun exor(a: Boolean, b: Boolean) = a.xor(b)").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumInt8(a: Byte, b: Byte) = (a + b).toByte()").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumInt16(a: Short, b: Short) = (a + b).toShort()").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumInt32(a: Int, b: Int) = a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumInt64(a: Long, b: Long) = a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumFloat32(a: Float, b: Float) = a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumFloat64(a: Double, b: Double) = a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumDecimal(a: BigDecimal, b: BigDecimal) = a + b").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumInt32Array(a: Array<Int>, l: Int) = a.sum()").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumInt32List(l: List<Int>) = l.sum()").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun sumInt32Set(s: Set<Int>) = s.sum()").append(CodeExecutorTestBase.NEW_LINE)
																															.append("fun getMapEntry(m: Map<Int, String>, k: Int) = m[k]").toString();

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
