package ch.bfh.progressor.executor;

/**
 * Custom exception class to use in {@link CodeExecutor}s.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class ExecutorException extends Exception {

	private static final long serialVersionUID = -8545556134081763337L;

	private boolean fatal;
	private String output;

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param fatal   whether or not the error is fatal
	 * @param message the detail message
	 */
	public ExecutorException(boolean fatal, String message) {
		super(message);

		this.fatal = fatal;
	}

	/**
	 * Constructs a new exception with the specified detail message and console output.
	 *
	 * @param fatal   whether or not the error is fatal
	 * @param message the detail message
	 * @param output  the console output
	 */
	public ExecutorException(boolean fatal, String message, String output) {
		this(fatal, message);

		this.output = output;
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param fatal   whether or not the error is fatal
	 * @param message the detail message
	 * @param cause   the cause of the exception
	 */
	public ExecutorException(boolean fatal, String message, Throwable cause) {
		super(message, cause);

		this.fatal = fatal;
	}

	/**
	 * Constructs a new exception with the specified detail message, console output and cause.
	 *
	 * @param fatal   whether or not the error is fatal
	 * @param message the detail message
	 * @param output  the console output
	 * @param cause   the cause of the exception
	 */
	public ExecutorException(boolean fatal, String message, String output, Throwable cause) {
		this(fatal, message, cause);

		this.output = output;
	}

	/**
	 * Gets whether or not the error is fatal.
	 *
	 * @return whether or not the error is fatal
	 */
	public boolean isFatal() {
		return this.fatal;
	}

	/**
	 * Gets the console output.
	 *
	 * @return the console output
	 */
	public String getOutput() {
		return this.output;
	}
}
