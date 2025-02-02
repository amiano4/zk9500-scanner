package com.sparkcleancebu.biometrics;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.http.HttpResponse;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
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

    @Override
    public void start(Stage stage) throws IOException {    	
    	FXMLLoader loader = new FXMLLoader(App.class.getResource("UI.fxml"));
        Parent root = loader.load();
        
        HttpClientHelper.setBaseUrl("http://localhost:8000/");
        HttpClientHelper.registeredHeaders.add("Accept", "application/json");
        
        this.ui = loader.getController();
        
        String icon = "icon.png";
        
        setIcon(stage, icon);
        setUIActions(stage);

        reader = new FingerprintReader();
        reader.addReadEventListener(this);
        
        Scene scene = new Scene(root);
        stage.setTitle("Fingerprint Scanner App");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
    
    public void setUIActions(Stage stage) {
      ui.cancelButton.setOnAction(event -> {
    	  System.out.println("Window has been closed.");
    	  stage.hide();
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
    	reader.close();
    	System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }
}