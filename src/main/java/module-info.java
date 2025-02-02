module com.sparkcleancebu.biometrics {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.sparkcleancebu.biometrics to javafx.fxml;
    exports com.sparkcleancebu.biometrics;
}
