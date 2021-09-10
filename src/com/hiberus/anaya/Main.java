package com.hiberus.anaya;

import com.hiberus.anaya.UI.MainScreen;
import com.hiberus.anaya.UI.SwingCalendar;
import com.hiberus.anaya.redmine.Data;
import com.hiberus.anaya.utils.IsoDate;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;

public class Main implements SwingCalendar.Listener {
    public static void main(String[] arguments) {
        new Main();
    }

    //-------------------------------------

    private final MainScreen screen;
    private final Data data = new Data();

    public Main() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        screen = new MainScreen();
        screen.calendar.setListener(this);

        onNewMonth();
    }

    @Override
    public void onNewMonth() {
        SwingCalendar calendar = screen.calendar;

        Calendar from = calendar.getMonth();
        Calendar to = (Calendar) from.clone();
        to.set(Calendar.DAY_OF_MONTH, from.getActualMaximum(Calendar.DAY_OF_MONTH));

        data.loadEntries(screen.configuration.getUser(), from, to);

        Calendar now = Calendar.getInstance();

        while (from.compareTo(to) <= 0 && from.compareTo(now) < 0) {
            int day = from.get(Calendar.DAY_OF_MONTH);

            double spent = data.getSpent(from);

            double expected = new double[]{-1, 0, 8.5, 8.5, 8.5, 8.5, 7, 0}[from.get(Calendar.DAY_OF_WEEK)];

            System.out.println(IsoDate.format(from) + ": Expected " + expected + " obtained " + spent);

            if (expected == spent) {
                if (expected != 0)
                    calendar.setDaycolor(day, Color.GREEN);
            } else {
                if (IsoDate.format(from).equals(IsoDate.format(now))) {
                    calendar.setDaycolor(day, Color.orange);
                } else {
                    calendar.setDaycolor(day, Color.red);
                }
            }

            from.add(Calendar.DAY_OF_MONTH, 1);
        }

    }

    @Override
    public void onNewDay() {

    }
}
