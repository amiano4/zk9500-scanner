package com.sparkcleancebu.zk9500_tray_app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
	private Properties properties = new Properties();
	private String filePath;

	public Config(String fileName) throws Exception {

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

	public String get(String key) throws Exception {
		String prop = properties.getProperty(key, null);

		if (prop == null || prop.trim().isEmpty()) {
			return null;
		}

		return prop;
	}

	public void set(String key, String value) throws Exception {
		try {
			this.properties.setProperty(key, value);
		} catch (Exception e) {
			throw new Exception("Unable to set property because " + key + " is " + value);
		}
	}

	public void save() throws IOException {
		try (FileOutputStream fos = new FileOutputStream(new File(this.filePath))) {
			properties.store(fos, "Updated Config File");
		}
	}

	public void clear() throws IOException {
		this.properties.clear();
		this.save();
	}
}
