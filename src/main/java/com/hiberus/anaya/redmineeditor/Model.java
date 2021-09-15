package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Redmine;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.scene.control.Alert;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Model {

    // properties
    public final ObservableProperty<String> user = new ObservableProperty<>("me");
    public final ObservableProperty<YearMonth> month = new ObservableProperty<>(YearMonth.now());
    public final ObservableProperty<Integer> day = new ObservableProperty<>(LocalDate.now().getDayOfMonth()); // if 0, no day selected
    public final ObservableProperty<TimeEntries> hour_entries = new ObservableProperty<>(new TimeEntries());

    // ------------------------- model logic -------------------------


    public Model() {
        // on new month, load it
        month.observe(newMonth -> {
            day.set(0);
            hour_entries.get().loadMonth(newMonth);
        });

        // start by loading entries of current month
        hour_entries.get().reload();
    }

    // ------------------------- entries -------------------------

    public class TimeEntries extends ObservableProperty.Data {

        private boolean loading = false;

        private final JSONArray entries = new JSONArray();

        private final Set<YearMonth> monthsLoaded = new HashSet<>();

        public void loadMonth(YearMonth month) {
            if (monthsLoaded.contains(month)) return;
            monthsLoaded.add(month);

            loading = true;
            notifyChanged();

            AtomicBoolean ok = new AtomicBoolean(true);
            JavaFXUtils.runInBackground(() -> {
                try {
                    entries.putAll(Redmine.getHourEntries(user.get(), month.atDay(1), month.atEndOfMonth()));

                    // debug data
                    for (int day = 1; day <= month.lengthOfMonth(); ++day) {
                        LocalDate date = month.atDay(day);
                        double expected = Schedule.getExpectedHours(date);
                        double spent = getSpent(date);

                        System.out.println(date + ": Expected " + expected + " obtained " + spent);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    ok.set(false);
                }
            }, () -> { // then in foreground
                loading = false;
                notifyChanged();
                if (!ok.get()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Network error");
                    alert.setContentText("Can't load content from Redmine. Try again later");
                    alert.showAndWait();
                }
            });
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
