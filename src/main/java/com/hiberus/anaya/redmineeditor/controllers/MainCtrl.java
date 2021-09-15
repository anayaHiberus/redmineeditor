package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;

public class MainCtrl {

    // ------------------------- model -------------------------

    private final Model model = new Model();

    // ------------------------- subcontrollers -------------------------

    @FXML
    SettingsCtrl settingsController;
    @FXML
    CalendarCtrl calendarController;
    @FXML
    SummaryCtrl summaryController;
    @FXML
    ActionsCtrl actionsController;

    // ------------------------- reactions -------------------------

    @FXML
    void initialize() {
        // init
        calendarController.setMainController(this);
        settingsController.setMainController(this);
        summaryController.setMainController(this);
        actionsController.setMainController(this);

        settingsController.init(model);

        // load
//        settingsController.setUser(model.getUser());
        reload();
    }

//    public void onChangedUser(String user) {
//        model.setUser(user);
//    }

    public void onDaySelected(int day) {
        calendarController.selectDay(day);
        model.setDay(day);

        LocalDate date = model.getMonth().atDay(day);
        summaryController.set(date, model.getSpent(date));

    }

    public void onNewMonth(YearMonth month) {
        model.setMonth(month);
        reload();
    }

    // ------------------------- actions -------------------------

    public void reload() {
        // draw month
        calendarController.drawMonth(model.getMonth());

        // prepare indicator
        summaryController.loading();

        // reload entries
        progressLoading(model::loadEntries, () -> {

            // check days
            for (int day = 1; day <= model.getMonth().lengthOfMonth(); ++day) {
                LocalDate date = model.getMonth().atDay(day);
                double expected = Schedule.getExpectedHours(date);
                double spent = model.getSpent(date);
                System.out.println(date + ": Expected " + expected + " obtained " + spent);

                // color day
                calendarController.setDayColor(day, Schedule.getColor(expected, spent, date));

                // set summary
                if (day == model.getDay()) {
                    summaryController.set(date, spent);
                }
            }

            // select day
            if (model.getDay() != 0) {
                calendarController.selectDay(model.getDay());
            }
        });
    }

    // ------------------------- loading -------------------------

    @FXML
    ProgressIndicator progress;

    @FXML
    VBox parent;

    public void progressLoading(Runnable background, Runnable foreground) {
        progress.setVisible(true);
        parent.setDisable(true);
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            background.run();
            Platform.runLater(() -> {
                foreground.run();
                progress.setVisible(false);
                parent.setDisable(false);
            });
        }).start();
    }

}
