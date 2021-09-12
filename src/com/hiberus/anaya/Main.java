package com.hiberus.anaya;

import com.hiberus.anaya.controller.Controller;

import javax.swing.*;

public class Main {
    public static void main(String[] arguments) {
        JFrame.setDefaultLookAndFeelDecorated(true);

        new Controller().init();
    }
}
