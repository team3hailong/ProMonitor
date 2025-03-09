module com.example.promonitor {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.slf4j;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires org.jfree.jfreechart;
    requires javafx.media;


    opens com.promonitor to javafx.fxml;
    exports com.promonitor;
}