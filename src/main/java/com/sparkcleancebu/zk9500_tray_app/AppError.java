package com.sparkcleancebu.zk9500_tray_app;

public class AppError {
	public static void handle(Exception ex) {
		String errorMessage = ex.getLocalizedMessage();

		ex.printStackTrace();

		if (ex instanceof AppException.ScannerInitializationException) {
			// scanner related
			try {
				App.Notif.error(errorMessage);
				App.closeWindow();
				Thread.sleep(3000);
				App.exitApp();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if (ex instanceof AppException.ConfigException) {
			// config file
			App.Notif.error(errorMessage);
		} else if (ex instanceof AppException.InvalidHostException) {
			// host validation and xsrf token
			App.Notif.error(errorMessage);
		} else if (ex instanceof AppException.AuthenticationException) {
			App.Notif.error(errorMessage);
			App.closeWindow();
		}

		// last
		else {
			System.err.println(errorMessage);
		}
	}
}
