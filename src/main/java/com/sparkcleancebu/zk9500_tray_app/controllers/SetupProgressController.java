package com.sparkcleancebu.zk9500_tray_app.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class SetupProgressController {
	@FXML
	private Label logsDisplay;

	@FXML
	ProgressBar progressBar;

	@FXML
	private Label percentageIndicator;

	@FXML
	private Button cancelBtn;

	@FXML
	private Button hideWindowBtn;

	@FXML
	public void initialize() {
		this.log(null);
		this.updateProgressIndicator(0);

		progressBar.progressProperty().addListener((observable, oldValue, newValue) -> {
			int percentage = (int) (newValue.floatValue() * 100);
			this.updateProgressIndicator(percentage);
		});
	}

	@FXML
	public void exitSetup() {
		try {
//			App.cancelSetup();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

	public void updateProgressIndicator(int value) {
		Platform.runLater(() -> {
			this.percentageIndicator.setText(String.valueOf(value) + "%");
		});
	}

	public void log(String content) {
		Platform.runLater(() -> {
			this.logsDisplay.setText(content);
		});
	}
}
