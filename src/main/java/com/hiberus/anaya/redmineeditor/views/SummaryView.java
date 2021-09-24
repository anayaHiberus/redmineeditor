package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static com.hiberus.anaya.redmineeditor.utils.TimeUtils.formatHours;

/**
 * A simple label with info of the current selected day
 */
public class SummaryView extends InnerView {

    // ------------------------- views -------------------------

    @FXML
    private Label summary;

    // ------------------------- actions -------------------------

    /**
     * Display a 'loading' indicator
     */
    public void asLoading() {
        summary.setText("Loading...");
        summary.setBackground(null);
    }

    /**
     * Display an unselected state
     */
    public void unselected() {
        // if nothing selected, just ask
        summary.setText("Select day");
        summary.setBackground(null);
    }

    /**
     * Display a selected state
     *
     * @param date  current date
     * @param spent current spent hours
     */
    public void selected(LocalDate date, double spent) {
        // on something selected

        double expected = Schedule.getExpectedHours(date);

        // display info
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
}
