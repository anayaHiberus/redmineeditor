package com.hiberus.anaya.redmineeditor.views;

import javafx.fxml.FXML;

/**
 * A list of action buttons
 */
public class ActionsView extends InnerView {

    // ------------------------- onActions -------------------------

    @FXML
    void reload() {
        // press the refresh button to reload the data
        controller.reload();
    }

    @FXML
    void upload() {
        // press the save button to upload data
        controller.upload();
    }
}
