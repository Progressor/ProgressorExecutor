package ch.bfh.progressor.executor;

/**
 * Custom exception class to use in {@link CodeExecutor}s.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class ExecutorException extends Exception {

	private static final long serialVersionUID = -8545556134081763337L;

	private String output;

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param message the detail message
	 */
	public ExecutorException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified detail message and console output.
	 *
	 * @param message the detail message
	 * @param output  the console output
	 */
	public ExecutorException(String message, String output) {
		this(message);

		this.output = output;
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause of the exception
	 */
	public ExecutorException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception with the specified detail message, console output and cause.
	 *
	 * @param message the detail message
	 * @param output  the console output
	 * @param cause   the cause of the exception
	 */
	public ExecutorException(String message, String output, Throwable cause) {
		this(message, cause);

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
}
