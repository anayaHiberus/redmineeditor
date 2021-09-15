package com.hiberus.anaya.redmineeditor.controllers;

import javafx.fxml.FXML;

public class ActionsCtrl extends InnerCtrl {

    // ------------------------- reactions -------------------------

    @FXML
    void onReload() {
        mainCtrl.reload();
    }
}
