package com.hiberus.anaya.redmineeditor.cells;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.Controller;
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell;
import com.hiberus.anaya.redmineeditor.utils.TimeUtils;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * One of the entries in the entries list
 */
public class EntryCell extends SimpleListCell<TimeEntry> {

    // ------------------------- views -------------------------
    @FXML
    private HBox ignored;
    @FXML
    private Label issue;
    @FXML
    private TextField comment;
    @FXML
    private Label hours;

    // ------------------------- init -------------------------
    private final Controller notifier; // to notify when the entry changes

    private EntryCell() {
        // to avoid error on fxml file
        super("");
        notifier = null;
    }

    public EntryCell(Controller onChangeListener) {
        // creates new cell
        super("entry_cell.fxml");
        notifier = onChangeListener;
    }

    @Override
    public void update() {
        // sets the cell data
        TimeEntry entry = getItem();
        issue.setText(entry.issue.toString());
        comment.setText(entry.getComment());
        updateHours(entry.getHours());
    }

    // ------------------------- actions -------------------------

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
        notifier.onHourChanged();
    }

    // ------------------------- private -------------------------

    private void updateHours(double amount) {
        // set text and disable state
        hours.setText(TimeUtils.formatHours(amount));
        ignored.setDisable(amount <= 0);
    }
}
