package econest.dao;

import econest.model.PriceHistory;
import econest.model.Product;
import econest.model.ProductCategory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** SQL for the products and product_price_history tables. */
public class ProductDao extends BaseDao implements CrudDao<Product> {

    private static final String SELECT =
            "SELECT p.product_id, p.name, p.category, p.description, p.price, p.eco_rating, p.active, "
            + "COALESCE(i.quantity_on_hand, 0) AS qty "
            + "FROM products p LEFT JOIN inventory i ON i.product_id = p.product_id ";

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("product_id"));
        p.setName(rs.getString("name"));
        p.setCategory(ProductCategory.valueOf(rs.getString("category")));
        p.setDescription(rs.getString("description"));
        p.setPrice(getMoney(rs, "price"));
        p.setEcoRating(rs.getInt("eco_rating"));
        p.setActive(rs.getInt("active") == 1);
        p.setQuantityOnHand(rs.getInt("qty"));
        return p;
    }

    @Override
    public List<Product> findAll() {
        return query(SELECT + "ORDER BY p.name", this::map);
    }

    /** Products that can still be sold. */
    public List<Product> findActive() {
        return query(SELECT + "WHERE p.active = 1 ORDER BY p.name", this::map);
    }

    @Override
    public Product findById(int id) {
        return queryOne(SELECT + "WHERE p.product_id = ?", this::map, id);
    }

    public Product findById(Connection con, int id) {
        return queryOne(con, SELECT + "WHERE p.product_id = ?", this::map, id);
    }

    @Override
    public int insert(Product p) {
        try (Connection con = DBConnection.getConnection()) {
            return insert(con, p);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public int insert(Connection con, Product p) {
        return insert(con,
                "INSERT INTO products (name, category, description, price, eco_rating, active) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                p.getName(), p.getCategory(), p.getDescription(), p.getPrice(), p.getEcoRating(), p.isActive());
    }

    @Override
    public void update(Product p) {
        try (Connection con = DBConnection.getConnection()) {
            update(con, p);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public void update(Connection con, Product p) {
        update(con,
                "UPDATE products SET name = ?, category = ?, description = ?, price = ?, eco_rating = ?, "
                + "active = ? WHERE product_id = ?",
                p.getName(), p.getCategory(), p.getDescription(), p.getPrice(), p.getEcoRating(),
                p.isActive(), p.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM products WHERE product_id = ?", id);
    }

    public void setActive(int id, boolean active) {
        update("UPDATE products SET active = ? WHERE product_id = ?", active, id);
    }

    /** True if the product appears in any order (then it must not be deleted). */
    public boolean isUsedInOrders(int id) {
        return queryInt("SELECT COUNT(*) FROM order_items WHERE product_id = ?", id) > 0;
    }

    public boolean nameExists(String name, int excludeId) {
        return queryInt("SELECT COUNT(*) FROM products WHERE LOWER(name) = LOWER(?) AND product_id <> ?",
                name, excludeId) > 0;
    }

    public int countActive() {
        return queryInt("SELECT COUNT(*) FROM products WHERE active = 1");
    }

    // ---------------------------------------------- price history (extra feature)

    public void insertPriceHistory(Connection con, PriceHistory h) {
        insert(con, "INSERT INTO product_price_history (product_id, old_price, new_price, changed_by, changed_at) "
                + "VALUES (?, ?, ?, ?, ?)",
                h.getProductId(), h.getOldPrice(), h.getNewPrice(), h.getChangedBy(),
                java.time.LocalDateTime.now().withNano(0));
    }

    public List<PriceHistory> findPriceHistory(int productId) {
        return query("SELECT product_id, old_price, new_price, changed_by, changed_at "
                + "FROM product_price_history WHERE product_id = ? ORDER BY changed_at DESC, history_id DESC",
                rs -> {
                    PriceHistory h = new PriceHistory();
                    h.setProductId(rs.getInt("product_id"));
                    h.setOldPrice(getMoney(rs, "old_price"));
                    h.setNewPrice(getMoney(rs, "new_price"));
                    h.setChangedBy(rs.getString("changed_by"));
                    h.setChangedAt(getDateTime(rs, "changed_at"));
                    return h;
                }, productId);
    }
}
