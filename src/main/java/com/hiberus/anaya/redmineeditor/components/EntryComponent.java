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
        Issue issue = entry.issue;

        // issue
        double issue_spent = issue.getSpent();
        double issue_estimated = issue.getEstimated();

        // text
        this.issue.setText(issue.toString());
        // estimated
        estimated.setText(
                issue_estimated == RedmineManager.UNINITIALIZED ? "?"
                        : issue_estimated == RedmineManager.NONE ? "none"
                        : TimeUtils.formatHours(issue_estimated)
        );
        // spent
        get_total.setVisible(issue_spent == RedmineManager.UNINITIALIZED);
        total.setVisible(issue_spent != RedmineManager.UNINITIALIZED);
        total.setText(issue_spent < 0 ? "none" : TimeUtils.formatHours(issue_spent));
        if (issue_spent >= 0 && issue_estimated > 0) {
            total.setText(total.getText() + " | " + (int) (issue_spent / issue_estimated * 100) + "%");
        }
        // realization
        realization.setText(issue.getRealization() + "%");

        // entry
        double spent = getItem().getSpent();

        // spent
        this.spent.setText(TimeUtils.formatHours(spent));
        substract.setDisable(spent <= 0);

        // comment
        comment.setText(entry.getComment());

        // general
        this.setOpacity(spent <= 0 ? 0.5 : 1);
    }

    /* ------------------------- actions ------------------------- */

    @FXML
    private void changedComment() {
        // update comment
        getItem().setComment(comment.getText());
    }

    @FXML
    private void changeSpent(Event node) {
        // increase or decrease this entry hours

        // update entry
        TimeEntry entry = getItem();
        entry.addSpent(Double.parseDouble(((Button) node.getTarget()).getUserData().toString())); // the button data is the amount

        // and notify
        controller.fireChanges(Set.of(Hours));
    }

    @FXML
    private void changeEstimated(Event node) {
        // increase or decrease the issue estimated hours

        // update issue entry
        Issue issue = getItem().issue;
        issue.addEstimated(Double.parseDouble(((Button) node.getTarget()).getUserData().toString())); // the button data is the amount

        // and notify
        controller.fireChanges(Set.of(Hours));
    }

    @FXML
    private void changeRealization(Event node) {
        // increase, decrease or sync the issue realization percentage
        String data = ((Button) node.getTarget()).getUserData().toString();

        // update issue entry
        Issue issue = getItem().issue;
        if (">".equals(data)) {
            // sync button
            issue.syncRealization();
        } else {
            // offset button
            issue.addRealization(Integer.parseInt(data)); // the button data is the amount
        }

        // and notify
        controller.fireChanges(Set.of(Hours));
    }

    @FXML
    private void getTotal() {
        // load spent hours
        controller.runBackground(model -> {
            try {
                getItem().issue.downloadSpent();
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
        String OPEN_BUTTON = "Open in Redmine";
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

}
