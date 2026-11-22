package econest.ui.panels;

import econest.ui.Refreshable;
import econest.ui.components.AppButton;
import econest.ui.components.DataTable;
import econest.ui.components.PageHeader;
import econest.ui.components.SearchField;
import econest.ui.components.Theme;
import econest.ui.components.UiUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Template for every "list + add / edit / delete" screen (products, clients,
 * agents, suppliers, users). Subclasses only describe their own columns and
 * actions; the searching, table and buttons are written once here
 * (inheritance + the Template Method pattern).
 *
 * @param <T> the model class shown in the table
 */
public abstract class AbstractCrudPanel<T> extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    protected final DataTable table;
    protected final SearchField search;
    protected final JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    protected final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    protected final AppButton addButton = new AppButton("Add", AppButton.Variant.PRIMARY);
    protected final AppButton editButton = new AppButton("Edit");
    protected final AppButton deleteButton = new AppButton("Delete", AppButton.Variant.DANGER);
    protected final PageHeader header;
    private final JLabel countLabel = new JLabel(" ");
    private List<T> allItems = new ArrayList<>();
    private List<T> shownItems = new ArrayList<>();

    protected AbstractCrudPanel(String title, String subtitle, String[] columns, Class<?>[] types, boolean asPage) {
        super(new BorderLayout());
        setOpaque(false);
        if (asPage) {
            setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        } else {
            setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        }
        header = new PageHeader(title, subtitle);
        table = new DataTable(columns, types);
        search = new SearchField("Search...", this::applyFilter);

        filterBar.setOpaque(false);
        filterBar.add(search);
        buttonBar.setOpaque(false);
        buttonBar.add(addButton);
        buttonBar.add(editButton);
        buttonBar.add(deleteButton);
        AppButton refresh = new AppButton("Refresh");
        refresh.addActionListener(e -> refresh());
        buttonBar.add(refresh);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        toolbar.add(filterBar, BorderLayout.WEST);
        toolbar.add(buttonBar, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        if (asPage) {
            top.add(header, BorderLayout.NORTH);
        }
        top.add(toolbar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(table.inScrollPane(), BorderLayout.CENTER);
        countLabel.setForeground(Theme.MUTED);
        countLabel.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));
        add(countLabel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> {
            if (onAdd()) {
                refresh();
            }
        });
        editButton.addActionListener(e -> editSelected());
        deleteButton.addActionListener(e -> {
            T item = requireSelection();
            if (item != null && onDelete(item)) {
                refresh();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editSelected();
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> selectionChanged(getSelected()));
    }

    private void editSelected() {
        T item = requireSelection();
        if (item != null && onEdit(item)) {
            refresh();
        }
    }

    // ------------------------------------------------ to be provided by subclasses

    protected abstract List<T> loadItems();

    protected abstract Object[] toRow(T item);

    /** Text used by the search box. */
    protected abstract String searchText(T item);

    /** @return true if something was saved and the table should reload */
    protected abstract boolean onAdd();

    protected abstract boolean onEdit(T item);

    protected abstract boolean onDelete(T item);

    /** Extra filters (combo boxes, check boxes) - override when needed. */
    protected boolean passesFilters(T item) {
        return true;
    }

    /** Called when the selected row changes - override to enable buttons. */
    protected void selectionChanged(T item) {
        // default: nothing
    }

    // ------------------------------------------------ shared behaviour

    protected void addFilter(JComponent c) {
        filterBar.add(c);
    }

    protected void addButton(JComponent c) {
        buttonBar.add(c, buttonBar.getComponentCount() - 1);
    }

    @Override
    public void refresh() {
        try {
            allItems = loadItems();
        } catch (RuntimeException e) {
            allItems = new ArrayList<>();
            UiUtil.handle(this, e);
        }
        applyFilter();
    }

    protected void applyFilter() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        shownItems = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        for (T item : allItems) {
            if (!q.isEmpty() && !searchText(item).toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            if (!passesFilters(item)) {
                continue;
            }
            shownItems.add(item);
            rows.add(toRow(item));
        }
        table.setRows(rows);
        countLabel.setText("Showing " + shownItems.size() + " of " + allItems.size());
        selectionChanged(null);
    }

    protected T getSelected() {
        int row = table.getSelectedModelRow();
        return row < 0 || row >= shownItems.size() ? null : shownItems.get(row);
    }

    protected T requireSelection() {
        T item = getSelected();
        if (item == null) {
            UiUtil.warn(this, "Please select a row in the table first.");
        }
        return item;
    }

    protected List<T> getAllItems() {
        return allItems;
    }
}
