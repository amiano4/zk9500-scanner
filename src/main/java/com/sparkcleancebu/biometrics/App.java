package com.sparkcleancebu.biometrics;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
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

		CsrfToken.acquire();

		FXMLLoader loader = new FXMLLoader(App.class.getResource("UI.fxml"));
		Parent root = loader.load();

		this.ui = loader.getController();

		setIcon(stage, "icon.png");
		setUIData();
		setUIActions(stage);

		this.reader = new FingerprintReader();
		this.reader.addReadEventListener(this);

		Scene scene = new Scene(root);
		stage.setTitle("Fingerprint Scanner App");
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
	}

	public void setUIData() {
		String baseUrl = config.get("api_url");

		if (baseUrl == null || baseUrl.isEmpty()) {
			// disable combo box since no data can be initialized
			this.ui.resetAndDisableBranchCode();
		} else {
			// show URL
			this.ui.urlField.setText(baseUrl);

			// enable branch code combo box
			this.ui.branchCodeComboBox.setDisable(false);

			String assignedBranch = config.get("branch");

			// no branch has been selected yet
			if (assignedBranch == null || assignedBranch.isEmpty()) {
				// fetch all branches
				try {
					CsrfToken.acquire();

					HttpResponse<String> response = HttpClientHelper.get("api/java-endpoint/branches");

					ObjectMapper objectMapper = new ObjectMapper();

					@SuppressWarnings("unchecked")
					List<String> branchList = objectMapper.readValue(response.body(), List.class);

					this.ui.branchCodeComboBox.setItems(FXCollections.observableArrayList(branchList));

					App.hasBranchList = true;

				} catch (Exception e) {
					// reset
					this.ui.branchCodeComboBox.setValue(null);
					this.ui.branchCodeComboBox.setDisable(true);
					this.ui.branchCodeComboBox.getItems().clear();

					e.printStackTrace();
				}
			} else {
				this.ui.branchCodeComboBox.setValue(assignedBranch);
			}
		}
	}

	public void setUIActions(Stage stage) {
		// cancel
		this.ui.cancelButton.setOnAction(event -> {
			this.ui.clearCredentialsFields();

			System.out.println("Window has been closed.");
			stage.hide();
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

			/**
			 * Branch list has not been initialized and no selected branch This could mean
			 * the current thread is a fresh setup
			 * 
			 * step 1: verify the API URL by acquiring a csrf-token step 2: verify
			 * credentials provided step 3: finalize
			 */
			if (!App.hasBranchList && values.get("branch").toString().isEmpty()) {
				String url = values.get("url").toString();

				// step 1
				// set base URL
				HttpClientHelper.setBaseUrl(url);

				// acquire token
				String token = CsrfToken.acquire();

				if (token == null || token.isEmpty()) {
					throw new IllegalStateException("Invalid URL.");
				}

				FormData formData = new FormData();
				formData.append("username", values.get("username").toString());
				formData.append("password", values.get("password").toString());

				HttpResponse<String> response = HttpClientHelper.post("api/java-endpoint/verify", formData);

				System.out.println(response.body());

				// save the URL to the config file
//				config.set("api_url", url);

			} else {
				FormData formData = new FormData();

				for (Map.Entry<String, Object> entry : values.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();

					if (!key.equals("isValid")) {
						// save key-value except the optional isValid property
						formData.append(key, (String) value);
					}
				}

				System.out.println(formData.toString());
			}
		} catch (Exception e) {
			this.ui.showError("An error has occured", e.getMessage());
			e.printStackTrace();
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