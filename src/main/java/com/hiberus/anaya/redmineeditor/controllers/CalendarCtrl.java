package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Locale;

public class CalendarCtrl extends InnerCtrl {

    // ------------------------- views -------------------------

    @FXML
    Label calendarLabel;
    @FXML
    GridPane calendar;
    @FXML
    GridPane calendarHeader;

    // ------------------------- data -------------------------

    private YearMonth month;
    private final Label[] days = new Label[31];

    // ------------------------- reactions -------------------------

    @FXML
    void initialize() {
        for (DayOfWeek field : DayOfWeek.values()) {
            calendarHeader.add(JavaFXUtils.getCenteredLabel(field.getDisplayName(TextStyle.SHORT, Locale.getDefault())), field.getValue() - 1, 0);
        }
    }

    @FXML
    void onNextMonth() {
        mainCtrl.onNewMonth(month.plusMonths(1));
    }

    @FXML
    void onPreviousMonth() {
        mainCtrl.onNewMonth(month.plusMonths(-1));
    }

    // ------------------------- actions -------------------------

    public void drawMonth(YearMonth month) {
        this.month = month;

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
            int listenerDay = day;
            centeredLabel.setOnMouseClicked(event -> mainCtrl.onDaySelected(listenerDay));
            calendar.add(centeredLabel, i % 7, i / 7);
        }
        while (calendar.getRowCount() > (numberOfDays + padding - 1) / 7 + 1)
            calendar.getRowConstraints().remove(calendar.getRowCount() - 1);
    }

    public void selectDay(int day) {
        for (Label label : days) {
            if (label != null) label.setBorder(null);
        }
        days[day - 1].setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5.0), new BorderWidths(1))));
    }

    public void setDayColor(int day, Color color) {
        JavaFXUtils.setBackgroundColor(days[day - 1], color);
    }

}