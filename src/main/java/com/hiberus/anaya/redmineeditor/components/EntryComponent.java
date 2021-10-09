package com.hiberus.anaya.redmineeditor.components;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineapi.RedmineManager;
import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.controller.Controller;
import com.hiberus.anaya.redmineeditor.controller.MyException;
import com.hiberus.anaya.redmineeditor.utils.Desktop;
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell;
import com.hiberus.anaya.redmineeditor.utils.TimeUtils;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.util.Set;

import static com.hiberus.anaya.redmineeditor.model.ChangeEvents.Hours;

/**
 * One of the entries in the entries list
 */
public class EntryComponent extends SimpleListCell<TimeEntry> {

    /* ------------------------- views ------------------------- */
    @FXML
    private HBox substract;
    @FXML
    private Label issue;
    @FXML
    private TextField comment;
    @FXML
    private Label spent;
    @FXML
    private Button get_total;
    @FXML
    private Label total;
    @FXML
    private Label estimated;
    @FXML
    private Label realization;

    /* ------------------------- init ------------------------- */
    private final Controller controller;

    private EntryComponent() {
        // to avoid error on fxml file
        super("");
        controller = null;
    }

    public EntryComponent(Controller controller) {
        // creates new cell
        super("entry_cell.fxml");
        this.controller = controller;

        // other properties
        get_total.managedProperty().bind(get_total.visibleProperty());
        total.managedProperty().bind(total.visibleProperty());
    }

    @Override
    public void update() {
        // sets the cell data
        TimeEntry entry = getItem();
        issue.setText(entry.issue.toString());
        comment.setText(entry.getComment());

        double spent_hours = entry.issue.getSpentHours();
        get_total.setVisible(spent_hours == RedmineManager.UNINITIALIZED);
        total.setVisible(spent_hours != RedmineManager.UNINITIALIZED);
        total.setText(spent_hours < 0 ? "none" : TimeUtils.formatHours(spent_hours));
        double estimated_hours = entry.issue.estimated_hours;
        estimated.setText(estimated_hours == RedmineManager.UNINITIALIZED ? "?" : estimated_hours == RedmineManager.NONE ? "none" : TimeUtils.formatHours(estimated_hours));
        if (spent_hours >= 0 && estimated_hours > 0) {
            estimated.setText(estimated.getText() + " | " + (int) (spent_hours / estimated_hours * 100) + "%");
        }
        realization.setText(entry.issue.done_ratio + "%");
        updateHours();
    }

    /* ------------------------- actions ------------------------- */

    @FXML
    private void changedComment() {
        // update comment
        getItem().setComment(comment.getText());
    }

    @FXML
    private void changeHours(Event node) {
        // increase or decrease this entry hours

        // update entry
        TimeEntry entry = getItem();
        entry.changeHours(Double.parseDouble(((Button) node.getTarget()).getUserData().toString())); // the button data is the amount

        // update views
        updateHours();

        // and notify
        controller.fireChanges(Set.of(Hours));
    }

    @FXML
    private void getTotal() {
        // load spent hours
        controller.runBackground(model -> {
            try {
                getItem().issue.loadSpent();
                model.registerExternalChange(Hours);
            } catch (IOException e) {
                throw new MyException("Network error", "Unable to fetch the issue details", e);
            }
        });
    }

    @FXML
    private void showDetails() {
        // TODO: improve
        Issue issue = getItem().issue;

        // build alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(issue.toShortString());
        alert.setHeaderText(issue.toString());
        if (!issue.description.isEmpty()) {
            // html description
            WebView webView = new WebView();
            webView.getEngine().loadContent(issue.description);
            alert.getDialogPane().setContent(webView);
        } else {
            // no description
            alert.setContentText("no description");
        }

        // button
        String OPEN_BUTTON = "Open in Readmine";
        alert.getButtonTypes().add(new ButtonType(OPEN_BUTTON));

        // display
        alert.showAndWait();

        if (OPEN_BUTTON.equals(alert.getResult().getText())) {
            // open in desktop
            new Thread(() -> {
                boolean opened = Desktop.openInBrowser(issue.getUrl());
                if (!opened) {
                    Platform.runLater(() -> {
                        Alert toast = new Alert(Alert.AlertType.ERROR);
                        toast.setContentText("Couldn't open the browser");
                        toast.showAndWait();
                    });
                }
            }).start();
        }
    }

    /* ------------------------- private ------------------------- */

    private void updateHours() {
        // update entry hours
        double amount = getItem().getHours();

        // set text
        spent.setText(TimeUtils.formatHours(amount));
        // disable substract buttons
        substract.setDisable(amount <= 0);
        // set transparent if not hours
        setOpacity(amount <= 0 ? 0.5 : 1);
    }
}
