package com.hiberus.anaya.view;

import com.hiberus.anaya.utils.hiberus.Schedule;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class SummaryPanel extends JPanel {

    private final JLabel info;

    public SummaryPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // info
        info = new JLabel();
        info.setOpaque(true);
        this.add(info);

        clear();
    }

    public void setInfo(LocalDate day, double spent) {
        double expected = Schedule.getExpectedHours(day);
        info.setText(
                day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                        + " --- Required: " + expected
                        + " --- Spent: " + spent
                        + (spent < expected ? " --- Missing: " + (expected - spent)
                        : spent > expected ? " --- Extra: " + (spent - expected)
                        : "")
        );
        info.setBackground(Schedule.getColor(expected, spent, day));
    }

    public void clear() {
        info.setText("Choose a day");
        info.setBackground(null);
    }
}
