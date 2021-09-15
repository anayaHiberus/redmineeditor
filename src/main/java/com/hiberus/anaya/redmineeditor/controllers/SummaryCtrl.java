package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * A simple label with info of the current selected day
 */
public class SummaryCtrl implements InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    public Label summary;

    // ------------------------- init -------------------------

    @Override
    public void initCtrl(Model model) {
        // when day, entries or month changes, update label
        model.day.observe(day -> update(model.month.get(), day, model.hour_entries.get()));
        model.hour_entries.observe(entries -> update(model.month.get(), model.day.get(), entries));
        model.month.observeAndNotify(month -> update(month, model.day.get(), model.hour_entries.get()));
    }


    // ------------------------- actions -------------------------

    private void update(YearMonth month, int day, Model.TimeEntries entries) {
        if (entries.isLoading()) {
            // while loading, inform
            summary.setText("Loading...");
            summary.setBackground(null);
        } else if (day == 0) {
            // if nothing selected, just ask
            summary.setText("Select day");
            summary.setBackground(null);
        } else {
            // on something selected
            LocalDate date = month.atDay(day);
            double spent = entries.getSpent(date);
            double expected = Schedule.getExpectedHours(date);

            // display info
            summary.setText(
                    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                            + " --- Hours: " + spent + "/" + expected
                            + (spent < expected ? " --- Missing: " + (expected - spent)
                            : spent > expected ? " --- Extra: " + (spent - expected)
                            : spent == expected && expected != 0 ? " --- OK"
                            : "")
            );

            // and change color
            JavaFXUtils.setBackgroundColor(summary, Schedule.getColor(expected, spent, date));
        }
    }
}
