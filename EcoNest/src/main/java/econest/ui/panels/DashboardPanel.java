package econest.ui.panels;

import econest.model.InventoryItem;
import econest.model.Order;
import econest.service.InventoryService;
import econest.service.OrderService;
import econest.service.ReportService;
import econest.ui.Navigator;
import econest.ui.Refreshable;
import econest.ui.components.AppButton;
import econest.ui.components.DataTable;
import econest.ui.components.PageHeader;
import econest.ui.components.StatCard;
import econest.ui.components.Theme;
import econest.ui.components.UiUtil;
import econest.util.Formatters;
import econest.util.Session;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Home screen: key figures, low-stock alerts, recent orders and shortcuts. */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final ReportService reportService = new ReportService();
    private final InventoryService inventoryService = new InventoryService();
    private final OrderService orderService = new OrderService();
    private final StatCard revenue = new StatCard("Revenue this month", Theme.PRIMARY);
    private final StatCard openOrders = new StatCard("Orders to fulfil", Theme.INFO);
    private final StatCard lowStock = new StatCard("Low-stock products", Theme.DANGER);
    private final StatCard deliveries = new StatCard("Open deliveries", Theme.ACCENT);
    private final StatCard emails = new StatCard("Failed e-mails", new Color(0x6A1B9A));
    private final DataTable lowTable = new DataTable(new String[] {"Product", "On hand", "Reorder at", "Status"},
            new Class<?>[] {null, Integer.class, Integer.class});
    private final DataTable recentTable = new DataTable(new String[] {"Order ref", "Client", "Total", "Status"});
    private final PageHeader header;

    public DashboardPanel(Navigator nav) {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        header = new PageHeader("Dashboard", "Today's overview of the EcoNest shop");
        AppButton newOrder = new AppButton("+ New Order", AppButton.Variant.PRIMARY);
        newOrder.addActionListener(e -> nav.navigate(Navigator.ORDERS + ":new"));
        AppButton stockIn = new AppButton("Record Stock-In");
        stockIn.addActionListener(e -> nav.navigate(Navigator.INVENTORY + ":stockin"));
        AppButton report = new AppButton("Monthly Report");
        report.addActionListener(e -> nav.navigate(Navigator.REPORTS));
        header.addAction(newOrder);
        header.addAction(stockIn);
        header.addAction(report);

        JPanel cards = new JPanel(new GridLayout(1, 5, 12, 0));
        cards.setOpaque(false);
        cards.add(revenue);
        cards.add(openOrders);
        cards.add(lowStock);
        cards.add(deliveries);
        cards.add(emails);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel lists = new JPanel(new GridLayout(1, 2, 16, 0));
        lists.setOpaque(false);
        lists.add(section("Low-stock alerts", lowTable, "View inventory", () -> nav.navigate(Navigator.INVENTORY)));
        lists.add(section("Recent orders", recentTable, "View all orders", () -> nav.navigate(Navigator.ORDERS)));
        recentTable.setColumnWidths(160, 160, 110, 90);
        lowTable.setColumnWidths(240, 70, 80, 90);
        add(lists, BorderLayout.CENTER);
    }

    private JPanel section(String title, DataTable table, String linkText, Runnable action) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.SURFACE);
        p.setBorder(Theme.card());
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 15));
        AppButton link = new AppButton(linkText);
        link.addActionListener(e -> action.run());
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(t, BorderLayout.WEST);
        head.add(link, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);
        p.add(table.inScrollPane(), BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        String name = Session.getCurrentUser() == null ? "" : ", " + Session.getCurrentUser().getFullName();
        header.setTitle(greeting + name, "Today's overview of the EcoNest shop - "
                + Formatters.date(java.time.LocalDate.now()));
        try {
            ReportService.DashboardStats s = reportService.getDashboardStats();
            revenue.setValue(Formatters.money(s.monthRevenue), s.monthOrders + " orders this month");
            openOrders.setValue(String.valueOf(s.pendingOrders), "pending or processing");
            lowStock.setValue(String.valueOf(s.lowStock), "at or below reorder level");
            lowStock.setValueColor(s.lowStock > 0 ? Theme.DANGER : Theme.TEXT);
            deliveries.setValue(String.valueOf(s.openDeliveries), s.availableAgents + " agents available");
            emails.setValue(String.valueOf(s.failedEmails), s.failedEmails > 0 ? "resend in Notifications" : "all good");
            emails.setValueColor(s.failedEmails > 0 ? Theme.DANGER : Theme.TEXT);

            List<Object[]> rows = new ArrayList<>();
            for (InventoryItem i : inventoryService.getLowStock()) {
                rows.add(new Object[] {i.getProductName(), i.getQuantityOnHand(), i.getReorderLevel(),
                    i.getStockStatus()});
            }
            lowTable.setRows(rows);
            List<Object[]> recent = new ArrayList<>();
            for (Order o : orderService.getRecent(10)) {
                recent.add(new Object[] {o.getOrderRef(), o.getClientName(), Formatters.money(o.getTotalAmount()),
                    o.getStatus()});
            }
            recentTable.setRows(recent);
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }
}
