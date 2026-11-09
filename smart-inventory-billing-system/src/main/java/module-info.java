
module com.inventory {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.pdfbox;
    requires java.desktop;

    opens com.inventory to javafx.fxml;
    opens com.inventory.model to javafx.base;
    exports com.inventory;
}
