module com.hiberus.anaya.redmineeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires kotlin.stdlib;
    requires org.json;


    exports com.hiberus.anaya.redmineeditor;
    exports com.hiberus.anaya.redmineeditor.controllers;
    exports com.hiberus.anaya.redmineeditor.utils;
    opens com.hiberus.anaya.redmineeditor.controllers to javafx.fxml;
}