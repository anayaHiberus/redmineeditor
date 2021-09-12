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
        addBox(configuration);

        // separator
        addBox(new JSeparator(SwingConstants.HORIZONTAL));

        // calendar
        calendar = new CalendarPanel(controller);
        addBox(calendar);


        setVisible(true);
    }

    private void addBox(Component component) {
        JPanel jPanel = new JPanel();
        jPanel.setBorder(new EmptyBorder(5, 5, 5, 5)); // TODO FIX
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.X_AXIS));
        jPanel.add(component);
        this.add(jPanel);
    }

}
