package econest.ui.panels;

import econest.model.InventoryItem;
import econest.model.ReorderSuggestion;
import econest.model.StockIn;
import econest.service.InventoryService;
import econest.ui.Refreshable;
import econest.ui.components.AppButton;
import econest.ui.components.Banner;
import econest.ui.components.DataTable;
import econest.ui.components.PageHeader;
import econest.ui.components.SearchField;
import econest.ui.components.Theme;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.StockInDialog;
import econest.util.CsvExporter;
import econest.util.Formatters;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Task 3 (Zija): stock levels, stock-in from suppliers and low-stock alerts.
 * Extra feature: reorder suggestions with a CSV purchase list.
 */
public class InventoryPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final InventoryService service = new InventoryService();
    private final Banner alert = new Banner();
    private final JTabbedPane tabs = new JTabbedPane();

    private final DataTable stockTable = new DataTable(
            new String[] {"ID", "Product", "Category", "On hand", "Reorder level", "Status", "Last updated"},
            new Class<?>[] {Integer.class, null, null, Integer.class, Integer.class});
    private final SearchField stockSearch = new SearchField("Search products...", this::fillStock);
    private final JCheckBox lowOnly = new JCheckBox("Low stock only");
    private List<InventoryItem> items = new ArrayList<>();
    private List<InventoryItem> shownItems = new ArrayList<>();

    private final DataTable historyTable = new DataTable(new String[] {"Date", "Product", "Supplier", "Qty",
        "Unit cost", "Total cost", "Note", "Recorded by"}, new Class<?>[] {null, null, null, Integer.class});

    private final SupplierPanel supplierPanel = new SupplierPanel(service);

    private final DataTable reorderTable = new DataTable(new String[] {"Product", "On hand", "Reorder level",
        "Sold (30 days)", "Avg / day", "Days left", "Suggested order"},
            new Class<?>[] {null, Integer.class, Integer.class, Integer.class, Double.class, null, Integer.class});
    private List<ReorderSuggestion> suggestions = new ArrayList<>();

    public InventoryPanel() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        PageHeader header = new PageHeader("Stock / Inventory",
                "Quantity on hand, reorder levels, stock received from suppliers and low-stock alerts");
        AppButton stockIn = new AppButton("Record Stock-In", AppButton.Variant.PRIMARY);
        stockIn.addActionListener(e -> recordStockIn(null, null));
        header.addAction(stockIn);
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(alert, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        tabs.addTab("Stock Levels", buildStockTab());
        tabs.addTab("Stock-In History", buildHistoryTab());
        tabs.addTab("Suppliers", supplierPanel);
        tabs.addTab("Reorder Suggestions", buildReorderTab());
        tabs.setFont(Theme.bold());
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        center.add(tabs);
        add(center, BorderLayout.CENTER);
    }

    // ------------------------------------------------ tab 1: stock levels

    private JPanel buildStockTab() {
        JPanel p = tabPanel();
        lowOnly.setOpaque(false);
        lowOnly.addActionListener(e -> fillStock());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(stockSearch);
        left.add(lowOnly);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        AppButton reorder = new AppButton("Set Reorder Level");
        reorder.addActionListener(e -> setReorderLevel());
        AppButton adjust = new AppButton("Adjust Quantity");
        adjust.addActionListener(e -> adjustQuantity());
        AppButton receive = new AppButton("Stock-In for Selected");
        receive.addActionListener(e -> {
            InventoryItem i = selectedItem();
            if (i != null) {
                recordStockIn(i.getProductId(), null);
            }
        });
        right.add(receive);
        right.add(reorder);
        right.add(adjust);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        p.add(bar, BorderLayout.NORTH);
        stockTable.setColumnWidths(45, 260, 120, 80, 100, 110, 140);
        p.add(stockTable.inScrollPane(), BorderLayout.CENTER);
        return p;
    }

    private void fillStock() {
        String q = stockSearch.getText().trim().toLowerCase(Locale.ROOT);
        shownItems = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        for (InventoryItem i : items) {
            if (!i.isProductActive()) {
                continue;
            }
            if (lowOnly.isSelected() && !i.isLowStock()) {
                continue;
            }
            if (!q.isEmpty() && !i.getProductName().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            shownItems.add(i);
            rows.add(new Object[] {i.getProductId(), i.getProductName(), i.getCategory(), i.getQuantityOnHand(),
                i.getReorderLevel(), i.getStockStatus(), Formatters.dateTime(i.getUpdatedAt())});
        }
        stockTable.setRows(rows);
    }

    private InventoryItem selectedItem() {
        int row = stockTable.getSelectedModelRow();
        if (row < 0) {
            UiUtil.warn(this, "Please select a product in the Stock Levels table first.");
            return null;
        }
        return shownItems.get(row);
    }

    private Integer askNumber(String message, int current) {
        String text = JOptionPane.showInputDialog(this, message, current);
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            UiUtil.warn(this, "Please enter a whole number.");
            return null;
        }
    }

    private void setReorderLevel() {
        InventoryItem i = selectedItem();
        if (i == null) {
            return;
        }
        Integer level = askNumber("New reorder level for " + i.getProductName() + ":", i.getReorderLevel());
        if (level != null) {
            try {
                service.updateReorderLevel(i.getProductId(), level);
                refresh();
            } catch (RuntimeException e) {
                UiUtil.handle(this, e);
            }
        }
    }

    private void adjustQuantity() {
        InventoryItem i = selectedItem();
        if (i == null) {
            return;
        }
        Integer qty = askNumber("Counted quantity on hand for " + i.getProductName()
                + "\n(use this after a stock count or for damaged items):", i.getQuantityOnHand());
        if (qty != null && UiUtil.confirm(this, "Change " + i.getProductName() + " from " + i.getQuantityOnHand()
                + " to " + qty + " units?")) {
            try {
                service.adjustQuantity(i.getProductId(), qty);
                refresh();
            } catch (RuntimeException e) {
                UiUtil.handle(this, e);
            }
        }
    }

    private void recordStockIn(Integer productId, Integer qty) {
        try {
            if (new StockInDialog(this, service, productId, qty).showDialog()) {
                refresh();
                UiUtil.info(this, "Stock-in saved and quantity on hand updated.");
            }
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }

    // ------------------------------------------------ tab 2: history

    private JPanel buildHistoryTab() {
        JPanel p = tabPanel();
        historyTable.setColumnWidths(90, 220, 200, 50, 90, 100, 200, 90);
        p.add(historyTable.inScrollPane(), BorderLayout.CENTER);
        return p;
    }

    private void fillHistory() {
        List<Object[]> rows = new ArrayList<>();
        for (StockIn s : service.getStockInHistory()) {
            rows.add(new Object[] {Formatters.date(s.getReceivedDate()), s.getProductName(), s.getSupplierName(),
                s.getQuantity(), Formatters.money(s.getUnitCost()), Formatters.money(s.getTotalCost()),
                s.getNote() == null ? "" : s.getNote(), s.getRecordedBy()});
        }
        historyTable.setRows(rows);
    }

    // ------------------------------------------------ tab 4: reorder suggestions (extra)

    private JPanel buildReorderTab() {
        JPanel p = tabPanel();
        JLabel help = new JLabel("<html>Products that are low or will run out within "
                + InventoryService.COVER_DAYS_WARNING + " days at the current sales speed. "
                + "The suggested quantity covers about " + InventoryService.TARGET_COVER_DAYS
                + " days of sales plus the reorder level.</html>");
        help.setForeground(Theme.MUTED);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        AppButton receive = new AppButton("Record Stock-In for Selected");
        receive.addActionListener(e -> {
            int row = reorderTable.getSelectedModelRow();
            if (row < 0) {
                UiUtil.warn(this, "Please select a suggestion first.");
                return;
            }
            ReorderSuggestion s = suggestions.get(row);
            recordStockIn(s.getItem().getProductId(), s.getSuggestedQuantity());
        });
        AppButton export = new AppButton("Export Purchase List (CSV)", AppButton.Variant.PRIMARY);
        export.addActionListener(e -> exportSuggestions());
        right.add(receive);
        right.add(export);
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        bar.add(help, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        p.add(bar, BorderLayout.NORTH);
        reorderTable.setColumnWidths(240, 70, 100, 100, 80, 90, 120);
        p.add(reorderTable.inScrollPane(), BorderLayout.CENTER);
        return p;
    }

    private void fillSuggestions() {
        suggestions = service.getReorderSuggestions();
        List<Object[]> rows = new ArrayList<>();
        for (ReorderSuggestion s : suggestions) {
            int days = s.getDaysOfStockLeft();
            rows.add(new Object[] {s.getItem().getProductName(), s.getItem().getQuantityOnHand(),
                s.getItem().getReorderLevel(), s.getSoldLast30Days(),
                Math.round(s.getAverageDailySales() * 100) / 100.0,
                days < 0 ? "no recent sales" : days + " days", s.getSuggestedQuantity()});
        }
        reorderTable.setRows(rows);
    }

    private void exportSuggestions() {
        if (reorderTable.getRowCount() == 0) {
            UiUtil.info(this, "Nothing needs reordering right now.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("econest-purchase-list-" + LocalDate.now() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV file", "csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CsvExporter.export(reorderTable.getModel(), chooser.getSelectedFile().toPath());
                UiUtil.info(this, "Purchase list saved to\n" + chooser.getSelectedFile());
            } catch (IOException e) {
                UiUtil.error(this, "Could not save the file: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------ common

    private static JPanel tabPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        return p;
    }

    @Override
    public void refresh() {
        try {
            items = service.getAll();
            fillStock();
            fillHistory();
            fillSuggestions();
            supplierPanel.refresh();
            long low = items.stream().filter(i -> i.isProductActive() && i.isLowStock()).count();
            if (low > 0) {
                alert.show(Banner.Kind.WARNING, "Low-stock alert: " + low + " product(s) are at or below their "
                        + "reorder level. Tick \"Low stock only\" to see them, or open Reorder Suggestions.");
            } else {
                alert.show(Banner.Kind.SUCCESS, "All products are above their reorder level.");
            }
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }

    /** Used by the dashboard quick action. */
    public void openStockIn() {
        recordStockIn(null, null);
    }
}
