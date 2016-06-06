package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;
import ch.bfh.progressor.executor.api.VersionInformation;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.impl.CodeExecutorDockerBase;

/**
 * Code execution engine for Python code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class PythonExecutor extends CodeExecutorDockerBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "python";

	/**
	 * Name of the Python executable.
	 */
	protected static final String EXECUTABLE_NAME = "main";

	/**
	 * Regular expression pattern for extracting the language/compiler version.
	 */
	protected static final Pattern VERSION_PATTERN = Pattern.compile("[\\d\\.]+");

	@Override
	public String getLanguage() {
		return PythonExecutor.CODE_LANGUAGE;
	}

	@Override
	public VersionInformation fetchVersionInformation() throws ExecutorException {

		String compiler = CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "python" : "python3";
		String version = null;

		String versionOutput = this.executeSafeCommand(CodeExecutorBase.CURRENT_DIRECTORY, compiler, "--version");
		Matcher versionMatcher = PythonExecutor.VERSION_PATTERN.matcher(versionOutput);
		if (versionMatcher.find())
			version = versionMatcher.group();

		return this.createVersionInformation(version, compiler, version);
	}

	@Override
	protected String getTemplatePath() {
		return String.format("%s/template.py", this.getLanguage());
	}

	@Override
	public List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException {

		final File codeFile = new File(codeDirectory, String.format("%s.py", PythonExecutor.EXECUTABLE_NAME));

		//*********************
		//*** GENERATE CODE ***
		//*********************
		this.generateCodeFile(codeFile, codeFragment, testCases);

		//********************
		//*** EXECUTE CODE ***
		//********************
		final long executionStart = System.nanoTime();

		String executionOutput;
		try {
			executionOutput = this.executeCommand(codeDirectory, CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "python" : "python3", codeFile.getName());

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not execute the user code.", ex);
		}

		final long executionEnd = System.nanoTime();

		//****************************
		//*** TEST CASE EVALUATION ***
		//****************************
		return this.createResults(executionOutput,
															Double.NaN,
															(executionEnd - executionStart) / CodeExecutorBase.MILLIS_IN_NANO);
	}

	@Override
	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {

			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException("Exactly one output type has to be defined for a Python sample.");

			sb.append("def ").append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(function.getInputNames().get(i));
			}

			sb.append("):").append(CodeExecutorBase.NEWLINE).append('\t').append(CodeExecutorBase.NEWLINE);
		}

		return sb.toString();
	}

	@Override
	protected String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		final String indentation = "\t";

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			if (testCase.getExpectedOutputValues().size() != 1)
				throw new ExecutorException("Exactly one output value has to be defined for a Python example.");

			sb.append(CodeExecutorBase.NEWLINE).append("try:").append(CodeExecutorBase.NEWLINE); //begin test case block

			sb.append(indentation).append("start = time.time()").append(CodeExecutorBase.NEWLINE);
			sb.append(indentation).append("result = ").append(testCase.getFunction().getName()).append('('); //test case invocation
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(')').append(CodeExecutorBase.NEWLINE);
			sb.append(indentation).append("end = time.time()").append(CodeExecutorBase.NEWLINE);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (testCase.getFunction().getOutputTypes().get(0).getBaseType()) {
				case FLOAT32:
				case FLOAT64:
					comparisonPrefix = "hasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSeparator = ", ";
					comparisonSuffix = ")";
					break;

				default:
					comparisonSeparator = " == "; //compare objects using equality operator
					break;
			}

			sb.append(indentation).append("success = ").append(comparisonPrefix).append("result").append(comparisonSeparator); //result evaluation
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(CodeExecutorBase.NEWLINE);

			sb.append(indentation).append("print('%s:%f:%s' % ('OK' if success else 'ER', (end - start) * 1e3, result))").append(CodeExecutorBase.NEWLINE); //print result to the console

			sb.append("except:").append(CodeExecutorBase.NEWLINE); //finish test case block / begin exception handling
			sb.append(indentation).append("print('ER:%s (%s)' % sys.exc_info()[0:2])").append(CodeExecutorBase.NEWLINE);

			sb.append("finally:").append(CodeExecutorBase.NEWLINE); //add empty line
			sb.append(indentation).append("print()").append(CodeExecutorBase.NEWLINE);
		}

		return sb.toString();
	}

	@Override
	protected String getValueLiteral(Value value) throws ExecutorException {

		switch (value.getType().getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				StringBuilder sb = new StringBuilder();
				sb.append(value.getType().getBaseType() == ValueType.BaseType.SET ? "{ " : "[ ");
				boolean first = true; //generate collection elements
				for (Value element : value.getCollection()) {
					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(element));
				}

				return sb.append(value.getType().getBaseType() == ValueType.BaseType.SET ? " }" : " ]").toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder("{ "); //begin map initialisation

				first = true; //generate key/value pairs
				for (List<Value> element : value.get2DCollection()) {
					if (element.size() != 2) //validate key/value pair
						throw new ExecutorException("Map entries always need a key and a value.");

					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(element.get(0))).append(": ").append(this.getValueLiteral(element.get(1)));
				}

				return sb.append(" }").toString(); //finish initialisation and return literal

			case STRING:
			case CHARACTER:
				String valueSafe = IntStream.range(0, value.getSingle().length()).map(value.getSingle()::charAt).mapToObj(i -> String.format("\\u%04X", i))
																		.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

				return String.format("'%s'", valueSafe);

			case BOOLEAN:
				return "true".equalsIgnoreCase(value.getSingle()) ? "True" : "False";

			case INT8:
			case INT16:
			case INT32:
			case INT64:
				if (!CodeExecutorBase.NUMERIC_INTEGER_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric integer literal.", value));
				return value.getSingle();

			case FLOAT32:
			case FLOAT64:
				if (!CodeExecutorBase.NUMERIC_FLOATING_EXPONENTIAL_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric literal.", value));
				return value.getSingle();

			case DECIMAL:
				if (!CodeExecutorBase.NUMERIC_FLOATING_EXPONENTIAL_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric literal.", value));
				return String.format("Decimal('%s')", value.getSingle());

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", value.getType()));
		}
	}

	@Override
	protected String getTypeName(ValueType type) {
		throw new UnsupportedOperationException();
	}
}
