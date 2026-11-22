package econest.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A client order with its product lines (Task 5). */
public class Order extends BaseEntity {

    private String orderRef;
    private int clientId;
    private String clientName;
    private LocalDateTime orderDate = LocalDateTime.now();
    private OrderStatus status = OrderStatus.PENDING;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String notes;
    private String createdBy;
    /** Name of the assigned agent, if any (read-only, from deliveries). */
    private String agentName;
    private final List<OrderItem> items = new ArrayList<>();

    public String getOrderRef() {
        return orderRef;
    }

    public void setOrderRef(String orderRef) {
        this.orderRef = orderRef;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    /**
     * Adds a product line. If the product is already in the order the
     * quantities are merged instead of creating a duplicate line.
     */
    public void addItem(OrderItem item) {
        for (OrderItem existing : items) {
            if (existing.getProductId() == item.getProductId()) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                recalculateTotal();
                return;
            }
        }
        items.add(item);
        recalculateTotal();
    }

    public void removeItem(int index) {
        items.remove(index);
        recalculateTotal();
    }

    /** Order total = sum of all line totals. */
    public BigDecimal calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }

    public void recalculateTotal() {
        totalAmount = calculateTotal();
    }

    public int getTotalQuantity() {
        int qty = 0;
        for (OrderItem item : items) {
            qty += item.getQuantity();
        }
        return qty;
    }

    @Override
    public String getDisplayName() {
        return orderRef + " - " + clientName;
    }
}
