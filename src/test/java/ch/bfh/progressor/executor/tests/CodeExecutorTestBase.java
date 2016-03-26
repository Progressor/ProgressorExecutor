package ch.bfh.progressor.executor.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ch.bfh.progressor.executor.CodeExecutor;
import ch.bfh.progressor.executor.ExecutorException;
import ch.bfh.progressor.executor.thrift.FunctionSignature;
import ch.bfh.progressor.executor.thrift.Result;
import ch.bfh.progressor.executor.thrift.TestCase;
import ch.bfh.progressor.executor.thrift.executorConstants;

public abstract class CodeExecutorTestBase {

	protected static final String NEW_LINE = String.format("%n");

	protected static final List<FunctionSignature> FUNCTIONS = Arrays.asList(new FunctionSignature("helloWorld", Collections.emptyList(), Collections.emptyList(), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeString)),
																																					 new FunctionSignature("concatStrings", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeString, executorConstants.TypeString), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeString)),
																																					 new FunctionSignature("minChar", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeCharacter, executorConstants.TypeCharacter), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeCharacter)),
																																					 new FunctionSignature("exor", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeBoolean, executorConstants.TypeBoolean), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeBoolean)),
																																					 new FunctionSignature("sumInt8", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeInt8, executorConstants.TypeInt8), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeInt8)),
																																					 new FunctionSignature("sumInt16", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeInt16, executorConstants.TypeInt16), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeInt16)),
																																					 new FunctionSignature("sumInt32", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeInt32, executorConstants.TypeInt32), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeInt32)),
																																					 new FunctionSignature("sumInt64", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeInt64, executorConstants.TypeInt64), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeInt64)),
																																					 new FunctionSignature("sumFloat32", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeFloat32, executorConstants.TypeFloat32), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeFloat32)),
																																					 new FunctionSignature("sumFloat64", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeFloat64, executorConstants.TypeFloat64), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeFloat64)),
																																					 new FunctionSignature("sumDecimal", Arrays.asList("a", "b"), Arrays.asList(executorConstants.TypeDecimal, executorConstants.TypeDecimal), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeDecimal)),
																																					 new FunctionSignature("sumInt32Array", Arrays.asList("a", "l"), Arrays.asList(String.format("%s<%s>", executorConstants.TypeContainerArray, executorConstants.TypeInt32), executorConstants.TypeInt32), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeInt32)),
																																					 new FunctionSignature("sumInt32List", Collections.singletonList("l"), Collections.singletonList(String.format("%s<%s>", executorConstants.TypeContainerList, executorConstants.TypeInt32)), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeInt32)),
																																					 new FunctionSignature("sumInt32Set", Collections.singletonList("s"), Collections.singletonList(String.format("%s<%s>", executorConstants.TypeContainerSet, executorConstants.TypeInt32)), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeInt32)),
																																					 new FunctionSignature("getMapEntry", Arrays.asList("m", "k"), Arrays.asList(String.format("%s<%s, %s>", executorConstants.TypeContainerMap, executorConstants.TypeInt32, executorConstants.TypeString), executorConstants.TypeInt32), Collections.singletonList("return"), Collections.singletonList(executorConstants.TypeString)));

	protected static final List<TestCase> TEST_CASES = Arrays.asList(new TestCase("helloWorld", Collections.emptyList(), Collections.singletonList("Hello, World!")),
																																	 new TestCase("concatStrings", Arrays.asList("Héllô, ", "Wörld£"), Collections.singletonList("Héllô, Wörld£")),
																																	 new TestCase("minChar", Arrays.asList("a", "b"), Collections.singletonList("a")),
																																	 new TestCase("minChar", Arrays.asList("b", "a"), Collections.singletonList("a")),
																																	 new TestCase("exor", Arrays.asList("false", "false"), Collections.singletonList("false")),
																																	 new TestCase("exor", Arrays.asList("true", "false"), Collections.singletonList("true")),
																																	 new TestCase("exor", Arrays.asList("false", "true"), Collections.singletonList("true")),
																																	 new TestCase("exor", Arrays.asList("true", "true"), Collections.singletonList("false")),
																																	 new TestCase("sumInt8", Arrays.asList("0", "0"), Collections.singletonList("0")),
																																	 new TestCase("sumInt8", Arrays.asList("-1", "1"), Collections.singletonList("0")),
																																	 new TestCase("sumInt8", Arrays.asList("0", "1"), Collections.singletonList("1")),
																																	 new TestCase("sumInt8", Arrays.asList("2", "3"), Collections.singletonList("5")),
																																	 new TestCase("sumInt16", Arrays.asList("0", "0"), Collections.singletonList("0")),
																																	 new TestCase("sumInt16", Arrays.asList("-1", "1"), Collections.singletonList("0")),
																																	 new TestCase("sumInt16", Arrays.asList("0", "1"), Collections.singletonList("1")),
																																	 new TestCase("sumInt16", Arrays.asList("2", "3"), Collections.singletonList("5")),
																																	 new TestCase("sumInt32", Arrays.asList("0", "0"), Collections.singletonList("0")),
																																	 new TestCase("sumInt32", Arrays.asList("-1", "1"), Collections.singletonList("0")),
																																	 new TestCase("sumInt32", Arrays.asList("0", "1"), Collections.singletonList("1")),
																																	 new TestCase("sumInt32", Arrays.asList("2", "3"), Collections.singletonList("5")),
																																	 new TestCase("sumInt64", Arrays.asList("0", "0"), Collections.singletonList("0")),
																																	 new TestCase("sumInt64", Arrays.asList("-1", "1"), Collections.singletonList("0")),
																																	 new TestCase("sumInt64", Arrays.asList("0", "1"), Collections.singletonList("1")),
																																	 new TestCase("sumInt64", Arrays.asList("2", "3"), Collections.singletonList("5")),
																																	 new TestCase("sumFloat32", Arrays.asList("0.0", "0.0"), Collections.singletonList("+0.0")),
																																	 new TestCase("sumFloat32", Arrays.asList("0.0", "0.0"), Collections.singletonList("-0.0")),
																																	 new TestCase("sumFloat32", Arrays.asList("-1.1", "+1.1"), Collections.singletonList("0.0")),
																																	 new TestCase("sumFloat32", Arrays.asList("0.0", "3.1415926535897932385"), Collections.singletonList("3.1415926535897932385")),
																																	 new TestCase("sumFloat32", Arrays.asList("3.1415926535897932385", "2.135135483544684"), Collections.singletonList("5.2767281371344772385")),
																																	 new TestCase("sumFloat64", Arrays.asList("0.0", "0.0"), Collections.singletonList("+0.0")),
																																	 new TestCase("sumFloat64", Arrays.asList("0.0", "0.0"), Collections.singletonList("-0.0")),
																																	 new TestCase("sumFloat64", Arrays.asList("-1.1", "+1.1"), Collections.singletonList("0.0")),
																																	 new TestCase("sumFloat64", Arrays.asList("0.0", "3.1415926535897932385"), Collections.singletonList("3.1415926535897932385")),
																																	 new TestCase("sumFloat64", Arrays.asList("3.1415926535897932385", "2.135135483544684"), Collections.singletonList("5.2767281371344772385")),
																																	 new TestCase("sumDecimal", Arrays.asList("0.0", "0.0"), Collections.singletonList("+0.0")),
																																	 new TestCase("sumDecimal", Arrays.asList("0.0", "0.0"), Collections.singletonList("-0.0")),
																																	 new TestCase("sumDecimal", Arrays.asList("-1.1", "+1.1"), Collections.singletonList("0.0")),
																																	 new TestCase("sumDecimal", Arrays.asList("0.0", "3.1415926535897932385"), Collections.singletonList("3.1415926535897932385")),
																																	 new TestCase("sumDecimal", Arrays.asList("3.1415926535897932385", "2.135135483544684"), Collections.singletonList("5.2767281371344772385")),
																																	 new TestCase("sumInt32Array", Arrays.asList("", "0"), Collections.singletonList("0")),
																																	 new TestCase("sumInt32Array", Arrays.asList("0", "1"), Collections.singletonList("0")),
																																	 new TestCase("sumInt32Array", Arrays.asList("2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97", "25"), Collections.singletonList("1060")),
																																	 new TestCase("sumInt32Array", Arrays.asList("1,2,3,5,8,13,21,34,55,89,144,233,377,610,987,1597,2584,4181,6765,10946,17711,28657,46368,75025,121393,196418,317811,514229", "28"), Collections.singletonList("1346267")),
																																	 new TestCase("sumInt32List", Collections.singletonList(""), Collections.singletonList("0")),
																																	 new TestCase("sumInt32List", Collections.singletonList("0"), Collections.singletonList("0")),
																																	 new TestCase("sumInt32List", Collections.singletonList("2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97"), Collections.singletonList("1060")),
																																	 new TestCase("sumInt32List", Collections.singletonList("1,2,3,5,8,13,21,34,55,89,144,233,377,610,987,1597,2584,4181,6765,10946,17711,28657,46368,75025,121393,196418,317811,514229"), Collections.singletonList("1346267")),
																																	 new TestCase("sumInt32Set", Collections.singletonList(""), Collections.singletonList("0")),
																																	 new TestCase("sumInt32Set", Collections.singletonList("0"), Collections.singletonList("0")),
																																	 new TestCase("sumInt32Set", Collections.singletonList("2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97"), Collections.singletonList("1060")),
																																	 new TestCase("sumInt32Set", Collections.singletonList("1,2,3,5,8,13,21,34,55,89,144,233,377,610,987,1597,2584,4181,6765,10946,17711,28657,46368,75025,121393,196418,317811,514229"), Collections.singletonList("1346267")),
																																	 new TestCase("getMapEntry", Arrays.asList("1:strut1", "1"), Collections.singletonList("strut1")),
																																	 new TestCase("getMapEntry", Arrays.asList("1:strut1,2:touwm1,3:weidj1", "2"), Collections.singletonList("touwm1")),
																																	 new TestCase("getMapEntry", Arrays.asList("2:touwm1,3:weidj1,1:strut1", "3"), Collections.singletonList("weidj1")));

	private CodeExecutor codeExecutor;
	private Logger logger;

	protected abstract CodeExecutor getCodeExecutor();

	protected abstract String getExpectedLanguage();

	protected abstract String getFragment();

	@BeforeClass
	public void setUp() {

		this.codeExecutor = this.getCodeExecutor();
		this.logger = Logger.getLogger(String.format("%sTest", this.codeExecutor.getClass().getName()));
	}

	@Test
	public void testGetLanguage() throws ExecutorException {

		Assert.assertEquals(this.codeExecutor.getLanguage(), this.getExpectedLanguage());
	}

	@Test
	public void testGetBlacklist() throws ExecutorException {

		this.logger.info(String.join(" ; ", this.codeExecutor.getBlacklist()));
	}

	@Test
	public void testGetFragment() throws ExecutorException {

		this.logger.info(this.codeExecutor.getFragment(CodeExecutorTestBase.FUNCTIONS));
	}

	@Test
	public void testExecute() throws ExecutorException {

		Assert.assertTrue(this.codeExecutor.execute(this.getFragment(), CodeExecutorTestBase.FUNCTIONS, CodeExecutorTestBase.TEST_CASES).stream().allMatch(Result::isSuccess));
	}
}