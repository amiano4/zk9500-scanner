package zk9500scanner;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;

/**
 * Handles fingerprint scanning operations using the ZKTeco SDK. This class manages device initialization, fingerprint acquisition, and fake finger detection.
 */
public class Scanner {
	public static final int TEMPLATE_SIZE = 2048;

	private long deviceHandle = 0;
	private int initValue;
	private int imgWidth = 0;
	private int imgHeight = 0;
	private int nFakeFunOn = 1;
	private int[] fingerprintTemplateLength = new int[1];
	private byte[] fingerprintTemplate = new byte[TEMPLATE_SIZE];
	private byte[] imgBuffer = null;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	public Consumer<byte[]> onScanEvent = null;

	/**
	 * Checks if the scanner device is open and available.
	 *
	 * @return true if the scanner is open and connected, false otherwise.
	 */
	public boolean isOpen() {
		int devices = FingerprintSensorEx.GetDeviceCount();
		boolean status = deviceHandle != 0 && devices > 0;

		if (!status) {
			releaseScanner();
		}

		return status;
	}

	/**
	 * Initializes the fingerprint scanner. This sets up necessary parameters and opens the device.
	 */
	public void init() {
		try {
			initValue = FingerprintSensorEx.Init();

			if (initValue != FingerprintSensorErrorCode.ZKFP_ERR_OK) {
				throw new IllegalStateException("Unable to initialize scanner resources.");
			}

			int deviceCount = FingerprintSensorEx.GetDeviceCount();
			if (deviceCount < 1) {
				throw new IllegalStateException("No device(s) connected");
			}

			deviceHandle = FingerprintSensorEx.OpenDevice(0);
			if (deviceHandle == 0) {
				throw new IOException("Failed to open fingerprint device");
			}

			byte[] paramValue = new byte[4];
			int[] size = new int[1];
			size[0] = 4;

			// Get image width and height
			FingerprintSensorEx.GetParameters(deviceHandle, 1, paramValue, size);
			imgWidth = byteArrayToInt(paramValue);
			FingerprintSensorEx.GetParameters(deviceHandle, 2, paramValue, size);
			imgHeight = byteArrayToInt(paramValue);

			imgBuffer = new byte[imgWidth * imgHeight];
			Main.log.info("Scanner initialized successfully.");
		} catch (Exception e) {
			releaseScanner();
			Main.log.severe("Scanner initialization failed: " + e.getMessage());
		}
	}

	/**
	 * Starts fingerprint scanning in a separate thread. Captured fingerprints are logged in Base64 format.
	 */
	public void startScanning() {
		executorService.submit(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				fingerprintTemplateLength[0] = TEMPLATE_SIZE;

				int ret = FingerprintSensorEx.AcquireFingerprint(deviceHandle, imgBuffer, fingerprintTemplate,
						fingerprintTemplateLength);
				if (ret == 0) {
					if (nFakeFunOn == 1) {
						byte[] paramValue = new byte[4];
						int[] size = new int[1];
						size[0] = 4;
						FingerprintSensorEx.GetParameters(deviceHandle, 2004, paramValue, size);
						int nFakeStatus = byteArrayToInt(paramValue);
						if ((nFakeStatus & 31) != 31) {
							Main.log.warning("Fake finger detected!");
							continue; // Skip processing this scan
						}
					}

					// String strBase64 = FingerprintSensorEx.BlobToBase64(fingerprintTemplate,
					// fingerprintTemplateLength[0]);
					// Main.log.info("Fingerprint captured: " + strBase64);

					if (onScanEvent != null) {
						onScanEvent.accept(fingerprintTemplate);
					}

					Arrays.fill(imgBuffer, (byte) 0);
					Arrays.fill(fingerprintTemplate, (byte) 0);
				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
	}

	/**
	 * Stops the scanning process and shuts down the executor service.
	 */
	public void stopScanning() {
		executorService.shutdownNow();
	}

	/**
	 * Releases scanner resources and closes the device connection.
	 */
	public void releaseScanner() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (deviceHandle > 0) {
			FingerprintSensorEx.CloseDevice(deviceHandle);
			deviceHandle = 0;
		}

		FingerprintSensorEx.Terminate();
		Main.log.info("Scanner resources released.");
	}

	/**
	 * Converts a byte array to an integer.
	 *
	 * @param bytes The byte array.
	 * @return The converted integer.
	 */
	private int byteArrayToInt(byte[] bytes) {
		return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) | ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);
	}
}
