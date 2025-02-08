module com.sparkcleancebu.zk9500_tray_app {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires java.desktop;
	requires com.dustinredmond.fxtrayicon;
	requires org.json;
	requires jakarta.websocket.client;
	requires java.net.http;
	requires ZKFingerReader;
	requires com.auth0.jwt;
	requires javafx.base;
	requires httpflux;

	opens com.sparkcleancebu.zk9500_tray_app to javafx.fxml;

	opens com.sparkcleancebu.zk9500_tray_app.controllers to javafx.fxml;

	exports com.sparkcleancebu.zk9500_tray_app;
}
