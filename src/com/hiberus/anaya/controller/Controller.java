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
            for (int day = 1; day <= model.getMonth().lengthOfMonth(); ++day) {
                LocalDate date = model.getMonth().atDay(day);
                double expected = Schedule.getExpectedHours(date);
                double spent = model.getSpent(date);
                System.out.println(date + ": Expected " + expected + " obtained " + spent);

                // color
                view.calendar.setDaycolor(day, Schedule.getColor(expected, spent, date));

                if (day == model.getDay()) {
                    view.summary.setInfo(date, spent);
                }
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

    public void selectDay(int day) {
        model.setDay(day);

        if (day == 0) {
            view.summary.clear();
        } else {
            LocalDate date = model.getMonth().atDay(day);
            view.summary.setInfo(date, model.getSpent(date));
        }
    }
}
