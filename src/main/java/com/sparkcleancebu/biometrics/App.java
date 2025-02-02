package com.sparkcleancebu.biometrics;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

import com.dustinredmond.fxtrayicon.FXTrayIcon;

/**
 * JavaFX App
 */
public class App extends Application {

	private UIController ui;

    @Override
    public void start(Stage stage) throws IOException {    	
    	FXMLLoader loader = new FXMLLoader(App.class.getResource("UI.fxml"));
        Parent root = loader.load();
        
        this.ui = loader.getController();
        
        String icon = "icon.png";
        
        setIcon(stage, icon);
        
        this.ui.applyButton.setOnAction(event -> {
	        System.out.println("Apply button clicked in starrt");
	    });
        
        Scene scene = new Scene(root);
        stage.setTitle("Fingerprint Scanner App");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
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
    
    public void exitApp() {
    	System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }

}