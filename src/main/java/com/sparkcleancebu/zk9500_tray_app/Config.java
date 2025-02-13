package com.sparkcleancebu.zk9500_tray_app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class Config {
	private Properties properties = new Properties();
	private String filePath;

	public Config(String fileName) throws AppException.ConfigException {
		try {
			init(fileName);
		} catch (Exception e) {
			throw new AppException.ConfigException("Config init failed", e);
		}
	}

	private void init(String fileName) throws Exception {
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

		File file = new File(filePath);

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

	private String get(String key) {
		String prop = properties.getProperty(key, null);
		return prop == null || prop.trim().isEmpty() ? null : prop;
	}

	private void set(String key, String value) {
		properties.setProperty(key, value == null ? "" : value);
	}

	private String generateLocalID() throws AppException.ConfigException {
		try {
			String id = uuid();
			set(App.CONF_LOCALID, id);
			save();
			return id;
		} catch (Exception e) {
			throw new AppException.ConfigException("Error while generating local ID", e);
		}
	}

	public void save() throws AppException.ConfigException {
		try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
			properties.store(fos, "Updated Config File");
		} catch (Exception e) {
			throw new AppException.ConfigException("Error in saving configurations", e);
		}
	}

	public void reset() throws AppException.ConfigException {
		properties.clear();
		save();
	}

	public static String uuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * Setters and Getters
	 */

	public String getAppID() {
		return this.get(App.CONF_APPID);
	}

	public void setAppID(String id) {
		this.set(App.CONF_APPID, id);
	}

	public String getLocalID() throws AppException.ConfigException {
		String id = get(App.CONF_LOCALID);
		return id == null ? generateLocalID() : id;
	}

	public String getBranch() {
		return get(App.CONF_BRANCH);
	}

	public void setBranch(String branch) {
		set(App.CONF_BRANCH, branch);
	}

	public String getHost() {
		return get(App.CONF_HOST);
	}

	public void setHost(String host) {
		set(App.CONF_HOST, host);
	}

	public String getSocketUrl() {
		return get(App.CONF_SOCKET);
	}

	public void setSocketUrl(String url) {
		set(App.CONF_SOCKET, url);
	}

	public void setVerifiedAt(String value) {
		set(App.CONF_VERIFIED_AT, value);
	}

	public String getVerifiedAt() {
		return get(App.CONF_VERIFIED_AT);
	}

	public void setName(String name) {
		set(App.CONF_NAME, name);
	}

	public String getName() {
		return get(App.CONF_NAME);
	}
}
