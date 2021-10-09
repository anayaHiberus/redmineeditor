package com.hiberus.anaya.redmineeditor.components;

import com.hiberus.anaya.redmineeditor.model.Model;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * A list of action buttons
 */
public class ActionsComponent extends BaseComponent {


    /* ------------------------- onActions ------------------------- */

    @Override
    void init() {
        // nothing to initialize
    }

    @FXML
    private void reload() {
        // press the refresh button to reload the data, asks if there are changes

        // if there are changes, ask first
        controller.runForeground(model -> {
            if (model.hasChanges()) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "There are unsaved changes, do you want to lose them and reload?",
                        ButtonType.YES, ButtonType.CANCEL
                );
                alert.setHeaderText("Unsaved changes");
                alert.setTitle("Warning");
                alert.showAndWait();

                // don't reload if the user didn't accept
                if (alert.getResult() != ButtonType.YES) {
                    return;
                }
            }
            forceReload();
        });
    }

    private void forceReload() {
        // reloads the data without any check
        controller.runBackground(editableModel -> {
            // clear data
            editableModel.clearAll();
            // notify so that the ui is updated at this step
            controller.fireChanges();
            // load month
            editableModel.loadMonth();
        });
    }

    @FXML
    private void upload() {
        controller.runBackground(
                // press the save button to upload data
                Model.Editor::uploadEntries,
                // then reload if everything was ok
                ok -> {
                    if (ok) forceReload();
                }
        );
    }
}
