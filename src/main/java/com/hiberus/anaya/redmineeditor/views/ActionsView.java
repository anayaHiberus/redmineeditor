package com.hiberus.anaya.redmineeditor.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * A list of action buttons
 */
public class ActionsView extends InnerView {


    /* ------------------------- onActions ------------------------- */

    @Override
    void init() {
        // nothing to initialize
    }

    @FXML
    private void reload() {
        // press the refresh button to reload the data

        // if there are changes, ask first
        if (model.hasChanges()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "There are unsaved changes, do you want to lose them and reload?",
                    ButtonType.YES, ButtonType.CANCEL);
            alert.setHeaderText("Unsaved changes");
            alert.setTitle("Warning");
            alert.showAndWait();
            if (alert.getResult() != ButtonType.YES) {
                // cancel if the user chose so
                return;
            }
        }

        // clear data
        model.clearAll();

        // load month
        inBackground(model::loadMonth);
    }

    @FXML
    private void upload() {
        // press the save button to upload data
        inBackground(model::uploadEntries, () -> {
            // then reload
            model.clearAll(); // clear first so that reload don't notify of unsaved changes
            reload();
        });
    }
}
