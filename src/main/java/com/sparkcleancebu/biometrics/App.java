package com.sparkcleancebu.biometrics;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkcleancebu.http_helper.FormData;
import com.sparkcleancebu.http_helper.HttpClientHelper;
import com.sparkcleancebu.zk9500.FingerprintReader;
import com.sparkcleancebu.zk9500.ReadEvent;
import com.sparkcleancebu.zk9500.ReadEventListener;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * JavaFX App
 */
public class App extends Application implements ReadEventListener {
	private FingerprintReader reader;
	private UIController ui;

	public static ConfigReader config = new ConfigReader("config.json");
	static boolean hasBranchList = false; // flag to be used by other classes to check if branches has been fetched

	@Override
	public void start(Stage stage) throws Exception {
		HttpClientHelper.registeredHeaders.add("Accept", "application/json");

		FXMLLoader loader = new FXMLLoader(App.class.getResource("UI.fxml"));
		Parent root = loader.load();

		this.ui = loader.getController();

		setIcon(stage, "icon.png");
		setUIActions(stage);

		this.reader = new FingerprintReader();
		this.reader.addReadEventListener(this);

		Scene scene = new Scene(root);
		stage.setTitle("Fingerprint Scanner App");
		stage.setScene(scene);
		stage.setResizable(false);
		// stage.show();
	}

	public void setUIData() {
		String baseUrl = config.get("api_url");

		if (baseUrl == null || baseUrl.isEmpty()) {
			// disable combo box since no data can be initialized
			this.ui.resetAndDisableBranchCode();
		} else {
			HttpClientHelper.setBaseUrl(baseUrl);
			CsrfToken.acquire();

			// show URL
			this.ui.urlField.setText(baseUrl);

			// fetch all branches
			try {
				HttpResponse<String> response = HttpClientHelper.get("api/java-endpoint/branches");

				ObjectMapper objectMapper = new ObjectMapper();

				@SuppressWarnings("unchecked")
				List<String> branchList = objectMapper.readValue(response.body(), List.class);

				// enable branch code combo box
				this.ui.branchCodeComboBox.setDisable(false);

				this.ui.branchCodeComboBox.setItems(FXCollections.observableArrayList(branchList));

				String assignedBranch = config.get("branch");
				this.ui.branchCodeComboBox.setValue(assignedBranch);

				App.hasBranchList = true;
			} catch (Exception e) {
				// reset
				this.ui.branchCodeComboBox.setValue(null);
				this.ui.branchCodeComboBox.setDisable(true);
				this.ui.branchCodeComboBox.getItems().clear();

				e.printStackTrace();
			}
		}
	}

	public void setUIActions(Stage stage) {
		stage.setOnCloseRequest(event -> {
			this.ui.clearCredentialsFields();
			System.out.println("Window was closed");
		});

		stage.setOnShowing(event -> {
			setUIData();
			System.out.println("Opening window...");
		});

		// cancel
		this.ui.cancelButton.setOnAction(event -> {
			stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
		});

		// apply button
		this.ui.applyButton.setOnAction(event -> applyChanges());
	}

	public void applyChanges() {
		try {
			HashMap<String, Object> values = this.ui.getValues();

			if ((boolean) values.get("isValid") == false) {
				throw new Exception("Incomplete form: unable to proceed.");
			}

			String newUrl = values.get("url").toString().trim();
			String oldUrl = config.get("api_url");
			String branch = values.get("branch").toString();
			boolean urlHasChanged = false;

			/**
			 * Test and verify the new URL. Only proceed to test if there are changes on the
			 * string.
			 */
			if ((oldUrl == null || oldUrl.isEmpty()) || !newUrl.equalsIgnoreCase(oldUrl)) {
				// set as base URL
				HttpClientHelper.setBaseUrl(newUrl);

				try {
					CsrfToken.resetToken();

					// acquire new token
					String token = CsrfToken.acquire();

					if (token == null || token.isEmpty()) {
						throw new IllegalStateException("Invalid URL " + newUrl);
					}

					urlHasChanged = true;
				} catch (Exception e) {
					if (oldUrl != null && !oldUrl.isEmpty()) {
						// revert change
						HttpClientHelper.setBaseUrl(oldUrl);
					}

					throw e;
				}
			} else {
				// invoke token requirement just in case
				CsrfToken.acquire();
			}

			/**
			 * URL is verified. Proceed with the authentication.
			 */
			FormData formData = new FormData();
			formData.append("username", values.get("username").toString());
			formData.append("password", values.get("password").toString());

			// include branch code if present
			if (branch != null || !branch.isEmpty()) {
				formData.append("branch", branch);
			}

			HttpResponse<String> response = HttpClientHelper.post("api/java-endpoint/apply", formData);

			// error response
			if (response.statusCode() != 200) {
				ObjectMapper objectMapper = new ObjectMapper();

				JsonNode json = objectMapper.readTree(response.body());

				String errorMessage = json.get("message").asText();
				errorMessage = errorMessage == null || errorMessage.isEmpty() ? "Unable to process your request."
						: errorMessage;

				throw new Exception(errorMessage);
			} else {
				System.out.println("Response: " + response.body());

				if (urlHasChanged) {
					// save the URL to the configuration file
					config.set("api_url", newUrl);

					// initialize data (for new setup)
					setUIData();
				}

				if (branch != null || !branch.isEmpty()) {
					config.set("branch", branch);
				}

				this.ui.successAlert("Saved!", "Changes has been applied.");
				this.ui.clearCredentialsFields();
			}
		} catch (Exception e) {
			this.ui.showError("An error has occured", e.getMessage());
			// e.printStackTrace();
		}
	}

	public void setIcon(Stage stage, String iconResource) {
		try {
			stage.getIcons().add(new Image(getClass().getResourceAsStream(iconResource)));

			new FXTrayIcon.Builder(stage, getClass().getResource(iconResource)).menuItem("Show", e -> {
				stage.show();
			}).addExitMenuItem("Exit", e -> exitApp()).show().build();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("exports")
	@Override
	public void readEventOccured(ReadEvent evt) {
		try {
			String base64Data = evt.getBase64Template();

			FormData formData = new FormData();
			formData.append("data", base64Data);
			formData.append("image", "fingerprint.bmp", "fingerprint.bmp");

			// System.out.println(formData.build().toString());

			HttpResponse<String> response = HttpClientHelper.post("api/sensor/scan", formData);

			System.out.println("Status Code => " + response.statusCode());
			System.out.println("Response => " + response.body());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void exitApp() {
		this.reader.close();
		System.exit(0);
	}

	public static void main(String[] args) {
		launch();
	}
}