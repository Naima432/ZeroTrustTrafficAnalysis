module App {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.logging;

    opens edu.metrostate.gui to javafx.fxml;
    exports edu.metrostate.gui;
    exports edu.metrostate.model;
    exports edu.metrostate.monitor;
}