package com.hiberus.anaya.UI;

import javax.swing.*;
import java.awt.*;

public class MainScreen extends JFrame {

    public final Configuration configuration;
    public final SwingCalendar calendar;

    public MainScreen() throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Redmine editor");
        this.setSize(500, 500);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // configuration
        configuration = new Configuration();
        this.add(configuration);

        // separator
        this.add(new JSeparator(SwingConstants.HORIZONTAL));
        this.add(new JSeparator(SwingConstants.HORIZONTAL));

        // calendar
        calendar = new SwingCalendar();
        this.add(calendar);


        setVisible(true);
    }
}
