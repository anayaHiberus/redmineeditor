package com.hiberus.anaya.redmineeditor.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SettingsCtrl extends InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    TextField user;

    // ------------------------- reactions -------------------------

    @FXML
    void onChangedUser() {
        mainCtrl.onChangedUser(user.getText());
    }

    // ------------------------- actions -------------------------

    void setUser(String newUser) {
        user.setText(newUser);
    }
}
