package econest.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Read-only, sortable table with consistent styling. Status words such as
 * "Low stock" or "Delivered" are coloured automatically.
 */
public class DataTable extends JTable {

    private static final long serialVersionUID = 1L;

    private final DefaultTableModel model;
    private final Class<?>[] columnTypes;

    public DataTable(String[] columns) {
        this(columns, null);
    }

    /** @param columnTypes e.g. Integer.class for number columns so they sort numerically (may be null) */
    public DataTable(String[] columns, Class<?>[] columnTypes) {
        this.columnTypes = columnTypes;
        this.model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (DataTable.this.columnTypes != null && columnIndex < DataTable.this.columnTypes.length
                        && DataTable.this.columnTypes[columnIndex] != null) {
                    return DataTable.this.columnTypes[columnIndex];
                }
                return Object.class;
            }
        };
        setModel(model);
        setAutoCreateRowSorter(true);
        setRowHeight(30);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setFont(Theme.regular());
        JTableHeader header = getTableHeader();
        header.setFont(Theme.bold());
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
        StatusRenderer renderer = new StatusRenderer();
        setDefaultRenderer(Object.class, renderer);
        setDefaultRenderer(Integer.class, renderer);
        setDefaultRenderer(Double.class, renderer);
    }

    public void setRows(List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    public DefaultTableModel getTableModel() {
        return model;
    }

    /** Index in the original data list of the selected row, or -1. */
    public int getSelectedModelRow() {
        int view = getSelectedRow();
        return view < 0 ? -1 : convertRowIndexToModel(view);
    }

    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < getColumnCount(); i++) {
            if (widths[i] > 0) {
                getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
        }
    }

    /** Wraps the table in a scroll pane with a light border. */
    public JScrollPane inScrollPane() {
        JScrollPane sp = new JScrollPane(this);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Colours status words and right-aligns numbers and money. */
    static class StatusRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            String text = value == null ? "" : value.toString();
            l.setHorizontalAlignment(value instanceof Number || text.startsWith("Rs. ")
                    ? SwingConstants.RIGHT : SwingConstants.LEFT);
            l.setFont(Theme.regular());
            if (!selected) {
                l.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF7FAF7));
                l.setForeground(colorFor(text));
                if (l.getForeground() != Theme.TEXT) {
                    l.setFont(Theme.bold());
                }
            }
            return l;
        }

        static Color colorFor(String text) {
            switch (text) {
                case "Out of stock":
                case "Failed":
                case "Cancelled":
                case "Unavailable":
                case "Inactive":
                    return Theme.DANGER;
                case "Low stock":
                case "Pending":
                case "Simulated (not sent)":
                    return new Color(0xB26A00);
                case "Delivered":
                case "In stock":
                case "Sent":
                case "Available":
                case "Active":
                    return Theme.PRIMARY;
                case "Dispatched":
                case "In transit (dispatched)":
                case "Processing":
                case "Assigned":
                    return Theme.INFO;
                default:
                    return Theme.TEXT;
            }
        }
    }
}
