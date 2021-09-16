package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.SimpleChangeListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.util.Objects;

/**
 * A view with editexts for editable data
 */
public class SettingsCtrl implements InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    TextField user;

    // ------------------------- properties -------------------------

    private SimpleStringProperty userProperty;

    // ------------------------- init -------------------------

    @Override
    public void initCtrl(Model model) {
        // on new user, change the edittext
        userProperty = SimpleChangeListener.register(model.user, newValue -> {
            if (!Objects.equals(newValue, user.getText()))
                user.setText(newValue);
        });
    }

    // ------------------------- onActions -------------------------

    @FXML
    void onChangedUser() {
        // when user changes
        userProperty.set(user.getText());
    }

}
