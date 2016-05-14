package ch.bfh.progressor.executor.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;

/**
 * Read-only implementation of a {@link TestCase}.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class TestCaseImpl implements TestCase {

	private final FunctionSignature function;
	private final List<Value> inputValues, expectedOutputValues;

	/**
	 * Construct a new {@link TestCase}.
	 *
	 * @param function             function this test case refers to
	 * @param inputValues          values to pass to the function
	 * @param expectedOutputValues values the function is expected to return
	 */
	public TestCaseImpl(FunctionSignature function, List<Value> inputValues, List<Value> expectedOutputValues) {

		this.function = function;
		this.inputValues = Collections.unmodifiableList(inputValues);
		this.expectedOutputValues = Collections.unmodifiableList(expectedOutputValues);
	}

	@Override
	public FunctionSignature getFunction() {
		return this.function;
	}

	@Override
	public List<Value> getInputValues() {
		return this.inputValues; //is unmodifiable
	}

	@Override
	public List<Value> getExpectedOutputValues() {
		return this.expectedOutputValues; //is unmodifiable
	}

	/**
	 * Converts thrift {@link ch.bfh.progressor.executor.thrift.TestCase}s to custom {@link TestCase} instances.
	 *
	 * @param functions thrift functions to use
	 * @param testCases thrift test cases to convert
	 *
	 * @return custom {@link TestCase} instances
	 *
	 * @throws ExecutorException if conversation failed
	 */
	public static List<TestCase> convertFromThrift(List<ch.bfh.progressor.executor.thrift.FunctionSignature> functions, List<ch.bfh.progressor.executor.thrift.TestCase> testCases) throws ExecutorException {

		List<FunctionSignature> _functions = FunctionSignatureImpl.convertFromThrift(functions);
		List<TestCase> result = new ArrayList<>(testCases.size());
		for (ch.bfh.progressor.executor.thrift.TestCase testCase : testCases)
			try {
				FunctionSignature function = _functions.stream().filter(f -> f.getName().equals(testCase.getFunctionName())).findAny().get();
				if (function.getInputTypes().size() != testCase.getInputValuesSize() || function.getOutputTypes().size() != testCase.getExpectedOutputValuesSize())
					throw new ExecutorException("Could not find a matching function signature.");

				List<Value> inputValues = new ArrayList<>(testCase.getInputValuesSize()), expectedOutputValues = new ArrayList<>(testCase.getExpectedOutputValuesSize());
				for (int i = 0; i < testCase.getInputValuesSize(); i++)
					inputValues.add(ValueImpl.parse(function.getInputTypes().get(i), testCase.getInputValues().get(i)));
				for (int i = 0; i < testCase.getExpectedOutputValuesSize(); i++)
					expectedOutputValues.add(ValueImpl.parse(function.getOutputTypes().get(i), testCase.getExpectedOutputValues().get(i)));
				result.add(new TestCaseImpl(function, inputValues, expectedOutputValues));

			} catch (NoSuchElementException ex) {
				throw new ExecutorException("Could not find a matching function.", ex);
			}

		return result;
	}
}
