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
