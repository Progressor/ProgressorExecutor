package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

/**
 * Code execution engine for C# code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class CSharpExecutor extends CodeExecutorBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "csharp";

	/**
	 * Name of the C# executable.
	 */
	protected static final String EXECUTABLE_NAME = "main";

	/**
	 * Regular expression pattern for extracting the language version.
	 */
	protected static final Pattern LANGUAGE_VERSION_PATTERN = Pattern.compile("[/-]langversion:.+?(((iso-|)\\d+)(,[\\s\\r\\n]+))+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/**
	 * Regular expression pattern for extracting the compiler name.
	 */
	protected static final Pattern COMPILER_NAME_PATTERN = Pattern.compile(".+compiler", Pattern.CASE_INSENSITIVE);

	/**
	 * Regular expression pattern for extracting the compiler version.
	 */
	protected static final Pattern COMPILER_VERSION_PATTERN = Pattern.compile("[\\d\\.]+");

	@Override
	public String getLanguage() {
		return CSharpExecutor.CODE_LANGUAGE;
	}

	@Override
	public VersionInformation fetchVersionInformation() throws ExecutorException {

		String languageOutput, compilerOutput;
		String languageVersion = null, compilerName = null, compilerVersion = null;

		switch (CodeExecutorBase.PLATFORM) {
			case WINDOWS:
				languageOutput = compilerOutput = this.executeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "csc", "/help");
				break;

			case UNIX_LINUX:
				languageOutput = this.executeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "mcs", "/help");
				compilerOutput = this.executeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "mcs", "--version");
				break;

			default:
				throw new ExecutorException(String.format("Platform %s is nt supported.", CodeExecutorBase.PLATFORM));
		}

		Matcher languageMatcher = CSharpExecutor.LANGUAGE_VERSION_PATTERN.matcher(languageOutput);
		if (languageMatcher.find())
			languageVersion = languageMatcher.group(2);

		Matcher compilerNameMatcher = CSharpExecutor.COMPILER_NAME_PATTERN.matcher(compilerOutput);
		if (compilerNameMatcher.find())
			compilerName = compilerNameMatcher.group();

		Matcher compilerVersionMatcher = CSharpExecutor.COMPILER_VERSION_PATTERN.matcher(compilerOutput);
		if (compilerVersionMatcher.find())
			compilerVersion = compilerVersionMatcher.group();

		return this.createVersionInformation(languageVersion, compilerName, compilerVersion);
	}

	@Override
	protected String getTemplatePath() {
		return String.format("%s/template.cs", this.getLanguage());
	}

	@Override
	protected List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException {

		final File codeFile = new File(codeDirectory, String.format("%s.cs", CSharpExecutor.EXECUTABLE_NAME));
		final File executableFile = new File(codeDirectory, String.format("%s.exe", CSharpExecutor.EXECUTABLE_NAME));

		//*********************
		//*** GENERATE CODE ***
		//*********************
		this.generateCodeFile(codeFile, codeFragment, testCases);

		//********************
		//*** COMPILE CODE ***
		//********************
		long compilationStart = System.nanoTime();

		try {
			this.executeCommand(codeDirectory, CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "csc" : "mcs", codeFile.getName(), "/debug");

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not compile the user code.", ex);
		}

		long compilationEnd = System.nanoTime();

		//********************
		//*** EXECUTE CODE ***
		//********************
		String[] executionArguments = CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS
																	? new String[] { executableFile.getAbsolutePath() }
																	: new String[] { "mono", this.willUseDocker() ? new File(CodeExecutorBase.CURRENT_DIRECTORY, executableFile.getName()).getPath() : executableFile.getAbsolutePath(), "--debug" };

		long executionStart = System.nanoTime();

		String executionOutput;
		try {
			executionOutput = this.executeCommand(codeDirectory, executionArguments);

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not execute the user code.", ex);
		}

		long executionEnd = System.nanoTime();

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

			//validate input / output types & names
			if (function.getInputTypes().size() != function.getInputNames().size())
				throw new ExecutorException("The same number of input types & names have to be defined.");
			if (function.getOutputTypes().size() != 1 || function.getOutputTypes().size() != function.getOutputNames().size())
				throw new ExecutorException("Exactly one output type has to be defined for a C# sample.");

			sb.append("public ").append(this.getTypeName(function.getOutputTypes().get(0))).append(' ').append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getTypeName(function.getInputTypes().get(i))).append(' ').append(function.getInputNames().get(i));
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

			//validate input / output types & values
			if (testCase.getInputValues().size() != testCase.getFunction().getInputTypes().size())
				throw new ExecutorException("The same number of input values & types have to be defined.");
			if (testCase.getExpectedOutputValues().size() != 1 || testCase.getExpectedOutputValues().size() != testCase.getFunction().getOutputTypes().size())
				throw new ExecutorException("Exactly one output value has to be defined for a C# sample.");

			sb.append(newLine).append("try {").append(newLine); //begin test case block

			ValueType oType = testCase.getFunction().getOutputTypes().get(0); //test case invocation and return value storage
			sb.append(this.getTypeName(oType)).append(" ret = ").append("inst.").append(testCase.getFunction().getName()).append('(');
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(");").append(newLine);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (oType.getBaseType()) {
				case STRING:
				case CHARACTER:
				case BOOLEAN:
				case INT8:
				case INT16:
				case INT32:
				case INT64:
				case DECIMAL:
					comparisonSeparator = " == "; //compare primitive types using equality operator
					break;

				case FLOAT32:
				case FLOAT64:
					comparisonPrefix = "HasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSeparator = ", ";
					comparisonSuffix = ", 1)";
					break;

				default:
					comparisonSeparator = ".Equals("; //compare objects using equality method
					comparisonSuffix = ")";
					break;
			}

			sb.append("bool suc = ").append(comparisonPrefix).append("ret").append(comparisonSeparator);
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(";").append(newLine);
			sb.append("Console.WriteLine(\"{0}:{1}\", suc ? \"OK\" : \"ER\", ret);").append(newLine).append("Console.WriteLine();").append(newLine); //print result to the console

			sb.append("} catch (Exception ex) {").append(newLine); //finish test case block / begin exception handling
			sb.append("Console.WriteLine(\"ER:{0}\", ex);").append(newLine).append("Console.WriteLine();").append(newLine);
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
				sb.append("new ").append(this.getTypeName(value.getType())).append(" { "); //begin initialisation

				boolean first = true; //generate collection elements
				if (!value.getCollection().isEmpty())
					for (Value element : value.getCollection()) {
						if (first) first = false;
						else sb.append(", ");
						sb.append(this.getValueLiteral(element));
					}

				return sb.append(" }").toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder(); //begin initialisation
				sb.append("new ").append(this.getTypeName(value.getType())).append(" { "); //begin initialisation

				first = true; //generate collection elements
				if (!value.get2DCollection().isEmpty())
					for (List<Value> element : value.get2DCollection()) { //generate key/value pairs
						if (element.size() != 2) //validate key/value pair
							throw new ExecutorException("Map entries always need a key and a value.");

						if (first) first = false;
						else sb.append(", ");
						sb.append('{').append(this.getValueLiteral(element.get(0))).append(", ").append(this.getValueLiteral(element.get(1))).append('}'); //use C# 5 syntax for mono compatibility
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

				return value.getType().getBaseType() == ValueType.BaseType.INT64 ? String.format("%sL", value.getSingle()) : value.getSingle();

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

					case DECIMAL:
					default:
						return String.format("%sM", value.getSingle());
				}

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", value.getType()));
		}
	}

	@Override
	protected String getTypeName(ValueType type) throws ExecutorException {

		switch (type.getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				return String.format(type.getBaseType() == ValueType.BaseType.ARRAY ? "%s[]" : type.getBaseType() == ValueType.BaseType.LIST ? "List<%s>" : "HashSet<%s>",
														 this.getTypeName(type.getGenericParameters().get(0))); //return class name

			case MAP:
				return String.format("Dictionary<%s, %s>", this.getTypeName(type.getGenericParameters().get(0)), this.getTypeName(type.getGenericParameters().get(1))); //return class name

			case STRING:
				return "string";

			case CHARACTER:
				return "char";

			case BOOLEAN:
				return "bool";

			case INT8:
				return "sbyte";

			case INT16:
				return "short";

			case INT32:
				return "int";

			case INT64:
				return "long";

			case FLOAT32:
				return "float";

			case FLOAT64:
				return "double";

			case DECIMAL:
				return "decimal";

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", type));
		}
	}
}
