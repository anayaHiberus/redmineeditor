package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class SummaryCtrl extends InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    public Label summary;

    // ------------------------- actions -------------------------

    public void set(LocalDate day, double spent) {
        double expected = Schedule.getExpectedHours(day);
        summary.setText(
                day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                        + " --- Hours: " + spent + "/" + expected
                        + (spent < expected ? " --- Missing: " + (expected - spent)
                        : spent > expected ? " --- Extra: " + (spent - expected)
                        : spent == expected && expected != 0 ? " --- OK"
                        : "")
        );
        JavaFXUtils.setBackgroundColor(summary, Schedule.getColor(expected, spent, day));
    }

    public void loading() {
        summary.setText("Loading...");
        summary.setBackground(null);
    }
}
