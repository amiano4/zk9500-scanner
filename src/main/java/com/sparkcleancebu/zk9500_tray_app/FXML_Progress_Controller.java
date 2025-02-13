package com.sparkcleancebu.zk9500_tray_app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

@SuppressWarnings("exports")
public class FXML_Progress_Controller {
	@FXML
	private Label progressLabel;

	@FXML
	public Label status;

	@FXML
	public ProgressBar progressBar;

	@FXML
	public void initialize() {
		progressBar.progressProperty().addListener((obs, oldValue, newValue) -> {
			Platform.runLater(() -> {
				double progress = newValue.doubleValue();
				String text = progress >= 1.0 ? "complete" : "in progress";
				int progressPercent = (int) (progress * 100);
				progressLabel.setText(progressPercent + "% " + text);
			});
		});
	}
}
