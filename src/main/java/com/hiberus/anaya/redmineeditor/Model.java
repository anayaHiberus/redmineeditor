package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineapi.RedmineManager;
import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;
import org.json.JSONException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * The data of the app
 */
public class Model {

    // ------------------------- month -------------------------

    /**
     * Current month
     */
    private YearMonth month = YearMonth.now();

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;

        // on new month, reset day and load hours
        setDay(0);
        hour_entries.loadMonth(month);
        notifyChanged();
    }

    // ------------------------- day -------------------------

    /**
     * Selected day. If 0 no day is displayed
     */
    private int day = LocalDate.now().getDayOfMonth();

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        if (day == 0 || month.isValidDay(day))
            this.day = day;
        // else assert?
        notifyChanged();
    }

    public LocalDate getDate() {
        if (day == 0) return null;
        else return month.atDay(day);
    }

    // ------------------------- entries -------------------------

    /**
     * Hour entries from redmine
     */
    public final TimeEntries hour_entries = new TimeEntries();

    // ------------------------- notifier -------------------------

    SimpleBooleanProperty changed = new SimpleBooleanProperty();

    public void onChanges(Runnable listener) {
        listener.run();
        changed.addListener((observable, oldValue, newValue) -> listener.run());
    }

    public void notifyChanged() {
        changed.set(!changed.get());
    }

    // ------------------------- entries -------------------------

    /**
     * Redmine entries manager
     */
    public class TimeEntries {
        private final RedmineManager manager = new RedmineManager(Settings.URL, Settings.KEY);

        private boolean loading = false; // if data is being loaded

        private final List<TimeEntry> entries = new ArrayList<>(); // raw api entries

        private final Set<YearMonth> monthsLoaded = new HashSet<>(); // months that are already loaded


        /**
         * Loads a specific month (if it is already loaded this does nothing)
         *
         * @param month month to load
         */
        public void loadMonth(YearMonth month) {
            // skip if already loaded
            if (monthsLoaded.contains(month)) return;
            monthsLoaded.add(month);

            // notify loading
            loading = true;
            notifyChanged();

            // in background...
            AtomicBoolean ok = new AtomicBoolean(true);
            JavaFXUtils.runInBackground(() -> {
                try {

                    // load from the internet
                    manager.getHourEntries(month.atDay(1), month.atEndOfMonth()).forEach(this::addEntry);

                    // debug data (display loaded value)
                    for (int day = 1; day <= month.lengthOfMonth(); ++day) {
                        LocalDate date = month.atDay(day);
                        double expected = Schedule.getExpectedHours(date);
                        double spent = getSpent(date);

                        System.out.println(date + ": Expected " + expected + " obtained " + spent);
                    }

                } catch (IOException | JSONException e) {
                    // error, mark
                    e.printStackTrace();
                    ok.set(false);
                }
            }, () -> { // then in foreground...
                // notify loaded ended
                loading = false;
                notifyChanged();

                if (!ok.get()) {
                    // on error, show dialog
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Network error");
                    alert.setContentText("Can't load content from Redmine. Try again later");
                    alert.showAndWait();
                }
            });
        }

        private void addEntry(TimeEntry timeEntry) {
            entries.add(timeEntry);
            notifyChanged();
        }

        /**
         * Discards all data
         */
        public void clear() {
            monthsLoaded.clear();
            entries.clear();
            notifyChanged();
        }

        /**
         * Calculates the hours spent in a day
         *
         * @param date day to check
         * @return hours spent that day
         */
        public double getSpent(LocalDate date) {
            return getEntriesForDate(date).stream().mapToDouble(TimeEntry::getHours).sum();
        }

        /**
         * @return true iff the data is being loaded (which means that internal data may not be accurate yet)
         */
        public boolean isLoading() {
            return loading;
        }

        public List<TimeEntry> getEntriesForDate(LocalDate date) {
            return entries.stream()
                    .filter(entry -> entry.wasSpentOn(date))
                    .collect(Collectors.toList());
        }

        public void prepareEntriesForDate(LocalDate date) {
            Set<Integer> issues = getEntriesForDate(date).stream().map(entry -> entry.issue).collect(Collectors.toSet());

            for (int days = 1; days <= 7; ++days) {
                for (TimeEntry prevEntry : getEntriesForDate(date.minusDays(days))) {
                    if (prevEntry.getHours() != 0 && !issues.contains(prevEntry.issue)) {
                        TimeEntry newEntry = new TimeEntry(prevEntry.issue, date);
                        newEntry.setComment(prevEntry.getComment());
                        addEntry(newEntry);
                        issues.add(prevEntry.issue); // in loops instead of stream because we only want one for each issue
                    }
                }
            }
        }

        public boolean update() {
            return entries.stream()
                    .map(manager::uploadTimeEntry)
                    .reduce(true, Boolean::logicalAnd);
        }

        public Set<Integer> getAllIssues() {
            return entries.stream()
                    .map(entries -> entries.issue)
                    .filter(issue -> issue != -1)
                    .collect(Collectors.toSet());
        }

        public void createIssue(LocalDate date, Integer issue) {
            addEntry(new TimeEntry(issue, date));
            notifyChanged();
        }
    }

}
