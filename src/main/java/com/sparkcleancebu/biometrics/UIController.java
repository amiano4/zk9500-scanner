package com.sparkcleancebu.biometrics;

import java.util.HashMap;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class UIController {
	@FXML
	ComboBox<String> branchCodeComboBox;

	@FXML
	TextField urlField;

	@FXML
	TextField usernameField;

	@FXML
	TextField passwordField;

	@FXML
	Button applyButton;

	@FXML
	Button cancelButton;

	@FXML
	Label footerText;

	@FXML
	public void handleUrlFieldAction() {
		HashMap<String, Object> values = getValues();

		if (values.get("url").toString().isEmpty()) {
			// disable dropdown if API URL field is empty
			branchCodeComboBox.setDisable(true);
		} else if (branchCodeComboBox.isDisabled() && App.hasBranchList) {
			branchCodeComboBox.setDisable(false);
		}

		// disable button if any of the inputs is empty
		applyButton.setDisable((boolean) values.get("isValid") == false);
	}

	@FXML
	public void toggleApplyButton() {
		// disable button if any of the inputs is empty
		applyButton.setDisable((boolean) getValues().get("isValid") == false);
	}

	@FXML
	public void showError(String title, String content) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(title);
		alert.setContentText(content);

		Image errorIcon = new Image(getClass().getResourceAsStream("error.png"));
		Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(errorIcon);

		Image errorGraphic = new Image(getClass().getResourceAsStream("error-graphic.png"));
		ImageView imageView = new ImageView(errorGraphic);
		imageView.setFitWidth(36);
		imageView.setFitHeight(36);
		imageView.setPreserveRatio(true);

		alert.getDialogPane().setGraphic(imageView);

		alert.showAndWait();

//		// timer implementation
//		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//		
//		Runnable task = new Runnable() {
//			@Override
//			public void run() {
//		
//			}
//		};
//		
//		scheduler.schedule(task, 5, TimeUnit.SECONDS);
	}

	public void successAlert(String title, String content) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Success");
		alert.setHeaderText(title);
		alert.setContentText(content);

		Image errorIcon = new Image(getClass().getResourceAsStream("success.png"));
		Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(errorIcon);

		Image errorGraphic = new Image(getClass().getResourceAsStream("success-graphic.png"));
		ImageView imageView = new ImageView(errorGraphic);
		imageView.setFitWidth(36);
		imageView.setFitHeight(36);
		imageView.setPreserveRatio(true);

		alert.getDialogPane().setGraphic(imageView);

		alert.showAndWait();

	}

	public HashMap<String, Object> getValues() {
		String url = urlField.getText() != null ? urlField.getText().trim() : "";
		String branch = branchCodeComboBox.getValue() != null ? branchCodeComboBox.getValue().trim() : "";
		String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
		String password = passwordField.getText() != null ? passwordField.getText().trim() : "";

		boolean allEmpty = url.isEmpty() || username.isEmpty() || password.isEmpty();

		// apply branch check if branches list has been acquired
		if (App.hasBranchList) {
			allEmpty = allEmpty || branch.isEmpty();
		}

		HashMap<String, Object> values = new HashMap<>();

		values.put("url", url);
		values.put("branch", branch);
		values.put("username", username);
		values.put("password", password);
		values.put("isValid", !allEmpty);

		return values;
	}

	public void clearCredentialsFields() {
		usernameField.setText(null);
		passwordField.setText(null);
		applyButton.setDisable(true);
	}

	public void resetAndDisableBranchCode() {
		branchCodeComboBox.setValue(null);
		branchCodeComboBox.setDisable(true);
		branchCodeComboBox.getItems().clear();
	}
}