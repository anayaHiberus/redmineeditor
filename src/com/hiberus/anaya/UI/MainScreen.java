package com.hiberus.anaya.UI;

import javax.swing.*;
import java.awt.*;

public class MainScreen extends JFrame {

    public final SwingCalendar calendar;

    public MainScreen() throws HeadlessException {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Redmine editor");
        setSize(300, 200);

        calendar = new SwingCalendar();
        add(calendar);

        setVisible(true);
    }
}
