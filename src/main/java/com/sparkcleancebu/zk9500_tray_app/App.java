package com.sparkcleancebu.zk9500_tray_app;

import java.io.File;

import com.amiano4.httpflux.HttpService;
import com.dustinredmond.fxtrayicon.FXTrayIcon;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
@SuppressWarnings("exports")
public class App extends Application implements FingerprintService.ReadEventListener {
	public static final String APP_NAME = "Biometrics App"; // default app name
	public static final String APP_ICON = "/images/icon.png";
	public static final String CONFIG_FILENAME = "config.properties";

	public static final String CONF_HOST = "HOST";
	public static final String CONF_APPID = "APP";
	public static final String CONF_LOCALID = "LOCAL_ID";
	public static final String CONF_BRANCH = "BRANCH";
	public static final String CONF_SOCKET = "SOCKET_CLIENT";
	public static final String CONF_VERIFIED_AT = "VERIFIED_AT";
	public static final String CONF_NAME = "NAME";

	public static final String URI_AUTH = "/api/java/auth";
	public static String appID = null;

	static boolean forExit = false;
	static Scene scene;
	static Stage stage;
	static FXMLLoader fxmlLoader;
	static Authenticator authenticator;
	static FXTrayIcon trayIcon;
	static FingerprintService scanner = null;
	static FXML_Details_Controller ui;

	@Override
	public void start(Stage stage) throws Exception {
		App.stage = stage;

		stage.setResizable(false);

		try {
			if (trayIcon == null) {
				stage.getIcons().add(new Image(getClass().getResourceAsStream(APP_ICON)));

				trayIcon = new FXTrayIcon.Builder(stage, getClass().getResource(APP_ICON)) //
						.applicationTitle(APP_NAME) //
						.menuItem("Restart", e -> restartApp()) //
						.separator() //
						.addExitMenuItem("Exit Application", e -> initiateExit()) //
						.show() //
						.build();
			}

			if (scanner == null) {
				// sdk only allows one instance per app
				scanner = new FingerprintService();
				scanner.addReadEventListener(this);
			}

			// accept json responses
			HttpService.registeredHeaders.add("Accept", "application/json");

			Config config = new Config(CONFIG_FILENAME);
			String appID = config.getAppID();

			authenticator = new Authenticator(config);
			authenticator.checkHost(); // verify host

			// check for stored app id in the local config
			if (appID == null) {
				// Start new setup
				showSetupDialog();
			} else {
				// authenticate setup
				authenticator.start(appID);
			}
		} catch (Exception e) {
			AppError.handle(e);
		}
	}

	@Override
	public void readEventOccured(FingerprintService.ReadEvent event) {
		try {
			String base64Data = event.getBase64Template(); // base64 encoded from template
			int retValue = event.getRetValue(); // return value of the scan
			byte[] template = event.getTemplate(); // original fingerprint template

			ui.debug(base64Data);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void showProgressDialog(Parent root) {
		Platform.runLater(() -> {
			try {
				if (stage.isShowing()) {
					stage.close();
					Thread.sleep(500);
				}

				stage.setTitle("Authentication");
				setRoot(root);
				scene.setCursor(Cursor.WAIT);
				stage.show();
			} catch (Exception e) {
				e.printStackTrace();
				exitApp();
			}
		});
	}

	public static void showSetupDialog() {
		Platform.runLater(() -> {
			try {
				if (stage.isShowing()) {
					stage.close();
					Thread.sleep(500);
				}

				FXMLLoader loader = new FXMLLoader(App.class.getResource("Setup.fxml"));
				Parent root = loader.load();

				stage.setTitle("Setup");
				setRoot(root);
				scene.setCursor(Cursor.DEFAULT);
				stage.show();
			} catch (Exception e) {
				e.printStackTrace();
				exitApp();
			}
		});
	}

	public static void closeWindow() {
		Platform.runLater(() -> {
			try {
				Thread.sleep(500);
				stage.close(); // close the window
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public static void prepareDetailsDialog(Config config) {
		Platform.runLater(() -> {
			try {
				FXMLLoader loader = new FXMLLoader(App.class.getResource("Details.fxml"));
				Parent root = loader.load();
				SocketClient.Channel channel = authenticator.getChannel();

				ui = loader.getController();
				ui.display(config);

				if (channel != null) {
					// display the channel id
					ui.channel(channel.getName());
				}

				stage.close();

				stage.setTitle(APP_NAME);

				scene.setRoot(root);

				scene.setCursor(Cursor.DEFAULT);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private static void setRoot(Parent root) {
		if (scene == null) {
			scene = new Scene(root);
			stage.setScene(scene);
		} else {
			scene.setRoot(root);
		}
	}

	public static void initiateExit() {
		try {
			forExit = true;
			closeWindow();
			authenticator.getChannel().send(Authenticator.SOCKET_EVT_DISCONNECT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void exitApp() {
		Platform.runLater(() -> {
			if (scanner != null) {
				scanner.close();
				App.Notif.show("Scanner device has been closed.");
			}

			// Delay exit to allow notification to be shown
			new Thread(() -> {
				Platform.exit(); // Closes JavaFX application
				System.exit(0); // Force exit
			}).start();
		});
	}

	public static void restartApp() {
		forExit = false;

		if (scanner != null) {
			scanner.close();
			scanner = null;
		}

		Platform.runLater(() -> {
			try {
				// Close the current stage
				stage.close();

				Thread.sleep(3000);

				// Relaunch the application
				App mainApp = new App();
				mainApp.start(stage);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public static class Notif {
		public static void error(String content) {
			if (isTray()) {
				trayIcon.showErrorMessage(APP_NAME, content);
			}
		}

		public static void info(String content) {
			if (isTray() && !forExit) {
				trayIcon.showInfoMessage(APP_NAME, content);
			}
		}

		public static void show(String content) {
			if (isTray()) {
				trayIcon.showMessage(APP_NAME, content);
			}
		}

		private static boolean isTray() {
			return trayIcon instanceof FXTrayIcon;
		}
	}

	public static void main(String[] args) {
		boolean isProduction = false;

		// store arguments to System properties
		for (String arg : args) {
			if (arg.equals("--production")) {
				isProduction = true;
			} else if (arg.startsWith("--host=")) {
				// api url
				System.setProperty(CONF_HOST, arg.substring("--host=".length()));
			}
		}

		String appData = System.getenv("APPDATA");

		if (isProduction && appData != null) {
			System.setProperty("APPDIR", appData + File.separator + APP_NAME + File.separator);
		} else {
			System.setProperty("APPDIR", "."); // Development: current directory
		}

		launch();
	}

}