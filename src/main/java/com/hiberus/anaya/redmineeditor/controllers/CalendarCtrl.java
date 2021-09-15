package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
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

public class CalendarCtrl implements InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    Label calendarLabel;
    @FXML
    GridPane calendar;
    @FXML
    GridPane calendarHeader;

    // ------------------------- data -------------------------

    private final Label[] days = new Label[31];

    private ObservableProperty<YearMonth>.ObservedProperty monthProperty;
    private ObservableProperty<Integer>.ObservedProperty dayProperty;
    private ObservableProperty<Model.TimeEntries>.ObservedProperty hourEntriesProperty;

    // ------------------------- init -------------------------

    @Override
    public void init(Model model) {
        monthProperty = model.month.registerObserver(month -> {
            drawMonth(month);
            colorDays(model.hour_entries.get());
        });
        dayProperty = model.day.registerObserver(this::selectDay);
        hourEntriesProperty = model.hour_entries.registerObserver(this::colorDays);
    }

    // ------------------------- reactions -------------------------

    @FXML
    void initialize() {
        for (DayOfWeek field : DayOfWeek.values()) {
            calendarHeader.add(JavaFXUtils.getCenteredLabel(field.getDisplayName(TextStyle.SHORT, Locale.getDefault())), field.getValue() - 1, 0);
        }
    }

    @FXML
    void onNextMonth() {
        monthProperty.setAndNotify(monthProperty.get().plusMonths(1));
    }

    @FXML
    void onPreviousMonth() {
        monthProperty.setAndNotify(monthProperty.get().plusMonths(-1));
    }

    // ------------------------- actions -------------------------

    private void colorDays(Model.TimeEntries entries) {
        if (entries.isLoading()) return;

        for (int day = 1; day <= monthProperty.get().lengthOfMonth(); ++day) {
            LocalDate date = monthProperty.get().atDay(day);
            double expected = Schedule.getExpectedHours(date);
            double spent = entries.getSpent(date);

            // color day
            setDayColor(day, Schedule.getColor(expected, spent, date));
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
            int i = day + padding - 1;
            if (i / 7 >= calendar.getRowCount()) {
                RowConstraints row = new RowConstraints();
                row.setVgrow(Priority.SOMETIMES);
                calendar.getRowConstraints().add(row);
            }
            Label centeredLabel = JavaFXUtils.getCenteredLabel(Integer.toString(day));
            days[day - 1] = centeredLabel;
            int finalDay = day;
            centeredLabel.setOnMouseClicked(event -> dayProperty.setAndNotify(finalDay));
            calendar.add(centeredLabel, i % 7, i / 7);
        }
        while (calendar.getRowCount() > (numberOfDays + padding - 1) / 7 + 1)
            calendar.getRowConstraints().remove(calendar.getRowCount() - 1);
    }

    private void selectDay(int day) {
        if(day == 0) return;

        for (Label label : days) {
            if (label != null) label.setBorder(null);
        }
        days[day - 1].setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5.0), new BorderWidths(1))));
    }

    private void setDayColor(int day, Color color) {
        JavaFXUtils.setBackgroundColor(days[day - 1], color);
    }

}