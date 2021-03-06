package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.languages.JavaProcessExecutor;

public class JavaProcessExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("public String helloWorld() { return \"Hello, World!\"; }").append(CodeExecutorBase.NEWLINE)
																															.append("public String concatStrings(String a, String b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public char minChar(char a, char b) { return a < b ? a : b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public boolean exor(boolean a, boolean b) { return a ^ b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public byte sumInt8(byte a, byte b) { return (byte)(a + b); }").append(CodeExecutorBase.NEWLINE)
																															.append("public short sumInt16(short a, short b) { return (short)(a + b); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int sumInt32(int a, int b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public long sumInt64(long a, long b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public float sumFloat32(float a, float b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public double sumFloat64(double a, double b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("public BigDecimal sumDecimal(BigDecimal a, BigDecimal b) { return a.add(b); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int sumInt32Array(int[] a, int l) { return Arrays.stream(a).sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int sumInt32List(List<Integer> l) { return l.stream().mapToInt(i -> i).sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int sumInt32Set(Set<Integer> s) { return s.stream().mapToInt(i -> i).sum(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public String getMapEntry(Map<Integer, String> m, int k) { return m.get(k); }").append(CodeExecutorBase.NEWLINE)
																															.append("public String getMapListEntry(Map<Integer, List<String>> m, int k, int i) { return m.get(k).get(i); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int[] intersectArray(int[] a1, int l1, int[] a2, int l2) { Set<Integer> s = Arrays.stream(a1).boxed().collect(Collectors.toSet()); return Arrays.stream(a2).filter(s::contains).toArray(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public List<Integer> intersectList(List<Integer> l1, List<Integer> l2) { List<Integer> l = new ArrayList<>(l1); l.retainAll(l2); return l; }").append(CodeExecutorBase.NEWLINE)
																															.append("public Set<Integer> intersectSet(Set<Integer> s1, Set<Integer> s2) { Set<Integer> s = new HashSet<>(s1); s.retainAll(s2); return s; }").append(CodeExecutorBase.NEWLINE)
																															.append("public Map<Integer, String> intersectMap(Map<Integer, String> m1, Map<Integer, String> m2) { return m1.entrySet().stream().filter(e -> m2.containsKey(e.getKey()) && Objects.equals(e.getValue(), m2.get(e.getKey()))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int infiniteLoop() { while(true); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int recursion() { return recursion(); }").append(CodeExecutorBase.NEWLINE)
																															.append("public int error() { throw new RuntimeException(); }").toString();

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
