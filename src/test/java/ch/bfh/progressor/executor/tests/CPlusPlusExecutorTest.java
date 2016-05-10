package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.languages.CPlusPlusExecutor;

public class CPlusPlusExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("string helloWorld() { return \"Hello, World!\"; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("string concatStrings(string a, string b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("char minChar(char a, char b) { return a < b ? a : b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("bool exor(bool a, bool b) { return a ^ b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("int8_t sumInt8(int8_t a, int8_t b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("int16_t sumInt16(int16_t a, int16_t b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("int32_t sumInt32(int32_t a, int32_t b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("int64_t sumInt64(int64_t a, int64_t b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("float sumFloat32(float a, float b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("double sumFloat64(double a, double b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("long double sumDecimal(long double a, long double b) { return a + b; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("int32_t sumInt32Array(int32_t* a, int32_t l) { int32_t s = 0; for (int i = 0; i < l; i++) s += a[i]; return s; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("int32_t sumInt32List(vector<int32_t> l) { int32_t s = 0; for (auto i : l) s += i; return s; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("int32_t sumInt32Set(set<int32_t> s) { int32_t u = 0; for (auto i : s) u += i; return u; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("string getMapEntry(map<int32_t, string> m, int k) { return m[k]; }").append(CodeExecutorTestBase.NEW_LINE)
																															.append("string getMapListEntry(map<int32_t, vector<string>> m, int k, int i) { return m[k][i]; }").toString();

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
