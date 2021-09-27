module com.hiberus.anaya.redmineeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires org.json;
    requires java.desktop;
    // requires redmine.java.api;


    exports com.hiberus.anaya.redmineapi;
    exports com.hiberus.anaya.redmineeditor;
    exports com.hiberus.anaya.redmineeditor.controllers;
    exports com.hiberus.anaya.redmineeditor.utils;
    opens com.hiberus.anaya.redmineeditor.controllers to javafx.fxml;
    opens com.hiberus.anaya.redmineeditor.cells to javafx.fxml;
    exports com.hiberus.anaya.redmineeditor.cells;
}