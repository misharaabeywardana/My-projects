package econest.dao;

import econest.model.StockIn;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** SQL for the stock_in table (stock received from suppliers). */
public class StockInDao extends BaseDao {

    private static final String SELECT =
            "SELECT s.stock_in_id, s.product_id, p.name AS product_name, s.supplier_id, "
            + "sp.name AS supplier_name, s.quantity, s.unit_cost, s.received_date, s.note, s.recorded_by "
            + "FROM stock_in s JOIN products p ON p.product_id = s.product_id "
            + "JOIN suppliers sp ON sp.supplier_id = s.supplier_id ";

    private StockIn map(ResultSet rs) throws SQLException {
        StockIn s = new StockIn();
        s.setId(rs.getInt("stock_in_id"));
        s.setProductId(rs.getInt("product_id"));
        s.setProductName(rs.getString("product_name"));
        s.setSupplierId(rs.getInt("supplier_id"));
        s.setSupplierName(rs.getString("supplier_name"));
        s.setQuantity(rs.getInt("quantity"));
        s.setUnitCost(getMoney(rs, "unit_cost"));
        s.setReceivedDate(getDate(rs, "received_date"));
        s.setNote(rs.getString("note"));
        s.setRecordedBy(rs.getString("recorded_by"));
        return s;
    }

    public List<StockIn> findAll() {
        return query(SELECT + "ORDER BY s.received_date DESC, s.stock_in_id DESC", this::map);
    }

    public int insert(Connection con, StockIn s) {
        return insert(con, "INSERT INTO stock_in (product_id, supplier_id, quantity, unit_cost, received_date, "
                + "note, recorded_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                s.getProductId(), s.getSupplierId(), s.getQuantity(), s.getUnitCost(), s.getReceivedDate(),
                s.getNote(), s.getRecordedBy());
    }

    /** Units received per product between two dates (inclusive start, exclusive end). */
    public Map<Integer, Integer> unitsReceived(LocalDate from, LocalDate toExclusive) {
        Map<Integer, Integer> received = new HashMap<>();
        query("SELECT product_id, SUM(quantity) AS qty FROM stock_in "
                + "WHERE received_date >= ? AND received_date < ? GROUP BY product_id",
                rs -> received.put(rs.getInt("product_id"), rs.getInt("qty")), from, toExclusive);
        return received;
    }
}
