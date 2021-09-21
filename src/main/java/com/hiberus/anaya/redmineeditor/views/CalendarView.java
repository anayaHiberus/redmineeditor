package com.hiberus.anaya.redmineeditor.views;

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
public class CalendarView extends InnerView {

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
    public void initView() {
    }

    // ------------------------- onActions -------------------------

    @FXML
    void onNextMonth() {
        // set next month
        controller.changeMonth(1);
    }

    @FXML
    void onPreviousMonth() {
        // set previous month
        controller.changeMonth(-1);
    }

    // ------------------------- actions -------------------------

    public void colorDays(YearMonth month, double[] spents) {
        for (int day = 1; day <= month.lengthOfMonth(); ++day) {
            colorDay(month.atDay(day), spents[day - 1]);
        }
    }

    public void colorDay(LocalDate day, double spent) {
        // assume date.month is the displayed one
        JavaFXUtils.setBackgroundColor(days[day.getDayOfMonth() - 1], Schedule.getColor(Schedule.getExpectedHours(day), spent, day));
    }

    public void clearColors() {
        for (Label day : days) {
            if (day != null) JavaFXUtils.setBackgroundColor(day, null);
        }
    }

    public void drawMonth(YearMonth month) {
        // clear
        calendar.getChildren().removeAll(days);
        Arrays.fill(days, null);
        unselect();

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
            centeredLabel.setOnMouseClicked(event -> controller.setDay(finalDay));
            calendar.add(centeredLabel, index % 7, column);
        }
        while (calendar.getRowCount() > (numberOfDays + padding - 1) / 7 + 2)
            // remove extra rows
            calendar.getRowConstraints().remove(calendar.getRowCount() - 1);
    }

    public void unselect() {
        // unselect if existing
        if (selected != -1 && days[selected] != null)
            days[selected].setBorder(null);
        selected = -1;
    }

    public void selectDay(int day) {
        // unselect
        unselect();

        // select
        if (day != 0) {
            selected = day - 1;
            days[selected].setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5.0), new BorderWidths(1))));
        }
    }

}