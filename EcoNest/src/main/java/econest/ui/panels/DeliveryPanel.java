package econest.ui.panels;

import econest.model.Delivery;
import econest.model.DeliveryStatus;
import econest.service.AgentService;
import econest.service.DeliveryService;
import econest.service.OrderService;
import econest.ui.components.AppButton;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.AssignDeliveryDialog;
import econest.util.Formatters;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

/**
 * Task 6 (Fahath): assign agents, track and update delivery status.
 * Marking a delivery "dispatched" e-mails the client (Task 8).
 */
public class DeliveryPanel extends AbstractCrudPanel<Delivery> {

    private static final long serialVersionUID = 1L;

    private final DeliveryService service = new DeliveryService();
    private final OrderService orderService = new OrderService();
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final AppButton dispatchButton = new AppButton("Mark Dispatched", AppButton.Variant.ACCENT);
    private final AppButton deliveredButton = new AppButton("Mark Delivered", AppButton.Variant.PRIMARY);
    private final AppButton failedButton = new AppButton("Mark Failed", AppButton.Variant.DANGER);

    public DeliveryPanel() {
        super("Deliveries", "Assign delivery agents to orders, schedule and track delivery status",
                new String[] {"Order ref", "Client", "Address", "Agent", "Scheduled", "Status", "Delivered at",
                    "Notes"}, null, true);
        table.setColumnWidths(150, 160, 220, 140, 120, 150, 120, 180);
        addButton.setText("Assign Agent");
        editButton.setText("Reassign");
        deleteButton.setVisible(false);
        statusFilter.addItem("All statuses");
        for (DeliveryStatus s : DeliveryStatus.values()) {
            statusFilter.addItem(s);
        }
        statusFilter.addActionListener(e -> applyFilter());
        addFilter(statusFilter);
        dispatchButton.setToolTipText("Order leaves the shop - the client is e-mailed automatically");
        dispatchButton.addActionListener(e -> change(DeliveryStatus.IN_TRANSIT));
        deliveredButton.addActionListener(e -> change(DeliveryStatus.DELIVERED));
        failedButton.addActionListener(e -> change(DeliveryStatus.FAILED));
        addButton(dispatchButton);
        addButton(deliveredButton);
        addButton(failedButton);
    }

    @Override
    protected List<Delivery> loadItems() {
        return service.getAll();
    }

    @Override
    protected Object[] toRow(Delivery d) {
        return new Object[] {d.getOrderRef(), d.getClientName(), d.getClientAddress(), d.getAgentName(),
            Formatters.dateTime(d.getScheduledAt()), d.getStatus(), Formatters.dateTime(d.getDeliveredAt()),
            d.getNotes() == null ? "" : d.getNotes()};
    }

    @Override
    protected String searchText(Delivery d) {
        return d.getOrderRef() + " " + d.getClientName() + " " + d.getAgentName() + " " + d.getClientAddress();
    }

    @Override
    protected boolean passesFilters(Delivery d) {
        Object s = statusFilter.getSelectedItem();
        return !(s instanceof DeliveryStatus) || d.getStatus() == s;
    }

    @Override
    protected void selectionChanged(Delivery d) {
        dispatchButton.setEnabled(d == null || d.getStatus().canChangeTo(DeliveryStatus.IN_TRANSIT));
        deliveredButton.setEnabled(d == null || d.getStatus().canChangeTo(DeliveryStatus.DELIVERED));
        failedButton.setEnabled(d == null || d.getStatus().canChangeTo(DeliveryStatus.FAILED));
        editButton.setEnabled(d == null || d.getStatus() == DeliveryStatus.ASSIGNED
                || d.getStatus() == DeliveryStatus.FAILED);
    }

    @Override
    protected boolean onAdd() {
        return openAssign(null);
    }

    @Override
    protected boolean onEdit(Delivery d) {
        if (d.getStatus() != DeliveryStatus.ASSIGNED && d.getStatus() != DeliveryStatus.FAILED) {
            UiUtil.warn(this, "Only Assigned or Failed deliveries can be re-assigned.");
            return false;
        }
        return openAssign(orderService.getById(d.getOrderId()));
    }

    @Override
    protected boolean onDelete(Delivery d) {
        return false;
    }

    private boolean openAssign(econest.model.Order preselected) {
        try {
            AssignDeliveryDialog dialog = new AssignDeliveryDialog(this, service, new AgentService(),
                    orderService.getAwaitingDelivery(), preselected);
            if (dialog.showDialog()) {
                UiUtil.info(this, "Delivery assigned to " + dialog.getResult().getDelivery().getAgentName() + ".\n"
                        + UiUtil.emailOutcome(dialog.getResult().getEmail()));
                return true;
            }
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
        return false;
    }

    private void change(DeliveryStatus next) {
        Delivery d = requireSelection();
        if (d == null) {
            return;
        }
        String notes = null;
        if (next == DeliveryStatus.FAILED) {
            notes = JOptionPane.showInputDialog(this, "Why did delivery " + d.getOrderRef() + " fail?",
                    "Delivery failed", JOptionPane.QUESTION_MESSAGE);
            if (notes == null) {
                return;
            }
        } else {
            String question = next == DeliveryStatus.IN_TRANSIT
                    ? "Mark " + d.getOrderRef() + " as dispatched?\nThe client will be e-mailed automatically."
                    : "Mark " + d.getOrderRef() + " as delivered?";
            if (!UiUtil.confirm(this, question)) {
                return;
            }
        }
        final String reason = notes;
        UiUtil.runAsync(this, () -> service.updateStatus(d.getId(), next, reason), result -> {
            String msg = d.getOrderRef() + " is now " + result.getDelivery().getStatus() + ".";
            if (result.getEmail() != null) {
                msg += "\n" + UiUtil.emailOutcome(result.getEmail());
            }
            if (next == DeliveryStatus.FAILED) {
                msg += "\nThe order is back to Processing - use Reassign to schedule it again.";
            }
            UiUtil.info(this, msg);
            refresh();
        });
    }
}
