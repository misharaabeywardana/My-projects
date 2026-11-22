package econest.dao;

import econest.model.Order;
import econest.model.OrderItem;
import econest.model.OrderStatus;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** SQL for the orders and order_items tables. */
public class OrderDao extends BaseDao {

    private static final String SELECT =
            "SELECT o.order_id, o.order_ref, o.client_id, c.name AS client_name, o.order_date, o.status, "
            + "o.total_amount, o.notes, o.created_by, a.name AS agent_name "
            + "FROM orders o JOIN clients c ON c.client_id = o.client_id "
            + "LEFT JOIN deliveries d ON d.order_id = o.order_id "
            + "LEFT JOIN delivery_agents a ON a.agent_id = d.agent_id ";

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("order_id"));
        o.setOrderRef(rs.getString("order_ref"));
        o.setClientId(rs.getInt("client_id"));
        o.setClientName(rs.getString("client_name"));
        o.setOrderDate(getDateTime(rs, "order_date"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setTotalAmount(getMoney(rs, "total_amount"));
        o.setNotes(rs.getString("notes"));
        o.setCreatedBy(rs.getString("created_by"));
        o.setAgentName(rs.getString("agent_name"));
        return o;
    }

    private OrderItem mapItem(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setItemId(rs.getInt("item_id"));
        item.setOrderId(rs.getInt("order_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(getMoney(rs, "unit_price"));
        return item;
    }

    public List<Order> findAll() {
        return query(SELECT + "ORDER BY o.order_date DESC, o.order_id DESC", this::map);
    }

    public List<Order> findRecent(int limit) {
        return query(SELECT + "ORDER BY o.order_date DESC, o.order_id DESC LIMIT ?", this::map, limit);
    }

    public List<Order> findByClient(int clientId) {
        return query(SELECT + "WHERE o.client_id = ? ORDER BY o.order_date DESC, o.order_id DESC",
                this::map, clientId);
    }

    /** Orders that may receive (or be re-given) a delivery agent. */
    public List<Order> findAwaitingDelivery() {
        return query(SELECT + "WHERE o.status IN ('PENDING','PROCESSING') "
                + "AND (d.delivery_id IS NULL OR d.status = 'FAILED') ORDER BY o.order_date", this::map);
    }

    /** Loads an order together with all its product lines. */
    public Order findById(int orderId) {
        Order order = queryOne(SELECT + "WHERE o.order_id = ?", this::map, orderId);
        if (order != null) {
            order.getItems().addAll(findItems(orderId));
        }
        return order;
    }

    public Order findById(Connection con, int orderId) {
        Order order = queryOne(con, SELECT + "WHERE o.order_id = ?", this::map, orderId);
        if (order != null) {
            order.getItems().addAll(query(con, ITEMS_SQL, this::mapItem, orderId));
        }
        return order;
    }

    private static final String ITEMS_SQL =
            "SELECT oi.item_id, oi.order_id, oi.product_id, p.name AS product_name, oi.quantity, oi.unit_price "
            + "FROM order_items oi JOIN products p ON p.product_id = oi.product_id "
            + "WHERE oi.order_id = ? ORDER BY oi.item_id";

    public List<OrderItem> findItems(int orderId) {
        return query(ITEMS_SQL, this::mapItem, orderId);
    }

    /** Inserts the order header. A temporary reference is replaced by {@link #updateRef}. */
    public int insertOrder(Connection con, Order o) {
        return insert(con, "INSERT INTO orders (order_ref, client_id, order_date, status, total_amount, notes, "
                + "created_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "TMP-" + java.util.UUID.randomUUID().toString().substring(0, 20), o.getClientId(),
                o.getOrderDate(), o.getStatus(), o.getTotalAmount(), o.getNotes(), o.getCreatedBy());
    }

    public void updateRef(Connection con, int orderId, String ref) {
        update(con, "UPDATE orders SET order_ref = ? WHERE order_id = ?", ref, orderId);
    }

    public void insertItem(Connection con, int orderId, OrderItem item) {
        insert(con, "INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) "
                + "VALUES (?, ?, ?, ?, ?)",
                orderId, item.getProductId(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
    }

    public void updateStatus(Connection con, int orderId, OrderStatus status) {
        update(con, "UPDATE orders SET status = ? WHERE order_id = ?", status, orderId);
    }

    public int countByStatus(OrderStatus status) {
        return queryInt("SELECT COUNT(*) FROM orders WHERE status = ?", status);
    }

    public int countSince(LocalDateTime since) {
        return queryInt("SELECT COUNT(*) FROM orders WHERE order_date >= ? AND status <> 'CANCELLED'", since);
    }
}
