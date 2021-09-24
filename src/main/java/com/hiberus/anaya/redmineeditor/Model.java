package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineapi.RedmineManager;
import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.utils.Notifiers;
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
    public final Notifiers<Events> notificator = new Notifiers<>();

    /* ------------------------- loading ------------------------- */

    private boolean loading = false; // loading state

    /**
     * @param loading new loading state
     */
    public void setLoading(boolean loading) {
        this.loading = loading;
        notificator.fire(Events.Loading);
    }

    /**
     * @return the loading state
     */
    public boolean isLoading() {
        return loading;
    }

    /* ------------------------- month ------------------------- */

    private YearMonth month = null; // displayed month

    /**
     * @return the current displayed month
     */
    public YearMonth getMonth() {
        return month;
    }

    /**
     * Changes the month
     *
     * @param month new month to save
     */
    public void setMonth(YearMonth month) {
        this.month = month;
        notificator.fire(Events.Month);
    }

    /* ------------------------- day ------------------------- */

    private int day = 0; // selected day, 0 for none

    /**
     * @return the selected day, or 0 if no day is selected
     */
    public int getDay() {
        return day;
    }

    /**
     * @param day new selected day (if invalid won't be saved)
     */
    public void setDay(int day) {
        if (day == 0 || month.isValidDay(day)) {
            this.day = day;
            notificator.fire(Events.Day);
        }
    }

    /**
     * @return the selected date (month+day) or null if no day is selected
     */
    public LocalDate getDate() {
        if (day != 0) return month.atDay(day);
        else return null;
    }

    /* ------------------------- entries ------------------------- */

    private final RedmineManager manager = new RedmineManager(Settings.URL, Settings.KEY); // the manager for online operations

    private final List<TimeEntry> entries = new ArrayList<>(); // time entries
    private final List<Issue> issues = new ArrayList<>(); // issues

    private final Set<YearMonth> monthsLoaded = new HashSet<>(); // months that are already loaded and don't need to be again

    /**
     * @return true iff the current month is already loaded
     */
    public boolean isMonthLoaded() {
        return monthsLoaded.contains(month);
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
            entries.addAll(manager.getTimeEntries(month.atDay(1), month.atEndOfMonth(), issues));
        } catch (IOException e) {
            throw new MyException("Network error", "Can't load content from Redmine. Try again later.", e);
        } catch (JSONException e) {
            throw new MyException("Parsing error", "Unknown Redmine response. Try again later.", e);
        }

        // mark
        monthsLoaded.add(month);
        notificator.fire(Events.Entries);
        notificator.fire(Events.Issues); // TODO: don't notify if no new entries are loaded
    }

    /**
     * Discards all entries and issues
     */
    public void clearAll() {
        monthsLoaded.clear();
        entries.clear();
        notificator.fire(Events.Entries);

        issues.clear();
        notificator.fire(Events.Issues);
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
     * Returns the entries that should be displayed on the selected day (empty if no day selected)
     *
     * @return entries for the current day
     */
    public List<TimeEntry> getDayEntries() {
        LocalDate date = getDate();
        if (date == null) return Collections.emptyList();

        // prepare
        List<Issue> todayIssues = _getEntriesForDate(date).stream().map(entry -> entry.issue).collect(Collectors.toList());
        for (int days = 1; days <= 7; ++days) {
            // add a new empty entry copied from previous week ones
            _getEntriesForDate(date.minusDays(days)).stream()
                    .filter(prevEntry -> prevEntry.getHours() != 0)
                    .filter(prevEntry -> !todayIssues.contains(prevEntry.issue))
                    .forEach(prevEntry -> {
                        TimeEntry newEntry = new TimeEntry(prevEntry.issue, date);
                        newEntry.setComment(prevEntry.getComment());
                        entries.add(newEntry);
                        todayIssues.add(prevEntry.issue);
                    });
        }

        // return
        return _getEntriesForDate(date);
    }

    /**
     * Uploads all modified entries
     * Long operation
     *
     * @throws MyException on error
     */
    public void uploadEntries() throws MyException {
        boolean ok = true;
        MyException exception = new MyException("Updating error", "An error occurred while updating entries", null);
        for (TimeEntry entry : entries) {
            try {
                manager.uploadTimeEntry(entry); // TODO: move all this logic to manager
            } catch (IOException e) {
                exception.addDetails(e);
                ok = false;
            }
        }
        if (!ok) {
            throw exception;
        }
    }

    /**
     * @return all distinct available issues
     */
    public List<Issue> getAllIssues() {
        return issues;
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
        entries.add(new TimeEntry(issue, date));
        notificator.fire(Events.Entries);
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
                this.issues.add(issue);
                notificator.fire(Events.Entries);
                notificator.fire(Events.Issues);
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

    /**
     * @return true iff there is at least something that was modified (and should be uploaded)
     */
    public boolean hasChanges() {
        return entries.stream().anyMatch(TimeEntry::requiresUpload);
    }

    /* ------------------------- private ------------------------- */

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
}
