package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class SummaryCtrl implements InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    public Label summary;

    // ------------------------- init -------------------------

    @Override
    public void init(Model model) {
        ObservableProperty.OnChangedListener listener = newValue -> {
            LocalDate day = model.month.get().atDay(model.day.get());
            if (model.hour_entries.get().isLoading())
                loading();
            else
                set(day, model.hour_entries.get().getSpent(day));
        };
        model.day.registerObserver(listener);
        model.hour_entries.registerObserver(listener);
        model.month.registerObserver(listener);
    }


    // ------------------------- actions -------------------------

    private void set(LocalDate day, double spent) {
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

    private void loading() {
        summary.setText("Loading...");
        summary.setBackground(null);
    }
}
