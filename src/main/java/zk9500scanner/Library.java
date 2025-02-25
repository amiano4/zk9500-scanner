package zk9500scanner;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import com.zkteco.biometric.FingerprintSensorEx;

/**
 * The `Library` class provides functionality for managing fingerprint templates, including registration, identification, and database management. It interfaces with the ZKTeco
 * biometric library to perform fingerprint operations.
 */
public class Library {
	/** The size of a fingerprint template in bytes. */
	public static final int TEMPLATE_SIZE = 2048;

	/** The number of templates required for registration. */
	public static final int REG_TEMPLATE_COUNT = 3;

	/** Error code indicating that the maximum number of registration attempts has been exceeded. */
	public static final int ERR_EXCEED = -1;

	/** Error code indicating that the fingerprint is already registered. */
	public static final int ERR_DUPLICATE = -2;

	/** Error code indicating that merging templates failed. */
	public static final int ERR_MERGE = -3;

	/** Error code indicating that the fingerprint does not match. */
	public static final int ERR_NOT_MATCH = -4;

	private long dbHandle = 0; // Handle to the fingerprint database
	private byte[][] registrationTemplates = new byte[REG_TEMPLATE_COUNT][TEMPLATE_SIZE]; // Templates for registration
	private boolean isRegisterMode = false; // Indicates if registration mode is active
	private int registerIndex = -1; // Current index during registration
	private int registrationID = 0; // ID for the fingerprint being registered
	private byte[] lastFaultyTemplate = new byte[TEMPLATE_SIZE]; // Stores the last faulty template

	/**
	 * Constructs a new `Library` instance and initializes the fingerprint database. If initialization fails, an error is logged.
	 */
	public Library() {
		if ((dbHandle = FingerprintSensorEx.DBInit()) == 0) {
			Main.log.severe("Failed to initialize algorithm library");
		}
	}

	/**
	 * Closes the fingerprint database and releases resources.
	 */
	public void close() {
		if (dbHandle != 0) {
			FingerprintSensorEx.DBFree(dbHandle);
			dbHandle = 0;
		}
	}

	/**
	 * Returns the handle to the fingerprint database.
	 *
	 * @return The database handle.
	 */
	public long getDBHandle() {
		return dbHandle;
	}

	/**
	 * Checks if the library is in registration mode.
	 *
	 * @return `true` if in registration mode, `false` otherwise.
	 */
	public boolean isRegistrationMode() {
		return isRegisterMode;
	}

	/**
	 * Returns the current registration index.
	 *
	 * @return The current index during registration.
	 */
	public int getRegisterIndex() {
		return registerIndex;
	}

	/**
	 * Starts the registration process for a fingerprint with the specified ID. If `id` is 0, registration mode is disabled.
	 *
	 * @param id The ID for the fingerprint being registered.
	 */
	public void startRegistration(int id) {
		registrationID = id;
		isRegisterMode = id != 0;
		lastFaultyTemplate = new byte[TEMPLATE_SIZE];
		resetRegistration();
	}

	/**
	 * Verifies a fingerprint template during the registration process.
	 *
	 * @param template The fingerprint template to register.
	 * @return `0` if the template is successfully registered, an error code otherwise.
	 * @throws Exception If registration mode is disabled or an error occurs during registration.
	 */
	public Object verifyTemplate(byte[] template) throws Exception {
		if (!isRegisterMode) {
			throw new Exception("Registration mode is disabled");
		}

		registerIndex++;

		if (registerIndex >= REG_TEMPLATE_COUNT) {
			return ERR_EXCEED;
		}

		boolean isFirstScan = registerIndex == 0;
		boolean hasFault = !Arrays.equals(lastFaultyTemplate, new byte[TEMPLATE_SIZE]);

		if (!isFirstScan || hasFault) {
			byte[] previousTemplate = hasFault ? lastFaultyTemplate : registrationTemplates[registerIndex - 1];

			int matchValue = FingerprintSensorEx.DBMatch(dbHandle, previousTemplate, template);

			if (matchValue <= 0) {
				resetRegistration();
				System.arraycopy(template, 0, lastFaultyTemplate, 0, TEMPLATE_SIZE);
				return ERR_NOT_MATCH;
			}

			if (hasFault) {
				lastFaultyTemplate = new byte[TEMPLATE_SIZE];
			}
		}

		if (isFirstScan) {
			try {
				if (identify(template) instanceof JSONObject) {
					resetRegistration();
					return ERR_DUPLICATE;
				}
			} catch (Exception e) {
				Main.log.warning("Error during fingerprint identification: " + e.getMessage());
			}
		}

		System.arraycopy(template, 0, registrationTemplates[registerIndex], 0, TEMPLATE_SIZE);
		return registerIndex;
	}

	/**
	 * Saves the registered fingerprint template to the database.
	 *
	 * @throws Exception If the template is empty or saving fails.
	 */
	public byte[] registerTemplates() throws Exception {
		if (registerIndex != REG_TEMPLATE_COUNT - 1) {
			throw new Exception("Insufficient registration requirements");
		}

		byte[] finalTemplate = new byte[TEMPLATE_SIZE];

		int[] retLen = new int[] { TEMPLATE_SIZE };
		int mergeValue = FingerprintSensorEx.DBMerge(dbHandle, registrationTemplates[0], registrationTemplates[1],
				registrationTemplates[2], finalTemplate, retLen);

		if (mergeValue != 0) {
			throw new Exception("Unable to merge fingerprint templates");
		}

		int ret = FingerprintSensorEx.DBAdd(dbHandle, registrationID, finalTemplate);

		if (ret != 0) {
			throw new Exception("Failed to register new fingerprint");
		}

		byte[] temp = new byte[finalTemplate.length];

		System.arraycopy(finalTemplate, 0, temp, 0, TEMPLATE_SIZE);
		return temp;
	}

	/**
	 * Resets the registration process, clearing all stored templates and resetting the index.
	 */
	public void resetRegistration() {
		registrationTemplates = new byte[REG_TEMPLATE_COUNT][TEMPLATE_SIZE];
		registerIndex = -1;
	}

	/**
	 * Returns the ID of the fingerprint currently being registered.
	 *
	 * @return The registration ID.
	 */
	public int getRegistrationID() {
		return registrationID;
	}

	/**
	 * Initializes the fingerprint database with the provided fingerprint data.
	 *
	 * @param fingerprintArr A JSON array containing fingerprint data in base64 format.
	 * @throws Exception If initialization fails or an error occurs while adding fingerprints.
	 */
	public void initialize(JSONArray data) throws Exception {
		Main.log.info("Initializing database...");
		close();

		if ((dbHandle = FingerprintSensorEx.DBInit()) == 0) {
			throw new Exception("Failed to initialize algorithm library");
		}

		for (int i = 0; i < data.length(); i++) {
			JSONObject fp = data.getJSONObject(i);
			int id = fp.getInt("id");
			String base64 = fp.getString("data");

			try {
				byte[] blob = new byte[TEMPLATE_SIZE];

				if (FingerprintSensorEx.Base64ToBlob(base64, blob, TEMPLATE_SIZE) == 0) {
					throw new Exception("Unable to extract from ID " + id);
				}

				if (FingerprintSensorEx.DBAdd(dbHandle, id, blob) != 0) {
					throw new Exception("Failed to register ID " + id + " to the algorithm database");
				}
			} catch (Exception e) {
				Main.log.severe("Error initializing fingerprint ID " + id + ": " + e.getMessage());
			}
		}

		Main.log.info("Fingerprint database has been successfully updated.");
	}

	/**
	 * Identifies a fingerprint template by comparing it against the database.
	 *
	 * @param template The fingerprint template to identify.
	 * @return A JSON object containing the ID and match score of the identified fingerprint.
	 * @throws Exception If identification fails.
	 */
	public JSONObject identify(byte[] template) throws Exception {
		int[] id = new int[1];
		int[] score = new int[1];

		if (FingerprintSensorEx.DBIdentify(dbHandle, template, id, score) != 0) {
			throw new Exception("Failed to identify the fingerprint data");
		}

		JSONObject data = new JSONObject();
		data.put("id", id[0]);
		data.put("score", score[0]);

		return data;
	}

	/**
	 * Converts a fingerprint template to a base64-encoded string.
	 *
	 * @param template The fingerprint template to convert.
	 * @return The base64-encoded string representation of the template.
	 */
	public static String toBase64(byte[] template) {
		return FingerprintSensorEx.BlobToBase64(template, TEMPLATE_SIZE);
	}
}