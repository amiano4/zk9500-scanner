package com.sparkcleancebu.zk9500_tray_app;

import java.io.IOException;
import java.util.EventListener;
import java.util.EventObject;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;

@SuppressWarnings("serial")
public class Scanner {
	private boolean standby = true; // allows fingerprint reading if set to false
	private long deviceHandle = 0;
	private int initValue;
	private int imgWidth = 0;
	private int imgHeight = 0;
	private int nFakeFunOn = 1;
	private int[] fingerprintTemplateLength = new int[1];
	private byte[] fingerprintTemplate = new byte[2048];
	private byte[] imgBuffer = null;
	private FingerprintListener listener = null;
	private WorkThread task = null;

	public Scanner() throws AppException.ScannerInitializationException {
		initValue = FingerprintSensorEx.Init();

		if (initValue != FingerprintSensorErrorCode.ZKFP_ERR_OK) {
			throw new AppException.ScannerInitializationException(
					"Unable to initialize scanner resources. Please check the device connection.");
		}

		// just check for connected devices
		open(null, false);
	}

	// release scanner resources, gracefully close it
	public void releaseScanner() {
		standby = true;

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (deviceHandle > 0) {
			FingerprintSensorEx.CloseDevice(deviceHandle);
			deviceHandle = 0;
		}

		FingerprintSensorEx.Terminate();
	}

	public void open(OpenScannerCallback callback, boolean enable) throws AppException.ScannerInitializationException {
		int deviceCount = FingerprintSensorEx.GetDeviceCount();

		// no device available
		if (deviceCount < 1) {
			releaseScanner();
			throw new AppException.ScannerInitializationException("No device(s) connected");
		}

		deviceHandle = FingerprintSensorEx.OpenDevice(0);

		// fail to open device
		if (deviceHandle == 0) {
			releaseScanner();
			throw new AppException.ScannerInitializationException("Failed to open fingerprint device");
		}

		if (callback != null) {
			callback.run();
		}

		// toggle standby mode
		standby = !enable;

		if (!standby) {
			byte[] paramValue = new byte[4];
			int[] size = new int[1];

			// get image width
			size[0] = 4;
			FingerprintSensorEx.GetParameters(deviceHandle, 1, paramValue, size);
			imgWidth = byteArrayToInt(paramValue);

			// get image height
			size[0] = 4;
			FingerprintSensorEx.GetParameters(deviceHandle, 2, paramValue, size);
			imgHeight = byteArrayToInt(paramValue);

			// set
			imgBuffer = new byte[imgWidth * imgHeight];

			task = new WorkThread();
			task.start();
		}
	}

	public void addListener(FingerprintListener listener) {
		this.listener = listener;
	}

	private byte[] changeByte(int data) {
		return intToByteArray(data);
	}

	@SuppressWarnings("unused")
	private void writeBitmap(byte[] imageBuf, int nWidth, int nHeight, String path) throws IOException {
		java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
		java.io.DataOutputStream dos = new java.io.DataOutputStream(fos);

		int w = (((nWidth + 3) / 4) * 4);
		int bfType = 0x424d;
		int bfSize = 54 + 1024 + w * nHeight;
		int bfReserved1 = 0;
		int bfReserved2 = 0;
		int bfOffBits = 54 + 1024;

		dos.writeShort(bfType);
		dos.write(changeByte(bfSize), 0, 4);
		dos.write(changeByte(bfReserved1), 0, 2);
		dos.write(changeByte(bfReserved2), 0, 2);
		dos.write(changeByte(bfOffBits), 0, 4);

		int biSize = 40;
		int biWidth = nWidth;
		int biHeight = nHeight;
		int biPlanes = 1;
		int biBitcount = 8;
		int biCompression = 0;
		int biSizeImage = w * nHeight;
		int biXPelsPerMeter = 0;
		int biYPelsPerMeter = 0;
		int biClrUsed = 0;
		int biClrImportant = 0;

		dos.write(changeByte(biSize), 0, 4);
		dos.write(changeByte(biWidth), 0, 4);
		dos.write(changeByte(biHeight), 0, 4);
		dos.write(changeByte(biPlanes), 0, 2);
		dos.write(changeByte(biBitcount), 0, 2);
		dos.write(changeByte(biCompression), 0, 4);
		dos.write(changeByte(biSizeImage), 0, 4);
		dos.write(changeByte(biXPelsPerMeter), 0, 4);
		dos.write(changeByte(biYPelsPerMeter), 0, 4);
		dos.write(changeByte(biClrUsed), 0, 4);
		dos.write(changeByte(biClrImportant), 0, 4);

		for (int i = 0; i < 256; i++) {
			dos.writeByte(i);
			dos.writeByte(i);
			dos.writeByte(i);
			dos.writeByte(0);
		}

		byte[] filter = null;
		if (w > nWidth) {
			filter = new byte[w - nWidth];
		}

		for (int i = 0; i < nHeight; i++) {
			dos.write(imageBuf, (nHeight - 1 - i) * nWidth, nWidth);
			if (w > nWidth)
				dos.write(filter, 0, w - nWidth);
		}
		dos.flush();
		dos.close();
		fos.close();
	}

	private byte[] intToByteArray(final int number) {
		byte[] abyte = new byte[4];

		abyte[0] = (byte) (0xff & number);
		abyte[1] = (byte) ((0xff00 & number) >> 8);
		abyte[2] = (byte) ((0xff0000 & number) >> 16);
		abyte[3] = (byte) ((0xff000000 & number) >> 24);

		return abyte;
	}

	private int byteArrayToInt(byte[] bytes) {
		int number = bytes[0] & 0xFF;

		number |= ((bytes[1] << 8) & 0xFF00);
		number |= ((bytes[2] << 16) & 0xFF0000);
		number |= ((bytes[3] << 24) & 0xFF000000);

		return number;
	}

	private class WorkThread extends Thread {
		@Override
		public void run() {
			super.run();

			int returnValue = 0;

			while (!standby) {
				fingerprintTemplateLength[0] = FingerprintLibrary.TEMPLATE_SIZE;

				if (0 == (returnValue = FingerprintSensorEx.AcquireFingerprint(deviceHandle, imgBuffer,
						fingerprintTemplate, fingerprintTemplateLength))) {
					if (nFakeFunOn == 1) {
						byte[] paramValue = new byte[4];
						int[] size = new int[1];
						int nFakeStatus = 0;

						size[0] = 4;

						returnValue = FingerprintSensorEx.GetParameters(deviceHandle, 2004, paramValue, size);
						// GetFakeStatus
						nFakeStatus = byteArrayToInt(paramValue);

						System.out.println("returnValue=" + returnValue + ", nFakeStatus=" + nFakeStatus);

						if (0 == returnValue && (byte) (nFakeStatus & 31) != 31) {
							// is a fake-finer?
						}
					}

					FingerprintEvent event = new FingerprintEvent(this, fingerprintTemplate, returnValue);

					// trigger listener
					if (listener != null) {
						listener.actionPerformed(event);
					}
				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public class FingerprintEvent extends EventObject {
		private final int returnValue;
		private final byte[] template;

		public FingerprintEvent(Object src, byte[] template, int ret) {
			super(src);
			this.returnValue = ret;
			this.template = template;
		}

		public String getBase64Template() {
			return FingerprintLibrary.toBase64(template);
		}

		public byte[] getTemplate() {
			return template;
		}

		public int getReturnValue() {
			return returnValue;
		}
	}

	public interface FingerprintListener extends EventListener {
		void actionPerformed(FingerprintEvent event);
	}

	@FunctionalInterface
	public interface OpenScannerCallback {
		void run() throws AppException.ScannerInitializationException;
	}
}
