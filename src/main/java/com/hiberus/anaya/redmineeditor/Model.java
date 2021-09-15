package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Redmine;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public class Model {

    public final ObservableProperty<String> user = new ObservableProperty<>("me");

    // ------------------------- date -------------------------

    public final ObservableProperty<YearMonth> month = new ObservableProperty<>(YearMonth.now());
    public final ObservableProperty<Integer> day = new ObservableProperty<>(LocalDate.now().getDayOfMonth()); // if 0, no day selected

    // ------------------------- entries -------------------------

    public final ObservableProperty<TimeEntries> hour_entries = new ObservableProperty<>(new TimeEntries());

    public class TimeEntries {

        private boolean loading = false;

        private JSONArray entries = new JSONArray();


        public void loadEntries() {
            loading = true;
            hour_entries.wasChanged();

            new Thread(() -> {
                try {
                    entries = Redmine.getHourEntries(user.get(), month.get().atDay(1), month.get().atEndOfMonth());
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load entries from Redmine", e);
                }
                Platform.runLater(() -> {
                    loading = false;
                    hour_entries.wasChanged();
                });
            }).start();
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
