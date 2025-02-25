package zk9500scanner;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class FileLogger extends Handler {
	private final FileHandler fileHandler;

	public FileLogger(String logFilePath) throws IOException {
		// Initialize FileHandler with append mode
		fileHandler = new FileHandler(logFilePath, true);

		// Set custom formatter for FileHandler
		fileHandler.setFormatter(new CustomFormatter());

		// Set the same custom formatter for this handler
		setFormatter(new CustomFormatter());
	}

	@Override
	public void publish(LogRecord record) {
		if (isLoggable(record)) {
			fileHandler.publish(record); // Directly publish the original record
		}
	}

	@Override
	public void flush() {
		fileHandler.flush();
	}

	@Override
	public void close() throws SecurityException {
		fileHandler.close();
	}

	/**
	 * Custom log formatter to define log message structure.
	 */
	private static class CustomFormatter extends Formatter {
		private final SimpleFormatter defaultFormatter = new SimpleFormatter();

		@Override
		public String format(LogRecord record) {
			// Get the default formatted log message
			String defaultFormatted = defaultFormatter.format(record);
			// Append an extra line
			return defaultFormatted + System.lineSeparator();
		}
	}
}