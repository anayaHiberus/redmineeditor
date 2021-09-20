package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Locale;

/**
 * A calendar view with colored days
 */
public class CalendarCtrl extends InnerCtrl {

    // ------------------------- properties -------------------------

    private final Label[] days = new Label[31]; // for coloring days
    private int selected = -1;

    // ------------------------- views -------------------------

    @FXML
    Label calendarLabel;
    @FXML
    GridPane calendar;

    // ------------------------- init -------------------------

    @FXML
    void initialize() {
        for (DayOfWeek field : DayOfWeek.values()) {
            // create the header
            calendar.add(JavaFXUtils.getCenteredLabel(field.getDisplayName(TextStyle.SHORT, Locale.getDefault())), field.getValue() - 1, 0);
        }
    }

    @Override
    public void initCtrl() {
        model.onChanges(() -> {
            // month changed, draw new and color days
            drawMonth(model.getMonth());
            colorDays(model.hour_entries);

            // day changed, select new
            selectDay(model.getDay());

            // hours changed, color days again
            colorDays(model.hour_entries);
        });
    }

    // ------------------------- onActions -------------------------

    @FXML
    void onNextMonth() {
        // set next month
        model.setMonth(model.getMonth().plusMonths(1));
    }

    @FXML
    void onPreviousMonth() {
        // set previous month
        model.setMonth(model.getMonth().minusMonths(1));
    }

    // ------------------------- actions -------------------------

    private void colorDays(Model.TimeEntries entries) {
        // skip if loading
        if (entries.isLoading()) return;

        for (int day = 1; day <= model.getMonth().lengthOfMonth(); ++day) {
            // foreach day of the month
            LocalDate date = model.getMonth().atDay(day);
            double expected = Schedule.getExpectedHours(date);
            double spent = entries.getSpent(date);

            // color the day
            JavaFXUtils.setBackgroundColor(days[day - 1], Schedule.getColor(expected, spent, date));
        }
    }

    private void drawMonth(YearMonth month) {
        // clear
        calendar.getChildren().removeAll(days);
        Arrays.fill(days, null);
        clearSelected();

        // draw label
        calendarLabel.setText(month.format(new DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR)
                .appendLiteral(", ")
                .appendText(ChronoField.YEAR)
                .toFormatter()));

        // draw month
        int padding = month.atDay(1).getDayOfWeek().getValue() - 1; // number of days between monday and 1
        int numberOfDays = month.lengthOfMonth(); // days in month

        for (int day = 1; day <= numberOfDays; day++) {
            // foreach day in month
            int index = day + padding - 1;
            int column = index / 7 + 1;
            if (column >= calendar.getRowCount()) {
                // add missing row
                RowConstraints row = new RowConstraints();
                row.setVgrow(Priority.SOMETIMES);
                calendar.getRowConstraints().add(row);
            }

            // add and save label
            Label centeredLabel = JavaFXUtils.getCenteredLabel(Integer.toString(day));
            days[day - 1] = centeredLabel;
            int finalDay = day;
            centeredLabel.setOnMouseClicked(event -> model.setDay(finalDay));
            calendar.add(centeredLabel, index % 7, column);
        }
        while (calendar.getRowCount() > (numberOfDays + padding - 1) / 7 + 2)
            // remove extra rows
            calendar.getRowConstraints().remove(calendar.getRowCount() - 1);
    }

    private void clearSelected() {
        // unselect if existing
        if (selected != -1 && days[selected] != null)
            days[selected].setBorder(null);
        selected = -1;
    }

    private void selectDay(int day) {
        // unselect
        clearSelected();

        // select
        if (day != 0) {
            selected = day - 1;
            days[selected].setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5.0), new BorderWidths(1))));
        }
    }

}