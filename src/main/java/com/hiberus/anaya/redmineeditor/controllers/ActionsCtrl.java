package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A list of action buttons
 */
public class ActionsCtrl implements InnerCtrl {

    // ------------------------- properties -------------------------

    private ObservableProperty<Model.TimeEntries> hour_entries;
    private ObservableProperty<YearMonth> month;

    // ------------------------- init -------------------------

    @Override
    public void initCtrl(Model model) {
        hour_entries = model.hour_entries;
        month = model.month;
    }

    // ------------------------- onActions -------------------------

    @FXML
    void reload() {
        // press the reload button to reload the data
        hour_entries.get().clear();
        hour_entries.get().loadMonth(month.get());
    }

    @FXML
    void update() {
        AtomicBoolean ok = new AtomicBoolean();
        JavaFXUtils.runInBackground(() -> {
            // update changes
            ok.set(hour_entries.get().update());
        }, () -> {
            if (!ok.get()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Network error");
                alert.setContentText("Can't upload content to Redmine. Try again later");
                alert.showAndWait();
            }
            // and reload
            reload();
        });
    }
}
