package com.sparkcleancebu.biometrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class ConfigReader {
	private Properties properties = new Properties();
	private String filePath;

	public ConfigReader(String fileName) throws IOException { // Use fileName, not filePath
		String appDir = System.getProperty("APPDIR");

		if (appDir == null || appDir.isEmpty()) { // Check for null or empty
			// Handle the case where APPDIR is not set (e.g., use current directory)
			appDir = "."; // Or "./config" if you have a dedicated config folder
			System.out.println("APPDIR not set, using default: " + appDir);
		}

		File dir = new File(appDir); // Create a File object for the directory
		if (!dir.exists()) {
			if (!dir.mkdirs()) { // Use mkdirs() to create parent directories
				throw new IOException("Failed to create application directory: " + appDir);
			}
		}

		this.filePath = new File(dir, fileName).getAbsolutePath(); // Use File constructor for path

		File file = new File(this.filePath);

		if (!file.exists()) {
			System.out.println("Config file not found. Creating default config...");
			Properties defaultProps = new Properties();

			try (FileOutputStream fos = new FileOutputStream(file)) {
				defaultProps.store(fos, "Default Config File");
			}

		}

		try (FileInputStream fis = new FileInputStream(file)) {
			properties.load(fis);
		}
	}

	public String get(String key) {
		return properties.getProperty(key, "KEY_NOT_FOUND");
	}

	public Set<String> getAllKeys() {
		return properties.stringPropertyNames();
	}

	public void set(String key, String value) {
		this.properties.setProperty(key, value);
	}

	public void save() throws IOException { // Add a save method
		try (FileOutputStream fos = new FileOutputStream(new File(this.filePath))) {
			properties.store(fos, "Updated Config File");
		}
	}
}