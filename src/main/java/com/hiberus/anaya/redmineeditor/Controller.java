package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineeditor.views.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Controller {

    // ------------------------- model -------------------------

    private final Model model = new Model();

    // ------------------------- views -------------------------

    private final CalendarView calendarView;
    private final SummaryView summaryView;
    private final ActionsView actionsView;
    private final EntriesView entriesView;
    private final ParentView parentView;

    public Controller(CalendarView calendarView, SummaryView summaryView, ActionsView actionsView, EntriesView entriesView, ParentView parentView) {
        this.calendarView = calendarView;
        this.summaryView = summaryView;
        this.actionsView = actionsView;
        this.entriesView = entriesView;
        this.parentView = parentView;
    }

    public void start() {
        // start by loading entries of current month
        loadMonth();
    }

    public void reload() {

        if (model.hasChanges()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "There are unsaved changes, do you want to lose them and reload?",
                    ButtonType.YES, ButtonType.CANCEL);
            alert.setHeaderText("Unsaved changes");
            alert.setTitle("Warning");


            alert.showAndWait();

            if (alert.getResult() != ButtonType.YES) {
                // cancel
                return;
            }
        }

        // clear and load month
        model.clear();
        loadMonth();
    }

    public void upload() {
        // upload entries
        backgroundLoad(() -> {
            // upload changes
            try {
                model.uploadEntries();
                return null;
            } catch (MyException e) {
                return e;
            }
        }, error -> {
            if (error != null) {
                error.showAndWait();
            }
            // and reload
            reload();
        });
    }

    public void changeMonth(int offset) {
        model.setMonth(model.getMonth().plusMonths(offset));

        // on new month, reset day and load hours
        setDay(0);
        loadMonth();
    }

    public void setDay(int day) {
        model.setDay(day);

        showDay();
    }

    private void showDay() {
        LocalDate date = model.getDate();
        if (date == null) {
            // unselect
            summaryView.unselected();
            calendarView.unselect();
            entriesView.clear();
        } else {
            // select
            updateDay(date);
            calendarView.selectDay(model.getDay());
            entriesView.replace(model.getEntriesForDate(date));
        }
    }

    public void addIssueForCurrentDate(int issueId) {
        LocalDate date = model.getDate();
        if (date != null) {
            model.createTimeEntry(date, issueId);
            entriesView.replace(model.getEntriesForDate(date));
        }
    }

    // ------------------------- private -------------------------

    private void loadMonth() {
        backgroundLoad(() -> {
            try {
                model.loadMonth();
                return null;
            } catch (MyException e) {
                return e;
            }
        }, (error) -> {

            if (error != null) {
                // on error, show dialog
                error.showAndWait();
            } else {
                // update
                calendarView.drawMonth(model.getMonth());
                colorDays();
                entriesView.setIssues(model.getAllIssues());
                showDay();
            }
        });
    }

    private <T> void backgroundLoad(Supplier<T> background, Consumer<T> foreground) {
        parentView.setLoading(true);
        calendarView.clearColors();
        summaryView.asLoading();

        new Thread(() -> {
            // in background
            T result = background.get();
            Platform.runLater(() -> {
                // in foreground
                foreground.accept(result);
                parentView.setLoading(false);
                updateDay(model.getDate());
            });
        }).start();
    }

    private void updateDay(LocalDate date) {
        if (date == null) {
            // unselect
            summaryView.unselected();
        } else {
            // color
            double spent = model.getSpent(date);
            summaryView.selected(date, spent);
            calendarView.colorDay(date, spent);
        }
    }

    public void onHourChanged() {
        updateDay(model.getDate());
    }

    private void colorDays() {
        YearMonth month = model.getMonth();
        for (int day = 1; day <= month.lengthOfMonth(); ++day) {
            // foreach day of the month
            LocalDate date = month.atDay(day);

            // color the day
            calendarView.colorDay(month.atDay(day), model.getSpent(date));
        }
    }
}
