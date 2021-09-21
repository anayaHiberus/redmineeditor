package com.hiberus.anaya.redmineeditor.views;

import javafx.fxml.FXML;

/**
 * A list of action buttons
 */
public class ActionsView extends InnerView {


    @Override
    public void initView() {
        // nothing to initialize
    }

    // ------------------------- onActions -------------------------

    @FXML
    void reload() {
        // press the reload button to reload the data
        controller.reload();
    }

    @FXML
    void upload() {
        controller.upload();
    }
}
