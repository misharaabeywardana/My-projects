package econest.ui.dialogs;

import econest.model.Delivery;
import econest.model.DeliveryAgent;
import econest.model.Order;
import econest.service.AgentService;
import econest.service.DeliveryService;
import econest.service.ValidationException;
import econest.ui.components.UiUtil;
import econest.util.Formatters;
import java.awt.Component;
import java.awt.Cursor;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

/**
 * Assign a delivery agent to an order and schedule it (Task 6). Saving also
 * e-mails the agent (Task 9). Warns about schedule clashes (extra feature).
 */
public class AssignDeliveryDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final DeliveryService deliveryService;
    private final JComboBox<Order> order = new JComboBox<>();
    private final JComboBox<DeliveryAgent> agent = new JComboBox<>();
    private final JSpinner when = new JSpinner(new SpinnerDateModel());
    private final JTextField notes = new JTextField(28);
    private final int existingDeliveryId;
    private DeliveryService.Result result;

    public AssignDeliveryDialog(Component parent, DeliveryService deliveryService, AgentService agentService,
                                List<Order> orders, Order preselected) {
        super(parent, "Assign delivery agent", "Assign & notify agent");
        this.deliveryService = deliveryService;
        for (Order o : orders) {
            order.addItem(o);
            if (preselected != null && o.getId() == preselected.getId()) {
                order.setSelectedItem(o);
            }
        }
        if (preselected != null && order.getSelectedItem() != preselected) {
            boolean found = false;
            for (int i = 0; i < order.getItemCount(); i++) {
                found |= order.getItemAt(i).getId() == preselected.getId();
            }
            if (!found) {
                order.addItem(preselected);
                order.setSelectedItem(preselected);
            }
        }
        for (DeliveryAgent a : agentService.getAvailable()) {
            agent.addItem(a);
        }
        Delivery existing = preselected == null ? null : deliveryService.getByOrder(preselected.getId());
        existingDeliveryId = existing == null ? 0 : existing.getId();
        LocalDateTime defaultTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        when.setValue(Date.from(defaultTime.atZone(ZoneId.systemDefault()).toInstant()));
        when.setEditor(new JSpinner.DateEditor(when, "yyyy-MM-dd HH:mm"));

        form.add("Order", order, true);
        form.add("Delivery agent", agent, true).hint("Least busy agents are listed first");
        if (agent.getItemCount() == 0) {
            form.hint("No available agents - add or re-activate one on the Delivery Agents screen");
        }
        form.add("Scheduled for", when, true).hint("Format: yyyy-MM-dd HH:mm (use the arrows or type)");
        form.add("Notes for agent", notes);
        if (existing != null) {
            form.hint("This order was already given to " + existing.getAgentName() + " ("
                    + existing.getStatus() + "). Saving re-assigns it.");
        }
    }

    @Override
    protected void save() {
        Order o = (Order) order.getSelectedItem();
        DeliveryAgent a = (DeliveryAgent) agent.getSelectedItem();
        if (o == null) {
            throw new ValidationException("There are no orders waiting for delivery.");
        }
        if (a == null) {
            throw new ValidationException("Please choose a delivery agent.");
        }
        LocalDateTime scheduled = LocalDateTime.ofInstant(((Date) when.getValue()).toInstant(), ZoneId.systemDefault())
                .withSecond(0).withNano(0);
        List<Delivery> clashes = deliveryService.findClashes(a.getId(), scheduled, existingDeliveryId);
        if (!clashes.isEmpty()) {
            StringBuilder sb = new StringBuilder(a.getName() + " already has a delivery within "
                    + DeliveryService.CLASH_WINDOW_MINUTES + " minutes of this time:\n");
            for (Delivery d : clashes) {
                sb.append("  - ").append(d.getOrderRef()).append(" at ").append(Formatters.dateTime(d.getScheduledAt()))
                        .append(" (").append(d.getClientAddress()).append(")\n");
            }
            sb.append("\nAssign anyway?");
            if (!UiUtil.confirm(this, sb.toString())) {
                throw new ValidationException("Schedule clash - choose another time or agent.");
            }
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            result = deliveryService.assignAgent(o.getId(), a.getId(), scheduled, notes.getText());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public DeliveryService.Result getResult() {
        return result;
    }
}
