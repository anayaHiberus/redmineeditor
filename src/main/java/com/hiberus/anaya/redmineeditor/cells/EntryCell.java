package com.hiberus.anaya.redmineeditor.cells;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell;
import com.hiberus.anaya.redmineeditor.utils.TimeUtils;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * One of the entries in the entries list
 */
public class EntryCell extends SimpleListCell<TimeEntry> {

    /* ------------------------- views ------------------------- */
    @FXML
    private Parent parent;
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

    /* ------------------------- private ------------------------- */

    private void updateHours(double amount) {
        // set text and disable state
        hours.setText(TimeUtils.formatHours(amount));
        setDisableNeeded(parent, amount <= 0);
    }

    private boolean setDisableNeeded(Node node, boolean state) {
        // sets the state of the nodes, so that all except the '+' buttons (and its parents) are disabled
        // this implements a tree-traversal algorithm, disabling all nodes except the required ones and its parents
        // node is the node to traverse, state is the new state to set, the return value is true if it needs to be skipped

        boolean skip = false;
        if (node instanceof Parent) {
            // run children first, if any needs to be skipped, skip this one too
            skip = ((Parent) node).getChildrenUnmodifiable().stream()
                    .map(child -> setDisableNeeded(child, state))
                    .reduce(Boolean::logicalOr).orElse(false);
        }

        // skip the '+' buttons
        if (node instanceof Button && ((Button) node).getText().contains("+")) {
            skip = true;
        }

        // set the state of this node if not skipped
        if (!skip) node.setDisable(state);

        // return the skipped status to the caller so that parents are skipped too
        return skip;
    }
}
