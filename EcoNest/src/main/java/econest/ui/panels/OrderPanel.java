package econest.ui.panels;

import econest.model.Order;
import econest.model.OrderStatus;
import econest.service.AgentService;
import econest.service.ClientService;
import econest.service.DeliveryService;
import econest.service.OrderService;
import econest.service.ProductService;
import econest.ui.components.AppButton;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.AssignDeliveryDialog;
import econest.ui.dialogs.NewOrderDialog;
import econest.ui.dialogs.OrderDetailsDialog;
import econest.util.Formatters;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

/**
 * Task 5 (Fahath): create orders, calculate totals, view and cancel them.
 * Extra: printable invoice. "Assign Agent" links to Task 6.
 */
public class OrderPanel extends AbstractCrudPanel<Order> {

    private static final long serialVersionUID = 1L;

    private final OrderService orderService = new OrderService();
    private final DeliveryService deliveryService = new DeliveryService();
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final AppButton assignButton = new AppButton("Assign Agent", AppButton.Variant.ACCENT);

    public OrderPanel() {
        super("Client Orders", "Create orders, add products and quantities - totals are calculated automatically",
                new String[] {"Order ref", "Date", "Client", "Total", "Status", "Delivery agent", "Created by"},
                null, true);
        table.setColumnWidths(160, 130, 220, 120, 100, 150, 90);
        addButton.setText("New Order");
        editButton.setText("View / Invoice");
        deleteButton.setText("Cancel Order");
        statusFilter.addItem("All statuses");
        for (OrderStatus s : OrderStatus.values()) {
            statusFilter.addItem(s);
        }
        statusFilter.addActionListener(e -> applyFilter());
        addFilter(statusFilter);
        assignButton.addActionListener(e -> assignSelected());
        addButton(assignButton);
    }

    @Override
    protected List<Order> loadItems() {
        return orderService.getAll();
    }

    @Override
    protected Object[] toRow(Order o) {
        return new Object[] {o.getOrderRef(), Formatters.dateTime(o.getOrderDate()), o.getClientName(),
            Formatters.money(o.getTotalAmount()), o.getStatus(), o.getAgentName() == null ? "-" : o.getAgentName(),
            o.getCreatedBy()};
    }

    @Override
    protected String searchText(Order o) {
        return o.getOrderRef() + " " + o.getClientName() + " " + o.getAgentName();
    }

    @Override
    protected boolean passesFilters(Order o) {
        Object s = statusFilter.getSelectedItem();
        return !(s instanceof OrderStatus) || o.getStatus() == s;
    }

    @Override
    protected void selectionChanged(Order o) {
        boolean open = o != null && o.getStatus().isCancellable();
        assignButton.setEnabled(o == null || open);
        deleteButton.setEnabled(o == null || open);
    }

    @Override
    protected boolean onAdd() {
        try {
            NewOrderDialog dialog = new NewOrderDialog(this, orderService, new ClientService(), new ProductService());
            if (dialog.showDialog()) {
                Order o = dialog.getCreatedOrder();
                if (UiUtil.confirm(this, "Order " + o.getOrderRef() + " saved. Total " + Formatters.money(
                        o.getTotalAmount()) + ".\nStock has been updated.\n\nAssign a delivery agent now?")) {
                    refresh();
                    openAssign(orderService.getById(o.getId()));
                }
                return true;
            }
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
        return false;
    }

    @Override
    protected boolean onEdit(Order o) {
        try {
            new OrderDetailsDialog(this, o, orderService.buildInvoice(o.getId())).setVisible(true);
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
        return false;
    }

    @Override
    protected boolean onDelete(Order o) {
        if (!o.getStatus().isCancellable()) {
            UiUtil.warn(this, "Only Pending or Processing orders can be cancelled.");
            return false;
        }
        String reason = JOptionPane.showInputDialog(this, "Cancel order " + o.getOrderRef()
                + "?\nThe products will be returned to stock.\n\nReason (optional):", "Cancel order",
                JOptionPane.QUESTION_MESSAGE);
        if (reason == null) {
            return false;
        }
        try {
            orderService.cancelOrder(o.getId(), reason);
            UiUtil.info(this, "Order " + o.getOrderRef() + " cancelled and stock restored.");
            return true;
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
            return false;
        }
    }

    private void assignSelected() {
        Order o = requireSelection();
        if (o != null) {
            openAssign(o);
        }
    }

    private void openAssign(Order o) {
        try {
            AssignDeliveryDialog d = new AssignDeliveryDialog(this, deliveryService, new AgentService(),
                    orderService.getAwaitingDelivery(), o);
            if (d.showDialog()) {
                UiUtil.info(this, "Delivery assigned to " + d.getResult().getDelivery().getAgentName() + ".\n"
                        + UiUtil.emailOutcome(d.getResult().getEmail()));
                refresh();
            }
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }

    /** Used by the dashboard quick action. */
    public void openNewOrder() {
        if (onAdd()) {
            refresh();
        }
    }
}
