package com.hiberus.anaya.controller;

import com.hiberus.anaya.model.Model;
import com.hiberus.anaya.utils.ProgressDialog;
import com.hiberus.anaya.utils.hiberus.Schedule;
import com.hiberus.anaya.view.MainScreen;

import java.time.LocalDate;

public class Controller {

    // model
    private final Model model;

    // views
    private final MainScreen view;

    public Controller() {
        this.model = new Model();
        this.view = new MainScreen(this);
    }

    public void init() {
        view.configuration.setUser(model.getUser());
        reload();
    }

    public void reload() {
        // draw month
        view.calendar.drawMonth(model.getMonth());

        ProgressDialog.showProgress(view, () -> {

            // reload entries
            model.loadEntries();

            // check days
            for (int i = 1; i <= model.getMonth().lengthOfMonth(); ++i) {
                LocalDate day = model.getMonth().atDay(i);
                double expected = Schedule.getExpectedHours(day);
                double spent = model.getSpent(day);
                System.out.println(day + ": Expected " + expected + " obtained " + spent);

                // color
                view.calendar.setDaycolor(i, Schedule.getColor(expected, spent, day));
            }

        });
    }

    public void setUser(String user) {
        model.setUser(user);
    }

    public void changeMonth(int offset) {
        model.setMonth(model.getMonth().plusMonths(offset));

        reload();
    }
}
