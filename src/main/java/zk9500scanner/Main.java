package zk9500scanner;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

/**
 * Main class for the fingerprint scanner application. Handles WebSocket communication, fingerprint scanning, and logging.
 */
public class Main {
	public static final int TEMPLATE_SIZE = 1024;
	public static final Logger log = Logger.getLogger("");
	public static final AtomicBoolean runner = new AtomicBoolean(false);
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");

	private final SocketServer socket;
	private final Scanner scanner;
	private final Library library;

	/**
	 * Initializes the scanner application components. Sets up WebSocket event handlers and scanner events.
	 */
	public Main() {
		int port = Integer.parseInt(System.getProperty("wsport", "1234")); // Default port if not set

		socket = new SocketServer(new InetSocketAddress("localhost", port));
		scanner = new Scanner();
		library = new Library();

		setupSocketEvents();
		setupScannerEvents();
	}

	/**
	 * Configures WebSocket event handlers for scanner interactions.
	 */
	private void setupSocketEvents() {
		socket.onRegistrationStart = (Integer id) -> {
			if (scanner.isOpen()) {
				log.info("Initiating fingerprint registration process!");
				library.startRegistration(id);

				// Notify client that registration has started
				JSONObject json = new JSONObject();
				json.put("event", "registration-start");
				socket.send(json);
			}
		};

		socket.onInitialize = data -> {
			try {
				library.initialize(data);
				socket.send(new JSONObject().put("event", "library-init-ok"));
			} catch (Exception e) {
				log.severe("Library initialization error: " + e.getMessage());
			}
		};
	}

	/**
	 * Sets up the fingerprint scanner event listener.
	 */
	private void setupScannerEvents() {
		scanner.onScanEvent = fingerprint -> {
			if (library.isRegistrationMode()) {
				try {
					Object result = library.verifyTemplate(fingerprint);
					handleRegistrationResult(result);
				} catch (Exception e) {
					log.severe("Fingerprint registration error: " + e.getLocalizedMessage());
				}
			} else {
				// normal fingerprint detection
				try {
					JSONObject data = library.identify(fingerprint);
					data.put("event", "biometric");
					data.put("timestamp", getCurrentTimestamp());

					int id = data.getInt("id");
					int score = data.getInt("score");

					log.info("Biometric detected [ID: " + id + ", SCORE: " + score + "]");
					socket.send(data);
				} catch (Exception e) {
					JSONObject json = new JSONObject();
					json.put("event", "biometric-error");
					json.put("message", e.getMessage());

					log.warning("Biometric error: " + e.getMessage());
					// send error to client
					socket.send(json);
				}
			}
		};
	}

	private String getCurrentTimestamp() {
		Instant now = Instant.now();
		// Convert Instant to LocalDateTime in UTC (or use system default zone)
		LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneId.of("Asia/Manila"));
		// Format it to a MySQL-compatible string
		return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	/**
	 * Processes the result of a fingerprint registration attempt.
	 *
	 * @param result The result of the registration attempt, either an error code or a fingerprint template.
	 * @throws Exception If an unexpected result type is encountered.
	 */
	private void handleRegistrationResult(Object result) throws Exception {
		if (result instanceof Integer) {
			int code = (int) result;

			if (code >= 0 && code < Library.REG_TEMPLATE_COUNT - 1) { // registration in progress
				JSONObject json = new JSONObject();
				json.put("event", "registration-ongoing");
				json.put("step", code + 1);

				// send status
				socket.send(json);
			} else if (code == Library.REG_TEMPLATE_COUNT - 1) {
				// completed the regisration requirements...
				byte[] template = library.registerTemplates();

				// successfully verified and saved
				if (template instanceof byte[]) {
					JSONObject json = new JSONObject();
					json.put("event", "registration-success");
					json.put("fingerprint", Library.toBase64(template));
					json.put("id", library.getRegistrationID());
					socket.send(json); // send fingerprint data

					// reset process
					library.startRegistration(0);
				}
			} else // handle error
				handleRegistrationError(code);
		} else {
			throw new Exception("Unexpected result type: " + result.getClass().getName());
		}
	}

	/**
	 * Logs registration errors based on the provided error code.
	 *
	 * @param errorCode The error code received during fingerprint registration.
	 */
	private void handleRegistrationError(int errorCode) {
		JSONObject json = new JSONObject();
		json.put("event", "registration-error");

		switch (errorCode) {
		case Library.ERR_EXCEED:
			log.warning("Fingerprint registration input exceeds the limit.");
			break;
		case Library.ERR_DUPLICATE:
			json.put("message", "Fingerprint is already been registered.");
			log.warning("Duplicate fingerprint detected.");
			break;
		case Library.ERR_NOT_MATCH:
			json.put("message", "Fingerprint does not match with the previous data.");
			log.warning("Fingerprint does not match expected data.");
			break;
		case Library.ERR_MERGE:
			log.warning("Fingerprint merge failed.");
			break;
		default:
			log.severe("Unknown fingerprint registration error: " + errorCode);
			break;
		}

		// notify client
		socket.send(json);
	}

	/**
	 * Main entry point for the application.
	 *
	 * @param args Command-line arguments for setting host and WebSocket port.
	 */
	public static void main(String[] args) {
		configureSystemProperties(args);
		addShutdownHook();

		try {
			setupLogging();
			Main main = new Main();
			runner.set(true);
			startBackgroundWorker(main);
		} catch (IOException e) {
			log.severe("Failed to initialize application: " + e.getMessage());
		}
	}

	/**
	 * Parses command-line arguments and sets system properties.
	 *
	 * @param args Command-line arguments.
	 */
	private static void configureSystemProperties(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("--wsport=")) {
				System.setProperty("wsport", arg.substring("--wsport=".length()));
			}
		}
	}

	/**
	 * Adds a shutdown hook to clean up resources before exiting.
	 */
	private static void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Shutting down... Cleaning up resources!");

			// Flush and close all handlers
			for (Handler handler : log.getHandlers()) {
				handler.flush();
				handler.close();
			}
		}));
	}

	/**
	 * Configures logging and creates a logs directory if needed.
	 *
	 * @throws IOException If an error occurs while setting up logging.
	 */
	private static void setupLogging() throws IOException {
		File logDir = new File("logs");
		if (!logDir.exists() && !logDir.mkdirs()) {
			throw new IOException("Unable to create logs directory");
		}

		String timestamp = dateFormat.format(new Date());
		log.addHandler(new FileLogger("logs/zk9500_" + timestamp + ".log"));
		log.setLevel(Level.ALL);
	}

	/**
	 * Starts a background worker to monitor scanner hardware and socket server.
	 *
	 * @param main The main application instance.
	 */
	private static void startBackgroundWorker(Main main) {
		Thread worker = new Thread(() -> {
			while (runner.get()) {
				try {
					if (!main.socket.isRunning()) {
						main.socket.start();
					}

					if (!main.scanner.isOpen()) {
						main.scanner.init();
						main.scanner.startScanning(); // Start scanning after initialization
						return;
					}

					Thread.sleep(2000);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Worker thread error", e);
				}
			}
			log.info("Scanner detection stopped.");
		});
		worker.start();
	}
}
