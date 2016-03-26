package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.CodeExecutor;
import ch.bfh.progressor.executor.languages.CSharpExecutor;

public class CSharpExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("public string helloWorld() => \"Hello, World!\";").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public string concatStrings(string a, string b) => a + b;").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public char minChar(char a, char b) => a < b ? a : b;").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public bool exor(bool a, bool b) => a ^ b;").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public sbyte sumInt8(sbyte a, sbyte b) => (sbyte)(a + b);").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public short sumInt16(short a, short b) => (short)(a + b);").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32(int a, int b) => a + b;").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public long sumInt64(long a, long b) => a + b;").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public float sumFloat32(float a, float b) => a + b;").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public double sumFloat64(double a, double b) => a + b;").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public decimal sumDecimal(decimal a, decimal b) => a + b;").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32Array(int[] a, int l) => a.Sum();").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32List(List<int> l) => l.Sum();").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public int sumInt32Set(HashSet<int> s) => s.Sum();").append(CodeExecutorTestBase.NEW_LINE)
																															.append("public string getMapEntry(Dictionary<int, string> d, int k) => d[k];").toString();

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
