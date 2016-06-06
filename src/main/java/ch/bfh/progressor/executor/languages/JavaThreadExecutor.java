package ch.bfh.progressor.executor.languages;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.api.TestCase;
import ch.bfh.progressor.executor.api.Value;
import ch.bfh.progressor.executor.api.ValueType;
import ch.bfh.progressor.executor.api.VersionInformation;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;

/**
 * Code execution engine for Java code. <br>
 * Uses a new thread in the current process to execute the custom Java code.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class JavaThreadExecutor extends CodeExecutorBase {

	private static final Logger LOGGER = Logger.getLogger(JavaThreadExecutor.class.getName());

	private static final int MAX_EXECUTION_TIMEOUT_MILLIS = 4500; //CodeExecutorBase.MAX_INITIAL_TIMEOUT_MILLIS
	private static final int MAX_INTERRUPT_TIMEOUT_MILLIS = 250;

	private static final Class<?>[] VALUE_TYPE_ARRAY = new Class<?>[0];

	@Override
	public String getLanguage() {
		return JavaProcessExecutor.CODE_LANGUAGE;
	}

	@Override
	public VersionInformation getVersionInformation() throws ExecutorException {

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
		return String.format("%s/template-thread.java", this.getLanguage());
	}

	@Override
	public List<Result> execute(String codeFragment, List<TestCase> testCases) {

		final File codeDirectory = Paths.get("temp", UUID.randomUUID().toString()).toFile(); //create a temporary directory
		final File codeFile = new File(codeDirectory, String.format("%s.java", JavaProcessExecutor.CODE_CLASS_NAME));

		List<Result> results = new ArrayList<>(testCases.size());

		try {
			if (!codeDirectory.exists() && !codeDirectory.mkdirs())
				throw new ExecutorException("Could not create a temporary directory for the user code.");

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
			//final long executionStart = System.nanoTime();

			try (URLClassLoader ldr = new URLClassLoader(new URL[] { codeDirectory.toURI().toURL() })) {
				Class<?> cls = ldr.loadClass(JavaProcessExecutor.CODE_CLASS_NAME);
				Constructor<?> cst = cls.getConstructor();
				Object obj = cst.newInstance();

				for (TestCase testCase : testCases)
					try {
						Method mtd = cls.getMethod(testCase.getFunction().getName(), this.getPrimitiveTypes(testCase.getFunction().getInputTypes()).toArray(VALUE_TYPE_ARRAY));
						if (!this.getPrimitiveType(testCase.getFunction().getOutputTypes().get(0)).equals(mtd.getReturnType()))
							throw new ExecutorException("Could not find method with matching return type.");

						final long testCaseExecutionStart = System.nanoTime();

						FutureTask<?> future = new FutureTask<>(() -> mtd.invoke(obj, this.getValues(testCase.getInputValues()).toArray()));
						Thread thread = new Thread(future);
						thread.start();

						Object output;
						try {
							output = future.get(JavaThreadExecutor.MAX_EXECUTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

						} catch (ExecutionException ex) {
							throw new ExecutorException("Could not execute the user code.", ex);

						} catch (TimeoutException ex) {
							thread.interrupt();

							thread.join(JavaThreadExecutor.MAX_INTERRUPT_TIMEOUT_MILLIS);
							if (thread.isAlive())
								thread.stop();

							throw new ExecutorException("Could not execute the user code in time.", ex);
						}

						final long testCaseExecutionEnd = System.nanoTime();

						//****************************
						//*** TEST CASE EVALUATION ***
						//****************************
						Object expectedOutput = this.getValue(testCase.getExpectedOutputValues().get(0));
						boolean success;
						switch (testCase.getFunction().getOutputTypes().get(0).getBaseType()) {
							//case CHARACTER:
							//case BOOLEAN:
							//case INT8:
							//case INT16:
							//case INT32:
							//case INT64:
							//	//compare primitive types using equality operator
							//	break;

							case FLOAT32:
								success = this.hasMinimalDifference((Float)output, (Float)expectedOutput); //compare floating-point numbers using custom equality comparison
								break;

							case FLOAT64:
								success = this.hasMinimalDifference((Double)output, (Double)expectedOutput); //compare floating-point numbers using custom equality comparison
								break;

							default:
								success = output.equals(expectedOutput); //compare objects using equality method
								break;
						}

						results.add(this.createResult(success, false, output.toString(),
																					(compilationEnd - compilationStart) / CodeExecutorBase.MILLIS_IN_NANO,
																					Double.NaN,
																					(testCaseExecutionEnd - testCaseExecutionStart) / CodeExecutorBase.MILLIS_IN_NANO));

					} catch (Exception ex) {
						StringBuilder sb = new StringBuilder("Could not invoke the user code.").append(CodeExecutorBase.NEWLINE);
						Throwable throwable = ex;
						do sb.append(throwable).append(CodeExecutorBase.NEWLINE);
						while ((throwable = throwable.getCause()) != null);

						results.add(this.createResult(false, false, sb.toString()));
					}
			}

			//final long executionEnd = System.nanoTime();

		} catch (Throwable ex) {
			StringBuilder sb = new StringBuilder("Could not invoke the user code.").append(CodeExecutorBase.NEWLINE);
			Throwable throwable = ex;
			do sb.append(throwable).append(CodeExecutorBase.NEWLINE);
			while ((throwable = throwable.getCause()) != null);

			try {
				results.addAll(Collections.nCopies(testCases.size() - results.size(), this.createResult(false, true, sb.toString())));

			} catch (ExecutorException ex2) {
				throw new RuntimeException("Could not invoke the user code.", ex);
			}

		} finally {
			if (codeDirectory.exists())
				if (!this.tryDeleteRecursive(codeDirectory))
					JavaThreadExecutor.LOGGER.warning("Could not delete temporary folder.");
		}

		return results;
	}

	private boolean hasMinimalDifference(float value1, float value2) {

		if (!Float.isFinite(value1) || !Float.isFinite(value2)) return value1 == value2;
		return value1 == value2 || Math.abs(value1 - value2) <= Math.ulp(value1);
	}

	private boolean hasMinimalDifference(double value1, double value2) {

		if (!Double.isFinite(value1) || !Double.isFinite(value2)) return value1 == value2;
		return value1 == value2 || Math.abs(value1 - value2) <= Math.ulp(value1);
	}

	@Override
	protected void generateCodeFile(File codeFile, String codeFragment, List<TestCase> testCases) throws ExecutorException {

		try {
			StringBuilder code = this.getTemplate(); //read the template

			int fragStart = code.indexOf(CodeExecutorBase.CODE_CUSTOM_FRAGMENT); //place fragment in template
			code.replace(fragStart, fragStart + CodeExecutorBase.CODE_CUSTOM_FRAGMENT.length(), codeFragment);

			Files.write(codeFile.toPath(), code.toString().getBytes(CodeExecutorBase.CHARSET)); //and write the generated code in it

		} catch (ExecutorException | IOException ex) {
			throw new ExecutorException("Could not generate the code file.", ex);
		}
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
		throw new UnsupportedOperationException();
	}

	@Override
	protected String getValueLiteral(Value value) throws ExecutorException {
		throw new UnsupportedOperationException();
	}

	private List<Object> getValues(List<Value> values) throws ExecutorException {

		List<Object> mapped = new ArrayList<>(values.size());
		for (Value value : values)
			mapped.add(this.getValue(value));

		return mapped;
	}

	/**
	 * Gets Java {@link Object} for an arbitrary value.
	 *
	 * @param value value to get {@link Object} for
	 *
	 * @return {@link Object} for value
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected Object getValue(Value value) throws ExecutorException {

		try {
			switch (value.getType().getBaseType()) {
				case ARRAY:
					Object array = Array.newInstance(this.getPrimitiveType(value.getType().getGenericParameters().get(0)), value.getCollection().size());
					for (int i = 0; i < value.getCollection().size(); i++)
						Array.set(array, i, this.getValue(value.getCollection().get(i)));
					return array;

				case LIST:
				case SET:
					Collection<Object> set = value.getType().getBaseType() == ValueType.BaseType.LIST ? new ArrayList<>(value.getCollection().size())
																																														: new HashSet<>(value.getCollection().size());
					for (Value element : value.getCollection())
						set.add(this.getValue(element));
					return set;

				case MAP:
					Map<Object, Object> map = new HashMap<>(value.get2DCollection().size());
					for (List<Value> element : value.get2DCollection())  //get key/value pairs
						if (element.size() != 2) //validate key/value pair
							throw new ExecutorException("Map entries always need a key and a value.");
						else
							map.put(this.getValue(element.get(0)), this.getValue(element.get(1)));
					return map;

				case STRING:
					return value.getSingle();

				case CHARACTER:
					if (value.getSingle().length() != 1)
						throw new ExecutorException(String.format("Value %s is not a valid character literal.", value));
					return value.getSingle().charAt(0);

				case BOOLEAN:
					return "true".equalsIgnoreCase(value.getSingle());

				case INT8:
					return Byte.parseByte(value.getSingle());

				case INT16:
					return Short.parseShort(value.getSingle());

				case INT32:
					return Integer.parseInt(value.getSingle());

				case INT64:
					return Long.parseLong(value.getSingle());

				case FLOAT32:
					return Float.parseFloat(value.getSingle());

				case FLOAT64:
					return Double.parseDouble(value.getSingle());

				case DECIMAL:
					return new BigDecimal(value.getSingle());

				default:
					throw new ExecutorException(String.format("Value type %s is not supported.", value.getType()));
			}

		} catch (NumberFormatException ex) {
			throw new ExecutorException(String.format("Value %s is not a valid numeric literal.", value));
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

	private List<Class<?>> getPrimitiveTypes(List<ValueType> types) throws ExecutorException {

		List<Class<?>> mapped = new ArrayList<>(types.size());
		for (ValueType type : types)
			mapped.add(this.getPrimitiveType(type));

		return mapped;
	}

	/**
	 * Gets the Java primitive {@link Class} of an arbitrary type.
	 *
	 * @param type type to get {@link Class} of
	 *
	 * @return Java primitive {@link Class} of type
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected Class<?> getPrimitiveType(ValueType type) throws ExecutorException {

		switch (type.getBaseType()) { //switch over primitive types
			case CHARACTER:
				return Character.TYPE;

			case BOOLEAN:
				return Boolean.TYPE;

			case INT8:
				return Byte.TYPE;

			case INT16:
				return Short.TYPE;

			case INT32:
				return Integer.TYPE;

			case INT64:
				return Long.TYPE;

			case FLOAT32:
				return Float.TYPE;

			case FLOAT64:
				return Double.TYPE;

			default:
				return this.getType(type);
		}
	}

	/**
	 * Gets the {@link Class} of an arbitrary type.
	 *
	 * @param type type to get {@link Class} of
	 *
	 * @return {@link Class} of type
	 *
	 * @throws ExecutorException if generation failed
	 */
	protected Class<?> getType(ValueType type) throws ExecutorException {

		switch (type.getBaseType()) {
			case ARRAY:
				return Array.newInstance(this.getPrimitiveType(type.getGenericParameters().get(0)), 0).getClass();

			case LIST:
				return List.class;

			case SET:
				return Set.class;

			case MAP:
				return Map.class;

			case STRING:
				return String.class;

			case CHARACTER:
				return Character.class;

			case BOOLEAN:
				return Boolean.class;

			case INT8:
				return Byte.class;

			case INT16:
				return Short.class;

			case INT32:
				return Integer.class;

			case INT64:
				return Long.class;

			case FLOAT32:
				return Float.class;

			case FLOAT64:
				return Double.class;

			case DECIMAL:
				return BigDecimal.class;

			default:
				throw new ExecutorException(String.format("Value type %s is not supported.", type));
		}
	}
}
