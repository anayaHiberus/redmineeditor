module com.hiberus.anaya.redmineeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires kotlin.stdlib;
    requires org.json;
    // requires redmine.java.api;


    exports com.hiberus.anaya.redmineapi;
    exports com.hiberus.anaya.redmineeditor;
    exports com.hiberus.anaya.redmineeditor.views;
    exports com.hiberus.anaya.redmineeditor.utils;
    opens com.hiberus.anaya.redmineeditor.views to javafx.fxml;
    opens com.hiberus.anaya.redmineeditor.cells to javafx.fxml;
    exports com.hiberus.anaya.redmineeditor.cells;
}