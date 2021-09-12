package com.hiberus.anaya.view;

import com.hiberus.anaya.controller.Controller;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainScreen extends JFrame {

    public final ConfigurationPanel configuration;
    public final CalendarPanel calendar;

    public MainScreen(Controller controller) throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Redmine editor");
        this.setSize(500, 500);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // configuration
        configuration = new ConfigurationPanel(controller);
        this.add(configuration);

        // separator
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setBorder(new EmptyBorder(5, 0, 5, 0)); // TODO FIX
        this.add(separator);

        // calendar
        calendar = new CalendarPanel(controller);
        this.add(calendar);


        setVisible(true);
    }
}
