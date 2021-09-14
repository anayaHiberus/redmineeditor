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
    private final JTable jTable;
    private int padding;
    private boolean disableListener = false;

    CalendarPanel(Controller controller) {


        // init variables
        tableColors = new Color[32];
        clearDayColors();

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
        jTable = new JTable(tableModel) {
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
        jTable.setCellSelectionEnabled(true);
        jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionListener listener = e -> {
            if (disableListener || e.getValueIsAdjusting()) return;
            Object value = jTable.getSelectedRow() == -1 ? null : jTable.getValueAt(jTable.getSelectedRow(), jTable.getSelectedColumn());
            controller.selectDay(value == null ? 0 : (int) value);
        };
        jTable.getSelectionModel().addListSelectionListener(listener);
        jTable.getColumnModel().getSelectionModel().addListSelectionListener(listener);
        jTable.setRowSelectionAllowed(false);
        jTable.setColumnSelectionAllowed(false);
        jTable.getTableHeader().setReorderingAllowed(false);


        // this panel content
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.NORTH);
        this.add(new JScrollPane(jTable), BorderLayout.CENTER);

    }

    public void drawMonth(YearMonth month) {

        // clear
        clearDayColors();
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

    public void setSelected(int day) {
        disableListener = true;
        int i = day + padding - 1;
        jTable.clearSelection();
        jTable.changeSelection(i / 7, i % 7, true, false);
        // TODO: show clicked
        disableListener = false;
    }

    public void clearDayColors() {
        Arrays.fill(tableColors, null);
    }

    public void setDayColor(int day, Color color) {
        tableColors[day] = color;
        int i = day + padding - 1;
        tableModel.fireTableCellUpdated(i / 7, i % 7);
    }

}
