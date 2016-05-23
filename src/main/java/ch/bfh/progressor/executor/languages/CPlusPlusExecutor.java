package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

/**
 * Code execution engine for C/C++ code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class CPlusPlusExecutor extends CodeExecutorBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "cpp";

	/**
	 * Name of the C/C++ executable.
	 */
	protected static final String EXECUTABLE_NAME = "main";

	/**
	 * Regular expression pattern for extracting the GCC version.
	 */
	protected static final Pattern GCC_VERSION_PATTERN = Pattern.compile("[\\d\\.]+");

	@Override
	public String getLanguage() {
		return CPlusPlusExecutor.CODE_LANGUAGE;
	}

	@Override
	public VersionInformation fetchVersionInformation() throws ExecutorException {

		String compilerVersion = null;

		String compilerOutput = this.executeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "g++", "--version");
		Matcher compilerMatcher = CPlusPlusExecutor.GCC_VERSION_PATTERN.matcher(compilerOutput);
		if (compilerMatcher.find())
			compilerVersion = compilerMatcher.group();

		return this.createVersionInformation("C++11", "GCC", compilerVersion);
	}

	@Override
	protected String getTemplatePath() {
		return String.format("%s/template.cpp", this.getLanguage());
	}

	@Override
	protected List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException {

		final File codeFile = new File(codeDirectory, String.format("%s.cpp", CPlusPlusExecutor.EXECUTABLE_NAME));
		final File executableFile = new File(codeDirectory, CPlusPlusExecutor.EXECUTABLE_NAME);

		//*********************
		//*** GENERATE CODE ***
		//*********************
		this.generateCodeFile(codeFile, codeFragment, testCases);

		//********************
		//*** COMPILE CODE ***
		//********************
		final long compilationStart = System.nanoTime();

		try {
			this.executeCommand(codeDirectory, "g++", codeFile.getName(), "-std=c++11", "-o", CPlusPlusExecutor.EXECUTABLE_NAME);
		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not compile the user code.", ex);
		}

		final long compilationEnd = System.nanoTime();

		//********************
		//*** EXECUTE CODE ***
		//********************
		final long executionStart = System.nanoTime();

		String executionOutput;
		try {
			executionOutput = this.executeCommand(codeDirectory, this.willUseDocker() ? new File(CodeExecutorBase.CURRENT_DIRECTORY, executableFile.getName()).getPath() : executableFile.getAbsolutePath());

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not execute the user code.", ex);
		}

		final long executionEnd = System.nanoTime();

		//****************************
		//*** TEST CASE EVALUATION ***
		//****************************
		return this.createResults(executionOutput, compilationEnd - compilationStart, executionEnd - executionStart, TimeUnit.NANOSECONDS);
	}

	@Override
	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {
			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException("Exactly one output type has to be defined for a C/C++ sample.");

			sb.append(this.getTypeName(function.getOutputTypes().get(0), true)).append(' ').append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getTypeName(function.getInputTypes().get(i), true)).append(' ').append(function.getInputNames().get(i));
			}

			sb.append(") {").append(newLine).append('\t').append(newLine).append('}').append(newLine);
		}

		return sb.toString();
	}

	@Override
	protected String getTestCaseSignatures(List<TestCase> testCases) throws ExecutorException {

		final String newLine = String.format("%n");

		StringBuilder sb = new StringBuilder();
		for (TestCase testCase : testCases) {
			if (testCase.getExpectedOutputValues().size() != 1)
				throw new ExecutorException("Exactly one output value has to be defined for a C/C++ example.");

			sb.append(newLine).append("try {").append(newLine); //begin test case block

			sb.append("high_resolution_clock::time_point start = high_resolution_clock::now();").append(newLine);
			sb.append(this.getTypeName(testCase.getFunction().getOutputTypes().get(0), true)).append(" result = ").append(testCase.getFunction().getName()).append('('); //test case invocation
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(");").append(newLine);
			sb.append("high_resolution_clock::time_point end = high_resolution_clock::now();").append(newLine);
			sb.append("duration<double, milli> duration = end - start;").append(newLine);

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
					comparisonSeparator = " == "; //compare objects using equality operator
					break;
			}

			sb.append("bool success = ").append(comparisonPrefix).append("result").append(comparisonSeparator); //result evaluation
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(";").append(newLine);

			String resultPrefix = "";
			switch (testCase.getFunction().getOutputTypes().get(0).getBaseType()) {
				case INT8: //force numeric types to be printed as numbers (not chars or the like)
				case INT16:
				case INT32:
				case INT64:
				case FLOAT32:
				case FLOAT64:
				case DECIMAL:
					resultPrefix = "+";
			}

			sb.append("cout << (success ? \"OK\" : \"ER\") << \":\" << ").append("duration.count()").append(" << \":\" << ").append(resultPrefix).append("result << endl << endl;").append(newLine); //print result to the console
			sb.append("} catch (const exception &ex) {").append(newLine); //finish test case block / begin exception handling (standard exception class)
			sb.append("cout << \"ER:\" << ex.what() << endl << endl;").append(newLine);
			sb.append("} catch (const string &ex) {").append(newLine); //secondary exception handling (exception C++-string)
			sb.append("cout << \"ER:\" << ex << endl << endl;").append(newLine);
			sb.append("} catch (char const* const &ex) {").append(newLine); //tertiary exception handling (exception C-string)
			sb.append("cout << \"ER:\" << ex << endl << endl;").append(newLine);
			sb.append("} catch (...) {").append(newLine); //last resort (handling all unknown exceptions)
			sb.append("cout << \"ER:unknown exception\" << endl << endl;").append(newLine);
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
				if (value.getType().getBaseType() == ValueType.BaseType.ARRAY) //begin array initialisation syntax
					sb.append("new ").append(this.getTypeName(value.getType().getGenericParameters().get(0))).append('[').append(value.getCollection().size()).append("] { ");
				else
					sb.append(this.getTypeName(value.getType())).append(" { ");

				boolean first = true; //generate collection elements
				for (Value element : value.getCollection()) {
					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(element));
				}

				return sb.append(" }").toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder(); //begin map initialisation
				sb.append(this.getTypeName(value.getType())).append(" { ");

				first = true; //generate collection elements
				if (!value.get2DCollection().isEmpty())
					for (List<Value> element : value.get2DCollection()) { //generate key/value pairs
						if (element.size() != 2) //validate key/value pair
							throw new ExecutorException("Map entries always need a key and a value.");

						if (first) first = false;
						else sb.append(", ");
						sb.append('{').append(this.getValueLiteral(element.get(0))).append(", ").append(this.getValueLiteral(element.get(1))).append('}');
					}

				return sb.append(" }").toString(); //finish initialisation and return literal

			case STRING:
			case CHARACTER:
				String valueSafe = IntStream.range(0, value.getSingle().length()).map(value.getSingle()::charAt).mapToObj(i -> String.format("\\u%04X", i))
																		.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

				char separator = value.getType().getBaseType() == ValueType.BaseType.CHARACTER ? '\'' : '"';
				return String.format("%1$c%2$s%1$c", separator, valueSafe);

			case BOOLEAN:
				return Boolean.toString("true".equalsIgnoreCase(value.getSingle()));

			case INT8:
			case INT16:
			case INT32:
			case INT64:
				if (!CodeExecutorBase.NUMERIC_INTEGER_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric integer literal.", value));

				switch (value.getType().getBaseType()) {
					case INT8:
					case INT16:
					case INT32:
						return value.getSingle();

					default:
						return String.format("%sLL", value.getSingle());
				}

			case FLOAT32:
			case FLOAT64:
			case DECIMAL:
				if (!CodeExecutorBase.NUMERIC_FLOATING_EXPONENTIAL_PATTERN.matcher(value.getSingle()).matches())
					throw new ExecutorException(String.format("Value %s is not a valid numeric literal.", value));

				switch (value.getType().getBaseType()) {
					case FLOAT32:
						return String.format("%sF", value.getSingle());

					case FLOAT64:
						return value.getSingle();

					default:
						return String.format("%sL", value.getSingle());
				}

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", value.getType()));
		}
	}

	@Override
	protected String getTypeName(ValueType type) throws ExecutorException {
		return this.getTypeName(type, false);
	}

	/**
	 * Gets the name of an arbitrary type.
	 *
	 * @param type          type to get name of
	 * @param isDeclaration whether or not this type is used for a declaration
	 *
	 * @return name of type
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getTypeName(ValueType type, boolean isDeclaration) throws ExecutorException {

		switch (type.getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				if (type.getBaseType() == ValueType.BaseType.ARRAY) //alternative: array<%s>
					return String.format(isDeclaration ? "%s*" : "%s[]", this.getTypeName(type.getGenericParameters().get(0), true)); //return class name
				else
					return String.format(type.getBaseType() == ValueType.BaseType.LIST ? "vector<%s>" : "set<%s>", this.getTypeName(type.getGenericParameters().get(0), true)); //return class name

			case MAP:
				return String.format("map<%s, %s>", this.getTypeName(type.getGenericParameters().get(0), true), this.getTypeName(type.getGenericParameters().get(1), true)); //return class name

			case STRING:
				return "string";

			case CHARACTER:
				return "char";

			case BOOLEAN:
				return "bool";

			case INT8:
				return "int8_t";

			case INT16:
				return "int16_t";

			case INT32:
				return "int32_t";

			case INT64:
				return "int64_t";

			case FLOAT32:
				return "float";

			case FLOAT64:
				return "double";

			case DECIMAL:
				return "long double";

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", type));
		}
	}
}
