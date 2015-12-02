package ch.bfh.progressor.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Main class.
 * Starts the executor service.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public final class Executor {

	private static final Logger LOGGER = Logger.getLogger(Executor.class.getName());

	private Executor() {
		//static class
	}

	/**
	 * The network port the server should listen on.
	 */
	public static final int SERVER_PORT = 9090;

	/**
	 * Main method.
	 * Starts the executor service.
	 *
	 * @param args command-line arguments (none used)
	 */
	public static void main(String... args) {

		try {
			try (TServerTransport transport = new TServerSocket(Executor.SERVER_PORT)) {
				TProcessor processor = new ExecutorService.Processor<>(new Executor.RequestHandler());
				TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(transport).processor(processor));

				Executor.LOGGER.info("Accepting requests...");
				server.serve();
				Executor.LOGGER.info("Server shut down.");
			}

		} catch (TTransportException ex) {
			Executor.LOGGER.log(Level.SEVERE, "Could not successfully start server.", ex);
		}
	}

	private static class RequestHandler implements ExecutorService.Iface {

		private final Map<String, CodeExecutor> codeExecutors = new HashMap<>();

		private CodeExecutor getCodeExecutor(String language) throws TException {

			if (!this.codeExecutors.containsKey(language)) {
				ServiceLoader<CodeExecutor> executors = ServiceLoader.load(CodeExecutor.class); //fetch the executor classes
				for (CodeExecutor executor : executors)
					if (language.equals(executor.getLanguage())) { //store the executor instance for the chosen language
						Executor.LOGGER.info(String.format("Loaded code executor '%s'.", executor.getClass().getName()));
						this.codeExecutors.put(language, executor);
					}

				if (!this.codeExecutors.containsKey(language)) //if no executor found, throw exception
					throw new TException(String.format("Could not find an executor for language '%s'.", language));
			}

			return this.codeExecutors.get(language); //return the instance
		}

		@Override
		public List<String> getBlacklist(String language) throws TException {

			Executor.LOGGER.info(String.format("getBlacklist(language=%s)", language));

			try {
				Collection<String> list = this.getCodeExecutor(language).getBlacklist(); //delegate call
				return list instanceof List ? (List<String>)list : new ArrayList<>(list);

			} catch (Exception ex) { //wrap exception
				String msg = String.format("Could not fetch the blacklist for language '%s'.", language);
				Executor.LOGGER.log(Level.WARNING, msg, ex);
				throw new TException(msg, ex);
			}
		}

		@Override
		public List<Result> execute(String language, String fragment, List<TestCase> testCases) throws TException {

			Executor.LOGGER.info(String.format("execute(language=%s, ..., %d testCases=%s...)", language, testCases.size(), !testCases.isEmpty() ? testCases.get(0) : null));

			try {
				return this.getCodeExecutor(language).execute(fragment, testCases); //delegate call

			} catch (Exception ex) { //wrap exception
				String msg = String.format("Could not execute the code fragment in language '%s'.", language);
				Executor.LOGGER.log(Level.WARNING, msg, ex);
				throw new TException(msg, ex);
			}
		}
	}
}
