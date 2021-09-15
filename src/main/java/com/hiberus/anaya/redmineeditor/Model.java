package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Redmine;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.scene.control.Alert;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The data of the app
 */
public class Model {

    // ------------------------- properties -------------------------

    /**
     * Current user
     */
    public final ObservableProperty<String> user = new ObservableProperty<>("me");

    /**
     * Current month
     */
    public final ObservableProperty<YearMonth> month = new ObservableProperty<>(YearMonth.now());

    /**
     * Selected day. If 0 no day is displayed
     */
    public final ObservableProperty<Integer> day = new ObservableProperty<>(LocalDate.now().getDayOfMonth());

    /**
     * Hour entries from redmine
     */
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

    /**
     * Redmine entries manager
     */
    public class TimeEntries extends ObservableProperty.Property {

        private boolean loading = false; // if data is being loaded

        private final JSONArray entries = new JSONArray(); // raw api entries

        private final Set<YearMonth> monthsLoaded = new HashSet<>(); // months that are already loaded

        /**
         * Loads a specific month (if it is already loaded this does nothing)
         * @param month month to load
         */
        public void loadMonth(YearMonth month) {
            // skip if already loaded
            if (monthsLoaded.contains(month)) return;
            monthsLoaded.add(month);

            // notify loading
            loading = true;
            notifyChanged();


            AtomicBoolean ok = new AtomicBoolean(true);
            JavaFXUtils.runInBackground(() -> {
                try {
                    // load from the internet
                    entries.putAll(Redmine.getHourEntries(user.get(), month.atDay(1), month.atEndOfMonth()));

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
            }, () -> { // then in foreground
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

        /**
         * Discards all data and loads current month again
         */
        public void reload() {
            monthsLoaded.clear();
            entries.clear();
            loadMonth(month.get());
        }

        /**
         * Calculates the hours spent in a day
         * @param day day to check
         * @return hours spent that day
         */
        public double getSpent(LocalDate day) {
            double spent = 0;
            for (int i = 0; i < entries.length(); i++) {
                // check each entry
                JSONObject entry = entries.getJSONObject(i);
                if (Objects.equals(entry.getString("spent_on"), day.toString()))
                    // if from day, count
                    spent += entry.getDouble("hours");
            }
            return spent;
        }

        /**
         * @return true iff the data is being loaded (which means that internal data may not be accurate yet)
         */
        public boolean isLoading() {
            return loading;
        }
    }

}
