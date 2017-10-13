package ch.bfh.progressor.executor.tests;

import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.languages.PHPExecutor;

public class PHPExecutorTest extends CodeExecutorTestBase {

	protected static final String FRAGMENT = new StringBuilder().append("function helloWorld() : string { return 'Hello, World!'; }").append(CodeExecutorBase.NEWLINE)
																															.append("function concatStrings(string $a, string $b) : string { return $a . $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function minChar(string $a, string $b) : string { return $a < $b ? $a : $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function exor(bool $a, bool $b) : bool { return $a !== $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt8(int $a, int $b) : int { return $a + $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt16(int $a, int $b) : int { return $a + $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt32(int $a, int $b) : int { return $a + $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt64(int $a, int $b) : int { return $a + $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumFloat32(float $a, float $b) : float { return $a + $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumFloat64(float $a, float $b) : float { return $a + $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumDecimal(float $a, float $b) : float { return $a + $b; }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt32Array(array $a, int $l) : int { return array_sum($a); }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt32List(array $l) : int { return array_sum($l); }").append(CodeExecutorBase.NEWLINE)
																															.append("function sumInt32Set(array $s) : int { return array_sum($s); }").append(CodeExecutorBase.NEWLINE)
																															.append("function getMapEntry(array $m, int $k) : string { return $m[$k]; }").append(CodeExecutorBase.NEWLINE)
																															.append("function getMapListEntry(array $m, int $k, int $i) : string { return $m[$k][$i]; }").append(CodeExecutorBase.NEWLINE)
																															.append("function intersectArray(array $a1, int $l1, array $a2, int $l2) : array { return array_intersect($a1, $a2); }").append(CodeExecutorBase.NEWLINE)
																															.append("function intersectList(array $l1, array $l2) : array { return array_intersect($l1, $l2); }").append(CodeExecutorBase.NEWLINE)
																															.append("function intersectSet(array $s1, array $s2) : array { return array_intersect($s1, $s2); }").append(CodeExecutorBase.NEWLINE)
																															.append("function intersectMap(array $m1, array $m2) : array { return array_filter($m1, function(string $v, int $k) use($m2) : bool { return array_key_exists($k, $m2) && $v === $m2[$k]; }, ARRAY_FILTER_USE_BOTH); }").append(CodeExecutorBase.NEWLINE)
																															.append("function infiniteLoop() : int { while(true); }").append(CodeExecutorBase.NEWLINE)
																															.append("function recursion() : int { return recursion(); }").append(CodeExecutorBase.NEWLINE)
																															.append("function error() : int { throw new Exception('error'); }").toString();

	@Override
	protected CodeExecutor getCodeExecutor() {
		return new PHPExecutor();
	}

	@Override
	protected String getExpectedLanguage() {
		return PHPExecutor.CODE_LANGUAGE;
	}

	@Override
	protected String getFragment() {
		return PHPExecutorTest.FRAGMENT;
	}

	@Override
	protected boolean hasTotalCompilationTime() {
		return false;
	}
}
