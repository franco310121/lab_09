module com.example.cliente {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires jdk.jdi;
    requires java.sql;
    requires java.desktop;
    requires org.json;

    opens com.example.cliente to javafx.fxml;
    exports com.example.cliente;
}