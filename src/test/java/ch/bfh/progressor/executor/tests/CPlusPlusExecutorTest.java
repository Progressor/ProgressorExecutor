package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.languages.CPlusPlusExecutor;

public class CPlusPlusExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("string helloWorld() { return \"Hello, World!\"; }").append(CodeExecutorBase.NEWLINE)
																															.append("string concatStrings(string a, string b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("char minChar(char a, char b) { return a < b ? a : b; }").append(CodeExecutorBase.NEWLINE)
																															.append("bool exor(bool a, bool b) { return a ^ b; }").append(CodeExecutorBase.NEWLINE)
																															.append("int8_t sumInt8(int8_t a, int8_t b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("int16_t sumInt16(int16_t a, int16_t b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("int32_t sumInt32(int32_t a, int32_t b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("int64_t sumInt64(int64_t a, int64_t b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("float sumFloat32(float a, float b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("double sumFloat64(double a, double b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("long double sumDecimal(long double a, long double b) { return a + b; }").append(CodeExecutorBase.NEWLINE)
																															.append("int32_t sumInt32Array(int32_t* a, int32_t l) { int32_t s = 0; for (int i = 0; i < l; i++) s += a[i]; return s; }").append(CodeExecutorBase.NEWLINE)
																															.append("int32_t sumInt32List(vector<int32_t> l) { int32_t s = 0; for (auto i : l) s += i; return s; }").append(CodeExecutorBase.NEWLINE)
																															.append("int32_t sumInt32Set(set<int32_t> s) { int32_t u = 0; for (auto i : s) u += i; return u; }").append(CodeExecutorBase.NEWLINE)
																															.append("string getMapEntry(map<int32_t, string> m, int k) { return m[k]; }").append(CodeExecutorBase.NEWLINE)
																															.append("string getMapListEntry(map<int32_t, vector<string>> m, int k, int i) { return m[k][i]; }").append(CodeExecutorBase.NEWLINE)
																															.append("int32_t* intersectArray(int32_t* a1, int32_t l1, int32_t* a2, int32_t l2) { sort(a1, a1 + l1); sort(a2, a2 + l2); int32_t* r = new int32_t[l1 > l2 ? l1 : l2]; set_intersection(a1, a1 + l1, a2, a2 + l2, r); return r; }").append(CodeExecutorBase.NEWLINE)
																															.append("vector<int32_t> intersectList(vector<int32_t> l1, vector<int32_t> l2) { sort(l1.begin(), l1.end()); sort(l2.begin(), l2.end()); vector<int32_t> r; set_intersection(l1.begin(), l1.end(), l2.begin(), l2.end(), back_inserter(r)); return r; }").append(CodeExecutorBase.NEWLINE)
																															.append("set<int32_t> intersectSet(set<int32_t> s1, set<int32_t> s2) { set<int32_t> r; set_intersection(s1.begin(), s1.end(), s2.begin(), s2.end(), inserter(r, r.begin())); return r; }").append(CodeExecutorBase.NEWLINE)
																															.append("map<int32_t, string> intersectMap(map<int32_t, string> m1, map<int32_t, string> m2) { map<int32_t, string> r; for (map<int32_t, string>::iterator i = m1.begin(), j = m2.begin(); i != m1.end() && j != m2.end();) { if (i->first < j->first) ++i; else if (i->first > j->first) ++j; else { if (i->second == j->second) r.insert(pair<int32_t, string>(i->first, i->second)); ++i; ++j; } } return r; }").append(CodeExecutorBase.NEWLINE)
																															.append("int32_t infiniteLoop() { while(true); }").append(CodeExecutorBase.NEWLINE)
																															.append("int32_t recursion() { return recursion(); }").append(CodeExecutorBase.NEWLINE)
																															.append("int32_t error() { throw runtime_error(\"exception\"); }").toString();

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new CPlusPlusExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return CPlusPlusExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return CPlusPlusExecutorTest.FRAGMENT;
	}
}
