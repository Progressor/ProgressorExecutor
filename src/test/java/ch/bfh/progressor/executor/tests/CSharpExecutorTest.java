package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.languages.CSharpExecutor;

public class CSharpExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("string helloWorld() { return \"Hello, World!\"; }").append(CodeExecutorBase.NEWLINE)
																															.append("string concatStrings(string a, string b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("char minChar(char a, char b) { return a < b ? a : b; }").append(CodeExecutorBase.NEWLINE)
																															.append("bool exor(bool a, bool b) { return a ^ b; }").append(CodeExecutorBase.NEWLINE)
																															.append("sbyte sumInt8(sbyte a, sbyte b) { return (sbyte)(a + b); }").append(CodeExecutorBase.NEWLINE)
																															.append("short sumInt16(short a, short b) { return (short)(a + b); }").append(CodeExecutorBase.NEWLINE)
																															.append("int sumInt32(int a, int b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("long sumInt64(long a, long b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("float sumFloat32(float a, float b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("double sumFloat64(double a, double b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("decimal sumDecimal(decimal a, decimal b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("int sumInt32Array(int[] a, int l) { return a.Sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("int sumInt32List(List<int> l) { return l.Sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("int sumInt32Set(HashSet<int> s) { return s.Sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("string getMapEntry(Dictionary<int, string> d, int k) { return d[k]; }").append(CodeExecutorBase.NEWLINE)
																															.append("string getMapListEntry(Dictionary<int, List<string>> d, int k, int i) { return d[k][i]; }").append(CodeExecutorBase.NEWLINE)
																															.append("int[] intersectArray(int[] a1, int l1, int[] a2, int l2) { return a1.Intersect(a2).ToArray(); }").append(CodeExecutorBase.NEWLINE)
																															.append("List<int> intersectList(List<int> l1, List<int> l2) { return l1.Intersect(l2).ToList(); }").append(CodeExecutorBase.NEWLINE)
																															.append("HashSet<int> intersectSet(HashSet<int> s1, HashSet<int> s2) { return new HashSet<int>(s1.Intersect(s2)); }").append(CodeExecutorBase.NEWLINE)
																															.append("Dictionary<int, string> intersectMap(Dictionary<int, string> m1, Dictionary<int, string> m2) { return m1.Intersect(m2).ToDictionary(p => p.Key, p => p.Value); }").append(CodeExecutorBase.NEWLINE)
																															.append("int infiniteLoop() { while(true); }").append(CodeExecutorBase.NEWLINE)
																															.append("int recursion() { return recursion(); }").append(CodeExecutorBase.NEWLINE)
																															.append("int error() { throw new Exception(); }").toString();

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
