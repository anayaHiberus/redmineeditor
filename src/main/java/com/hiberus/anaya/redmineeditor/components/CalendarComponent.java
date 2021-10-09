package com.hiberus.anaya.redmineeditor.components;

import com.hiberus.anaya.redmineeditor.controller.MyException;
import com.hiberus.anaya.redmineeditor.model.ChangeEvents;
import com.hiberus.anaya.redmineeditor.model.Model;
import com.hiberus.anaya.redmineeditor.utils.FXUtils;
import com.hiberus.anaya.redmineeditor.utils.TimeUtils;
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
import java.util.Set;

import static com.hiberus.anaya.redmineeditor.model.ChangeEvents.*;

/**
 * A calendar view with colored days
 */
public class CalendarComponent extends BaseComponent {

    /* ------------------------- properties ------------------------- */

    private final Label[] days = new Label[31]; // for coloring days
    private int selected = -1; // the selected day index
    private boolean needsColoring = false; // to draw colors after month loads

    /* ------------------------- views ------------------------- */

    @FXML
    private Label calendarLabel; // month/year label
    @FXML
    private GridPane calendar; // grid

    /* ------------------------- init ------------------------- */

    @FXML
    private void initialize() {
        // create the header
        for (DayOfWeek field : DayOfWeek.values()) {
            // append each day
            calendar.add(FXUtils.getCenteredLabel(
                    field.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            ), field.getValue() - 1, 0);
        }
    }

    @Override
    void init() {
        // on new month, draw it and prepare to draw colors

        controller.register(Set.of(Month), model -> {
            drawGrid(model);
            updateLabel(model);
            needsColoring = true;
        });

        // when hours change, recolor today
        controller.register(Set.of(Hours), model -> {
            // when hours change (and a recoloring is not pending), recolor day
            if (needsColoring) return;
            int day = model.getDay();
            if (day != 0) colorDay(day, model);
        });

        // when finished loading, color days
        controller.register(Set.of(Month, Loading), model -> {
            // don't color if still loading or a recoloring is not pending
            if (model.isLoading() || !needsColoring) return;
            colorDays(model);
            updateLabel(model);
            needsColoring = false;
        });

        // when day changes (or month), set selection
        controller.register(Set.of(ChangeEvents.Day, ChangeEvents.Month), model -> {
            // unselect
            unselectDay();

            int day = model.getDay();
            if (day != 0) {
                // select new
                selected = day - 1;
                days[selected].setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5.0), new BorderWidths(1))));
            }
        });
    }

    /* ------------------------- onActions ------------------------- */

    @FXML
    private void onNextMonth() {
        // next month
        controller.runBackground(model -> loadMonth(1, model));
    }

    @FXML
    private void onPreviousMonth() {
        // previous month
        controller.runBackground(model -> loadMonth(-1, model));
    }

    /* ------------------------- effects ------------------------- */

    private void loadMonth(int offset, Model.Editor editor) throws MyException {
        // change month by offset
        editor.setMonth(editor.getMonth().plusMonths(offset));
        // unselect the day
        editor.unsetDay();
        controller.fireChanges();
        // and load if necessary
        if (!editor.isMonthLoaded()) editor.loadMonth();
    }

    private void colorDays(Model model) {
        // color all days of current month
        YearMonth month = model.getMonth();
        for (int day = 1; day <= month.lengthOfMonth(); ++day) {
            colorDay(day, model);
        }
    }

    private void colorDay(int day, Model model) {
        // color a single day of current month
        LocalDate date = model.getMonth().atDay(day);
        FXUtils.setBackgroundColor(days[day - 1], Schedule.getColor(Schedule.getExpectedHours(date), model.getSpent(date), date));
    }

    private void updateLabel(Model model) {
        // month info
        String label = model.getMonth().format(new DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR)
                .appendLiteral(", ")
                .appendText(ChronoField.YEAR)
                .toFormatter());

        if (!model.isLoading()) {
            // spent/expected
            double spent = model.getSpent(model.getMonth());
            double expected = Schedule.getExpectedHours(model.getMonth());
            label += " (" + TimeUtils.formatHours(spent) + "/" + TimeUtils.formatHours(expected) + ")";
            FXUtils.setBackgroundColor(calendarLabel, Schedule.getColor(expected, spent, model.getMonth().atEndOfMonth()));
        } else {
            // still not loaded, clear
            FXUtils.setBackgroundColor(calendarLabel, null);
        }

        // set
        calendarLabel.setText(label);
    }

    private void drawGrid(Model model) {
        // clear
        calendar.getChildren().removeAll(days);
        Arrays.fill(days, null);
        unselectDay();

        // draw month
        YearMonth month = model.getMonth();
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
            Label centeredLabel = FXUtils.getCenteredLabel(Integer.toString(day));
            days[day - 1] = centeredLabel;
            int finalDay = day;
            centeredLabel.setOnMouseClicked(event -> selectDay(finalDay));
            calendar.add(centeredLabel, index % 7, column);
            assert false;
        }
        // remove extra rows
        while (calendar.getRowCount() > (numberOfDays + padding - 1) / 7 + 2) {
            calendar.getRowConstraints().remove(calendar.getRowCount() - 1);
        }
    }

    private void selectDay(int day) {
        // select a specific day
        controller.runBackground(model -> model.setDay(day));
    }

    private void unselectDay() {
        // unselect if existing
        if (selected != -1 && days[selected] != null)
            days[selected].setBorder(null);
        selected = -1;
    }

}