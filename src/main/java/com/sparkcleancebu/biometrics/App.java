package com.sparkcleancebu.biometrics;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkcleancebu.http_helper.FormData;
import com.sparkcleancebu.http_helper.HttpClientHelper;
import com.sparkcleancebu.zk9500.FingerprintReader;
import com.sparkcleancebu.zk9500.ReadEvent;
import com.sparkcleancebu.zk9500.ReadEventListener;

/**
 * JavaFX App
 */
public class App extends Application implements ReadEventListener {
	private FingerprintReader reader;
	private UIController ui;
	private final String configPath = "config.json";
	private ConfigReader config;

    @Override
    public void start(Stage stage) throws IOException {    	
    	try {
    		config = new ConfigReader(configPath);
         	// config.set("api_url", "http://localhost:8000/"); // set base url
    		String baseUrl = config.get("api_url");
             
            HttpClientHelper.setBaseUrl(baseUrl);
            HttpClientHelper.registeredHeaders.add("Accept", "application/json");
    		
    		CsrfToken.acquire();
    		
    		FXMLLoader loader = new FXMLLoader(App.class.getResource("UI.fxml"));
            Parent root = loader.load();            
            
            this.ui = loader.getController();
            
            String icon = "icon.png";
            
            setIcon(stage, icon);
            setUIData();
            setUIActions(stage);

            this.reader = new FingerprintReader();
            this.reader.addReadEventListener(this);
            
            Scene scene = new Scene(root);
            stage.setTitle("Fingerprint Scanner App");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
    	} catch(Throwable e) {
    		e.printStackTrace();
    	}
    }
    
    public void setUIData() throws Exception {
        this.ui.urlField.setText(config.get("api_url"));
        this.ui.populateBranchCodeDropdown();
        
        String currentBranchCode = config.get("branch");
        
        this.ui.branchCodeComboBox.setValue(currentBranchCode);
    }
    
    public void setUIActions(Stage stage) {
    	// cancel
    	this.ui.cancelButton.setOnAction(event -> {
    		System.out.println("Window has been closed.");
    		stage.hide();
    	});
    	
    	// apply changes
    	this.ui.applyButton.setOnAction(event -> {
    		String url = this.ui.urlField.getText();
    		String branch = this.ui.branchCodeComboBox.getValue();
    		String username = this.ui.usernameField.getText();
    		String password = this.ui.passwordField.getText();
    		
    		
    	});
    }
    
    public void setIcon(Stage stage, String iconResource) {
    	 try {    		 
    		 stage.getIcons().add(new Image(getClass().getResourceAsStream(iconResource)));
    		 
    		 new FXTrayIcon.Builder(stage, getClass().getResource(iconResource))
             		.menuItem("Show", e-> {
             			stage.show();
             		})
              		.addExitMenuItem("Exit", e -> exitApp())
              		.show()
              		.build();
             
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
    	} catch(Exception e) {
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