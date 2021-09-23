package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineapi.RedmineManager;
import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import org.json.JSONException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The data of the app
 */
public class Model {

    // ------------------------- month -------------------------

    private YearMonth month = YearMonth.now(); // current month

    /**
     * @return the current displayed month
     */
    public YearMonth getMonth() {
        return month;
    }

    /**
     * @param month new month to save
     */
    public void setMonth(YearMonth month) {
        this.month = month;
    }

    // ------------------------- day -------------------------

    private int day = LocalDate.now().getDayOfMonth(); // selected day, 0 for none

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
        if (day == 0 || month.isValidDay(day))
            this.day = day;
    }

    /**
     * @return the selected date (month+day) or null if no day is selected
     */
    public LocalDate getDate() {
        if (day != 0) return month.atDay(day);
        else return null;
    }

    // ------------------------- entries -------------------------

    private final RedmineManager manager = new RedmineManager(Settings.URL, Settings.KEY); // the manager for online operations

    private final List<TimeEntry> entries = new ArrayList<>(); // time entries
    private final List<Issue> issues = new ArrayList<>(); // issues

    private final Set<YearMonth> monthsLoaded = new HashSet<>(); // months that are already loaded and don't need to be again

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

        // debug data (display loaded value)
        for (int day = 1; day <= month.lengthOfMonth(); ++day) {
            LocalDate date = month.atDay(day);
            double expected = Schedule.getExpectedHours(date);
            double spent = getSpent(date);

            System.out.println(date + ": Expected " + expected + " obtained " + spent);
        }
    }

    /**
     * Discards all entries
     */
    public void clearEntries() {
        monthsLoaded.clear();
        entries.clear();
        issues.clear();
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
     * Returns the entries that should be displayed on a specific date
     *
     * @param date date to check
     * @return entries for that date
     */
    public List<TimeEntry> getEntriesForDate(LocalDate date) {
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
        MyException exception = new MyException("Updating error", "An error ocurred while updating entries", null);
        for (TimeEntry entry : entries) {
            try {
                manager.uploadTimeEntry(entry);
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
     * Creates a new time entry
     *
     * @param date  for this date
     * @param issue for this issue
     */
    public void createTimeEntry(LocalDate date, Issue issue) {
        entries.add(new TimeEntry(issue, date));
    }

    /**
     * Creates multiple new time entries
     *
     * @param date for this date
     * @param ids  each one with an id from this
     */
    public void createTimeEntries(LocalDate date, List<Integer> ids) throws MyException {
        List<Integer> idsToLoad = new ArrayList<>();

        ids.forEach(id -> {
            Issue issue = getIssueFromId(id);
            if (issue != null) {
                // already present, add
                createTimeEntry(date, issue);
            } else {
                // still not present, mark for load
                idsToLoad.add(id);
            }
        });

        try {
            List<Issue> loadedIssues = manager.getIssues(idsToLoad);
            loadedIssues.forEach(issue -> {
                // create and add issue
                createTimeEntry(date, issue);
                this.issues.add(issue);
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

    /**
     * @return the spent hours for each day of the current month
     */
    public double[] getSpentForMonth() {
        double[] spent = new double[month.lengthOfMonth()];
        for (int day = 1; day <= month.lengthOfMonth(); ++day) {
            // calculate and save spent hours for each day in the month
            spent[day - 1] = getSpent(month.atDay(day));
        }
        return spent;
    }

    // ------------------------- private -------------------------

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
