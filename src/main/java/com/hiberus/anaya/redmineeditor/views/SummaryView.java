package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * A simple label with info of the current selected day
 */
public class SummaryView extends InnerView {

    // ------------------------- views -------------------------

    @FXML
    public Label summary;

    // ------------------------- init -------------------------

    @Override
    public void initView() {
    }


    // ------------------------- actions -------------------------

    public void asLoading() {
        // while loading, inform
        summary.setText("Loading...");
        summary.setBackground(null);
    }

    public void unselected() {
        // if nothing selected, just ask
        summary.setText("Select day");
        summary.setBackground(null);
    }

    public void selected(LocalDate date, double spent) {
        // on something selected

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
