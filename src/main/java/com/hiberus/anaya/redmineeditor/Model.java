package com.hiberus.anaya.redmineeditor;

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

    /**
     * Current month
     */
    private YearMonth month = YearMonth.now();

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;
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
    }

    public LocalDate getDate() {
        if (day == 0) return null;
        else return month.atDay(day);
    }

    // ------------------------- entries -------------------------

    private final RedmineManager manager = new RedmineManager(Settings.URL, Settings.KEY);

    private final List<TimeEntry> entries = new ArrayList<>(); // raw api entries

    private final Set<YearMonth> monthsLoaded = new HashSet<>(); // months that are already loaded

    /**
     * Loads the current month (if it is already loaded this does nothing)
     * Long operation
     */
    public void loadMonth() throws MyException {
        // skip if already loaded
        if (monthsLoaded.contains(month)) return;

        try {
            // load from the internet
            manager.getTimeEntries(month.atDay(1), month.atEndOfMonth())
                    .forEach(this::addEntry);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MyException("Network error", "Can't load content from Redmine. Try again later.", e);
        } catch (JSONException e) {
            e.printStackTrace();
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

    private void addEntry(TimeEntry timeEntry) {
        entries.add(timeEntry);
    }

    /**
     * Discards all data
     */
    public void clear() {
        monthsLoaded.clear();
        entries.clear();
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

    public List<TimeEntry> getEntriesForDate(LocalDate date) {
        // prepare
        Set<Integer> issues = _getEntriesForDate(date).stream().map(entry -> entry.issue).collect(Collectors.toSet());

        for (int days = 1; days <= 7; ++days) {
            _getEntriesForDate(date.minusDays(days)).stream()
                    .filter(prevEntry -> prevEntry.getHours() != 0)
                    .filter(prevEntry -> !issues.contains(prevEntry.issue))
                    .forEach(prevEntry -> {
                        TimeEntry newEntry = new TimeEntry(prevEntry.issue, date);
                        newEntry.setComment(prevEntry.getComment());
                        addEntry(newEntry);
                        issues.add(prevEntry.issue);
                    });
        }

        return _getEntriesForDate(date);
    }

    public void uploadEntries() throws MyException {
        boolean ok = true;
        MyException exception = new MyException("Updating error", "An error ocurred while updating entries", null);
        for (TimeEntry entry : entries) {
            try {
                manager.uploadTimeEntry(entry);
            } catch (MyException e) {
                exception.merge(e);
                ok = false;
            }
        }
        if (!ok) {
            throw exception;
        }
    }

    public Set<Integer> getAllIssues() {
        return entries.stream()
                .map(entries -> entries.issue)
                .filter(issue -> issue != -1)
                .collect(Collectors.toSet());
    }

    public void createTimeEntry(LocalDate date, int issue) {
        addEntry(new TimeEntry(issue, date));
    }

    // ------------------------- private -------------------------

    public List<TimeEntry> _getEntriesForDate(LocalDate date) {
        // todo replace with a map with date as key
        return entries.stream()
                .filter(entry -> entry.wasSpentOn(date))
                .collect(Collectors.toList());
    }

    public boolean hasChanges() {
        return entries.stream().anyMatch(manager::requiresUpload);
    }

    public double[] getSpentForMonth() {
        double[] spent = new double[month.lengthOfMonth()];
        for (int day = 1; day <= month.lengthOfMonth(); ++day) {
            // foreach day of the month
            LocalDate date = month.atDay(day);

            // color the day
            spent[day - 1] = getSpent(month.atDay(day));
        }
        return spent;
    }
}
