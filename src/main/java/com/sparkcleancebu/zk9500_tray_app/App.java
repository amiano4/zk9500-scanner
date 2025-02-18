package com.sparkcleancebu.zk9500_tray_app;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;

import com.amiano4.httpflux.FormDataBuilder;
import com.amiano4.httpflux.HttpService;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.sparkcleancebu.zk9500_tray_app.Scanner.FingerprintEvent;
import com.sparkcleancebu.zk9500_tray_app.Scanner.FingerprintListener;

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
public class App extends Application implements FingerprintListener {
	public static final String APP_NAME = "Biometrics App"; // default app name
	public static final String APP_ICON = "/images/icon.png";
	public static final String CONFIG_FILENAME = "config.properties";
	public static final String TIMEZONE = "Asia/Manila";

	public static final String CONF_HOST = "HOST";
	public static final String CONF_APPID = "APP";
	public static final String CONF_LOCALID = "LOCAL_ID";
	public static final String CONF_BRANCH = "BRANCH";
	public static final String CONF_SOCKET = "SOCKET_CLIENT";
	public static final String CONF_VERIFIED_AT = "VERIFIED_AT";
	public static final String CONF_NAME = "NAME";

	public static final String BASE_URL_PATH = "/api/java";
	public static final String URI_AUTH = "/auth";
	public static final String URI_FP_REG = "/register";
	public static final String URI_FINGERPRINTS = "/fingerprints";
	public static final String URI_CONFIRM_CONNECTION = "/confirm-connection";
	public static final String URI_ATTENDANCE = "/attendance";
	public static final String URI_NO_MATCH = "/no-match";

	public static final String HEADER_APP = "SCANNER-APP";
	public static final String HEADER_LOCAL = "SCANNER-LOCAL";

	static boolean forExit = false;
	static Scene scene;
	static Stage stage;
	static FXMLLoader fxmlLoader;
	static Authenticator authenticator;
	static FXTrayIcon trayIcon;
	static Scanner scanner = null;
	static FingerprintLibrary library = null;
	static FXML_Details_Controller ui;

	@Override
	public void start(Stage stage) throws Exception {
		HttpService.setBaseUrl("");
		HttpService.registeredHeaders.add("Accept", "application/json"); // accept json responses

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
				scanner = new Scanner();
				scanner.addListener(this);
			}

			if (library == null) {
				library = new FingerprintLibrary();
			}

			Config config = new Config(CONFIG_FILENAME);
			String appID = config.getAppID();

			authenticator = new Authenticator(config);
			authenticator.checkHost(); // verify host

			library.initialize();

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
	public void actionPerformed(FingerprintEvent event) {
		try {
			byte[] template = event.getTemplate();

			if (library == null)
				return;

			// registration process
			if (library.registrationMode()) {
				FormDataBuilder formData = new FormDataBuilder();
				byte[] registeredTemplate = null;

				try {
					registeredTemplate = library.registerTemplate(template);

					if (registeredTemplate == null) {
						// continue registration
						int index = library.getRegisterIndex();
						int count = FingerprintLibrary.REG_TEMPLATE_COUNT - (index + 1);

						formData.append("index", String.valueOf(index));
						formData.append("message",
								"Press the same finger again " + count + " more time" + (count > 1 ? "s" : ""));
					}
				} catch (AppException.FingerprintRegistrationException e) {
					System.err.println(e.getLocalizedMessage());

					if (e.getMessage().equals("Incorrect finger")) {
						formData.append("index", "-1");
						formData.append("message", "Matching error. Please press the same finger 3 times");
					} else if (e.getMessage().equals("Already exists")) {
						formData.append("index", "-1");
						formData.append("message", "Fingerprint is already registered. Please use a different one");
					} else
						throw e;
				}

				if (registeredTemplate != null) {
					// requirements acquired
					formData.append("id", String.valueOf(library.getRegistrationID()));
					formData.append("base64", FingerprintLibrary.toBase64(registeredTemplate));
					formData.append("index", "3");
					formData.append("message", "save");
				}

				// send to api
				HttpService.post(URI_FP_REG, formData).onSuccess(response -> {
					// saved successfully
					if (201 == response.statusCode()) {
						library.saveRegisteredTemplate(); // save to local db
						library.startRegistration(0); // reset process
						// Notif.info("Fingerprint has been registered successfully!");
					}
				}).onError(err -> {
					System.err.println("response " + err.getResponse().body());
				}).executeAsync();
			} else {
				try {
					// normal scanning
					JSONObject data = library.identify(template);

					int id = data.getInt("id");
					int score = data.getInt("score");

					FormDataBuilder formData = new FormDataBuilder();
					formData.append("id", String.valueOf(id));
					formData.append("score", String.valueOf(score));
					formData.append("timestamp", getCurrentTimestamp());

					// send to server
					HttpService.post(URI_ATTENDANCE, formData).onError(err -> {
						System.err.println(err.getResponse().body());
						System.err.println("Attendance response error: " + err.getLocalizedMessage());
					}).executeAsync();

					// Notif.info("Attendance has been verified");
				} catch (Exception e) {
					HttpService.get(URI_NO_MATCH).executeAsync(); // notify client
					System.err.println("No match: " + e.getLocalizedMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("Scan error: " + e.getLocalizedMessage());
		}
	}

	public static String getCurrentTimestamp() {
		Instant now = Instant.now();
		// Convert Instant to LocalDateTime in UTC (or use system default zone)
		LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneId.of(TIMEZONE));
		// Format it to a MySQL-compatible string
		return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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

				ui = loader.getController();
				ui.display(config);

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
			authenticator.disconnect();

			Thread.sleep(3000);
		} catch (Exception e) {
			e.printStackTrace();
		}

		exitApp();
	}

	public static void exitApp() {
		Platform.runLater(() -> {
			if (scanner != null) {
				if (library != null) {
					library.close();
				}

				scanner.releaseScanner();
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
		if (scanner != null) {
			if (library != null) {
				library.close();
			}
			scanner.releaseScanner();
		}

		// disconnect from websockets first
		SocketClient socket = authenticator.getSocketClient();
		if (socket != null) {
			socket.disconnect();
		}

		forExit = false;
		scene = null;
		authenticator = null;
		scanner = null;
		ui = null;
		library = null;

		Platform.runLater(() -> {
			try {
				// Close the current stage
				stage.close();

				Thread.sleep(1000);

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