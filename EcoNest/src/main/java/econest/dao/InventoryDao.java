package econest.dao;

import econest.model.InventoryItem;
import econest.model.ProductCategory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** SQL for the inventory table (quantity on hand and reorder level). */
public class InventoryDao extends BaseDao {

    private static final String SELECT =
            "SELECT i.product_id, p.name, p.category, p.active, i.quantity_on_hand, i.reorder_level, i.updated_at "
            + "FROM inventory i JOIN products p ON p.product_id = i.product_id ";

    private InventoryItem map(ResultSet rs) throws SQLException {
        InventoryItem item = new InventoryItem();
        item.setProductId(rs.getInt("product_id"));
        item.setProductName(rs.getString("name"));
        item.setCategory(ProductCategory.valueOf(rs.getString("category")));
        item.setProductActive(rs.getInt("active") == 1);
        item.setQuantityOnHand(rs.getInt("quantity_on_hand"));
        item.setReorderLevel(rs.getInt("reorder_level"));
        item.setUpdatedAt(getDateTime(rs, "updated_at"));
        return item;
    }

    public List<InventoryItem> findAll() {
        return query(SELECT + "ORDER BY p.name", this::map);
    }

    public InventoryItem findByProduct(int productId) {
        return queryOne(SELECT + "WHERE i.product_id = ?", this::map, productId);
    }

    public InventoryItem findByProduct(Connection con, int productId) {
        return queryOne(con, SELECT + "WHERE i.product_id = ?", this::map, productId);
    }

    /** Active products at or below their reorder level (low-stock alert). */
    public List<InventoryItem> findLowStock() {
        return query(SELECT + "WHERE p.active = 1 AND i.quantity_on_hand <= i.reorder_level "
                + "ORDER BY i.quantity_on_hand, p.name", this::map);
    }

    public int countLowStock() {
        return queryInt("SELECT COUNT(*) FROM inventory i JOIN products p ON p.product_id = i.product_id "
                + "WHERE p.active = 1 AND i.quantity_on_hand <= i.reorder_level");
    }

    public void createRow(Connection con, int productId, int quantity, int reorderLevel) {
        update(con, "INSERT INTO inventory (product_id, quantity_on_hand, reorder_level, updated_at) "
                + "VALUES (?, ?, ?, ?)", productId, quantity, reorderLevel, now());
    }

    public void updateReorderLevel(int productId, int reorderLevel) {
        update("UPDATE inventory SET reorder_level = ?, updated_at = ? WHERE product_id = ?",
                reorderLevel, now(), productId);
    }

    /** Stock count correction (e.g. damaged plants). */
    public void setQuantity(int productId, int quantity) {
        update("UPDATE inventory SET quantity_on_hand = ?, updated_at = ? WHERE product_id = ?",
                quantity, now(), productId);
    }

    public void increase(Connection con, int productId, int quantity) {
        update(con, "UPDATE inventory SET quantity_on_hand = quantity_on_hand + ?, updated_at = ? "
                + "WHERE product_id = ?", quantity, now(), productId);
    }

    /**
     * Takes stock away only if enough is available. The check and the change
     * happen in one SQL statement, so two users cannot oversell the same item.
     *
     * @return false when there was not enough stock
     */
    public boolean decrease(Connection con, int productId, int quantity) {
        int rows = update(con, "UPDATE inventory SET quantity_on_hand = quantity_on_hand - ?, updated_at = ? "
                + "WHERE product_id = ? AND quantity_on_hand >= ?", quantity, now(), productId, quantity);
        return rows == 1;
    }

    /** Units sold per product since a date (non-cancelled orders). Used for reorder suggestions. */
    public Map<Integer, Integer> unitsSoldSince(LocalDateTime since) {
        Map<Integer, Integer> sold = new HashMap<>();
        query("SELECT oi.product_id, SUM(oi.quantity) AS qty FROM order_items oi "
                + "JOIN orders o ON o.order_id = oi.order_id "
                + "WHERE o.status <> 'CANCELLED' AND o.order_date >= ? GROUP BY oi.product_id",
                rs -> sold.put(rs.getInt("product_id"), rs.getInt("qty")), since);
        return sold;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }
}
