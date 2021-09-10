package com.hiberus.anaya.UI;

import javax.swing.*;
import java.awt.*;

public class MainScreen extends JFrame {

    public final ConfigurationPanel configuration;
    public final CalendarPanel calendar;

    public MainScreen() throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Redmine editor");
        this.setSize(500, 500);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // configuration
        configuration = new ConfigurationPanel();
        this.add(configuration);

        // separator
        this.add(new JSeparator(SwingConstants.HORIZONTAL));
        this.add(new JSeparator(SwingConstants.HORIZONTAL));

        // calendar
        calendar = new CalendarPanel();
        this.add(calendar);


        setVisible(true);
    }
}
