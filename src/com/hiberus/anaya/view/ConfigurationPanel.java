package com.hiberus.anaya.view;

import com.hiberus.anaya.controller.Controller;
import com.hiberus.anaya.utils.JTextUtils;

import javax.swing.*;

public class ConfigurationPanel extends JPanel {

    private final JTextField user_field;
//    private final JTextField token;

    public ConfigurationPanel(Controller controller) {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // user
        this.add(new JLabel("User:"));
        user_field = new JTextField();
        JTextUtils.addChangeListener(user_field, e -> controller.setUser(user_field.getText()));
        this.add(user_field);

//        // token
//        this.add(new JLabel("Token:"));
//        token = new JTextField("89c99d1d6adbfebbd08d8e7960b15f282159dccc");
//        this.add(token);

    }

    public void setUser(String user) {
        user_field.setText(user);
    }

}
