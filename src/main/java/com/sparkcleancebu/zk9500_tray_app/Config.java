package com.sparkcleancebu.zk9500_tray_app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Config {
	private Properties properties = new Properties();
	private Encryption encryption;
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

		// initialize encryption service
		this.encryption = new Encryption();

		// retrieve the encryption key
		String key = this.properties.getProperty(Encryption.ENCRYPTION_KEY_NAME, null);

		if (key == null) {
			// create fresh key
			this.encryption.createKey();

			// save it
			this.properties.setProperty(Encryption.ENCRYPTION_KEY_NAME, this.encryption.getEncodedKey());
			this.save();
		} else {
			this.encryption.setKey(key);
		}
	}

	public String get(String key) throws Exception {
		String prop = properties.getProperty(key, null);

		if (prop == null || prop.trim().isEmpty()) {
			return null;
		}

		return this.encryption.decrypt(prop);
	}

	public void set(String key, String value) throws Exception {
		try {
			this.properties.setProperty(key, this.encryption.encrypt(value));
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

	public static class Encryption {
		public static final String ENCRYPTION_KEY_NAME = "CIPHER";
		public static final String ENCRYPTION_ALGORITHM = "AES";
		public SecretKey secretKey = null;

		public void createKey() throws Exception {
			KeyGenerator keyGen = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
			keyGen.init(128);
			this.secretKey = keyGen.generateKey();
		}

		public String getEncodedKey() throws Exception {
			if (this.secretKey == null) {
				throw new Exception("Key is null");
			}
			return Base64.getEncoder().encodeToString(this.secretKey.getEncoded());
		}

		public void setKey(String encodedKey) throws Exception {
			byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
			this.secretKey = new SecretKeySpec(decodedKey, ENCRYPTION_ALGORITHM);
		}

		public String encrypt(String data) throws Exception {
			Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, this.secretKey);
			byte[] encryptedBytes = cipher.doFinal(data.getBytes());
			return Base64.getEncoder().encodeToString(encryptedBytes);
		}

		public String decrypt(String encryptedData) throws Exception {
			Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, this.secretKey);
			byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
			byte[] decryptedBytes = cipher.doFinal(decodedBytes);
			return new String(decryptedBytes);
		}
	}
}
