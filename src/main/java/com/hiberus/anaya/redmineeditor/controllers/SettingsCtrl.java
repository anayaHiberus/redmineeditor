package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.IntelligentProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SettingsCtrl extends InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    TextField user;

    private IntelligentProperty<String> userProperty;


    public void init(Model model) {
        userProperty = new IntelligentProperty<>(model.user, newUser -> user.setText(newUser));
    }

    // ------------------------- reactions -------------------------

    @FXML
    void onChangedUser() {
        userProperty.set(user.getText());
    }

}
