package com.hiberus.anaya.utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * From https://stackoverflow.com/a/14114663
 */
public class ProgressDialog extends JDialog {

    private final JProgressBar progressBar;

    public ProgressDialog(Component parent, SwingWorker worker) {

        super(parent == null ? null : SwingUtilities.getWindowAncestor(parent));
        setModal(true);

        ((JComponent) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel message = new JLabel("Loading...");
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        add(message, gbc);

        gbc.gridy++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(progressBar, gbc);

        pack();

        worker.addPropertyChangeListener(new PropertyChangeHandler());
        if (worker.getState() == SwingWorker.StateValue.PENDING) {
            worker.execute();
        }

    }

    public static void showProgress(Component parent, Runnable worker) {
        ProgressDialog dialog = new ProgressDialog(parent, new SwingWorker<>() {
            @Override
            protected Object doInBackground() {
                worker.run();
                return null;
            }
        });
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public class PropertyChangeHandler implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("state")) {
                SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
                if (state == SwingWorker.StateValue.DONE) {
                    dispose();
                }
            } else if (evt.getPropertyName().equals("progress")) {
                progressBar.setValue((int) evt.getNewValue());
            }
        }

    }

}
