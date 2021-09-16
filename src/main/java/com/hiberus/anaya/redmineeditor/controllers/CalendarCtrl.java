package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.SimpleChangeListener;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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
public class CalendarCtrl implements InnerCtrl {

    // ------------------------- properties -------------------------

    private SimpleObjectProperty<YearMonth> monthProperty;
    private SimpleIntegerProperty dayProperty;

    private final Label[] days = new Label[31]; // for coloring days

    // ------------------------- views -------------------------

    @FXML
    Label calendarLabel;
    @FXML
    GridPane calendar;
    @FXML
    GridPane calendarHeader;

    // ------------------------- init -------------------------

    @FXML
    void initialize() {
        for (DayOfWeek field : DayOfWeek.values()) {
            // create the header
            calendarHeader.add(JavaFXUtils.getCenteredLabel(field.getDisplayName(TextStyle.SHORT, Locale.getDefault())), field.getValue() - 1, 0);
        }
    }

    @Override
    public void initCtrl(Model model) {
        monthProperty = SimpleChangeListener.register(model.month, month -> {
            // month changed, draw new and color days
            drawMonth(month);
            colorDays(model.hour_entries);
        });
        // day changed, select new
        dayProperty = SimpleChangeListener.register(model.day, newDay -> selectDay(newDay.intValue()));
        // hours changed, color days again
        SimpleChangeListener.register(model.hour_entries, unused -> colorDays(model.hour_entries));
    }

    // ------------------------- onActions -------------------------

    @FXML
    void onNextMonth() {
        // set next month
        monthProperty.set(monthProperty.get().plusMonths(1));
    }

    @FXML
    void onPreviousMonth() {
        // set previous month
        monthProperty.set(monthProperty.get().plusMonths(-1));
    }

    // ------------------------- actions -------------------------

    private void colorDays(Model.TimeEntries entries) {
        // skip if loading
        if (entries.isLoading()) return;

        for (int day = 1; day <= monthProperty.get().lengthOfMonth(); ++day) {
            // foreach day of the month
            LocalDate date = monthProperty.get().atDay(day);
            double expected = Schedule.getExpectedHours(date);
            double spent = entries.getSpent(date);

            // color the day
            JavaFXUtils.setBackgroundColor(days[day - 1], Schedule.getColor(expected, spent, date));
        }
    }

    private void drawMonth(YearMonth month) {
        // clear
        calendar.getChildren().clear();
        Arrays.fill(days, null);

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
            if (index / 7 >= calendar.getRowCount()) {
                // add missing row
                RowConstraints row = new RowConstraints();
                row.setVgrow(Priority.SOMETIMES);
                calendar.getRowConstraints().add(row);
            }

            // add and save label
            Label centeredLabel = JavaFXUtils.getCenteredLabel(Integer.toString(day));
            days[day - 1] = centeredLabel;
            int finalDay = day;
            centeredLabel.setOnMouseClicked(event -> dayProperty.set(finalDay));
            calendar.add(centeredLabel, index % 7, index / 7);
        }
        while (calendar.getRowCount() > (numberOfDays + padding - 1) / 7 + 1)
            // remove extra rows
            calendar.getRowConstraints().remove(calendar.getRowCount() - 1);
    }

    private void selectDay(int day) {
        // skip no day
        if (day == 0) return;

        for (Label label : days) {
            // unselect all days (maybe save selected day?)
            if (label != null) label.setBorder(null);
        }

        // select day
        days[day - 1].setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5.0), new BorderWidths(1))));
    }

}