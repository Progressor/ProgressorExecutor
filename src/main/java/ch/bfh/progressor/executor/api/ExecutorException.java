package ch.bfh.progressor.executor.api;

/**
 * Custom exception class to use in {@link CodeExecutor}s.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class ExecutorException extends Exception {

	private static final long serialVersionUID = -8545556134081763337L;

	private final String output;

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param message the detail message
	 */
	public ExecutorException(String message) {
		this(message, (String)null);
	}

	/**
	 * Constructs a new exception with the specified detail message and console output.
	 *
	 * @param message the detail message
	 * @param output  the console output
	 */
	public ExecutorException(String message, String output) {
		super(message);

		this.output = output;
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause of the exception
	 */
	public ExecutorException(String message, Throwable cause) {
		this(message, null, cause);
	}

	/**
	 * Constructs a new exception with the specified detail message, console output and cause.
	 *
	 * @param message the detail message
	 * @param output  the console output
	 * @param cause   the cause of the exception
	 */
	public ExecutorException(String message, String output, Throwable cause) {
		super(message, cause);

		this.output = output;
	}

	/**
	 * Gets the console output.
	 *
	 * @return the console output
	 */
	public String getOutput() {
		return this.output;
	}

	/**
	 * Returns a description of the {@code ExecutorException}.
	 *
	 * @return the message as well as the output (if present)
	 */
	@Override
	public String toString() {

		return String.format(this.output != null ? "%s%n%s" : "%s", super.toString(), this.output);
	}
}
