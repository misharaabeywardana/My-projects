package econest.ui.panels;

import econest.model.InventoryItem;
import econest.model.MonthlySalesSummary;
import econest.model.ProductSales;
import econest.service.MonthlyReport;
import econest.service.ReportService;
import econest.ui.Refreshable;
import econest.ui.components.AppButton;
import econest.ui.components.BarChartPanel;
import econest.ui.components.DataTable;
import econest.ui.components.PageHeader;
import econest.ui.components.StatCard;
import econest.ui.components.Theme;
import econest.ui.components.UiUtil;
import econest.util.CsvExporter;
import econest.util.Formatters;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Task 7 (Rashmi): monthly sales and inventory reports with low-stock alerts.
 * Extra: daily revenue chart, CSV export and printing.
 */
public class ReportPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final ReportService service = new ReportService();
    private final JComboBox<String> month = new JComboBox<>();
    private final JSpinner year = new JSpinner(new SpinnerNumberModel(YearMonth.now().getYear(), 2020, 2100, 1));
    private final StatCard revenue = new StatCard("Revenue", Theme.PRIMARY);
    private final StatCard orders = new StatCard("Orders", Theme.INFO);
    private final StatCard average = new StatCard("Average order", Theme.ACCENT);
    private final StatCard units = new StatCard("Units sold", new Color(0x00838F));
    private final StatCard lowStock = new StatCard("Low-stock items", Theme.DANGER);
    private final BarChartPanel chart = new BarChartPanel();
    private final DataTable salesTable = new DataTable(new String[] {"Product", "Category", "Units sold", "Revenue",
        "Share of revenue"}, new Class<?>[] {null, null, Integer.class});
    private final DataTable inventoryTable = new DataTable(new String[] {"Product", "Category", "Received",
        "Sold", "On hand", "Reorder level", "Status"},
            new Class<?>[] {null, null, Integer.class, Integer.class, Integer.class, Integer.class});
    private final DataTable lowTable = new DataTable(new String[] {"Product", "On hand", "Reorder level",
        "Shortfall", "Status"}, new Class<?>[] {null, Integer.class, Integer.class, Integer.class});
    private final JLabel footnote = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();
    private MonthlyReport current;

    public ReportPanel() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        PageHeader header = new PageHeader("Monthly Sales & Inventory Reports",
                "Revenue summaries, product sales and low-stock alerts - calculated from saved orders");
        for (Month m : Month.values()) {
            month.addItem(m.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        month.setSelectedIndex(YearMonth.now().getMonthValue() - 1);
        year.setEditor(new JSpinner.NumberEditor(year, "#"));
        AppButton generate = new AppButton("Generate", AppButton.Variant.PRIMARY);
        generate.addActionListener(e -> refresh());
        AppButton export = new AppButton("Export CSV");
        export.addActionListener(e -> exportCsv());
        AppButton print = new AppButton("Print Table");
        print.addActionListener(e -> printCurrentTable());
        header.addAction(month);
        header.addAction(year);
        header.addAction(generate);
        header.addAction(export);
        header.addAction(print);

        JPanel cards = new JPanel(new GridLayout(1, 5, 12, 0));
        cards.setOpaque(false);
        cards.add(revenue);
        cards.add(orders);
        cards.add(average);
        cards.add(units);
        cards.add(lowStock);

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        JPanel chartWrap = new JPanel(new BorderLayout());
        chartWrap.setBackground(Color.WHITE);
        chartWrap.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        chartWrap.add(chart);
        tabs.addTab("Revenue Chart", chartWrap);
        tabs.addTab("Sales by Product", salesTable.inScrollPane());
        tabs.addTab("Inventory Report", inventoryTable.inScrollPane());
        tabs.addTab("Low-Stock Alerts", lowTable.inScrollPane());
        tabs.setFont(Theme.bold());
        salesTable.setColumnWidths(260, 120, 90, 130, 120);
        inventoryTable.setColumnWidths(260, 120, 80, 70, 80, 100, 110);
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        center.add(tabs, BorderLayout.CENTER);
        footnote.setForeground(Theme.MUTED);
        center.add(footnote, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private YearMonth selectedMonth() {
        return YearMonth.of((Integer) year.getValue(), month.getSelectedIndex() + 1);
    }

    @Override
    public void refresh() {
        YearMonth ym = selectedMonth();
        UiUtil.runAsync(this, () -> service.buildMonthlyReport(ym), this::show);
    }

    /** Shows a report for a given month (used by tests and the dashboard). */
    public void showMonth(YearMonth ym) {
        year.setValue(ym.getYear());
        month.setSelectedIndex(ym.getMonthValue() - 1);
        refresh();
    }

    private void show(MonthlyReport r) {
        current = r;
        MonthlySalesSummary s = r.getSummary();
        String label = r.getMonth().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " "
                + r.getMonth().getYear();
        revenue.setValue(Formatters.money(s.getRevenue()), label);
        orders.setValue(String.valueOf(s.getCompletedOrders()), s.getCancelledOrders() + " cancelled (not counted)");
        average.setValue(Formatters.money(s.getAverageOrderValue()), s.getNewClients() + " new clients");
        units.setValue(String.valueOf(s.getItemsSold()),
                r.getBestSeller() == null ? "" : "Top: " + r.getBestSeller().getProductName());
        lowStock.setValue(String.valueOf(r.getLowStockCount()), "current stock vs reorder level");
        lowStock.setValueColor(r.getLowStockCount() > 0 ? Theme.DANGER : Theme.TEXT);
        chart.setData("Daily revenue - " + label, r.getDailyRevenue());

        List<Object[]> rows = new ArrayList<>();
        for (ProductSales p : r.getProductSales()) {
            double share = s.getRevenue().signum() == 0 ? 0
                    : p.getRevenue().doubleValue() * 100 / s.getRevenue().doubleValue();
            rows.add(new Object[] {p.getProductName(), p.getCategory(), p.getQuantity(),
                Formatters.money(p.getRevenue()), String.format("%.1f%%", share)});
        }
        salesTable.setRows(rows);

        List<Object[]> inv = new ArrayList<>();
        List<Object[]> low = new ArrayList<>();
        for (InventoryItem i : r.getInventory()) {
            inv.add(new Object[] {i.getProductName(), i.getCategory(), r.getUnitsReceived(i.getProductId()),
                r.getUnitsSold(i.getProductId()), i.getQuantityOnHand(), i.getReorderLevel(),
                i.isProductActive() ? i.getStockStatus().toString() : "Inactive"});
            if (i.isProductActive() && i.isLowStock()) {
                low.add(new Object[] {i.getProductName(), i.getQuantityOnHand(), i.getReorderLevel(),
                    Math.max(0, i.getReorderLevel() - i.getQuantityOnHand() + 1), i.getStockStatus()});
            }
        }
        inventoryTable.setRows(inv);
        lowTable.setRows(low);
        footnote.setText("Orders are counted by order date. Cancelled orders are excluded from revenue and units. "
                + "Stock purchases this month: " + Formatters.money(r.getStockPurchaseCost())
                + ". Stock levels show the current quantity.");
    }

    private void exportCsv() {
        if (current == null) {
            UiUtil.warn(this, "Generate a report first.");
            return;
        }
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new File("econest-report-" + current.getMonth() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV file (opens in Excel)", "csv"));
        if (chooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            try {
                CsvExporter.exportLines(service.toCsvRows(current), chooser.getSelectedFile().toPath());
                UiUtil.info(this, "Report saved to\n" + chooser.getSelectedFile());
            } catch (IOException e) {
                UiUtil.error(this, "Could not save the file: " + e.getMessage());
            }
        }
    }

    private void printCurrentTable() {
        JTable table;
        switch (tabs.getSelectedIndex()) {
            case 2:
                table = inventoryTable;
                break;
            case 3:
                table = lowTable;
                break;
            default:
                table = salesTable;
        }
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat("EcoNest - " + tabs.getTitleAt(
                    Math.max(1, tabs.getSelectedIndex())) + " - " + selectedMonth()), new MessageFormat("Page {0}"));
        } catch (PrinterException e) {
            UiUtil.error(this, "Printing failed: " + e.getMessage());
        }
    }
}
