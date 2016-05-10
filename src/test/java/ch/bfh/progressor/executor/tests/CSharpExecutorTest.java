package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.languages.CSharpExecutor;

public class CSharpExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("public string helloWorld() { return \"Hello, World!\"; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public string concatStrings(string a, string b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public char minChar(char a, char b) { return a < b ? a : b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public bool exor(bool a, bool b) { return a ^ b;}").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public sbyte sumInt8(sbyte a, sbyte b) { return (sbyte)(a + b); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public short sumInt16(short a, short b) { return (short)(a + b); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32(int a, int b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public long sumInt64(long a, long b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public float sumFloat32(float a, float b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public double sumFloat64(double a, double b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public decimal sumDecimal(decimal a, decimal b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32Array(int[] a, int l) { return a.Sum(); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32List(List<int> l) { return l.Sum(); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32Set(HashSet<int> s) { return s.Sum(); }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public string getMapEntry(Dictionary<int, string> d, int k) { return d[k]; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public string getMapListEntry(Dictionary<int, List<string>> d, int k, int i) { return d[k][i]; }").toString();

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new CSharpExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return CSharpExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return CSharpExecutorTest.FRAGMENT;
	}
}
