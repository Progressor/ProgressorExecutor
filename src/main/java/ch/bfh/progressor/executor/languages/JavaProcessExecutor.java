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
 * Code execution engine for Java code. <br>
 * Uses a new process to execute the custom Java code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class JavaProcessExecutor extends CodeExecutorDockerBase {

	/**
	 * Unique name of the language this executor supports.
	 */
	public static final String CODE_LANGUAGE = "java";

	/**
	 * Name of the Java main class.
	 */
	protected static final String CODE_CLASS_NAME = "Program";

	/**
	 * Regular expression pattern for extracting the Java compiler version.
	 */
	protected static final Pattern JAVAC_VERSION_PATTERN = Pattern.compile("[\\d\\._]+");

	/**
	 * Regular expression pattern for extracting the Java language version.
	 */
	protected static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("java\\b.+\\bse\\b.+\\bruntime\\b.+?([\\d\\._]+(-b\\d+|))", Pattern.CASE_INSENSITIVE);

	@Override
	public String getLanguage() {
		return JavaProcessExecutor.CODE_LANGUAGE;
	}

	@Override
	public VersionInformation fetchVersionInformation() throws ExecutorException {

		String javaVersion = null, javacVersion = null;

		String javaOutput = this.executeSafeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "java", "-version");
		Matcher javaMatcher = JavaProcessExecutor.JAVA_VERSION_PATTERN.matcher(javaOutput);
		if (javaMatcher.find())
			javaVersion = javaMatcher.group(1);

		String javacOutput = this.executeSafeCommand(CodeExecutorBase.CURRENT_DIRECTORY, "javac", "-version");
		Matcher javacMatcher = JavaProcessExecutor.JAVAC_VERSION_PATTERN.matcher(javacOutput);
		if (javacMatcher.find())
			javacVersion = javacMatcher.group();

		return this.createVersionInformation(javaVersion, "javac", javacVersion);
	}

	@Override
	protected String getTemplatePath() {
		return String.format("%s/template-process.java", this.getLanguage());
	}

	@Override
	protected List<Result> executeTestCases(String codeFragment, List<TestCase> testCases, File codeDirectory) throws ExecutorException {

		final File codeFile = new File(codeDirectory, String.format("%s.java", JavaProcessExecutor.CODE_CLASS_NAME));

		//*********************
		//*** GENERATE CODE ***
		//*********************
		this.generateCodeFile(codeFile, codeFragment, testCases);

		//********************
		//*** COMPILE CODE ***
		//********************
		final long compilationStart = System.nanoTime();

		try {
			this.executeSafeCommand(codeDirectory, "javac", codeFile.getName());
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
			executionOutput = this.executeCommand(codeDirectory, "java", JavaProcessExecutor.CODE_CLASS_NAME);

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
			if (function.getOutputTypes().size() != 1)
				throw new ExecutorException("Exactly one output type has to be defined for a Java sample.");

			sb.append("public ").append(this.getPrimitiveTypeName(function.getOutputTypes().get(0))).append(' ').append(function.getName()).append('(');

			for (int i = 0; i < function.getInputTypes().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getPrimitiveTypeName(function.getInputTypes().get(i))).append(' ').append(function.getInputNames().get(i));
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
				throw new ExecutorException("Exactly one output value has to be defined for a Java example.");

			sb.append(CodeExecutorBase.NEWLINE).append("try {").append(CodeExecutorBase.NEWLINE); //begin test case block

			sb.append("long start = System.nanoTime();").append(CodeExecutorBase.NEWLINE);
			sb.append(this.getPrimitiveTypeName(testCase.getFunction().getOutputTypes().get(0))).append(" result = ").append("inst.").append(testCase.getFunction().getName()).append('('); //test case invocation
			for (int i = 0; i < testCase.getInputValues().size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(this.getValueLiteral(testCase.getInputValues().get(i)));
			}
			sb.append(");").append(CodeExecutorBase.NEWLINE);
			sb.append("long end = System.nanoTime();").append(CodeExecutorBase.NEWLINE);

			String comparisonPrefix = "", comparisonSeparator = "", comparisonSuffix = "";
			switch (testCase.getFunction().getOutputTypes().get(0).getBaseType()) {
				case CHARACTER:
				case BOOLEAN:
				case INT8:
				case INT16:
				case INT32:
				case INT64:
					comparisonSeparator = " == "; //compare primitive types using equality operator
					break;

				case FLOAT32:
				case FLOAT64:
					comparisonPrefix = "hasMinimalDifference("; //compare floating-point numbers using custom equality comparison
					comparisonSeparator = ", ";
					comparisonSuffix = ")";
					break;

				default:
					comparisonSeparator = ".equals("; //compare objects using equality method
					comparisonSuffix = ")";
					break;
			}

			sb.append("boolean success = ").append(comparisonPrefix).append("result").append(comparisonSeparator); //result evaluation
			sb.append(this.getValueLiteral(testCase.getExpectedOutputValues().get(0))).append(comparisonSuffix).append(';').append(CodeExecutorBase.NEWLINE);

			sb.append("out.write(String.format(\"%s:%f:%s%n%n\", success ? \"OK\" : \"ER\", (end - start) / 1e6, result));").append(CodeExecutorBase.NEWLINE); //print result to the console

			sb.append("} catch (Exception ex) {").append(CodeExecutorBase.NEWLINE); //finish test case block / begin exception handling
			sb.append("out.write(\"ER:\"); out.flush();").append(CodeExecutorBase.NEWLINE);
			sb.append("ex.printStackTrace(System.out);").append(CodeExecutorBase.NEWLINE);

			sb.append("} finally {").append(CodeExecutorBase.NEWLINE);
			sb.append("out.flush();").append(CodeExecutorBase.NEWLINE);
			sb.append('}');
		}

		return sb.toString();
	}

	@Override
	protected String getValueLiteral(Value value) throws ExecutorException {

		switch (value.getType().getBaseType()) {
			case ARRAY:
			case LIST:
			case SET:
				boolean isArr = value.getType().getBaseType() == ValueType.BaseType.ARRAY;
				boolean isLst = value.getType().getBaseType() == ValueType.BaseType.LIST;

				StringBuilder sb = new StringBuilder();
				if (isArr) //begin array initialisation syntax
					sb.append("new ").append(this.getPrimitiveTypeName(value.getType())).append(" { ");
				else if (isLst) //begin list initialisation using helper method
					sb.append("Arrays.<").append(this.getTypeName(value.getType().getGenericParameters().get(0))).append(">asList(");
				else {//begin set initialisation using constructor and helper method
					String elmCls = this.getTypeName(value.getType().getGenericParameters().get(0));
					sb.append("new HashSet<").append(elmCls).append(">(Arrays.<").append(elmCls).append(">asList(");
				}

				boolean first = true; //generate collection elements
				for (Value element : value.getCollection()) {
					if (first) first = false;
					else sb.append(", ");
					sb.append(this.getValueLiteral(element));
				}

				return sb.append(isArr ? " }" : isLst ? ')' : "))").toString(); //finish collection initialisation and return literal

			case MAP:
				sb = new StringBuilder(); //begin map initialisation using anonymous class with initialisation block
				sb.append("new HashMap<").append(this.getTypeName(value.getType().getGenericParameters().get(0))).append(", ");
				sb.append(this.getTypeName(value.getType().getGenericParameters().get(1))).append(">() {{ ");

				for (List<Value> element : value.get2DCollection()) { //generate key/value pairs
					if (element.size() != 2) //validate key/value pair
						throw new ExecutorException("Map entries always need a key and a value.");

					sb.append("put(").append(this.getValueLiteral(element.get(0))).append(", ").append(this.getValueLiteral(element.get(1))).append("); ");
				}

				return sb.append("}}").toString(); //finish initialisation and return literal

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
						return String.format("(%s)%s", this.getPrimitiveTypeName(value.getType()), value.getSingle());

					case INT32:
						return value.getSingle();

					default:
						return String.format("%sL", value.getSingle());
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
						return String.format("new BigDecimal(\"%s\")", value.getSingle());
				}

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", value.getType()));
		}
	}

	/**
	 * Gets the Java primitive name of an arbitrary type.
	 *
	 * @param type type to get name of
	 *
	 * @return Java primitive name of type
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected String getPrimitiveTypeName(ValueType type) throws ExecutorException {

		switch (type.getBaseType()) { //switch over primitive types
			case CHARACTER:
				return "char";

			case BOOLEAN:
				return "boolean";

			case INT8:
				return "byte";

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

			default:
				return this.getTypeName(type);
		}
	}

	@Override
	protected String getTypeName(ValueType type) throws ExecutorException {

		switch (type.getBaseType()) {
			case ARRAY:
				return String.format("%s[]", this.getPrimitiveTypeName(type.getGenericParameters().get(0))); //return primitive type name

			case LIST:
			case SET:
				return String.format(type.getBaseType() == ValueType.BaseType.LIST ? "List<%s>" : "Set<%s>", this.getTypeName(type.getGenericParameters().get(0))); //return class name

			case MAP:
				return String.format("Map<%s, %s>", this.getTypeName(type.getGenericParameters().get(0)), this.getTypeName(type.getGenericParameters().get(1))); //return class name

			case STRING:
				return "String";

			case CHARACTER:
				return "Character";

			case BOOLEAN:
				return "Boolean";

			case INT8:
				return "Byte";

			case INT16:
				return "Short";

			case INT32:
				return "Integer";

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
	}
}
