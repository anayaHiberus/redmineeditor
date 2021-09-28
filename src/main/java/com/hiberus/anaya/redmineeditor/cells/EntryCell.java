package com.hiberus.anaya.redmineeditor.cells;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell;
import com.hiberus.anaya.redmineeditor.utils.TimeUtils;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
    private Label hours;

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
        updateHours(entry.getHours());
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
        updateHours(entry.getHours());
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
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(model.manager.getIssueUrl(issue)));
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /* ------------------------- private ------------------------- */

    private void updateHours(double amount) {
        // set text
        hours.setText(TimeUtils.formatHours(amount));
        // disable substract buttons
        substract.setDisable(amount <= 0);
        // set transparent if not hours
        setOpacity(amount <= 0 ? 0.5 : 1);
    }
}
