package ch.bfh.progressor.executor;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import ch.bfh.progressor.executor.languages.JavaProcessExecutor;

/**
 * Main class.
 * Starts the executor service.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class Executor {

	public static final int DEFAULT_SERVER_PORT = 9090;

	private TServer server;

	public static void main(String... args) {

		try {
			new Executor().startService(Executor.DEFAULT_SERVER_PORT);
			System.out.println("Server started. Accepting requests:");

		} catch (TException ex) {
			System.err.println("Could not successfully start server.");
			ex.printStackTrace(System.err);
		}
	}

	public void startService(int port) throws TException {

		TServerTransport transport = new TServerSocket(port);
		TProcessor processor = new ExecutorService.Processor<>(this::execute);
		this.server = new TThreadPoolServer(new TThreadPoolServer.Args(transport).processor(processor));

		this.server.serve();
	}

	private List<Result> execute(String language, String fragment, List<TestCase> testCases) throws TException {

		System.out.printf("%s[language=%s, %d testCases]: %s...%n", LocalDateTime.now(),
											language, testCases.size(), testCases.size() > 0 ? testCases.get(0) : null);

		if (!"java".equals(language)) throw new TException("Unhandled language.");

		return new JavaProcessExecutor().execute(fragment, testCases);
	}
}
