package com.hiberus.anaya.redmineeditor;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.SimpleChangeListener;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Redmine;
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
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
    public final SimpleStringProperty user = new SimpleStringProperty("me");

    /**
     * Current month
     */
    public final SimpleObjectProperty<YearMonth> month = new SimpleObjectProperty<>(YearMonth.now());

    /**
     * Selected day. If 0 no day is displayed
     */
    public final SimpleIntegerProperty day = new SimpleIntegerProperty(LocalDate.now().getDayOfMonth());

    /**
     * Hour entries from redmine
     */
    public final TimeEntries hour_entries = new TimeEntries();

    // ------------------------- model logic -------------------------

    public Model() {
        // on new month, load it
        SimpleChangeListener.registerSilently(month, newMonth -> {
            day.set(0);
            hour_entries.loadMonth(newMonth);
        });

        // start by reloading
        hour_entries.reload();
    }

    // ------------------------- entries -------------------------

    /**
     * Redmine entries manager
     */
    public class TimeEntries extends SimpleSetProperty<TimeEntry> {

        private boolean loading = false; // if data is being loaded

        private final JSONArray entries = new JSONArray(); // raw api entries

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
            fireValueChangedEvent();


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
                fireValueChangedEvent();

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
         *
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


        // ------------------------- why? -------------------------


        @Override
        public TimeEntries get() {
            return this;
        }

        @Override
        public Object getBean() {
            return null;
        }

        @Override
        public String getName() {
            return "";
        }
    }

    public class TimeEntry {
    }
}
