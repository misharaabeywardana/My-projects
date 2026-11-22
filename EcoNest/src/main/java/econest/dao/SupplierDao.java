package econest.dao;

import econest.model.Supplier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** SQL for the suppliers table. */
public class SupplierDao extends BaseDao implements CrudDao<Supplier> {

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("supplier_id"));
        s.setName(rs.getString("name"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        return s;
    }

    @Override
    public List<Supplier> findAll() {
        return query("SELECT supplier_id, name, phone, email FROM suppliers ORDER BY name", this::map);
    }

    @Override
    public Supplier findById(int id) {
        return queryOne("SELECT supplier_id, name, phone, email FROM suppliers WHERE supplier_id = ?", this::map, id);
    }

    @Override
    public int insert(Supplier s) {
        return insert("INSERT INTO suppliers (name, phone, email) VALUES (?, ?, ?)",
                s.getName(), s.getPhone(), s.getEmail());
    }

    @Override
    public void update(Supplier s) {
        update("UPDATE suppliers SET name = ?, phone = ?, email = ? WHERE supplier_id = ?",
                s.getName(), s.getPhone(), s.getEmail(), s.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM suppliers WHERE supplier_id = ?", id);
    }

    public boolean hasStockRecords(int supplierId) {
        return queryInt("SELECT COUNT(*) FROM stock_in WHERE supplier_id = ?", supplierId) > 0;
    }
}
