package ch.bfh.progressor.executor;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import ch.bfh.progressor.executor.thrift.ExecutorService;
import ch.bfh.progressor.executor.thrift.FunctionSignature;
import ch.bfh.progressor.executor.thrift.Result;
import ch.bfh.progressor.executor.thrift.TestCase;

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
	 * The default network port the server should listen on.
	 */
	public static final int DEFAULT_SERVER_PORT = 9090;

	/**
	 * The time (in milliseconds) to wait after telling server to stop.
	 */
	public static final int SERVER_STOP_TIMEOUT_MILLISECONDS = 250;

	/**
	 * Main method.
	 * Starts the executor service.
	 *
	 * @param args command-line arguments (none used)
	 */
	public static void main(String... args) {

		int port = Executor.DEFAULT_SERVER_PORT;

		for (int i = 0; i < args.length; i++)
			switch (args[i]) {
				case "-p":
				case "-port":
					port = Integer.parseInt(args[++i]);
					break;

				default:
					throw new InvalidParameterException(String.format("Command-line argument '%s' is invalid.", args[i]));
			}

		try (TServerTransport transport = new TServerSocket(port)) {
			TProcessor processor = new ExecutorService.Processor<>(new Executor.RequestHandler());
			TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(transport).processor(processor));

			Executor.LOGGER.info(String.format("Accepting requests on port %d...", port));
			Thread thread = new Thread(server::serve);
			thread.setDaemon(true);
			thread.start();

			System.out.print("Press enter to stop server.");
			try (Scanner scanner = new Scanner(System.in)) {
				scanner.nextLine();
			}

			server.stop();
			thread.join(Executor.SERVER_STOP_TIMEOUT_MILLISECONDS);

			if (!thread.isAlive())
				Executor.LOGGER.info("Server stopped.");
			else
				Executor.LOGGER.log(Level.SEVERE, "Could not stop server. Forcefully abort application!");

		} catch (TTransportException ex) {
			Executor.LOGGER.log(Level.SEVERE, "Could not successfully start server.", ex);

		} catch (InterruptedException ex) {
			Executor.LOGGER.log(Level.SEVERE, "Could not wait for server to stop.", ex);
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
		public String getFragment(String language, List<FunctionSignature> functions) throws TException {

			Executor.LOGGER.info(String.format("getFragment(language=%s)", language));

			try {
				return this.getCodeExecutor(language).getFragment(functions); //delegate call

			} catch (Exception ex) { //wrap exception
				String msg = String.format("Could not generate the fragment for language '%s'.", language);
				Executor.LOGGER.log(Level.WARNING, msg, ex);
				throw new TException(msg, ex);
			}
		}

		@Override
		public List<Result> execute(String language, String fragment, List<FunctionSignature> functions, List<TestCase> testCases) throws TException {

			Executor.LOGGER.info(String.format("execute(language=%s, fragment=..., %d testCases: %s...)", language, testCases.size(), !testCases.isEmpty() ? testCases.get(0) : null));

			try {
				CodeExecutor codeExecutor = this.getCodeExecutor(language);

				List<String> blacklist = codeExecutor.getBlacklist().stream().filter(fragment::contains).collect(Collectors.toList());
				if (!blacklist.isEmpty()) { //validate fragment against blacklist
					Result result = new Result(false, true, String.format("Validation against blacklist failed (illegal: %s).", String.join(", ", blacklist)), null);
					List<Result> results = new ArrayList<>(testCases.size());
					while (results.size() < testCases.size())
						results.add(result);
					return results;
				}

				return codeExecutor.execute(fragment, functions, testCases); //delegate execution call

			} catch (Exception ex) { //wrap exception
				String msg = String.format("Could not execute the code fragment in language '%s'.", language);
				Executor.LOGGER.log(Level.WARNING, msg, ex);
				throw new TException(msg, ex);
			}
		}
	}
}
