package com.sparkcleancebu.zk9500_tray_app;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import com.amiano4.httpflux.HttpService;
import com.zkteco.biometric.FingerprintSensorEx;

@SuppressWarnings("exports")
public class FingerprintLibrary {
	public static final int TEMPLATE_SIZE = 2048;
	public static final int REG_TEMPLATE_COUNT = 3;

	private long dbHandle = 0;
	private byte[][] registrationTemplate = new byte[REG_TEMPLATE_COUNT][TEMPLATE_SIZE];
	private boolean registerMode = false;
	private int registerIndex = -1; // reset
	private int registrationID = 0;
	private byte[] registeredTemplate = new byte[TEMPLATE_SIZE];
	private byte[] lastFaultyTemplate = new byte[TEMPLATE_SIZE];

	public FingerprintLibrary() throws AppException.FingerprintException {
		if (0 == (dbHandle = FingerprintSensorEx.DBInit())) {
			throw new AppException.FingerprintException("Failed to initialize algorithm library");
		}
	}

	public void close() {
		if (0 != dbHandle) {
			FingerprintSensorEx.DBFree(dbHandle);
			dbHandle = 0;
		}
	}

	public long getDBHandle() {
		return dbHandle;
	}

	public boolean registrationMode() {
		return registerMode;
	}

	public int getRegisterIndex() {
		return registerIndex;
	}

	public void startRegistration(int id) { // 0 for disable reg mode
		if (id != 0) {
			try {
				HttpService.get(App.URI_CONFIRM_CONNECTION).onError(err -> {
					err.printStackTrace();
				}).executeAsync();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		registrationID = id;
		registerMode = id != 0;
		registeredTemplate = new byte[TEMPLATE_SIZE];
		lastFaultyTemplate = new byte[TEMPLATE_SIZE];
		resetRegistration();
	}

	public byte[] registerTemplate(byte[] template) throws AppException.FingerprintRegistrationException {
		if (!registerMode) {
			throw new AppException.FingerprintRegistrationException("Registration mode is disabled");
		}

		registerIndex++;

		if (registerIndex >= REG_TEMPLATE_COUNT) {
			throw new AppException.FingerprintRegistrationException("Fingerprint registration input exceeded");
		}

		boolean noFault = Arrays.equals(lastFaultyTemplate, new byte[TEMPLATE_SIZE]);

		if (registerIndex > 0 || !noFault) {
			byte[] template0 = noFault ? registrationTemplate[registerIndex - 1] : lastFaultyTemplate;

			// compare current template from previous
			int matchValue = FingerprintSensorEx.DBMatch(dbHandle, template0, template);

			// no match
			if (matchValue <= 0) {
				resetRegistration(); // reset registration

				// save as faulty template
				System.arraycopy(template, 0, lastFaultyTemplate, 0, TEMPLATE_SIZE);
				throw new AppException.FingerprintRegistrationException("Incorrect finger");
			}

			// always reset the buffer
			if (!noFault) {
				lastFaultyTemplate = new byte[TEMPLATE_SIZE];
			}
		}

		// first scan (fingerprint 1)
		if (registerIndex == 0) {
			boolean exists = false;

			try {
				// check for existing fingerprint
				exists = identify(template) instanceof JSONObject;
			} catch (Exception e) {
				// continue
			}

			if (exists) {
				resetRegistration(); // reset registration
				throw new AppException.FingerprintRegistrationException("Already exists");
			}
		}

		// save to array
		System.arraycopy(template, 0, registrationTemplate[registerIndex], 0, TEMPLATE_SIZE);

		// last input
		if (registerIndex == REG_TEMPLATE_COUNT - 1 && registrationTemplate.length == REG_TEMPLATE_COUNT) {
			int[] _retLen = new int[1];
			_retLen[0] = TEMPLATE_SIZE;

			// merge templates
			int mergeValue = FingerprintSensorEx.DBMerge(dbHandle, registrationTemplate[0], registrationTemplate[1],
					registrationTemplate[2], registeredTemplate, _retLen);

			if (mergeValue != 0) {
				throw new AppException.FingerprintRegistrationException("Unable to complete the registration process");
			}

			// copy registered template
			int registeredTemplateLength = _retLen[0];
			byte[] registeredTemplateCopy = new byte[TEMPLATE_SIZE];
			System.arraycopy(registeredTemplate, 0, registeredTemplateCopy, 0, registeredTemplateLength);

			return registeredTemplateCopy;
		}

		return null;
	}

	public void saveRegisteredTemplate() throws AppException.FingerprintRegistrationException {
		if (Arrays.equals(registeredTemplate, new byte[TEMPLATE_SIZE])) {
			throw new AppException.FingerprintRegistrationException("Cannot save an empty template");
		}

		int ret = FingerprintSensorEx.DBAdd(dbHandle, registrationID, registeredTemplate);

		if (ret != 0) {
			throw new AppException.FingerprintRegistrationException("Failed to register new fingerprint");
		}
	}

	public void resetRegistration() {
		registrationTemplate = new byte[REG_TEMPLATE_COUNT][TEMPLATE_SIZE]; // reset registration templates
		registerIndex = -1;
	}

	public int getRegistrationID() {
		return registrationID;
	}

	public void initialize() throws Exception {
		System.out.println("Initializing database...");

		HttpService.get(App.URI_FINGERPRINTS).onSuccess(response -> {
			close();

			if (0 == (dbHandle = FingerprintSensorEx.DBInit())) {
				throw new AppException.FingerprintException("Failed to initialize algorithm library");
			}

			JSONArray data = new JSONArray(response.body());

			for (int i = 0; i < data.length(); i++) {
				JSONObject fp = data.getJSONObject(i);

				int id = fp.getInt("id");
				String base64 = fp.getString("data");

				// System.out.println("ID " + id);
				// System.out.println("Base64 " + base64);
				// System.out.println();

				try {
					byte[] blob = new byte[TEMPLATE_SIZE];

					if (0 == FingerprintSensorEx.Base64ToBlob(base64, blob, TEMPLATE_SIZE)) {
						throw new Exception("Unable to extract from ID " + id);
					}

					if (0 != FingerprintSensorEx.DBAdd(dbHandle, id, blob)) {
						throw new Exception("Failed to register ID " + id + " to the algorithm database");
					}
				} catch (Exception e) {
					System.err.println(e.getLocalizedMessage());
				}
			}

			System.out.println("Fingerprint database has been successfully updated.");
		}).onError(err -> {
			App.Notif.error(err.getMessage());
		}).executeAsync();
	}

	public JSONObject identify(byte[] template) throws Exception {
		int[] id = new int[1];
		int[] score = new int[1];

		if (0 != FingerprintSensorEx.DBIdentify(dbHandle, template, id, score)) {
			throw new Exception("Failed to identify the fingerprint data");
		}

		JSONObject data = new JSONObject();

		data.put("id", id[0]);
		data.put("score", score[0]);

		return data;
	}

	public static String toBase64(byte[] template) {
		return FingerprintSensorEx.BlobToBase64(template, TEMPLATE_SIZE);
	}
}
