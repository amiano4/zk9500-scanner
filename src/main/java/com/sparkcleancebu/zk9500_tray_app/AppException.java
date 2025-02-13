package com.sparkcleancebu.zk9500_tray_app;

@SuppressWarnings("serial")
public class AppException {
	public static class InvalidHostException extends IllegalStateException {
		public InvalidHostException(String message) {
			super(message);
		}
	}

	// app auth
	public static class AuthenticationException extends IllegalStateException {
		public AuthenticationException(String message, Throwable cause) {
			super(message, cause);
		}

		public AuthenticationException(String message) {
			super(message);
		}
	}

	// scanner related errors
	public static class ScannerInitializationException extends IllegalStateException {
		public ScannerInitializationException(String message) {
			super(message);
		}
	}

	// config class
	public static class ConfigException extends Exception {
		public ConfigException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
