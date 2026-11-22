package econest.dao;

import econest.model.Delivery;
import econest.model.DeliveryStatus;
import econest.model.OrderStatus;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** SQL for the deliveries table. */
public class DeliveryDao extends BaseDao {

    private static final String SELECT =
            "SELECT d.delivery_id, d.order_id, o.order_ref, o.status AS order_status, d.agent_id, "
            + "a.name AS agent_name, a.email AS agent_email, c.name AS client_name, c.address AS client_address, "
            + "d.scheduled_at, d.status, d.assigned_at, d.delivered_at, d.notes "
            + "FROM deliveries d JOIN orders o ON o.order_id = d.order_id "
            + "JOIN delivery_agents a ON a.agent_id = d.agent_id "
            + "JOIN clients c ON c.client_id = o.client_id ";

    private Delivery map(ResultSet rs) throws SQLException {
        Delivery d = new Delivery();
        d.setId(rs.getInt("delivery_id"));
        d.setOrderId(rs.getInt("order_id"));
        d.setOrderRef(rs.getString("order_ref"));
        d.setOrderStatus(OrderStatus.valueOf(rs.getString("order_status")));
        d.setAgentId(rs.getInt("agent_id"));
        d.setAgentName(rs.getString("agent_name"));
        d.setAgentEmail(rs.getString("agent_email"));
        d.setClientName(rs.getString("client_name"));
        d.setClientAddress(rs.getString("client_address"));
        d.setScheduledAt(getDateTime(rs, "scheduled_at"));
        d.setStatus(DeliveryStatus.valueOf(rs.getString("status")));
        d.setAssignedAt(getDateTime(rs, "assigned_at"));
        d.setDeliveredAt(getDateTime(rs, "delivered_at"));
        d.setNotes(rs.getString("notes"));
        return d;
    }

    public List<Delivery> findAll() {
        return query(SELECT + "ORDER BY d.scheduled_at DESC, d.delivery_id DESC", this::map);
    }

    public Delivery findById(int id) {
        return queryOne(SELECT + "WHERE d.delivery_id = ?", this::map, id);
    }

    public Delivery findById(Connection con, int id) {
        return queryOne(con, SELECT + "WHERE d.delivery_id = ?", this::map, id);
    }

    public Delivery findByOrder(int orderId) {
        return queryOne(SELECT + "WHERE d.order_id = ?", this::map, orderId);
    }

    public Delivery findByOrder(Connection con, int orderId) {
        return queryOne(con, SELECT + "WHERE d.order_id = ?", this::map, orderId);
    }

    public int insert(Connection con, Delivery d) {
        return insert(con, "INSERT INTO deliveries (order_id, agent_id, scheduled_at, status, assigned_at, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                d.getOrderId(), d.getAgentId(), d.getScheduledAt(), DeliveryStatus.ASSIGNED,
                LocalDateTime.now().withNano(0), d.getNotes());
    }

    /** Gives an existing (failed) delivery to a new agent / new time. */
    public void reassign(Connection con, Delivery d) {
        update(con, "UPDATE deliveries SET agent_id = ?, scheduled_at = ?, status = ?, assigned_at = ?, "
                + "delivered_at = NULL, notes = ? WHERE delivery_id = ?",
                d.getAgentId(), d.getScheduledAt(), DeliveryStatus.ASSIGNED, LocalDateTime.now().withNano(0),
                d.getNotes(), d.getId());
    }

    public void updateStatus(Connection con, int deliveryId, DeliveryStatus status, LocalDateTime deliveredAt,
                             String notes) {
        update(con, "UPDATE deliveries SET status = ?, delivered_at = ?, notes = COALESCE(?, notes) "
                + "WHERE delivery_id = ?", status, deliveredAt, notes, deliveryId);
    }

    /**
     * Other open deliveries of the same agent scheduled close to the given
     * time (extra feature: schedule clash warning).
     */
    public List<Delivery> findClashes(int agentId, LocalDateTime from, LocalDateTime to, int excludeDeliveryId) {
        return query(SELECT + "WHERE d.agent_id = ? AND d.status IN ('ASSIGNED','IN_TRANSIT') "
                + "AND d.scheduled_at >= ? AND d.scheduled_at <= ? AND d.delivery_id <> ? "
                + "ORDER BY d.scheduled_at", this::map, agentId, from, to, excludeDeliveryId);
    }

    public int countOpen() {
        return queryInt("SELECT COUNT(*) FROM deliveries WHERE status IN ('ASSIGNED','IN_TRANSIT')");
    }
}
