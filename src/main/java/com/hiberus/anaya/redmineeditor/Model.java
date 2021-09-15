package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Redmine;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Model {

    // properties
    public final ObservableProperty<String> user = new ObservableProperty<>("me");
    public final ObservableProperty<YearMonth> month = new ObservableProperty<>(YearMonth.now());
    public final ObservableProperty<Integer> day = new ObservableProperty<>(LocalDate.now().getDayOfMonth()); // if 0, no day selected
    public final ObservableProperty<TimeEntries> hour_entries = new ObservableProperty<>(new TimeEntries());

    // ------------------------- model logic -------------------------


    public Model() {
        // on new month, load it
        month.registerSilently(newMonth -> {
            day.set(0);
            hour_entries.get().loadMonth(newMonth);
        });

        // start by loading entries of current month
        hour_entries.get().reload();
    }

    // ------------------------- entries -------------------------

    public class TimeEntries {

        private boolean loading = false;

        private final JSONArray entries = new JSONArray();

        private final Set<YearMonth> monthsLoaded = new HashSet<>();

        public void loadMonth(YearMonth month) {
            if(monthsLoaded.contains(month)) return;
            monthsLoaded.add(month);

            loading = true;
            hour_entries.wasChanged();

            new Thread(() -> {
                try {
                    entries.putAll(Redmine.getHourEntries(user.get(), month.atDay(1), month.atEndOfMonth()));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load entries from Redmine", e);
                }

                // debug data
                for (int day = 1; day <= month.lengthOfMonth(); ++day) {
                    LocalDate date = month.atDay(day);
                    double expected = Schedule.getExpectedHours(date);
                    double spent = getSpent(date);

                    System.out.println(date + ": Expected " + expected + " obtained " + spent);
                }

                Platform.runLater(() -> {
                    loading = false;
                    hour_entries.wasChanged();
                });
            }).start();
        }

        public void reload() {
            monthsLoaded.clear();
            entries.clear();
            loadMonth(month.get());
        }

        public double getSpent(LocalDate day) {
            double spent = 0;
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                if (Objects.equals(entry.getString("spent_on"), day.toString()))
                    spent += entry.getDouble("hours");
            }
            return spent;
        }

        public boolean isLoading() {
            return loading;
        }
    }

}
