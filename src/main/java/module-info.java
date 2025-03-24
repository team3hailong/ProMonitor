module com.example.promonitor {
    requires javafx.fxml;
    requires java.desktop;

    requires org.controlsfx.controls;
    requires org.slf4j;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires com.google.gson;
    requires org.apache.pdfbox;
    requires org.jfree.jfreechart;
    requires javafx.media;
    requires AnimateFX;
    requires com.fasterxml.jackson.databind;
    requires com.jfoenix;
    requires de.jensd.fx.glyphs.fontawesome;

    opens com.promonitor to javafx.fxml;
    exports com.promonitor;

    opens com.promonitor.view to javafx.base, javafx.fxml, javafx.graphics;
    opens com.promonitor.util to com.google.gson, javafx.base, com.fasterxml.jackson.databind;
    opens com.promonitor.model to com.fasterxml.jackson.databind;

}