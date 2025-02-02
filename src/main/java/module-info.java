module com.sparkcleancebu.biometrics {
    requires javafx.controls;
    requires javafx.fxml;
	requires com.dustinredmond.fxtrayicon;
	requires java.desktop;

    opens com.sparkcleancebu.biometrics to javafx.fxml;
    exports com.sparkcleancebu.biometrics;
}
