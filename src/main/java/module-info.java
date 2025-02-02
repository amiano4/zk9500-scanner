module com.sparkcleancebu.biometrics {
    requires javafx.controls;
    requires javafx.fxml;
	requires com.dustinredmond.fxtrayicon;
	requires java.desktop;
	requires ZKFingerReader;
	requires java.net.http;

    opens com.sparkcleancebu.biometrics to javafx.fxml;
    exports com.sparkcleancebu.biometrics;
}
