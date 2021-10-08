package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineapi.RedmineManager;
import com.hiberus.anaya.redmineapi.TimeEntry;
import org.json.JSONException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The data of the app
 */
public class Model {
    private static final int PREV_DAYS = 7;

    /* ------------------------- data ------------------------- */

    private boolean loading = false; // loading state
    private YearMonth month = null; // displayed month
    private int day = 0; // selected day, 0 for none


    public final RedmineManager manager = new RedmineManager(Settings.URL, Settings.KEY); // the manager for online operations
    private final List<TimeEntry> entries = new ArrayList<>(); // time entries
    private final List<Issue> issues = new ArrayList<>(); // issues
    private final Set<YearMonth> monthsLoaded = new HashSet<>(); // months that are already loaded and don't need to be again

    /* ------------------------- getters ------------------------- */

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
        return _getEntriesForDate(date).stream().mapToDouble(TimeEntry::getHours).sum();
    }

    /**
     * Calculates the hours spent in a month
     *
     * @param month month to check
     * @return hours spent that month
     */
    public double getSpent(YearMonth month) {
        return _getEntriesForMonth(month).stream().mapToDouble(TimeEntry::getHours).sum();
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
        return entries.stream().anyMatch(TimeEntry::requiresUpload);
    }

    private Issue getIssueFromId(int id) {
        // gets the issue with the given id, null if not present
        return issues.stream().filter(issue -> issue.id == id).findAny().orElse(null);
    }

    private List<TimeEntry> _getEntriesForDate(LocalDate date) {
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

    public ModelEditor edit() {
        return this.new ModelEditor();
    }

    public class ModelEditor {

        public Model getModel() {
            return Model.this;
        }

        /* ------------------------- notification service ------------------------- */

        public enum Events {
            /**
             * The loading state changed
             */
            Loading,
            /**
             * The displayed month changed
             */
            Month,
            /**
             * The displayed day changed
             */
            Day,
            /**
             * Entries for the displayed day changed (not its content)
             */
            Entries,
            /**
             * Hours from displayed day were changed
             */
            Hours,
            /**
             * List of issues changed
             */
            Issues,
        }

        /**
         * The notificator service
         */
        public final Set<Events> changes = new HashSet<>();


        /* ------------------------- setters ------------------------- */


        /**
         * @param loading new loading state
         */
        public void setLoading(boolean loading) {
            Model.this.loading = loading;
            changes.add(Events.Loading);
        }


        /**
         * Changes the month
         *
         * @param month new month to save
         */
        public void setMonth(YearMonth month) {
            Model.this.month = month;
            changes.add(Events.Month);
        }


        /**
         * @param day new selected day (if invalid won't be saved)
         */
        public void setDay(int day) throws MyException {
            if (Model.this.month.isValidDay(day)) {
                Model.this.day = day;
                prepareDay();
                changes.add(Events.Day);
            }
        }

        public void unsetDay() {
            Model.this.day = 0;
            changes.add(Events.Day);
        }


        /**
         * Loads the current month (if it is already loaded this does nothing)
         * Long operation
         *
         * @throws MyException on error
         */
        public void loadMonth() throws MyException {
            // skip if already loaded
            if (Model.this.monthsLoaded.contains(Model.this.month)) return;

            try {
                // load from the internet
                LocalDate from = Model.this.month.atDay(1);
                if (!Model.this.monthsLoaded.contains(Model.this.month.minusMonths(1))) {
                    // load previous days if previous month was not loaded
                    from = from.minusDays(PREV_DAYS);
                }
                LocalDate to = Model.this.month.atEndOfMonth();
                if (Model.this.monthsLoaded.contains(Model.this.month.plusMonths(1))) {
                    // don't load last days if next month was loaded
                    to = to.minusDays(PREV_DAYS);
                }
                Model.this.entries.addAll(Model.this.manager.getTimeEntries(from, to, Model.this.issues));
            } catch (IOException e) {
                throw new MyException("Network error", "Can't load content from Redmine. Try again later.", e);
            } catch (JSONException e) {
                throw new MyException("Parsing error", "Unknown Redmine response. Try again later.", e);
            }

            // prepare
            prepareDay();

            // mark
            Model.this.monthsLoaded.add(Model.this.month);
            changes.add(Events.Entries);
            changes.add(Events.Issues); // TODO: don't notify if no new entries are loaded
        }

        /**
         * Discards all entries and issues
         */
        public void clearAll() {
            Model.this.monthsLoaded.clear();
            Model.this.entries.clear();
            changes.add(Events.Entries);
            changes.add(Events.Month); // technically month doesn't change, but its data does, this forces a reload in calendar

            Model.this.issues.clear();
            changes.add(Events.Issues);
        }


        private void prepareDay() throws MyException {
            LocalDate date = Model.this.getDate();
            if (date == null) return;

            List<Issue> todayIssues = Model.this._getEntriesForDate(date).stream().map(entry -> entry.issue).collect(Collectors.toList());

            // add a new empty entry copied from previous week ones
            for (int days = 1; days <= PREV_DAYS; ++days) {
                Model.this._getEntriesForDate(date.minusDays(days)).stream()
                        .filter(prevEntry -> prevEntry.getHours() != 0)
                        .filter(prevEntry -> !todayIssues.contains(prevEntry.issue))
                        .forEach(prevEntry -> {
                            TimeEntry newEntry = Model.this.manager.newTimeEntry(prevEntry.issue, date);
                            newEntry.setComment(prevEntry.getComment());
                            Model.this.entries.add(newEntry);
                            todayIssues.add(prevEntry.issue);
                        });
            }

            // fill issues for the days
            MyException exception = new MyException("Issue exception", "Can't load issues data", null);
            for (Issue todayIssue : todayIssues) {
                try {
                    todayIssue.loadUninitialized();
                } catch (IOException e) {
                    exception.addDetails(e);
                }
            }
            if (exception.hasDetails()) {
                throw exception;
            }
        }


        /**
         * Uploads all modified entries
         * Long operation
         *
         * @throws MyException on error
         */
        public void uploadEntries() throws MyException {
            MyException exception = new MyException("Updating error", "An error occurred while updating entries", null);
            for (TimeEntry entry : Model.this.entries) {
                try {
                    entry.uploadTimeEntry(); // TODO: move all this logic to manager
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
            LocalDate date = Model.this.getDate();
            if (date == null) return;
            // add
            Model.this.entries.add(Model.this.manager.newTimeEntry(issue, date));
            changes.add(Events.Entries);
        }

        /**
         * Creates multiple new time entries for current date
         *
         * @param ids each one with an id from this
         */
        public void createTimeEntries(List<Integer> ids) throws MyException {
            if (Model.this.getDate() == null) return;

            List<Integer> idsToLoad = new ArrayList<>();

            // check all ids
            ids.forEach(id -> {
                Issue issue = Model.this.getIssueFromId(id);
                if (issue != null) {
                    // already present, add
                    createTimeEntry(issue);
                } else {
                    // still not present, mark for load
                    idsToLoad.add(id);
                }
            });

            try {
                List<Issue> loadedIssues = Model.this.manager.getIssues(idsToLoad);
                loadedIssues.forEach(issue -> {
                    // create and add issue
                    createTimeEntry(issue);
                    Model.this.issues.add(issue);
                    changes.add(Events.Entries);
                    changes.add(Events.Issues);
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
    }


}
