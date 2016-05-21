package ch.bfh.progressor.executor;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import ch.bfh.progressor.executor.api.Configuration;
import ch.bfh.progressor.executor.api.ExecutorPlatform;
import ch.bfh.progressor.executor.impl.CodeExecutorBase;
import ch.bfh.progressor.executor.impl.ConfigurationImpl;

/**
 * Main class.
 * Starts the {@link ExecutorService}.
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
		boolean useDocker = Configuration.DEFAULT_CONFIGURATION.shouldUseDocker();

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
					break;

				case "-d":
				case "-docker":
					switch (args[++i]) {
						case "true":
						case "yes":
							if (!platform.hasDockerSupport())
								throw new IllegalArgumentException(String.format("Cannot use Docker on %s platform.", platform));

							useDocker = true;
							break;

						case "false":
						case "no":
							useDocker = false;
							break;

						default:
							throw new IllegalArgumentException(String.format("Value '%s' for command-line argument '%s' is invalid. Use true/false or yes/no.", args[i], args[i - 1]));
					}
					break;

				default:
					throw new IllegalArgumentException(String.format("Command-line argument '%s' is invalid.", args[i]));
			}

		Executor.LOGGER.fine(String.format("Using port %d.", port));
		Executor.LOGGER.fine(useDocker ? "Using Docker containers." : "Not using Docker containers.");

		Configuration configuration = new ConfigurationImpl(useDocker);

		try (TServerTransport transport = new TServerSocket(port)) {
			TProcessor processor = new ch.bfh.progressor.executor.thrift.ExecutorService.Processor<>(new ExecutorService(configuration));
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
				Executor.LOGGER.log(Level.SEVERE, "Could not stop server. Forcefully abort application!");

		} catch (TTransportException ex) {
			Executor.LOGGER.log(Level.SEVERE, "Could not successfully start server.", ex);

		} catch (InterruptedException ex) {
			Executor.LOGGER.log(Level.WARNING, "Could not wait for server to stop.", ex);
		}
	}
}
