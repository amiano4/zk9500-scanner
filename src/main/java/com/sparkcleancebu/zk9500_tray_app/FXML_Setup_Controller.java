package com.sparkcleancebu.zk9500_tray_app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class FXML_Setup_Controller {
	@FXML
	private TextField scannerIDField;

	@FXML
	private Button startSetupBtn;

	@FXML
	public void initialize() {
		startSetupBtn.setDisable(true);
	}

	@FXML
	public void handleStartSetupBtnClick() {
		System.out.println("auth");

		try {
			String id = scannerIDField.getText();
			App.authenticator.start(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void handleScannerIDFieldInput() {
		String id = scannerIDField.getText();
		startSetupBtn.setDisable(id == null || id.trim().isEmpty());
	}
}
