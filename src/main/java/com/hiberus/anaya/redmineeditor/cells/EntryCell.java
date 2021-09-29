package com.hiberus.anaya.redmineeditor.cells;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.Desktop;
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell;
import com.hiberus.anaya.redmineeditor.utils.TimeUtils;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;

/**
 * One of the entries in the entries list
 */
public class EntryCell extends SimpleListCell<TimeEntry> {

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
    private Label estimated;
    @FXML
    private Label realization;

    /* ------------------------- init ------------------------- */
    private final Model model; // to notify when the entry changes

    private EntryCell() {
        // to avoid error on fxml file
        super("");
        model = null;
    }

    public EntryCell(Model model) {
        // creates new cell
        super("entry_cell.fxml");
        this.model = model;
    }

    @Override
    public void update() {
        // sets the cell data
        TimeEntry entry = getItem();
        issue.setText(entry.issue.toString());
        comment.setText(entry.getComment());
        estimated.setText(entry.issue.estimated_hours == -1 ? "none" : TimeUtils.formatHours(entry.issue.estimated_hours));
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
        model.notificator.fire(Model.Events.Hours); // TODO: maybe move this logic to model?
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
