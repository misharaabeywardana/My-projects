package econest.ui.dialogs;

import econest.model.Client;
import econest.model.ClientSummary;
import econest.model.Order;
import econest.model.OrderStatus;
import econest.ui.components.AppButton;
import econest.ui.components.DataTable;
import econest.ui.components.StatCard;
import econest.ui.components.Theme;
import econest.util.Formatters;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Client purchase history and loyalty tier (additional feature - Mishara).
 * Tiers: Gold from Rs. 50,000, Silver from Rs. 20,000 lifetime spend.
 */
public class ClientHistoryDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public static final BigDecimal GOLD = new BigDecimal("50000");
    public static final BigDecimal SILVER = new BigDecimal("20000");

    public static String tier(BigDecimal lifetimeValue) {
        if (lifetimeValue.compareTo(GOLD) >= 0) {
            return "Gold";
        }
        if (lifetimeValue.compareTo(SILVER) >= 0) {
            return "Silver";
        }
        return "Green";
    }

    public ClientHistoryDialog(Component parent, Client client, ClientSummary s, List<Order> orders) {
        super(SwingUtilities.getWindowAncestor(parent), "Purchase history - " + client.getDisplayName(),
                ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        String tier = tier(s.getLifetimeValue());
        JLabel title = new JLabel("<html><span style='font-size:15pt'><b>" + client.getDisplayName() + "</b></span>"
                + "<br>" + client.getRoleDescription() + " &middot; " + client.getPhone() + " &middot; "
                + client.getEmail() + " &middot; <b>" + tier + " tier</b></html>");
        root.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setOpaque(false);
        StatCard total = new StatCard("Lifetime value", Theme.PRIMARY);
        total.setValue(Formatters.money(s.getLifetimeValue()), tier + " tier");
        StatCard count = new StatCard("Orders", Theme.INFO);
        count.setValue(String.valueOf(s.getTotalOrders()), s.getCancelledOrders() + " cancelled");
        BigDecimal avg = s.getTotalOrders() == 0 ? BigDecimal.ZERO
                : s.getLifetimeValue().divide(BigDecimal.valueOf(s.getTotalOrders()), 2, RoundingMode.HALF_UP);
        StatCard average = new StatCard("Average order", Theme.ACCENT);
        average.setValue(Formatters.money(avg), "per completed order");
        StatCard last = new StatCard("Last order", new Color(0x6A1B9A));
        long days = s.getDaysSinceLastOrder();
        last.setValue(days < 0 ? "Never" : days + " days ago",
                s.getFavouriteProduct() == null ? "" : "Favourite: " + s.getFavouriteProduct());
        cards.add(total);
        cards.add(count);
        cards.add(average);
        cards.add(last);

        DataTable table = new DataTable(new String[] {"Order ref", "Date", "Status", "Total", "Agent"});
        List<Object[]> rows = new ArrayList<>();
        for (Order o : orders) {
            rows.add(new Object[] {o.getOrderRef(), Formatters.dateTime(o.getOrderDate()), o.getStatus(),
                o.getStatus() == OrderStatus.CANCELLED ? "(" + Formatters.money(o.getTotalAmount()) + ")"
                    : Formatters.money(o.getTotalAmount()), o.getAgentName() == null ? "-" : o.getAgentName()});
        }
        table.setRows(rows);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(table.inScrollPane(), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        AppButton close = new AppButton("Close", AppButton.Variant.PRIMARY);
        close.addActionListener(e -> dispose());
        south.add(close);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(new Dimension(900, 520));
        setLocationRelativeTo(getOwner());
    }
}
