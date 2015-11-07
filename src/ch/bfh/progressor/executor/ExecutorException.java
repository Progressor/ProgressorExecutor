package ch.bfh.progressor.executor;

public class ExecutorException extends Exception {

	private static final long serialVersionUID = -8545556134081763337L;

	public ExecutorException(String message) {
		super(message);
	}

	public ExecutorException(String message, Throwable cause) {
		super(message, cause);
	}
}
