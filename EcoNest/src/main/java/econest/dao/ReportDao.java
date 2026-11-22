package econest.dao;

import econest.model.DailyRevenue;
import econest.model.MonthlySalesSummary;
import econest.model.ProductCategory;
import econest.model.ProductSales;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only SQL for the monthly reports (Task 7). All totals come from the
 * saved orders - nothing is hard-coded. Cancelled orders are excluded from
 * revenue and counted separately.
 */
public class ReportDao extends BaseDao {

    public MonthlySalesSummary salesSummary(LocalDateTime from, LocalDateTime to) {
        MonthlySalesSummary s = queryOne(
                "SELECT "
                + "SUM(CASE WHEN status <> 'CANCELLED' THEN 1 ELSE 0 END) AS completed, "
                + "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled, "
                + "SUM(CASE WHEN status <> 'CANCELLED' THEN total_amount ELSE 0 END) AS revenue "
                + "FROM orders WHERE order_date >= ? AND order_date < ?",
                rs -> {
                    MonthlySalesSummary m = new MonthlySalesSummary();
                    m.setCompletedOrders(rs.getInt("completed"));
                    m.setCancelledOrders(rs.getInt("cancelled"));
                    m.setRevenue(getMoney(rs, "revenue"));
                    return m;
                }, from, to);
        if (s == null) {
            s = new MonthlySalesSummary();
        }
        s.setItemsSold(queryInt("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi "
                + "JOIN orders o ON o.order_id = oi.order_id "
                + "WHERE o.status <> 'CANCELLED' AND o.order_date >= ? AND o.order_date < ?", from, to));
        s.setNewClients(queryInt("SELECT COUNT(*) FROM clients WHERE created_at >= ? AND created_at < ?", from, to));
        return s;
    }

    public List<ProductSales> productSales(LocalDateTime from, LocalDateTime to) {
        return query("SELECT p.product_id, p.name, p.category, SUM(oi.quantity) AS qty, SUM(oi.line_total) AS revenue "
                + "FROM order_items oi JOIN orders o ON o.order_id = oi.order_id "
                + "JOIN products p ON p.product_id = oi.product_id "
                + "WHERE o.status <> 'CANCELLED' AND o.order_date >= ? AND o.order_date < ? "
                + "GROUP BY p.product_id, p.name, p.category ORDER BY revenue DESC, p.name",
                rs -> new ProductSales(rs.getInt("product_id"), rs.getString("name"),
                        ProductCategory.valueOf(rs.getString("category")), rs.getInt("qty"), getMoney(rs, "revenue")),
                from, to);
    }

    public List<DailyRevenue> dailyRevenue(LocalDateTime from, LocalDateTime to) {
        return query("SELECT DATE(order_date) AS sale_day, SUM(total_amount) AS revenue FROM orders "
                + "WHERE status <> 'CANCELLED' AND order_date >= ? AND order_date < ? "
                + "GROUP BY DATE(order_date) ORDER BY sale_day",
                rs -> new DailyRevenue(getDate(rs, "sale_day"), getMoney(rs, "revenue")), from, to);
    }

    /** Total amount spent on stock received from suppliers in the period. */
    public java.math.BigDecimal stockPurchaseCost(LocalDate from, LocalDate toExclusive) {
        java.math.BigDecimal cost = queryOne("SELECT COALESCE(SUM(quantity * unit_cost), 0) AS cost FROM stock_in "
                + "WHERE received_date >= ? AND received_date < ?",
                rs -> getMoney(rs, "cost"), from, toExclusive);
        return cost == null ? java.math.BigDecimal.ZERO : cost;
    }
}
