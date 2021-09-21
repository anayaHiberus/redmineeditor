package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineeditor.views.CalendarView;
import com.hiberus.anaya.redmineeditor.views.EntriesView;
import com.hiberus.anaya.redmineeditor.views.ParentView;
import com.hiberus.anaya.redmineeditor.views.SummaryView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Controller {

    // ------------------------- model -------------------------

    private final Model model = new Model();

    // ------------------------- views -------------------------

    private final CalendarView calendarView;
    private final SummaryView summaryView;
    private final EntriesView entriesView;
    private final ParentView parentView;

    public Controller(CalendarView calendarView, SummaryView summaryView, EntriesView entriesView, ParentView parentView) {
        this.calendarView = calendarView;
        this.summaryView = summaryView;
        this.entriesView = entriesView;
        this.parentView = parentView;
    }

    // ------------------------- actions -------------------------

    public void start() {
        // start by loading entries of current month
        loadMonth();
    }

    public void reload() {
        // reload everything
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

        // clear
        model.clear();

        // load month
        loadMonth();
    }

    public void upload() {
        // upload entries
        backgroundLoad(() -> {
            try {
                // upload changes
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
            model.clear(); // clear first so that reload don't notify of unsaved changes
            reload();
        });
    }

    public void changeMonth(int offset) {
        // change month and reset day
        model.setMonth(model.getMonth().plusMonths(offset));
        model.setDay(0);

        // load month
        loadMonth();
    }

    public void setDay(int day) {
        // change day
        model.setDay(day);

        // and show it
        showDay();
    }

    public void addIssueForCurrentDate(int issueId) {
        LocalDate date = model.getDate();
        if (date == null) return;

        // create and add issue
        model.createTimeEntry(date, issueId);

        // update entries
        entriesView.replace(model.getEntriesForDate(date));
    }

    // ------------------------- callables -------------------------

    public void onHourChanged() {
        // when hours from an item were changed
        updateDay(model.getDate());
    }

    // ------------------------- private -------------------------

    private void showDay() {
        // day was changed
        LocalDate date = model.getDate();
        // update day
        updateDay(date);
        // update calendar and entries
        if (date == null) {
            // unselect
            calendarView.unselect();
            entriesView.clear();
        } else {
            // select
            calendarView.selectDay(model.getDay());
            entriesView.replace(model.getEntriesForDate(date));
        }
    }

    private void loadMonth() {
        // load current month
        backgroundLoad(() -> {
            try {
                // first load
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
                calendarView.colorDays(model.getMonth(), model.getSpentForMonth());
                entriesView.setIssues(model.getAllIssues());
                showDay();
            }
        });
    }

    private <T> void backgroundLoad(Supplier<T> background, Consumer<T> foreground) {
        // run something in background
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
        // updates a specific day
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

}
