package com.sparkcleancebu.zk9500;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;

public class FingerprintReader implements AutoCloseable {
	private long mhDevice = 0;
	private boolean mbStop = true;
	private int nFakeFunOn = 1;
	private boolean renderImage = false;

	private int[] templateLen = new int[1];

	private byte[] template = new byte[2048];
	private byte[] imgBuffer = null;

	int imgWidth = 0;
	int imgHeight = 0;

	private WorkThread workThread = null;

	private List<ReadEventListener> listeners = new ArrayList<>();

	public FingerprintReader(boolean renderImage) {
		try {
			this.renderImage = renderImage;
			init();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public FingerprintReader() {
		try {
			init();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void init() {
		int ret = FingerprintSensorErrorCode.ZKFP_ERR_OK;

		// Initialize sensor
		ret = FingerprintSensorEx.Init();
		if (ret != FingerprintSensorErrorCode.ZKFP_ERR_OK) {
			throw new IllegalStateException("FingerprintSensor class failed to initialize");
		}

		// Check device count
		int count = FingerprintSensorEx.GetDeviceCount();
		if (count < 1) {
			FreeSensor();
			throw new IllegalStateException("No devices connected!");
		}

		// Open the device
		mhDevice = FingerprintSensorEx.OpenDevice(0);
		if (mhDevice == 0) {
			FreeSensor();
			throw new IllegalStateException("Failed to open fingerprint device");
		}

		mbStop = false;

		setImageBuffer();

		workThread = new WorkThread();
		workThread.start();

		System.out.println("Device count: " + count);
	}

	private void FreeSensor() {
		mbStop = true;

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (mhDevice > 0) {
			FingerprintSensorEx.CloseDevice(mhDevice);
			mhDevice = 0;
		}

		FingerprintSensorEx.Terminate();
	}

	private void setImageBuffer() {
		byte[] paramValue = new byte[4];
		int[] size = new int[1];

		// get image width
		size[0] = 4;
		FingerprintSensorEx.GetParameters(mhDevice, 1, paramValue, size);
		imgWidth = byteArrayToInt(paramValue);

		// get image height
		size[0] = 4;
		FingerprintSensorEx.GetParameters(mhDevice, 2, paramValue, size);
		imgHeight = byteArrayToInt(paramValue);

		// set
		imgBuffer = new byte[imgWidth * imgHeight];
	}

	public static byte[] intToByteArray(final int number) {
		byte[] abyte = new byte[4];

		abyte[0] = (byte) (0xff & number);
		abyte[1] = (byte) ((0xff00 & number) >> 8);
		abyte[2] = (byte) ((0xff0000 & number) >> 16);
		abyte[3] = (byte) ((0xff000000 & number) >> 24);

		return abyte;
	}

	public static int byteArrayToInt(byte[] bytes) {
		int number = bytes[0] & 0xFF;

		number |= ((bytes[1] << 8) & 0xFF00);
		number |= ((bytes[2] << 16) & 0xFF0000);
		number |= ((bytes[3] << 24) & 0xFF000000);

		return number;
	}

	public void addReadEventListener(ReadEventListener listener) {
		this.listeners.add(listener);
	}

	private void triggerReadEvents(ReadEvent event) {
		for (ReadEventListener listener : listeners) {
			listener.readEventOccured(event);
		}
	}

	@Override
	public void close() {
		if (mhDevice != 0) {
			FreeSensor();
			System.out.println("Sensor device has been closed.");
		}
	}

	public static byte[] changeByte(int data) {
		return intToByteArray(data);
	}

	public static void writeBitmap(byte[] imageBuf, int nWidth, int nHeight, String path) throws IOException {
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

	public void renderFingerprint() {
		if (!this.renderImage) {
			return;
		}

		try {
			writeBitmap(imgBuffer, imgWidth, imgHeight, "fingerprint.bmp");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class WorkThread extends Thread {
		@Override
		public void run() {
			super.run();

			int ret = 0;

			while (!mbStop) {

				if (mhDevice == 0) {
					mbStop = true;
					System.err.println("Device handle not initialized. Exiting thread...");
					break;
				}

				templateLen[0] = 2048;

				if (0 == (ret = FingerprintSensorEx.AcquireFingerprint(mhDevice, imgBuffer, template, templateLen))) {
					if (nFakeFunOn == 1) {
						byte[] paramValue = new byte[4];
						int[] size = new int[1];
						int nFakeStatus = 0;

						size[0] = 4;

						ret = FingerprintSensorEx.GetParameters(mhDevice, 2004, paramValue, size);
						// GetFakeStatus
						nFakeStatus = byteArrayToInt(paramValue);

						if (0 == ret && (byte) (nFakeStatus & 31) != 31) {
							throw new IllegalStateException("Fake");
						}
					}

					renderFingerprint();

					String strBase64 = FingerprintSensorEx.BlobToBase64(template, templateLen[0]);

					ReadEvent readEvt = new ReadEvent(this, strBase64, ret);

					triggerReadEvents(readEvt);
				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}