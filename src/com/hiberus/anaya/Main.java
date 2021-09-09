package com.hiberus.anaya;

import com.hiberus.anaya.UI.MainScreen;
import com.hiberus.anaya.UI.SwingCalendar;
import com.hiberus.anaya.utils.IsoDate;
import com.hiberus.anaya.utils.JsonReader;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

public class Main implements SwingCalendar.Listener {
    public static void main(String[] arguments) {
        new Main();
    }

    //-------------------------------------

    private final MainScreen screen;

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

        try {
            JSONArray entries = JsonReader.fromUrl("https://redmine.hiberus.com/redmine/time_entries.json?utf8=✓&"
                    + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=" + "me"
                    + "&f[]=spent_on&op[spent_on]=><&v[spent_on][]=" + IsoDate.format(from) + "&v[spent_on][]=" + IsoDate.format(to)
                    + "&key=89c99d1d6adbfebbd08d8e7960b15f282159dccc"
            ).getJSONArray("time_entries");

            Calendar now = Calendar.getInstance();

            while (from.compareTo(to) <= 0 && from.compareTo(now) < 0) {
                int day = from.get(Calendar.DAY_OF_MONTH);

                double spent = 0;
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    if (Objects.equals(entry.getString("spent_on"), IsoDate.format(from)))
                        spent += entry.getDouble("hours");
                }

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

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onNewDay() {

    }
}
