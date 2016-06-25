package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;
import ch.bfh.progressor.executor.api.VersionInformation;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.impl.CodeExecutorDockerBase;

/**
 * Code execution engine for JavaScript code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class JavaScriptExecutor extends CodeExecutorDockerBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "javascript";

	/**
	 * Name of the JavaScript file.
	 */
	protected static final String FILE_NAME = "main";

	/**
	 * Regular expression pattern for extracting the compiler version.
	 */
	protected static final Pattern VERSION_PATTERN = Pattern.compile("\\d[\\d\\.]*");

	@Override
	public String getLanguage() {
		return JavaScriptExecutor.CODE_LANGUAGE;
	}

	@Override
	public VersionInformation fetchVersionInformation() throws ExecutorException {

		String version = null;

		String versionOutput = this.executeSafeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "node", "--version");
		Matcher versionMatcher = JavaScriptExecutor.VERSION_PATTERN.matcher(versionOutput);
		if (versionMatcher.find())
			version = versionMatcher.group();

		return this.createVersionInformation("2015", "Node.js", version);
	}

	@Override
	protected String getTemplatePath() {
		return String.format("%s/template.js", this.getLanguage());
	}

	@Override
	protected List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException {

		final File codeFile = new File(codeDirectory, String.format("%s.js", JavaScriptExecutor.FILE_NAME));

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
			executionOutput = this.executeCommand(codeDirectory, "node", new File(CodeExecutorBase.CURRENT_DIRECTORY, codeFile.getName()).getPath());

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

			//validate input / output types & names
			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException("Exactly one output type has to be defined for a JavaScript sample.");

			sb.append("function ").append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(function.getInputNames().get(i));
			}

			sb.append(") {").append(CodeExecutorBase.NEWLINE).append('\t').append(CodeExecutorBase.NEWLINE).append('}').append(CodeExecutorBase.NEWLINE);
		}

		return sb.toString();
	}

	@Override
	protected String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			if (testCase.getExpectedOutputValues().size() != 1)
				throw new ExecutorException("Exactly one output value has to be defined for a JavaScript example.");

			sb.append(CodeExecutorBase.NEWLINE).append("try {").append(CodeExecutorBase.NEWLINE); //begin test case block

			sb.append("const start = process.hrtime();").append(CodeExecutorBase.NEWLINE);
			sb.append("const result = ").append(testCase.getFunction().getName()).append('('); //test case invocation
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(");").append(CodeExecutorBase.NEWLINE);
			sb.append("const diff = process.hrtime(start);").append(CodeExecutorBase.NEWLINE);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (testCase.getFunction().getOutputTypes().get(0).getBaseType()) {
				case FLOAT32:
				case FLOAT64:
				case DECIMAL:
					comparisonPrefix = "hasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSeparator = ", ";
					comparisonSuffix = ")";
					break;

				default:
					comparisonSeparator = " === "; //compare objects using equality operator
					break;
			}

			sb.append("const success = ").append(comparisonPrefix).append("result").append(comparisonSeparator); //result evaluation
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(";").append(CodeExecutorBase.NEWLINE);

			sb.append("console.log(`${success ? 'OK' : 'ER'}:${diff[0] * 1e3 + diff[1] * 1e-6}:${result}\\n`);").append(CodeExecutorBase.NEWLINE); //print result to the console

			sb.append("} catch (ex) {").append(CodeExecutorBase.NEWLINE); //finish test case block / begin exception handling
			sb.append("console.log(`ER:${ex}\\n`);").append(CodeExecutorBase.NEWLINE);
			sb.append('}'); //finish exception handling
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
				if (value.getType().getBaseType() == ValueType.BaseType.SET)
					sb.append("new Set(");
				sb.append("[ "); //begin initialisation

				boolean first = true; //generate collection elements
				for (Value element : value.getCollection()) {
					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(element));
				}

				sb.append(" ]");
				if (value.getType().getBaseType() == ValueType.BaseType.SET)
					sb.append(')');
				return sb.toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder(); //begin initialisation
				sb.append("new Map([ "); //begin initialisation

				first = true; //generate collection elements
				for (List<Value> element : value.get2DCollection()) { //generate key/value pairs
					if (element.size() != 2) //validate key/value pair
						throw new ExecutorException("Map entries always need a key and a value.");

					if (first) first = false;
					else sb.append(", ");
					sb.append("[ ").append(this.getValueLiteral(element.get(0))).append(", ").append(this.getValueLiteral(element.get(1))).append(" ]");
				}

				return sb.append(" ])").toString(); //finish initialisation and return literal

			case STRING:
			case CHARACTER:
				String valueSafe = IntStream.range(0, value.getSingle().length()).map(value.getSingle()::charAt).mapToObj(i -> String.format("\\u%04X", i))
																		.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

				return String.format("'%s'", valueSafe);

			case BOOLEAN:
				return Boolean.toString("true".equalsIgnoreCase(value.getSingle()));

			case INT8:
			case INT16:
			case INT32:
			case INT64:
				if (!CodeExecutorBase.NUMERIC_INTEGER_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric integer literal.", value));
				return value.getSingle();

			case FLOAT32:
			case FLOAT64:
			case DECIMAL:
				if (!CodeExecutorBase.NUMERIC_FLOATING_EXPONENTIAL_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric literal.", value));
				return value.getSingle();

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", value.getType()));
		}
	}

	@Override
	protected String getTypeName(ValueType type) throws ExecutorException {
		throw new UnsupportedOperationException();
	}
}
