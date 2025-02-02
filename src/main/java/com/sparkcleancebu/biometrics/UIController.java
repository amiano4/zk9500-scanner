package com.sparkcleancebu.biometrics;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class UIController {
	@FXML ComboBox<String> branchCodeComboBox;
	
	@FXML TextField urlField;
	
	@FXML TextField usernameField;
	
	@FXML TextField passwordField;
	
	@FXML Button applyButton;
	
	@FXML Button cancelButton;
	
	@FXML
	public void initialize() {
		//
	}
}