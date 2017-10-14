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
 * Code execution engine for C# code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class CSharpExecutor extends CodeExecutorDockerBase {

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
	protected static final Pattern COMPILER_VERSION_PATTERN = Pattern.compile("\\d[\\d\\.]*");

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
				languageOutput = compilerOutput = this.executeSafeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "csc", "/help");
				break;

			case UNIX_LINUX:
				languageOutput = this.executeSafeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "mcs", "/help");
				compilerOutput = this.executeSafeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "mcs", "--version");
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
		final long compilationStart = System.nanoTime();

		try {
			this.executeSafeCommand(codeDirectory, CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS ? "csc" : "mcs", codeFile.getName(), "/debug");

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not compile the user code.", ex);
		}

		final long compilationEnd = System.nanoTime();

		//********************
		//*** EXECUTE CODE ***
		//********************
		String[] executionArguments = CodeExecutorBase.PLATFORM == ExecutorPlatform.WINDOWS
																	? new String[] { executableFile.getAbsolutePath() }
																	: new String[] { "mono", this.willUseDocker() ? new File(CodeExecutorBase.CURRENT_DIRECTORY, executableFile.getName()).getPath() : executableFile.getAbsolutePath(), "--debug" };

		final long executionStart = System.nanoTime();

		String executionOutput;
		try {
			executionOutput = this.executeCommand(codeDirectory, executionArguments);

		} catch (ExecutorException ex) {
			throw new ExecutorException("Could not execute the user code.", ex);
		}

		final long executionEnd = System.nanoTime();

		//****************************
		//*** TEST CASE EVALUATION ***
		//****************************
		return this.createResults(executionOutput,
															(compilationEnd - compilationStart) / CodeExecutorBase.MILLIS_IN_NANO,
															(executionEnd - executionStart) / CodeExecutorBase.MILLIS_IN_NANO);
	}

	@Override
	protected String getFunctionSignatures(List<FunctionSignature> functions) throws ExecutorException {

		StringBuilder sb = new StringBuilder();
		for (FunctionSignature function : functions) {

			//validate input / output types & names
			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException("Exactly one output type has to be defined for a C# sample.");

			sb.append(this.getTypeName(function.getOutputTypes().get(0))).append(' ').append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getTypeName(function.getInputTypes().get(i))).append(' ').append(function.getInputNames().get(i));
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
				throw new ExecutorException("Exactly one output value has to be defined for a C# example.");

			sb.append(CodeExecutorBase.NEWLINE).append("try {").append(CodeExecutorBase.NEWLINE); //begin test case block

			sb.append("var watch = new System.Diagnostics.Stopwatch();").append(CodeExecutorBase.NEWLINE);
			sb.append("watch.Start();").append(CodeExecutorBase.NEWLINE);
			sb.append(this.getTypeName(testCase.getFunction().getOutputTypes().get(0))).append(" result = ").append("inst.").append(testCase.getFunction().getName()).append('('); //test case invocation
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(");").append(CodeExecutorBase.NEWLINE);
			sb.append("watch.Stop();").append(CodeExecutorBase.NEWLINE);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (testCase.getFunction().getOutputTypes().get(0).getBaseType()) {
				case ARRAY:
				case LIST:
					comparisonSeparator = ".SequenceEqual("; //use extension method to compare ordered collections
					comparisonSuffix = ")";
					break;

				case SET:
					comparisonSeparator = ".SetEquals("; //use extension method to compare sets
					comparisonSuffix = ")";
					break;

				case MAP:
					comparisonPrefix = "DictionaryEquals("; //use helper method to compare directories
					comparisonSeparator = ", ";
					comparisonSuffix = ")";
					break;

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

			sb.append("bool success = ").append(comparisonPrefix).append("result").append(comparisonSeparator); //result evaluation
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(';').append(CodeExecutorBase.NEWLINE);

			String  formattingPrefix = "", formattingSuffix = "";
			switch (testCase.getFunction().getOutputTypes().get(0).getBaseType()) {
				case ARRAY:
				case LIST:
				case SET:
					formattingPrefix = "string.Format(\"{{ {0} }}\", string.Join(\", \", ";
					formattingSuffix = "))";
					break;

				case MAP:
					formattingPrefix = "string.Format(\"{{ {0} }}\", string.Join(\", \", ";
					formattingSuffix = ".Select(p => string.Format(\"{0}: {1}\", p.Key, p.Value))))";
					break;
			}

			sb.append("Console.WriteLine(\"{0}:{1}:{2}\", success ? \"OK\" : \"ER\", watch.Elapsed.TotalMilliseconds, "); //print result to the console
			sb.append(formattingPrefix).append("result").append(formattingSuffix).append(");");

			sb.append("} catch (Exception ex) {").append(CodeExecutorBase.NEWLINE); //finish test case block / begin exception handling
			sb.append("Console.WriteLine(\"ER:{0}\", ex);").append(CodeExecutorBase.NEWLINE);

			sb.append("} finally {").append(CodeExecutorBase.NEWLINE);
			sb.append("Console.WriteLine();").append(CodeExecutorBase.NEWLINE);
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
				for (List<Value> element : value.get2DCollection()) { //generate key/value pairs
					if (element.size() != 2) //validate key/value pair
						throw new ExecutorException("Map entries always need a key and a value.");

					if (first) first = false;
					else sb.append(", ");
					sb.append('[').append(this.getValueLiteral(element.get(0))).append("] = ").append(this.getValueLiteral(element.get(1))); //use C# 5 syntax for mono compatibility
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
