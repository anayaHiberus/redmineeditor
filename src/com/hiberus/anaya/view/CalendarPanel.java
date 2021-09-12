package com.hiberus.anaya.view;

import com.hiberus.anaya.controller.Controller;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;

/**
 * A Calendar Component
 * Base from https://javacodex.com/Swing/Swing-Calendar
 */
public class CalendarPanel extends JPanel {

    // layout
    private final DefaultTableModel tableModel;
    private final JLabel label;

    // variables
    private final Color[] tableColors;
    private int padding;

    CalendarPanel(Controller controller) {


        // init variables
        tableColors = new Color[32];
        clearDaycolors();

        // top panel content
        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JButton btn_prev = new JButton("<-");
        btn_prev.addActionListener(ae -> controller.changeMonth(-1));

        JButton btn_next = new JButton("->");
        btn_next.addActionListener(ae -> controller.changeMonth(1));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(btn_prev, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);
        panel.add(btn_next, BorderLayout.EAST);


        // calendar data
        String[] columns = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        tableModel = new DefaultTableModel(null, columns);

        // calendar content
        JTable table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                        int index = row * 7 + column - padding + 1;
                        if (index >= 1 && index <= 31)
                            setBackground(tableColors[index]);

                        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    }
                };
            }
        };
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionListener listener = e -> {
            if (e.getValueIsAdjusting()) return;
            Object value = table.getSelectedRow() == -1 ? null : table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
            controller.selectDay(value == null ? 0 : (int) value);
        };
        table.getSelectionModel().addListSelectionListener(listener);
        table.getColumnModel().getSelectionModel().addListSelectionListener(listener);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);

        // this panel content
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.NORTH);
        this.add(new JScrollPane(table), BorderLayout.CENTER);

    }

    public void drawMonth(YearMonth month) {

        // clear
        clearDaycolors();
        tableModel.setRowCount(0);

        // draw label
        label.setText(month.format(new DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR)
                .appendLiteral(", ")
                .appendText(ChronoField.YEAR)
                .toFormatter()));

        // draw month
        padding = month.atDay(1).getDayOfWeek().getValue() - 1; // number of days between monday and 1
        int numberOfDays = month.getMonth().maxLength(); // days in month
        tableModel.setRowCount((int) Math.ceil((numberOfDays + padding) / 7d));
        for (int day = 1; day <= numberOfDays; day++) {
            int i = day + padding - 1;
            tableModel.setValueAt(day, i / 7, i % 7);
            tableModel.getValueAt(i / 7, i % 7);
        }
    }

    //--------------------

    public void clearDaycolors() {
        Arrays.fill(tableColors, null);
    }

    public void setDaycolor(int day, Color color) {
        tableColors[day] = color;
        this.repaint();
    }

}
