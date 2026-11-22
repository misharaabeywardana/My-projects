package econest.service;

import econest.model.Client;
import econest.model.Delivery;
import econest.model.Order;
import econest.model.OrderItem;
import econest.util.Formatters;

/**
 * Builds a plain-text invoice that can be previewed, printed or saved
 * (additional feature - Fahath). Uses a fixed-width layout for printing.
 */
public final class InvoiceGenerator {

    private static final int WIDTH = 64;

    private InvoiceGenerator() {
    }

    public static String generate(Order order, Client client, Delivery delivery) {
        StringBuilder sb = new StringBuilder();
        line(sb, '=');
        center(sb, "ECONEST");
        center(sb, "Sustainable plants, planters & gardening kits");
        line(sb, '=');
        sb.append(String.format("INVOICE / ORDER : %s%n", order.getOrderRef()));
        sb.append(String.format("Date            : %s%n", Formatters.dateTime(order.getOrderDate())));
        sb.append(String.format("Status          : %s%n", order.getStatus()));
        sb.append('\n');
        sb.append("BILL TO\n");
        if (client != null) {
            sb.append("  ").append(client.getName()).append('\n');
            if (client.getCompanyName() != null) {
                sb.append("  ").append(client.getCompanyName()).append('\n');
            }
            sb.append("  ").append(client.getAddress()).append('\n');
            sb.append("  ").append(client.getPhone()).append("  |  ").append(client.getEmail()).append('\n');
        } else {
            sb.append("  ").append(order.getClientName()).append('\n');
        }
        sb.append('\n');
        line(sb, '-');
        sb.append(String.format("%-28s %5s %13s %14s%n", "Item", "Qty", "Unit price", "Line total"));
        line(sb, '-');
        for (OrderItem item : order.getItems()) {
            String name = item.getProductName();
            if (name.length() > 28) {
                name = name.substring(0, 27) + ".";
            }
            sb.append(String.format("%-28s %5d %13s %14s%n", name, item.getQuantity(),
                    Formatters.plainMoney(item.getUnitPrice()), Formatters.plainMoney(item.getLineTotal())));
        }
        line(sb, '-');
        sb.append(String.format("%-34s %27s%n", "Items: " + order.getTotalQuantity(),
                "TOTAL  " + Formatters.money(order.getTotalAmount())));
        line(sb, '=');
        if (delivery != null) {
            sb.append("DELIVERY\n");
            sb.append("  Agent     : ").append(delivery.getAgentName()).append('\n');
            sb.append("  Scheduled : ").append(Formatters.dateTime(delivery.getScheduledAt())).append('\n');
            sb.append("  Status    : ").append(delivery.getStatus()).append('\n');
            sb.append('\n');
        }
        if (order.getNotes() != null) {
            sb.append("Notes: ").append(order.getNotes()).append("\n\n");
        }
        center(sb, "Thank you for growing greener with EcoNest!");
        line(sb, '=');
        return sb.toString();
    }

    private static void line(StringBuilder sb, char c) {
        sb.append(String.valueOf(c).repeat(WIDTH)).append('\n');
    }

    private static void center(StringBuilder sb, String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        sb.append(" ".repeat(pad)).append(text).append('\n');
    }
}
