package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Set;

import static com.hiberus.anaya.redmineeditor.utils.TimeUtils.formatHours;

/**
 * A simple label with info of the current selected day
 */
public class SummaryCtrl extends InnerCtrl {

    /* ------------------------- views ------------------------- */

    @FXML
    private Label summary; // the label

    /* ------------------------- actions ------------------------- */


    @Override
    void init() {
        // when month, day, hours or the loading state changes, update
        model.notificator.register(Set.of(Model.Events.Month, Model.Events.Day, Model.Events.Hours, Model.Events.Loading), () -> {
            if (model.isLoading()) {
                // while loading, notify user
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
                // on something selected, display info
                double spent = model.getSpent(date);
                double expected = Schedule.getExpectedHours(date);
                summary.setText(
                        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                                + " --- Time: " + formatHours(spent) + "/" + formatHours(expected)
                                + (spent < expected ? " --- Missing: " + formatHours(expected - spent)
                                : spent > expected ? " --- Extra: " + formatHours(spent - expected)
                                : spent == expected && expected != 0 ? " --- OK"
                                : "")
                );

                // and change color
                JavaFXUtils.setBackgroundColor(summary, Schedule.getColor(expected, spent, date));
            }
        });
    }
}
