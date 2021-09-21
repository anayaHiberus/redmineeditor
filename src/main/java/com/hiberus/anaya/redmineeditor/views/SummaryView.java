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
        // when day, entries or month changes, update label
        model.onChanges(this::update);
    }


    // ------------------------- actions -------------------------

    private void update() {
        if (model.time_entries.isLoading()) {
            // while loading, inform
            summary.setText("Loading...");
            summary.setBackground(null);
            return;
        }
        LocalDate date = model.getDate();
        if (date == null) {
            // if nothing selected, just ask
            summary.setText("Select day");
            summary.setBackground(null);
        } else {
            // on something selected
            double spent = model.time_entries.getSpent(date);
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
