package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.languages.JavaProcessExecutor;

public class JavaProcessExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("public String helloWorld() { return \"Hello, World!\"; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public String concatStrings(String a, String b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public char minChar(char a, char b) { return a < b ? a : b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public boolean exor(boolean a, boolean b) { return a ^ b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public byte sumInt8(byte a, byte b) { return (byte)(a + b); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public short sumInt16(short a, short b) { return (short)(a + b); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32(int a, int b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public long sumInt64(long a, long b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public float sumFloat32(float a, float b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public double sumFloat64(double a, double b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public BigDecimal sumDecimal(BigDecimal a, BigDecimal b) { return a.add(b); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32Array(int[] a, int l) { return Arrays.stream(a).sum(); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32List(List<Integer> l) { return l.stream().mapToInt(i -> i).sum(); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32Set(Set<Integer> s) { return s.stream().mapToInt(i -> i).sum(); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public String getMapEntry(Map<Integer, String> m, int k) { return m.get(k); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public String getMapListEntry(Map<Integer, List<String>> m, int k, int i) { return m.get(k).get(i); }").toString();

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new JavaProcessExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return JavaProcessExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return JavaProcessExecutorTest.FRAGMENT;
	}
}
