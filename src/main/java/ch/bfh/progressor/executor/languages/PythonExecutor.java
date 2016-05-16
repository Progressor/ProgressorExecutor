package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.impl.PerformanceIndicatorsImpl;
import ch.bfh.progressor.executor.impl.ResultImpl;

/**
 * Code execution engine for Python code. <br>
 * Compiles and executes the Python code in two steps.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class PythonExecutor extends CodeExecutorBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "python";

	/**
	 * Name of the Python executable.
	 */
	protected static final String EXECUTABLE_NAME = "main";

	/**
	 * Maximum time to use for the execution of the user code (in seconds).
	 */
	public static final int EXECUTION_TIMEOUT_SECONDS = 10;

	@Override
	public String getLanguage() {
		return PythonExecutor.CODE_LANGUAGE;
	}

	@Override
	public String getFragment(List<FunctionSignature> functions) throws ExecutorException {
		return this.getFunctionSignatures(functions);
	}

	@Override
	public List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException {

		final File localDirectory = new File(".");
		final File codeFile = new File(codeDirectory, String.format("%s.py", PythonExecutor.EXECUTABLE_NAME));

		//*********************
		//*** GENERATE CODE ***
		//*********************
		this.generateCodeFile(codeDirectory, codeFragment, testCases);

		//********************
		//*** EXECUTE CODE ***
		//********************
		long executionStart = System.nanoTime();

		Process executionProcess;
		try {
			executionProcess = this.executeCommand(codeDirectory, PythonExecutor.EXECUTION_TIMEOUT_SECONDS,
																						 CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ?
																						 new String[] { "python", codeFile.getName() } :
																						 new String[] { new File(localDirectory, codeFile.getName()).getPath() });

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not execute the user code.", ex);
		}

		long executionEnd = System.nanoTime();

		//****************************
		//*** TEST CASE EVALUATION ***
		//****************************
		List<Result> results = new ArrayList<>(testCases.size());
		for (String result : this.readDelimited(executionProcess, String.format("%n%n")))
			results.add(this.getResult(result.startsWith("OK"), false, result.substring(3), (executionEnd - executionStart) / 1e6));
		return results;

	}

	/**
	 * Generates the Python function signatures.
	 *
	 * @param functions functions to generate signatures for
	 *
	 * @return Python function signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {

			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException("Exactly one output type has to be defined for a Python sample.");

			sb.append("def ").append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(function.getInputNames().get(i));
			}

			sb.append(") : ").append(newLine);
		}

		return sb.toString();
	}

	/**
	 * Generates the Python test case signatures.
	 *
	 * @param testCases test cases to generate signatures for
	 *
	 * @return Python test case signatures
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		final String newLine = String.format("%n");
		final String indent = String.format("%c", '\t');

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			sb.append(newLine).append("try:").append(newLine); //begin test case block

			sb.append(indent).append("ret = ").append(testCase.getFunction().getName()).append('(');
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(')').append(newLine);

			String comparisonSeparator = " == ";
			/*
			switch (oType.getBaseType()) {
				case FLOAT32:
				case FLOAT64:
					comparisonSeparator = ".hasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSuffix = ")";
					break;

				default:
					comparisonSeparator = " == "; //compare objects using equality operator
					break;
			}
			*/
			sb.append(indent).append("suc = ").append("ret").append(comparisonSeparator);
			sb.append(indent).append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(newLine);
			sb.append(indent).append("print(\'%s:%s\' %(\"OK\" if suc else \"ER\", ret))").append(newLine); //print result to the console

			sb.append("except:").append(newLine); //finish test case block / begin exception handling
			sb.append(indent).append("print(\'ER: %s\' %(sys.exc_info()[0])").append(newLine);
		}
		return sb.toString();
	}

	/**
	 * Gets the Python literal for an arbitrary value.
	 *
	 * @param value value to get literal for
	 *
	 * @return Python literal for value
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getValueLiteral(Value value) throws ExecutorException {

		// Todo: PYTHON TUPLES (List as queues?) https://docs.python.org/3/tutorial/datastructures.html

		switch (value.getType().getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				StringBuilder sb = new StringBuilder();
				sb.append(value.getType().getBaseType() == ValueType.BaseType.SET ? "set([" : "[");
				boolean first = true; //generate collection elements
				if (!value.getCollection().isEmpty())
					for (Value element : value.getCollection()) {
						if (first) first = false;
						else sb.append(", ");
						sb.append(this.getValueLiteral(element));
					}

				return sb.append(value.getType().getBaseType() == ValueType.BaseType.SET ? "])" : "]").toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder("{"); //begin map initialisation

				first = true; //generate key/value pairs
				if (!value.get2DCollection().isEmpty())
					for (List<Value> element : value.get2DCollection()) {
						if (element.size() != 2) //validate key/value pair
							throw new ExecutorException("Map entries always need a key and a value.");

						if (first) first = false;
						else sb.append(", ");
						sb.append(this.getValueLiteral(element.get(0))).append(":").append(this.getValueLiteral(element.get(1)));
					}

				return sb.append("}").toString(); //finish initialisation and return literal

			case STRING:
			case CHARACTER:
				String valueSafe = IntStream.range(0, value.getSingle().length()).map(value.getSingle()::charAt).mapToObj(i -> String.format("\\u%04X", i))
																		.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

				char separator = '"';
				return String.format("%1$c%2$s%1$c", separator, valueSafe);

			case BOOLEAN:
				return Boolean.toString("true".equalsIgnoreCase(value.getSingle()));

			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case FLOAT32:
			case FLOAT64:
			case DECIMAL:
				if (!CodeExecutorBase.NUMERIC_INTEGER_PATTERN.matcher(value.getSingle()).matches() || !CodeExecutorBase.NUMERIC_FLOATING_EXPONENTIAL_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric literal.", value));
				return value.getSingle();
			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", value.getType()));
		}
	}


	/**
	 * Gets the Python name of an arbitrary type.
	 *
	 * @param type type to get name of
	 *
	 * @return Python name of type
	 *
	 * @throws ExecutorException if generation failed
	 */

	protected String getTypeName(ValueType type) throws ExecutorException {
		return null;
		/*
		switch (type.getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				return String.format("%s<%s>", type.getBaseType() == ValueType.BaseType.ARRAY ? "Array" : type.getBaseType() == ValueType.BaseType.LIST ? "List" : "Set",
														 this.getTypeName(type.getGenericParameters().get(0))); //return class name

			case MAP:
				return String.format("Map<%s, %s>", this.getTypeName(type.getGenericParameters().get(0)), this.getTypeName(type.getGenericParameters().get(1))); //return class name

			case STRING:
				return "String";

			case CHARACTER:
				return "Char";

			case BOOLEAN:
				return "Boolean";

			case INT8:
				return "Byte";

			case INT16:
				return "Short";

			case INT32:
				return "Int";

			case INT64:
				return "Long";

			case FLOAT32:
				return "Float";

			case FLOAT64:
				return "Double";

			case DECIMAL:
				return "BigDecimal";

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", type));
		}
		*/
	}

}
