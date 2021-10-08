package com.hiberus.anaya.redmineeditor.components;

import com.hiberus.anaya.redmineeditor.utils.FXUtils;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Set;

import static com.hiberus.anaya.redmineeditor.Model.ModelEditor.Events.*;
import static com.hiberus.anaya.redmineeditor.utils.TimeUtils.formatHours;

/**
 * A simple label with info of the current selected day
 */
public class SummaryComponent extends BaseComponent {

    /* ------------------------- views ------------------------- */

    @FXML
    private Label summary; // the label

    /* ------------------------- actions ------------------------- */


    @Override
    void init() {
        // when month, day, hours or the loading state changes, update
        controller.register(Set.of(Month, Day, Hours, Loading), model -> {
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
                FXUtils.setBackgroundColor(summary, Schedule.getColor(expected, spent, date));
            }
        });
    }
}
