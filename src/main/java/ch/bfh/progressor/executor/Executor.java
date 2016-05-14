package ch.bfh.progressor.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import ch.bfh.progressor.executor.api.CodeExecutor;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.api.Result;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.impl.FunctionSignatureImpl;
import ch.bfh.progressor.executor.impl.ResultImpl;
import ch.bfh.progressor.executor.impl.TestCaseImpl;
import ch.bfh.progressor.executor.thrift.ExecutorService;

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

		final ExecutorPlatform platform = ExecutorPlatform.determine();

		if (platform == ExecutorPlatform.UNSUPPORTED)
			throw new UnsupportedOperationException(String.format("Operating system '%s' (%s, %s) is not supported.", ExecutorPlatform.OPERATING_SYSTEM_NAME, ExecutorPlatform.OPERATING_SYSTEM_VERSION, ExecutorPlatform.OPERATING_SYSTEM_ARCHITECTURE));

		int port = Executor.DEFAULT_SERVER_PORT;

		for (int i = 0; i < args.length; i++)
			switch (args[i]) {
				case "-p":
				case "-port":
					try {
						port = Integer.parseInt(args[++i]);

					} catch (NumberFormatException ex) {
						throw new IllegalArgumentException(String.format("Value '%s' for command-line argument '%s' is invalid. Use integer number.", args[i], args[i - 1]));
					}

					if (port < 0 || 65535 < port)
						throw new IllegalArgumentException(String.format("Value '%s' for command-line argument '%s' is invalid. Use unsigned 16-bit integer (0 to 65535).", args[i], args[i - 1]));

					Executor.LOGGER.fine(String.format("Using port %d.", port));
					break;

				//case "-d":
				//case "-docker":
				//	switch (args[++i]) {
				//		case "true":
				//		case "yes":
				//			if (!platform.hasDockerSupport())
				//				throw new IllegalArgumentException(String.format("Cannot use Docker on %s platform.", platform));
				//
				//			CodeExecutorBase.setShouldUseDocker(true);
				//			Executor.LOGGER.fine("Using Docker containers.");
				//			break;
				//
				//		case "false":
				//		case "no":
				//			CodeExecutorBase.setShouldUseDocker(false);
				//			Executor.LOGGER.fine("Not using Docker containers.");
				//			break;
				//
				//		default:
				//			throw new IllegalArgumentException(String.format("Value '%s' for command-line argument '%s' is invalid. Use true/false or yes/no.", args[i], args[i - 1]));
				//	}
				//	break;

				default:
					throw new IllegalArgumentException(String.format("Command-line argument '%s' is invalid.", args[i]));
			}

		try (TServerTransport transport = new TServerSocket(port)) {
			TProcessor processor = new ExecutorService.Processor<>(new Executor.RequestHandler());
			TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(transport).processor(processor));

			Executor.LOGGER.info(String.format("Accepting requests on port %d...", port));
			Thread thread = new Thread(server::serve);
			thread.setDaemon(true);
			thread.start();

			System.out.print("Press enter to stop server.");
			try (Scanner scanner = new Scanner(System.in, CodeExecutorBase.CHARSET.name())) {
				scanner.nextLine();
			}

			server.stop();
			thread.join(Executor.SERVER_STOP_TIMEOUT_MILLISECONDS);

			if (!thread.isAlive())
				Executor.LOGGER.info("Server stopped.");
			else
				Executor.LOGGER.log(Level.WARNING, "Could not stop server. Forcefully abort application!");

		} catch (TTransportException ex) {
			Executor.LOGGER.log(Level.SEVERE, "Could not successfully start server.", ex);

		} catch (InterruptedException ex) {
			Executor.LOGGER.log(Level.WARNING, "Could not wait for server to stop.", ex);
		}
	}

	private static class RequestHandler implements ExecutorService.Iface {

		private int logId;
		private final Map<String, CodeExecutor> codeExecutors = new HashMap<>();

		private synchronized int getLogId() {

			return this.logId++;
		}

		private CodeExecutor getCodeExecutor(String language) throws ExecutorException {

			if (!this.codeExecutors.containsKey(language)) {
				ServiceLoader<CodeExecutor> executors = ServiceLoader.load(CodeExecutor.class); //fetch the executor classes
				for (CodeExecutor executor : executors)
					if (language.equals(executor.getLanguage())) { //store the executor instance for the chosen language
						Executor.LOGGER.fine(String.format("Loaded code executor '%s'.", executor.getClass().getName()));
						this.codeExecutors.put(language, executor);
					}

				if (!this.codeExecutors.containsKey(language)) //if no executor found, throw exception
					throw new ExecutorException(String.format("Could not find an executor for language '%s'.", language));
			}

			return this.codeExecutors.get(language); //return the instance
		}

		@Override
		public List<String> getBlacklist(String language) throws TException {

			int logId = this.getLogId();
			Executor.LOGGER.info(String.format("%-6d: getBlacklist(language=%s)", logId, language));

			try {
				Collection<String> list = this.getCodeExecutor(language).getBlacklist(); //delegate call
				return list instanceof List ? (List<String>)list : new ArrayList<>(list);

			} catch (Exception ex) { //wrap exception
				String msg = String.format("Could not fetch the blacklist for language '%s'.", language);
				Executor.LOGGER.log(Level.WARNING, msg, ex);
				throw new TException(msg, ex);

			} finally {
				Executor.LOGGER.finer(String.format("%-6d: finished", logId));
			}
		}

		@Override
		public String getFragment(String language, List<ch.bfh.progressor.executor.thrift.FunctionSignature> functions) throws TException {

			int logId = this.getLogId();
			Executor.LOGGER.info(String.format("%-6d: getFragment(language=%s)", logId, language));

			try {
				return this.getCodeExecutor(language).getFragment(FunctionSignatureImpl.convertFromThrift(functions)); //delegate call

			} catch (Exception ex) { //wrap exception
				String msg = String.format("Could not generate the fragment for language '%s'.", language);
				Executor.LOGGER.log(Level.WARNING, msg, ex);
				throw new TException(msg, ex);

			} finally {
				Executor.LOGGER.finer(String.format("%-6d: finished", logId));
			}
		}

		@Override
		public List<ch.bfh.progressor.executor.thrift.Result> execute(String language, String fragment, List<ch.bfh.progressor.executor.thrift.FunctionSignature> functions, List<ch.bfh.progressor.executor.thrift.TestCase> testCases) throws TException {

			int logId = this.getLogId();
			Executor.LOGGER.info(String.format("%-6d: execute(language=%s, fragment=..., %d testCases: %s...)", logId, language, testCases.size(), !testCases.isEmpty() ? testCases.get(0) : null));

			try {
				CodeExecutor codeExecutor = this.getCodeExecutor(language);
				List<Result> results;

				List<String> blacklist = codeExecutor.getBlacklist().stream().filter(fragment::contains).collect(Collectors.toList());
				if (!blacklist.isEmpty()) { //validate fragment against blacklist
					results = Collections.nCopies(testCases.size(),
																				new ResultImpl(false, true, String.format("Validation against blacklist failed (illegal: %s).", String.join(", ", blacklist)), null));

				} else {
					results = codeExecutor.execute(fragment, TestCaseImpl.convertFromThrift(functions, testCases)); //delegate execution call

					if (results.size() < testCases.size())
						results.addAll(Collections.nCopies(testCases.size() - results.size(),
																							 new ResultImpl(false, true, "Could not read execution result for test case.", null)));
				}

				return ResultImpl.convertToThrift(results);

			} catch (Exception ex) { //wrap exception
				String msg = String.format("Could not execute the code fragment in language '%s'.", language);
				Executor.LOGGER.log(Level.WARNING, msg, ex);
				throw new TException(msg, ex);

			} finally {
				Executor.LOGGER.finer(String.format("%-6d: finished", logId));
			}
		}
	}
}
