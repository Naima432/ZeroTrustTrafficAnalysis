module App {
    requires javafx.controls;
    requires javafx.fxml;

    opens edu.metrostate.gui to javafx.fxml;
    exports edu.metrostate.gui;
    exports edu.metrostate.model;
    exports edu.metrostate.monitor;
}