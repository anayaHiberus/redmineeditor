package com.hiberus.anaya.redmineeditor.controller;

import javafx.scene.control.Alert;

import java.util.StringJoiner;

/**
 * A custom exception that allows displaying it as dialog
 */
public class MyException extends Exception {

    /**
     * A runnable that allows to throw MyException
     */
    public interface Runnable {
        void run() throws MyException;
    }

    private boolean isWarning = false; // a warning still allows to

    private final String title; // the exception title

    private final StringJoiner details = new StringJoiner("\n"); // the exception details (different from the message)

    /**
     * Creates a new exception
     *
     * @param title   with this title
     * @param message and this message
     * @param cause   and this cause
     */
    public MyException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }

    /**
     * Marks this exception as warning
     *
     * @return this (for chained calls)
     */
    public MyException asWarning() {
        isWarning = true;
        return this;
    }

    /**
     * Displays an error dialog with this exception details
     */
    public void showAndWait() {
        if (getCause() != null) getCause().printStackTrace();
        Alert alert = new Alert(isWarning ? Alert.AlertType.WARNING : Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(getMessage() + "\n\n" + details);
        alert.showAndWait();
    }

    /**
     * Adds a detail whose message will be shown as a detail
     *
     * @param other another exception to add
     */
    public void addDetails(Exception other) {
        details.add(other.getMessage());
    }

    public boolean hasDetails() {
        return details.length() > 0;
    }

    /**
     * @return if this exception is a warning
     */
    public boolean isWarning() {
        return isWarning;
    }
}
