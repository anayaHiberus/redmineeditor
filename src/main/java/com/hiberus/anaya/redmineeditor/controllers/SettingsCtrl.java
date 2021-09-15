package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SettingsCtrl implements InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    TextField user;

    private ObservableProperty<String>.ObservedProperty userProperty;

    @Override
    public void init(Model model) {
        userProperty = model.user.observeAndNotify(newUser -> user.setText(newUser));
    }

    // ------------------------- reactions -------------------------

    @FXML
    void onChangedUser() {
        userProperty.set(user.getText());
    }

}
