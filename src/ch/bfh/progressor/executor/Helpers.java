package ch.bfh.progressor.executor;

/**
 * Utility class providing global helper methods.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class Helpers {

	private static final String END_OF_LINE = String.format("%n");

	private Helpers() { }

	/**
	 * Get a human-readable exception message.
	 *
	 * @param message top-level message
	 * @param ex      thrown exception to get message from
	 *
	 * @return human-readable exception message
	 */
	public static String getExceptionMessage(String message, Throwable ex) {

		StringBuilder sb = new StringBuilder(message);
		do sb.append(Helpers.END_OF_LINE).append('>').append(ex);
		while ((ex = ex.getCause()) != null);

		return sb.toString();
	}
}
