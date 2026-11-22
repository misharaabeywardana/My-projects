package econest.dao;

import econest.model.DeliveryAgent;
import econest.model.VehicleType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** SQL for the delivery_agents table. */
public class AgentDao extends BaseDao implements CrudDao<DeliveryAgent> {

    private static final String SELECT =
            "SELECT a.agent_id, a.name, a.phone, a.email, a.vehicle_type, a.vehicle_number, a.active, "
            + "(SELECT COUNT(*) FROM deliveries d WHERE d.agent_id = a.agent_id "
            + " AND d.status IN ('ASSIGNED','IN_TRANSIT')) AS open_count "
            + "FROM delivery_agents a ";

    private DeliveryAgent map(ResultSet rs) throws SQLException {
        DeliveryAgent a = new DeliveryAgent();
        a.setId(rs.getInt("agent_id"));
        a.setName(rs.getString("name"));
        a.setPhone(rs.getString("phone"));
        a.setEmail(rs.getString("email"));
        a.setVehicleType(VehicleType.valueOf(rs.getString("vehicle_type")));
        a.setVehicleNumber(rs.getString("vehicle_number"));
        a.setActive(rs.getInt("active") == 1);
        a.setOpenDeliveries(rs.getInt("open_count"));
        return a;
    }

    @Override
    public List<DeliveryAgent> findAll() {
        return query(SELECT + "ORDER BY a.name", this::map);
    }

    /** Agents who can receive new deliveries, least busy first. */
    public List<DeliveryAgent> findActive() {
        return query(SELECT + "WHERE a.active = 1 ORDER BY open_count, a.name", this::map);
    }

    @Override
    public DeliveryAgent findById(int id) {
        return queryOne(SELECT + "WHERE a.agent_id = ?", this::map, id);
    }

    @Override
    public int insert(DeliveryAgent a) {
        return insert("INSERT INTO delivery_agents (name, phone, email, vehicle_type, vehicle_number, active) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                a.getName(), a.getPhone(), a.getEmail(), a.getVehicleType(), a.getVehicleNumber(), a.isActive());
    }

    @Override
    public void update(DeliveryAgent a) {
        update("UPDATE delivery_agents SET name = ?, phone = ?, email = ?, vehicle_type = ?, vehicle_number = ?, "
                + "active = ? WHERE agent_id = ?",
                a.getName(), a.getPhone(), a.getEmail(), a.getVehicleType(), a.getVehicleNumber(), a.isActive(),
                a.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM delivery_agents WHERE agent_id = ?", id);
    }

    public void setActive(int id, boolean active) {
        update("UPDATE delivery_agents SET active = ? WHERE agent_id = ?", active, id);
    }

    public boolean hasDeliveries(int agentId) {
        return queryInt("SELECT COUNT(*) FROM deliveries WHERE agent_id = ?", agentId) > 0;
    }

    public boolean vehicleNumberExists(String vehicleNumber, int excludeId) {
        return queryInt("SELECT COUNT(*) FROM delivery_agents WHERE UPPER(vehicle_number) = UPPER(?) "
                + "AND agent_id <> ?", vehicleNumber, excludeId) > 0;
    }

    public int countActive() {
        return queryInt("SELECT COUNT(*) FROM delivery_agents WHERE active = 1");
    }
}
