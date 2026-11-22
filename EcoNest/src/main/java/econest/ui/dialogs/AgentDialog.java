package econest.ui.dialogs;

import econest.model.DeliveryAgent;
import econest.model.VehicleType;
import econest.service.AgentService;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/** Add / edit a delivery agent and their vehicle (Task 4). */
public class AgentDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final AgentService service;
    private final DeliveryAgent agent;
    private final JTextField name = new JTextField(24);
    private final JTextField phone = new JTextField(16);
    private final JTextField email = new JTextField(24);
    private final JComboBox<VehicleType> vehicleType = new JComboBox<>(VehicleType.values());
    private final JTextField vehicleNumber = new JTextField(14);
    private final JCheckBox available = new JCheckBox("Available for new deliveries", true);

    public AgentDialog(Component parent, AgentService service, DeliveryAgent existing) {
        super(parent, existing == null ? "Add delivery agent" : "Edit delivery agent",
                existing == null ? "Add agent" : "Save");
        this.service = service;
        this.agent = existing == null ? new DeliveryAgent() : existing;
        available.setOpaque(false);
        form.add("Full name", name, true);
        form.add("Phone", phone, true);
        form.add("E-mail", email, true).hint("New delivery assignments are e-mailed here");
        form.add("Vehicle type", vehicleType, true);
        form.add("Vehicle number", vehicleNumber, true).hint("e.g. WP CAB-1234");
        form.add("Status", available);
        if (existing != null) {
            name.setText(existing.getName());
            phone.setText(existing.getPhone());
            email.setText(existing.getEmail());
            vehicleType.setSelectedItem(existing.getVehicleType());
            vehicleNumber.setText(existing.getVehicleNumber());
            available.setSelected(existing.isActive());
        }
    }

    @Override
    protected void save() {
        agent.setName(name.getText());
        agent.setPhone(phone.getText());
        agent.setEmail(email.getText());
        agent.setVehicleType((VehicleType) vehicleType.getSelectedItem());
        agent.setVehicleNumber(vehicleNumber.getText());
        agent.setActive(available.isSelected());
        if (agent.isNew()) {
            service.addAgent(agent);
        } else {
            service.updateAgent(agent);
        }
    }
}
