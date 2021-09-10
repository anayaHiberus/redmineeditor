package com.hiberus.anaya.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A Calendar Component
 * Base from https://javacodex.com/Swing/Swing-Calendar
 */
public class SwingCalendar extends JPanel {

    // layout
    private final DefaultTableModel model;
    private final JLabel label;

    // variables
    private final Calendar cal;
    private final Color[] tablecolors;
    private int padding;
    private Listener listener = null;

    SwingCalendar() {

        // init variables
        cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        tablecolors = new Color[32];
        clearDaycolors();

        // top panel content
        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JButton btn_prev = new JButton("<-");
        btn_prev.addActionListener(ae -> changeMonth(-1));

        JButton btn_next = new JButton("->");
        btn_next.addActionListener(ae -> changeMonth(1));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(btn_prev, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);
        panel.add(btn_next, BorderLayout.EAST);


        // calendar data
        String[] columns = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        model = new DefaultTableModel(null, columns);

        // calendar content
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                        int index = row * 7 + column - padding;
                        if (index >= 1 && index <= 31)
                            setBackground(tablecolors[index]);

                        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    }
                };
            }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);

        // this panel content
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.NORTH);
        this.add(new JScrollPane(table), BorderLayout.CENTER);

        refresh();
    }

    private void changeMonth(int offset) {
        cal.add(Calendar.MONTH, offset);
        clearDaycolors();
        refresh();
        if (listener != null) listener.onNewMonth();
    }

    public void refresh() {

        cal.set(Calendar.DAY_OF_MONTH, 1);

        String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        int year = cal.get(Calendar.YEAR);
        label.setText(month + " " + year);

        padding = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7 - 1;
        int numberOfDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        model.setRowCount(0);
        model.setRowCount(cal.getActualMaximum(Calendar.WEEK_OF_MONTH));

        for (int day = 1; day <= numberOfDays; day++) {
            int i = day + padding;
            model.setValueAt(day, i / 7, i % 7);
            model.getValueAt(i / 7, i % 7);
        }

    }

    //--------------------

    public interface Listener {
        void onNewMonth();

        void onNewDay();
    }

    public void clearDaycolors() {
        Arrays.fill(tablecolors, null);
    }

    public void setDaycolor(int day, Color color) {
        tablecolors[day] = color;
        this.repaint();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public Calendar getMonth() {
        return (Calendar) cal.clone();
    }
}
