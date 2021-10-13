package com.hiberus.anaya.redmineeditor.model;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineapi.RedmineManager;
import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.controller.MyException;
import com.hiberus.anaya.redmineeditor.controller.Settings;
import org.json.JSONException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The data of the app
 */
public abstract class Model {
    static final int PREV_DAYS = 7;

    /* ------------------------- data ------------------------- */

    boolean loading = false; // loading state
    YearMonth month = null; // displayed month
    int day = 0; // selected day, 0 for none


    public final RedmineManager manager = new RedmineManager(Settings.URL, Settings.KEY); // the manager for online operations
    final List<TimeEntry> entries = new ArrayList<>(); // time entries
    final List<Issue> issues = new ArrayList<>(); // issues
    final Set<YearMonth> monthsLoaded = new HashSet<>(); // months that are already loaded and don't need to be again

    /* ------------------------- public getters ------------------------- */

    /**
     * @return the loading state
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * @return the current displayed month
     */
    public YearMonth getMonth() {
        return month;
    }

    /**
     * @return the selected day, or 0 if no day is selected
     */
    public int getDay() {
        return day;
    }

    /**
     * @return the selected date (month+day) or null if no day is selected
     */
    public LocalDate getDate() {
        if (day != 0) return month.atDay(day);
        else return null;
    }

    /**
     * @return true iff the current month is already loaded
     */
    public boolean isMonthLoaded() {
        return monthsLoaded.contains(month);
    }

    /**
     * Calculates the hours spent in a day
     *
     * @param date day to check
     * @return hours spent that day
     */
    public double getSpent(LocalDate date) {
        return _getEntriesForDate(date).stream().mapToDouble(TimeEntry::getSpent).sum();
    }

    /**
     * Calculates the hours spent in a month
     *
     * @param month month to check
     * @return hours spent that month
     */
    public double getSpent(YearMonth month) {
        return _getEntriesForMonth(month).stream().mapToDouble(TimeEntry::getSpent).sum();
    }

    /**
     * Returns the entries that should be displayed on the selected day (empty if no day selected)
     *
     * @return entries for the current day
     */
    public List<TimeEntry> getDayEntries() {
        LocalDate date = getDate();
        if (date == null) return Collections.emptyList();
        // return
        return _getEntriesForDate(date);
    }

    /**
     * @return all distinct available issues
     */
    public List<Issue> getAllIssues() {
        return issues;
    }

    /**
     * @return true iff there is at least something that was modified (and should be uploaded)
     */
    public boolean hasChanges() {
        return entries.stream().anyMatch(TimeEntry::requiresUpload)
                || issues.stream().anyMatch(Issue::requiresUpload);
    }

    /* ------------------------- private getters ------------------------- */

    Issue getIssueFromId(int id) {
        // gets the issue with the given id, null if not present
        return issues.stream().filter(issue -> issue.id == id).findAny().orElse(null);
    }

    List<TimeEntry> _getEntriesForDate(LocalDate date) {
        // return entries of a specific date
        // todo replace with a map with date as key
        return entries.stream()
                .filter(entry -> entry.wasSpentOn(date))
                .toList();
    }

    private List<TimeEntry> _getEntriesForMonth(YearMonth month) {
        // todo replace with a map with month as key
        return entries.stream()
                .filter(entry -> entry.wasSpentOn(month))
                .toList();
    }

    public static class Editor extends Model {

        /* ------------------------- changes ------------------------- */

        private final Set<ChangeEvents> changes = new HashSet<>(); // list of changes

        /**
         * @return changes made to this model since last call (or initialization)
         */
        public Set<ChangeEvents> getChanges() {
            Set<ChangeEvents> copy = Set.copyOf(changes);
            changes.clear();
            return copy;
        }

        /**
         * Registers an external change.
         * TODO: somehow remove this
         *
         * @param event event to register
         */
        public void registerExternalChange(ChangeEvents event) {
            changes.add(event);
        }


        /* ------------------------- public setters ------------------------- */

        /**
         * @param loading new loading state
         */
        public void setLoading(boolean loading) {
            this.loading = loading;
            changes.add(ChangeEvents.Loading);
        }

        /**
         * Changes the month
         *
         * @param month new month to save
         */
        public void setMonth(YearMonth month) {
            this.month = month;
            changes.add(ChangeEvents.Month);
        }

        /**
         * @param day new selected day (if invalid won't be saved)
         */
        public void setDay(int day) throws MyException {
            if (month.isValidDay(day)) {
                this.day = day;
                prepareDay();
                changes.add(ChangeEvents.Day);
            }
        }

        /**
         * Unsets the day
         */
        public void unsetDay() {
            day = 0;
            changes.add(ChangeEvents.Day);
        }

        /**
         * Loads the current month (if it is already loaded this does nothing)
         * Long operation
         *
         * @throws MyException on error
         */
        public void loadMonth() throws MyException {
            // skip if already loaded
            if (monthsLoaded.contains(month)) return;

            try {
                // load from the internet
                LocalDate from = month.atDay(1);
                if (!monthsLoaded.contains(month.minusMonths(1))) {
                    // load previous days if previous month was not loaded
                    from = from.minusDays(PREV_DAYS);
                }
                LocalDate to = month.atEndOfMonth();
                if (monthsLoaded.contains(month.plusMonths(1))) {
                    // don't load last days if next month was loaded
                    to = to.minusDays(PREV_DAYS);
                }
                entries.addAll(manager.getTimeEntries(from, to, issues));
            } catch (IOException e) {
                throw new MyException("Network error", "Can't load content from Redmine. Try again later.", e);
            } catch (JSONException e) {
                throw new MyException("Parsing error", "Unknown Redmine response. Try again later.", e);
            }

            // prepare
            prepareDay();

            // mark
            monthsLoaded.add(month);
            changes.add(ChangeEvents.Entries);
            changes.add(ChangeEvents.Issues); // TODO: don't notify if no new entries are loaded
        }

        /**
         * Discards all entries and issues
         */
        public void clearAll() {
            monthsLoaded.clear();
            entries.clear();
            changes.add(ChangeEvents.Entries);
            changes.add(ChangeEvents.Month); // technically month doesn't change, but its data does, this forces a reload in calendar

            issues.clear();
            changes.add(ChangeEvents.Issues);
        }

        /**
         * Uploads all modified data
         * Long operation
         *
         * @throws MyException on error
         */
        public void uploadAll() throws MyException {
            MyException exception = new MyException("Updating error", "An error occurred while updating data", null);
            for (TimeEntry entry : entries) {
                try {
                    entry.uploadTimeEntry(); // TODO: move all this logic to manager
                } catch (IOException e) {
                    exception.addDetails(e);
                }
            }
            for (Issue issue : issues) {
                try {
                    issue.uploadTimeEntry(); // TODO: move all this logic to manager
                } catch (IOException e) {
                    exception.addDetails(e);
                }
            }
            if (exception.hasDetails()) {
                throw exception;
            }
        }

        /**
         * Creates a new time entry for current day
         *
         * @param issue for this issue
         */
        public void createTimeEntry(Issue issue) {
            LocalDate date = getDate();
            if (date == null) return;
            // add
            entries.add(manager.newTimeEntry(issue, date));
            changes.add(ChangeEvents.Entries);
        }

        /**
         * Creates multiple new time entries for current date
         *
         * @param ids each one with an id from this
         */
        public void createTimeEntries(List<Integer> ids) throws MyException {
            if (getDate() == null) return;

            List<Integer> idsToLoad = new ArrayList<>();

            // check all ids
            ids.forEach(id -> {
                Issue issue = getIssueFromId(id);
                if (issue != null) {
                    // already present, add
                    createTimeEntry(issue);
                } else {
                    // still not present, mark for load
                    idsToLoad.add(id);
                }
            });

            try {
                List<Issue> loadedIssues = manager.getIssues(idsToLoad);
                loadedIssues.forEach(issue -> {
                    // create and add issue
                    createTimeEntry(issue);
                    issues.add(issue);
                    changes.add(ChangeEvents.Entries);
                    changes.add(ChangeEvents.Issues);
                });
                // remove loaded
                idsToLoad.removeAll(loadedIssues.stream().map(issue -> issue.id).toList());
                if (idsToLoad.size() == 1) {
                    // missing single issue
                    throw new MyException("Unknown issue", "The issue #" + idsToLoad.get(0) + " was not found or couldn't be loaded", null).asWarning();
                }
                if (idsToLoad.size() >= 2) {
                    // missing multiple issues
                    throw new MyException("Unknown issues", "The issues " + idsToLoad.stream().map(issue -> "#" + issue).collect(Collectors.joining(", ")) + " were not found or couldn't be loaded", null).asWarning();
                }
            } catch (IOException e) {
                throw new MyException("Error loading issues", "Can't load issues", e);
            }
        }

        /* ------------------------- private setters ------------------------- */

        private void prepareDay() throws MyException {
            // prepares the current day

            // do nothing if no selected day
            LocalDate date = getDate();
            if (date == null) return;

            // get all unique issues for today
            Set<Issue> todayIssues = _getEntriesForDate(date).stream().map(entry -> entry.issue).collect(Collectors.toSet());

            // add a new empty entry copied from previous week ones
            for (int days = 1; days <= PREV_DAYS; ++days) {
                // for each previous day
                _getEntriesForDate(date.minusDays(days)).stream()
                        // wich was used
                        .filter(prevEntry -> prevEntry.getSpent() != 0)
                        // and still not in today
                        .filter(prevEntry -> !todayIssues.contains(prevEntry.issue))
                        // do
                        .forEach(prevEntry -> {
                            // create new entry
                            TimeEntry newEntry = manager.newTimeEntry(prevEntry.issue, date);
                            newEntry.setComment(prevEntry.getComment());
                            entries.add(newEntry);
                            todayIssues.add(prevEntry.issue);
                        });
            }

            // fill issues for the days
            MyException exception = new MyException("Issue exception", "Can't load issues data", null);
            for (Issue todayIssue : todayIssues) {
                try {
                    todayIssue.downloadSpent();
                } catch (IOException e) {
                    exception.addDetails(e);
                }
            }
            if (exception.hasDetails()) {
                throw exception;
            }
        }
    }

}
