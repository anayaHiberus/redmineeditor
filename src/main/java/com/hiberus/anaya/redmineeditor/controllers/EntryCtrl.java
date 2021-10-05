package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.MyException;
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

/**
 * One of the entries in the entries list
 */
public class EntryCtrl extends SimpleListCell<TimeEntry> {

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
    private final InnerCtrl cellCtrl; // to notify when the entry changes

    private EntryCtrl() {
        // to avoid error on fxml file
        super("");
        cellCtrl = null;
    }

    public EntryCtrl(Model model) {
        // creates new cell
        super("entry_cell.fxml");
        cellCtrl = new InnerCtrl() {
            @Override
            void init() {
            }
        };
        cellCtrl.injectModel(model);

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

        double spent_hours = entry.issue.spent_hours;
        get_total.setVisible(spent_hours == -2);
        total.setVisible(spent_hours != -2);
        total.setText(spent_hours < 0 ? "none" : TimeUtils.formatHours(spent_hours));
        double estimated_hours = entry.issue.estimated_hours;
        estimated.setText(estimated_hours == -2 ? "?" : estimated_hours == -1 ? "none" : TimeUtils.formatHours(estimated_hours));
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
        // update entry
        TimeEntry entry = getItem();
        entry.changeHours(Double.parseDouble(((Button) node.getTarget()).getUserData().toString())); // the button data is the amount

        // update views
        updateHours();
        cellCtrl.model.notificator.fire(Model.Events.Hours); // TODO: maybe move this logic to model?
    }

    @FXML
    private void getTotal() {
        cellCtrl.inBackground(() -> {
            try {
                getItem().issue.fill();
            } catch (IOException e) {
                throw new MyException("Network error", "Unable to fetch the issue details", e);
            }
        }, () -> cellCtrl.model.notificator.fire(Model.Events.Hours));
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
        double amount = getItem().getHours();

        // set text
        spent.setText(TimeUtils.formatHours(amount));
        // disable substract buttons
        substract.setDisable(amount <= 0);
        // set transparent if not hours
        setOpacity(amount <= 0 ? 0.5 : 1);
    }
}
