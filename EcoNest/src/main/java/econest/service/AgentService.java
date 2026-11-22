package econest.service;

import econest.dao.AgentDao;
import econest.model.DeliveryAgent;
import econest.util.Validator;
import java.util.List;

/** Business rules for delivery agents (Task 4 - Zija). */
public class AgentService {

    private final AgentDao agentDao;

    public AgentService() {
        this(new AgentDao());
    }

    public AgentService(AgentDao agentDao) {
        this.agentDao = agentDao;
    }

    public List<DeliveryAgent> getAll() {
        return agentDao.findAll();
    }

    /** Active agents, least busy first (used when assigning deliveries). */
    public List<DeliveryAgent> getAvailable() {
        return agentDao.findActive();
    }

    public DeliveryAgent getById(int id) {
        return agentDao.findById(id);
    }

    public void validate(DeliveryAgent a) {
        if (Validator.isBlank(a.getName())) {
            throw new ValidationException("Agent name is required.");
        }
        if (a.getName().length() > 100) {
            throw new ValidationException("Agent name must be 100 characters or less.");
        }
        if (!Validator.isValidPhone(a.getPhone())) {
            throw new ValidationException("Enter a valid phone number, e.g. 0771234567.");
        }
        if (!Validator.isValidEmail(a.getEmail())) {
            throw new ValidationException("Enter a valid e-mail address. Agents receive delivery assignments by e-mail.");
        }
        if (a.getVehicleType() == null) {
            throw new ValidationException("Please choose a vehicle type.");
        }
        if (!Validator.isValidVehicleNumber(a.getVehicleNumber())) {
            throw new ValidationException("Enter a valid vehicle number, e.g. WP CAB-1234 (3-20 letters/digits).");
        }
        if (agentDao.vehicleNumberExists(a.getVehicleNumber(), a.getId())) {
            throw new ValidationException("Vehicle number " + a.getVehicleNumber() + " is already registered.");
        }
    }

    private void clean(DeliveryAgent a) {
        a.setName(a.getName() == null ? null : a.getName().trim());
        a.setPhone(a.getPhone() == null ? null : a.getPhone().trim());
        a.setEmail(a.getEmail() == null ? null : a.getEmail().trim());
        a.setVehicleNumber(a.getVehicleNumber() == null ? null : a.getVehicleNumber().trim().toUpperCase());
    }

    public int addAgent(DeliveryAgent a) {
        clean(a);
        validate(a);
        int id = agentDao.insert(a);
        a.setId(id);
        return id;
    }

    public void updateAgent(DeliveryAgent a) {
        clean(a);
        validate(a);
        DeliveryAgent current = agentDao.findById(a.getId());
        if (current != null && current.isActive() && !a.isActive() && current.getOpenDeliveries() > 0) {
            throw new ValidationException(a.getName() + " still has " + current.getOpenDeliveries()
                    + " open deliveries. Reassign or complete them before marking the agent unavailable.");
        }
        agentDao.update(a);
    }

    /** Extra feature: switch availability without deleting the agent. */
    public void setAvailable(int agentId, boolean available) {
        DeliveryAgent a = agentDao.findById(agentId);
        if (a == null) {
            throw new ValidationException("This agent no longer exists.");
        }
        if (!available && a.getOpenDeliveries() > 0) {
            throw new ValidationException(a.getName() + " still has " + a.getOpenDeliveries()
                    + " open deliveries. Reassign or complete them first.");
        }
        agentDao.setActive(agentId, available);
    }

    public void deleteAgent(int agentId) {
        if (agentDao.hasDeliveries(agentId)) {
            throw new ValidationException("This agent has delivery history and cannot be deleted.\n"
                    + "Use \"Toggle availability\" to mark them unavailable instead.");
        }
        agentDao.delete(agentId);
    }
}
