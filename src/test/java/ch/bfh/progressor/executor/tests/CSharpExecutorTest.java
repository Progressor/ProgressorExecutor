package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.languages.CSharpExecutor;

public class CSharpExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("public string helloWorld() { return \"Hello, World!\"; }").append(CodeExecutorBase.NEWLINE)
																															.append("public string concatStrings(string a, string b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public char minChar(char a, char b) { return a < b ? a : b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public bool exor(bool a, bool b) { return a ^ b;}").append(CodeExecutorBase.NEWLINE)
																															.append("public sbyte sumInt8(sbyte a, sbyte b) { return (sbyte)(a + b); }").append(CodeExecutorBase.NEWLINE)
																															.append("public short sumInt16(short a, short b) { return (short)(a + b); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int sumInt32(int a, int b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public long sumInt64(long a, long b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public float sumFloat32(float a, float b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public double sumFloat64(double a, double b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public decimal sumDecimal(decimal a, decimal b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public int sumInt32Array(int[] a, int l) { return a.Sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int sumInt32List(List<int> l) { return l.Sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int sumInt32Set(HashSet<int> s) { return s.Sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public string getMapEntry(Dictionary<int, string> d, int k) { return d[k]; }").append(CodeExecutorBase.NEWLINE)
																															.append("public string getMapListEntry(Dictionary<int, List<string>> d, int k, int i) { return d[k][i]; }").append(CodeExecutorBase.NEWLINE)
																															.append("public int infiniteLoop() { while(true); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int recursion() { return recursion(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int error() { throw new Exception(); }").toString();

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
