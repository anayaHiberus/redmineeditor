package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A list of action buttons
 */
public class ActionsCtrl extends InnerCtrl {


    @Override
    public void initCtrl() {
        // nothing to initialize
    }

    // ------------------------- onActions -------------------------

    @FXML
    void reload() {
        // press the reload button to reload the data
        model.hour_entries.clear();
        model.hour_entries.loadMonth(model.getMonth());
    }

    @FXML
    void update() {
        AtomicBoolean ok = new AtomicBoolean();
        JavaFXUtils.runInBackground(() -> {
            // update changes
            ok.set(model.hour_entries.update());
        }, () -> {
            if (!ok.get()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Network error");
                alert.setContentText("Can't upload content to Redmine. Try again later");
                alert.showAndWait();
            }
            // and reload
            reload();
        });
    }
}
