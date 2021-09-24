package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.MyException;
import javafx.application.Platform;

/**
 * Just an interface for inner views, so that they can be populated with the model from the parent (instead of using dependency injection)
 */
abstract class InnerView {

    /**
     * The global controller
     */
    Model model;

    /**
     * Initializes this inner view with the global controller
     *
     * @param model global controller
     */
    void injectModel(Model model) {
        this.model = model;
        init();
    }

    /**
     * Initialization to do when the model is populated
     */
    abstract void init();

    /* ------------------------- utils ------------------------- */

    private int backgroundLevels = 0;

    /**
     * Run something in background. Updates the loading status
     *
     * @param background to run in background
     */
    public void inBackground(MyException.Runnable background) {
        inBackground(background, null);
    }

    /**
     * Run something in background, and then something in foreground. Updates the loading status
     *
     * @param background to run in background
     * @param foreground to run in foreground after the background thread finishes without errors
     */
    public void inBackground(MyException.Runnable background, MyException.Runnable foreground) {
        // set as loading
        model.setLoading(true);
        backgroundLevels++;

        // error container
        MyException[] error = new MyException[1];

        new Thread(() -> {
            try {
                // run in background
                background.run();
            } catch (MyException e) {
                error[0] = e; // save directly
            } catch (Throwable e) {
                // background error
                error[0] = new MyException("Internal error", "Something unexpected happened in background", e);
            }
            Platform.runLater(() -> {
                if (error[0] == null || error[0].isWarning()) {
                    // if not error or it was a warning
                    try {
                        // run in foreground
                        if (foreground != null) foreground.run();
                    } catch (MyException e) {
                        error[0] = e; // save directly
                    } catch (Throwable e) {
                        // foreground error
                        error[0] = new MyException("Internal error", "Something unexpected happened in foreground", e);
                    }
                }
                // unset as loading
                backgroundLevels--;
                if (backgroundLevels == 0) model.setLoading(false);
                // show error
                if (error[0] != null) error[0].showAndWait();
            });
        }).start();
    }
}
