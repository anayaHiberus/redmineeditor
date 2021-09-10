package com.hiberus.anaya.UI;

import javax.swing.*;

public class ConfigurationPanel extends JPanel {

    private final JTextField user;
//    private final JTextField token;

    public ConfigurationPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // user
        this.add(new JLabel("User:"));
        user = new JTextField("me");
        this.add(user);

//        // token
//        this.add(new JLabel("Token:"));
//        token = new JTextField("89c99d1d6adbfebbd08d8e7960b15f282159dccc");
//        this.add(token);

    }

    //---------------------

    public String getUser() {
        return user.getText();
    }

//    public String getToken() {
//        return token.getText();
//    }

}
