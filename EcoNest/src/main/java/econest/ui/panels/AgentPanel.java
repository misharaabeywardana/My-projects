package econest.ui.panels;

import econest.model.DeliveryAgent;
import econest.model.VehicleType;
import econest.service.AgentService;
import econest.ui.components.AppButton;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.AgentDialog;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

/**
 * Task 4 (Zija): manage delivery agents and vehicles.
 * Extra: availability switch and live workload (open deliveries) column.
 */
public class AgentPanel extends AbstractCrudPanel<DeliveryAgent> {

    private static final long serialVersionUID = 1L;

    private final AgentService service = new AgentService();
    private final JCheckBox availableOnly = new JCheckBox("Available only");
    private final JComboBox<Object> vehicleFilter = new JComboBox<>();

    public AgentPanel() {
        super("Delivery Agents", "Delivery personnel, their vehicles, availability and current workload",
                new String[] {"ID", "Name", "Phone", "E-mail", "Vehicle", "Vehicle no.", "Availability",
                    "Open deliveries"},
                new Class<?>[] {Integer.class, null, null, null, null, null, null, Integer.class}, true);
        table.setColumnWidths(45, 170, 110, 220, 110, 110, 100, 110);
        addButton.setText("Add Agent");
        vehicleFilter.addItem("All vehicles");
        for (VehicleType v : VehicleType.values()) {
            vehicleFilter.addItem(v);
        }
        vehicleFilter.addActionListener(e -> applyFilter());
        availableOnly.setOpaque(false);
        availableOnly.addActionListener(e -> applyFilter());
        addFilter(vehicleFilter);
        addFilter(availableOnly);
        AppButton toggle = new AppButton("Toggle Availability");
        toggle.addActionListener(e -> toggle());
        addButton(toggle);
    }

    @Override
    protected List<DeliveryAgent> loadItems() {
        return service.getAll();
    }

    @Override
    protected Object[] toRow(DeliveryAgent a) {
        return new Object[] {a.getId(), a.getName(), a.getPhone(), a.getEmail(), a.getVehicleType(),
            a.getVehicleNumber(), a.isActive() ? "Available" : "Unavailable", a.getOpenDeliveries()};
    }

    @Override
    protected String searchText(DeliveryAgent a) {
        return a.getName() + " " + a.getPhone() + " " + a.getEmail() + " " + a.getVehicleNumber();
    }

    @Override
    protected boolean passesFilters(DeliveryAgent a) {
        Object v = vehicleFilter.getSelectedItem();
        if (v instanceof VehicleType && a.getVehicleType() != v) {
            return false;
        }
        return !availableOnly.isSelected() || a.isActive();
    }

    @Override
    protected boolean onAdd() {
        return new AgentDialog(this, service, null).showDialog();
    }

    @Override
    protected boolean onEdit(DeliveryAgent a) {
        return new AgentDialog(this, service, service.getById(a.getId())).showDialog();
    }

    @Override
    protected boolean onDelete(DeliveryAgent a) {
        if (!UiUtil.confirm(this, "Delete agent \"" + a.getName() + "\"?")) {
            return false;
        }
        try {
            service.deleteAgent(a.getId());
            return true;
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
            return false;
        }
    }

    private void toggle() {
        DeliveryAgent a = requireSelection();
        if (a == null) {
            return;
        }
        try {
            service.setAvailable(a.getId(), !a.isActive());
            refresh();
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }
}
