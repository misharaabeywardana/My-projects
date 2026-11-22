package econest.dao;

import econest.model.Client;
import econest.model.ClientSummary;
import econest.model.ClientType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** SQL for the clients table. */
public class ClientDao extends BaseDao implements CrudDao<Client> {

    private static final String SELECT =
            "SELECT client_id, client_type, name, company_name, phone, email, address, created_at FROM clients ";

    private Client map(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId(rs.getInt("client_id"));
        c.setClientType(ClientType.valueOf(rs.getString("client_type")));
        c.setName(rs.getString("name"));
        c.setCompanyName(rs.getString("company_name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setCreatedAt(getDateTime(rs, "created_at"));
        return c;
    }

    @Override
    public List<Client> findAll() {
        return query(SELECT + "ORDER BY name", this::map);
    }

    @Override
    public Client findById(int id) {
        return queryOne(SELECT + "WHERE client_id = ?", this::map, id);
    }

    @Override
    public int insert(Client c) {
        return insert("INSERT INTO clients (client_type, name, company_name, phone, email, address, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                c.getClientType(), c.getName(), c.getCompanyName(), c.getPhone(), c.getEmail(), c.getAddress(),
                java.time.LocalDateTime.now().withNano(0));
    }

    @Override
    public void update(Client c) {
        update("UPDATE clients SET client_type = ?, name = ?, company_name = ?, phone = ?, email = ?, address = ? "
                + "WHERE client_id = ?",
                c.getClientType(), c.getName(), c.getCompanyName(), c.getPhone(), c.getEmail(), c.getAddress(),
                c.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM clients WHERE client_id = ?", id);
    }

    public boolean hasOrders(int clientId) {
        return queryInt("SELECT COUNT(*) FROM orders WHERE client_id = ?", clientId) > 0;
    }

    public boolean emailExists(String email, int excludeId) {
        return queryInt("SELECT COUNT(*) FROM clients WHERE LOWER(email) = LOWER(?) AND client_id <> ?",
                email, excludeId) > 0;
    }

    public int count() {
        return queryInt("SELECT COUNT(*) FROM clients");
    }

    /** Lifetime statistics for the client history screen (extra feature). */
    public ClientSummary getSummary(int clientId) {
        ClientSummary s = queryOne(
                "SELECT "
                + "SUM(CASE WHEN status <> 'CANCELLED' THEN 1 ELSE 0 END) AS total_orders, "
                + "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_orders, "
                + "SUM(CASE WHEN status <> 'CANCELLED' THEN total_amount ELSE 0 END) AS lifetime_value, "
                + "MIN(CASE WHEN status <> 'CANCELLED' THEN order_date END) AS first_order, "
                + "MAX(CASE WHEN status <> 'CANCELLED' THEN order_date END) AS last_order "
                + "FROM orders WHERE client_id = ?",
                rs -> {
                    ClientSummary cs = new ClientSummary();
                    cs.setTotalOrders(rs.getInt("total_orders"));
                    cs.setCancelledOrders(rs.getInt("cancelled_orders"));
                    cs.setLifetimeValue(getMoney(rs, "lifetime_value"));
                    cs.setFirstOrder(getDateTime(rs, "first_order"));
                    cs.setLastOrder(getDateTime(rs, "last_order"));
                    return cs;
                }, clientId);
        if (s == null) {
            s = new ClientSummary();
        }
        String favourite = queryOne(
                "SELECT p.name, SUM(oi.quantity) AS qty FROM order_items oi "
                + "JOIN orders o ON o.order_id = oi.order_id "
                + "JOIN products p ON p.product_id = oi.product_id "
                + "WHERE o.client_id = ? AND o.status <> 'CANCELLED' "
                + "GROUP BY p.product_id, p.name ORDER BY qty DESC, p.name LIMIT 1",
                rs -> rs.getString("name"), clientId);
        s.setFavouriteProduct(favourite);
        return s;
    }
}
