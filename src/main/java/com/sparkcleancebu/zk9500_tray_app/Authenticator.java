package com.sparkcleancebu.zk9500_tray_app;

import java.net.URI;
import java.util.Optional;

import org.json.JSONObject;

import com.amiano4.httpflux.FormDataBuilder;
import com.amiano4.httpflux.HttpService;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class Authenticator {
	public static final String SOCKET_EVT_CONNECT = "ScannerWentOnline";
	public static final String SOCKET_EVT_DISCONNECT = "ScannerDisconnect";
	public static final String SOCKET_EVT_FINGERPRINT_REGISTRATION = "FingerprintRegistration";
	public static final String SOCKET_EVT_REFRESH = "RefreshScannerLibrary";

	private String csrfToken = null;
	private Config config;
	private SocketClient socket;
	private Parent progressControllerRoot;

	public Authenticator(Config config) throws Exception {
		this.config = config;
		this.socket = null;
	}

	public void start(String appID) throws AppException.AuthenticationException {
		if (appID == null) {
			throw new AppException.AuthenticationException("Unable to authenticate when APP ID is missing");
		}

		FXMLLoader loader = new FXMLLoader(App.class.getResource("Progress.fxml"));
		FXML_Progress_Controller ui = null;

		try {
			// to get the ui controller
			progressControllerRoot = loader.load();
			ui = loader.getController();
		} catch (Exception e) {
			App.Notif.error("An error has occured");
			e.printStackTrace();
			App.exitApp();
		}

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() throws Exception {
				try {
					String localID = config.getLocalID();

					FormDataBuilder formData = new FormDataBuilder();
					formData.append("localId", localID);
					formData.append("appId", appID);

					HttpService.post(App.URI_AUTH, formData).onSuccess((response) -> {
						// display ui
						App.showProgressDialog(progressControllerRoot);

						updateProgress(0.1, 1);
						updateMessage("Initializing authentication request");
						Thread.sleep(1000);

						JSONObject json = new JSONObject(response.body());
						try {
							updateProgress(0.5, 1);
							updateMessage("Updating local configurations. Sync in progress...");

							Thread.sleep(1000);

							// Save configurations
							config.setAppID(appID);
							config.setBranch(json.getString("branch"));
							config.setName(json.getString("name"));
							config.setVerifiedAt(json.getString("verified"));
							config.save();

							updateProgress(0.7, 1);
							updateMessage("Starting socket client...");

							// set default headers (for api validation)
							HttpService.registeredHeaders.add(App.HEADER_APP, appID);
							HttpService.registeredHeaders.add(App.HEADER_LOCAL, localID);

							updateProgress(0.8, 1);
							updateMessage("Setting headers...");

							// connect to websockets
							initSocketClient(json.getString("socket"));
						} catch (Exception e) {
							AppError.handle(new AppException.AuthenticationException(
									"An error has occurred while syncing local configurations", e));
						}
					}).onError(err -> {
						if (err.getResponse() != null) {
							if (401 == err.getResponse().statusCode()
									&& ("\"Invalid identification.\"").equals(err.getResponse().body())) {
								// incorrect app id/setup
								App.showSetupDialog();
								App.Notif.error("The ID is already registered to different setup");
								return;
							}

							AppError.handle(new AppException.AuthenticationException(
									err.getResponse().body() + err.getResponse().statusCode(), err));
						} else {
							AppError.handle(new AppException.AuthenticationException(err.getMessage(), err));
						}
					}).executeSync();

				} catch (Exception e) {
					AppError.handle(
							new AppException.AuthenticationException("Unable to start authentication process", e));
				}
				return null;
			}

			// Socket initialization here
			private void initSocketClient(String url) {
				try {
					if (url == null) {
						throw new IllegalStateException("Missing websocket server protocol");
					}

					URI uri = new URI(url);

					updateProgress(0.9, 1);
					updateMessage("Starting websocket client...");

					socket = new SocketClient(uri, config.getAppID() + "." + config.getLocalID());

					updateProgress(0.95, 1);
					updateMessage("Subscribed to app channel");

					Thread.sleep(500);

					updateProgress(0.97, 1);
					updateMessage("Testing connection...");

					// Final step: receiving server event after online status sent (pinged)
					socket.listen(SOCKET_EVT_CONNECT, data -> {
						updateProgress(1.0, 1);
						updateMessage("Connected successfully!");

						try {
							Thread.sleep(500);
							updateMessage("Checking fingerprint scanner...");

							if (App.scanner != null) {
								App.scanner.open(() -> {
									updateMessage("Fingerprint scanner ready.");

									App.closeWindow();
									App.prepareDetailsDialog(config);

									listenToEvents();

									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}

									App.Notif.info("App is ready");
								}, true);
							} else {
								App.closeWindow();
								throw new AppException.ScannerInitializationException(
										"No fingerprint scanner has been detected");
							}
						} catch (Exception e) {
							AppError.handle(e);
						}
					});

					// send http request to update db status
					HttpService.get("/online").onError(err -> {
						AppError.handle(new AppException.ScannerInitializationException(err.getLocalizedMessage()));
					}).executeSync();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		ui.progressBar.progressProperty().bind(task.progressProperty());
		ui.status.textProperty().bind(task.messageProperty());

		// Start
		new Thread(task).start();
	}

	public void checkHost() throws Exception {
		String host = System.getProperty(App.CONF_HOST);
		acquireCsrfToken(host);
		config.setHost(host);
		config.save();

		// set base url
		HttpService.setBaseUrl(host + App.BASE_URL_PATH);
	}

	private void acquireCsrfToken(String baseUrl) throws AppException.InvalidHostException {
		csrfToken = null;
		try {
			HttpService.get(baseUrl + "/sanctum/csrf-cookie").onSuccess((response) -> {
				Optional<String> tokenBody = response.headers().firstValue("Set-Cookie");
				Exception exception = null;

				if (tokenBody.isPresent()) {
					// Extract CSRF token value
					String[] parts = tokenBody.get().split(";");
					if (parts.length > 0) {
						String[] keyValue = parts[0].split("=");
						if (keyValue.length > 1) {
							// store the token
							csrfToken = keyValue[1];
						} else {
							exception = new AppException.InvalidHostException("Invalid CSRF token format");
						}
					} else {
						exception = new AppException.InvalidHostException("Incorrect header received");
					}
				} else {
					exception = new AppException.InvalidHostException("CSRF token header is missing");
				}

				if (exception != null)
					AppError.handle(exception);

			}).onError(err -> {
				String errorMessage = err.getResponse() != null ? err.getResponse().body() : err.getLocalizedMessage();
				AppError.handle(new AppException.InvalidHostException(errorMessage));
			}).executeSync();

		} catch (Exception e) {
			AppError.handle(new AppException.InvalidHostException("Unable verify host"));
		}
	}

	public String getCrsfToken() {
		return csrfToken;
	}

	public SocketClient getSocketClient() {
		return socket;
	}

	public void disconnect() throws Exception {
		socket.disconnect();

		// send disconnection update
		HttpService.get("/offline").onError(err -> {
			err.printStackTrace();
		}).executeAsync();
	}

	public void listenToEvents() {
		socket.listen(SOCKET_EVT_FINGERPRINT_REGISTRATION, data -> {
			JSONObject mainData = data.getJSONObject("data");
			String action = mainData.getString("action");
			// String fingerType = mainData.getString("finger");

			if (action.equals("start")) {
				int fingerprintId = mainData.getInt("id");
				System.out.println("registration started id: " + fingerprintId);

				// start fingerprint registration process
				App.library.startRegistration(fingerprintId);
			}
		});

		// refresh data
		socket.listen(SOCKET_EVT_REFRESH, data -> {
			try {
				App.library.initialize();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
