module App {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    opens edu.metrostate.gui to javafx.fxml;
    exports edu.metrostate.gui;

}