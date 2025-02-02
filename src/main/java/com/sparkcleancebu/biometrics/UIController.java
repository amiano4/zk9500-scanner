package com.sparkcleancebu.biometrics;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkcleancebu.http_helper.HttpClientHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class UIController {
	@FXML ComboBox<String> branchCodeComboBox;
	
	@FXML TextField urlField;
	
	@FXML TextField usernameField;
	
	@FXML TextField passwordField;
	
	@FXML Button applyButton;
	
	@FXML Button cancelButton;
	
	@FXML Label footerText;
	
	@FXML
	public void initialize() {
		//
	}
	
	@FXML
	public void populateBranchCodeDropdown() throws Exception {
		HttpResponse<String> response = HttpClientHelper.get("api/java-endpoint/branches");
        
        ObjectMapper objectMapper = new ObjectMapper();

        @SuppressWarnings("unchecked")
		List<String> branchList = objectMapper.readValue(response.body(), List.class);
        
        branchCodeComboBox.setItems(FXCollections.observableArrayList(branchList));
	}
	
	@FXML 
	public void showError(String title, String content) {		
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(title);
		alert.setContentText(content);

		alert.showAndWait();
		
//		timer implementation
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
}