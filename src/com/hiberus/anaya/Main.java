package com.hiberus.anaya;

import com.hiberus.anaya.UI.CalendarPanel;
import com.hiberus.anaya.UI.MainScreen;
import com.hiberus.anaya.hiberus.Schedule;
import com.hiberus.anaya.redmine.Data;
import com.hiberus.anaya.utils.IsoDate;

import javax.swing.*;
import java.util.Calendar;

public class Main implements CalendarPanel.Listener {
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
        CalendarPanel calendar = screen.calendar;

        Calendar from = calendar.getMonth();
        Calendar to = (Calendar) from.clone();
        to.set(Calendar.DAY_OF_MONTH, from.getActualMaximum(Calendar.DAY_OF_MONTH));

        data.loadEntries(screen.configuration.getUser(), from, to);

        while (from.compareTo(to) <= 0) {

            double spent = data.getSpent(from);
            double expected = Schedule.getExpectedHours(from);

            System.out.println(IsoDate.format(from) + ": Expected " + expected + " obtained " + spent);
            calendar.setDaycolor(from.get(Calendar.DAY_OF_MONTH), Schedule.getColor(expected, spent, from));

            from.add(Calendar.DAY_OF_MONTH, 1);
        }

    }

    @Override
    public void onNewDay() {

    }
}
