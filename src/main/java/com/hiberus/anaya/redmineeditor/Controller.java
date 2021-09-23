package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineeditor.views.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The class between the views and the model
 * Should I replace it with listeners? callables? events? hmmmm
 */
public class Controller {

    // ------------------------- model -------------------------

    private final Model model = new Model(); // the global model class

    // ------------------------- views -------------------------

    private final ParentView parentView;
    private final CalendarView calendarView;
    private final SummaryView summaryView;
    private final EntriesView entriesView;
    private final InsertView insertView;

    /**
     * Initializes the controller with all the views
     */
    public Controller(ParentView parentView, CalendarView calendarView, SummaryView summaryView, EntriesView entriesView, InsertView insertView) {
        this.parentView = parentView;
        this.calendarView = calendarView;
        this.summaryView = summaryView;
        this.entriesView = entriesView;
        this.insertView = insertView;
    }

    // ------------------------- actions -------------------------

    /**
     * Reload the app
     */
    public void reload() {
        if (model.hasChanges()) {
            // if there are changes, ask first
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
        model.clearEntries();

        // load month
        loadMonth();
    }

    /**
     * Upload changes
     */
    public void upload() {
        backgroundLoad(() -> {
            try {
                // upload changes in background
                model.uploadEntries();
                return null;
            } catch (MyException e) {
                return e; // TODO move to backgroundLoad logic
            }
        }, error -> {
            if (error != null) {
                // an error occurred
                error.showAndWait();
            } else {
                // and reload in foreground
                model.clearEntries(); // clear first so that reload don't notify of unsaved changes
                reload();
            }
        });
    }

    /**
     * Changes the displayed month.
     * Also resets the day
     *
     * @param offset offset for the new month to load
     */
    public void changeMonth(int offset) {
        // change month and reset day
        model.setMonth(model.getMonth().plusMonths(offset));
        model.setDay(0);

        // load month
        loadMonth();
    }

    /**
     * Selects a day
     *
     * @param day day to select (from current month)
     */
    public void selectDay(int day) {
        // change day
        model.setDay(day);

        // and show it
        showDay();
    }

    /**
     * Add a new entry for the current date
     *
     * @param issue id of the issue the entry will be attached to
     */
    public void addEntryForCurrentDate(Issue issue) {
        LocalDate date = model.getDate();
        assert date != null;

        // create and add issue
        model.createTimeEntry(date, issue);

        // update entries
        entriesView.replace(model.getEntriesForDate(date));
    }

    public void addEntriesForCurrentDate(List<Integer> ids) {
        // notify
        LocalDate date = model.getDate();
        assert date != null;

        backgroundLoad(() -> {
            // add
            try {
                model.createTimeEntries(date, ids);
                return null;
            } catch (MyException e) {
                e.printStackTrace();
                return e;
            }

        }, error -> {
            if (error != null) {
                // on error, show dialog
                error.showAndWait();
            }
            if (error == null || error.isWarning()) {
                // update entries
                entriesView.replace(model.getEntriesForDate(date));
                insertView.setIssues(model.getAllIssues());
            }
        });
    }

    // ------------------------- callables -------------------------

    /**
     * When the app starts
     */
    public void onStart() {
        // start by loading entries of current month
        loadMonth();
    }

    /**
     * When hours from an entry in the current day change
     */
    public void onHourChanged() {
        // update that item
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
            insertView.setEnabled(false);
        } else {
            // select
            calendarView.selectDay(model.getDay());
            entriesView.replace(model.getEntriesForDate(date));
            insertView.setEnabled(true);
        }
    }

    private void loadMonth() {
        // load current month
        backgroundLoad(() -> {
            try {
                // first load in background
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
                // update all
                calendarView.drawMonth(model.getMonth());
                calendarView.colorDays(model.getMonth(), model.getSpentForMonth());
                insertView.setIssues(model.getAllIssues());
                showDay();
            }
        });
    }

    private <T> void backgroundLoad(Supplier<T> background, Consumer<T> foreground) {
        // set as loading
        parentView.setLoading(true);
//        calendarView.clearColors();
//        summaryView.asLoading();
//        entriesView.clear();

        // container
        var ref = new Object() {
            T result = null; // transfer from background to foreground
            MyException error = null; // unexpected
        };

        new Thread(() -> {
            try {
                // run in background
                ref.result = background.get();
            } catch (Throwable e) {
                // background error
                e.printStackTrace();
                ref.error = new MyException("Internal error", "Something unexpected happened in background", e);
            }
            Platform.runLater(() -> {
                if (ref.error == null) {
                    try {
                        // run in foreground
                        foreground.accept(ref.result);
                    } catch (Throwable e) {
                        // foreground error
                        ref.error = new MyException("Internal error", "Something unexpected happened in foreground", e);
                    }
                }
                // unset as loading
                parentView.setLoading(false);
                if (ref.error != null) {
                    // show error
                    ref.error.showAndWait();
                }
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
