package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.utils.JavaFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A list of action buttons
 */
public class ActionsView extends InnerView {


    @Override
    public void initView() {
        // nothing to initialize
    }

    // ------------------------- onActions -------------------------

    @FXML
    void reload() {
        // press the reload button to reload the data
        model.time_entries.clear();
        model.time_entries.loadMonth(model.getMonth());
    }

    @FXML
    void update() {
        AtomicBoolean ok = new AtomicBoolean();
        JavaFXUtils.runInBackground(() -> {
            // update changes
            ok.set(model.time_entries.update());
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
