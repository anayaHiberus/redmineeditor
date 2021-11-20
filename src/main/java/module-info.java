module com.hiberus.anaya.redmineeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires org.json;
    requires java.desktop;
    requires kotlin.stdlib;
    requires java.prefs;
    // requires redmine.java.api;


    exports com.hiberus.anaya.redmineapi;
    exports com.hiberus.anaya.redmineeditor;
    exports com.hiberus.anaya.redmineeditor.components;
    exports com.hiberus.anaya.redmineeditor.controller;
    exports com.hiberus.anaya.redmineeditor.model;
    exports com.hiberus.anaya.redmineeditor.utils;

    opens com.hiberus.anaya.redmineeditor.components to javafx.fxml;
    opens com.hiberus.anaya.redmineeditor.settings to javafx.fxml;
}