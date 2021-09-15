package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * A view with editexts for editable data
 */
public class SettingsCtrl implements InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    TextField user;

    // ------------------------- properties -------------------------

    private ObservableProperty<String>.ObservedProperty userProperty;

    // ------------------------- init -------------------------

    @Override
    public void initCtrl(Model model) {
        // on new user, change the edittext
        userProperty = model.user.observeAndNotify(newUser -> user.setText(newUser));
    }

    // ------------------------- onActions -------------------------

    @FXML
    void onChangedUser() {
        // when user changes
        userProperty.set(user.getText());
    }

}
