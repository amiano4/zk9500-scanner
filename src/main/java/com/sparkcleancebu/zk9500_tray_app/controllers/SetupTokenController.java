package com.sparkcleancebu.zk9500_tray_app.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class SetupTokenController {
	@FXML
	private TextArea tokenTextArea;

	@FXML
	private Button clearButton;

	@FXML
	private Button proceedButton;

	@FXML
	public void initialize() {
		this.proceedButton.setDisable(false);
	}

	@FXML
	public void handleClearButtonClick() {
		this.tokenTextArea.setText("");
		this.proceedButton.setDisable(false);
	}

	@FXML
	public void handleTokenTextAreaInput() {
		String value = this.tokenTextArea.getText();
		// disable proceed button if empty or null
		this.proceedButton.setDisable(value == null || value.trim().isEmpty());
	}

	@FXML
	public void handleProceedButtonClick() {
		try {
			String value = this.tokenTextArea.getText();
			if (value != null && !value.trim().isEmpty()) {
				this.proceedButton.setDisable(true);
//				App.setup.verify(value); // verify token then start the process
				this.proceedButton.setDisable(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
//			AlertDialog.error("An error has occured", e.getLocalizedMessage());
		}
	}
}
