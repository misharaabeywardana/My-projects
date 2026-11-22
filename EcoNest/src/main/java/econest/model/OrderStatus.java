package econest.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life-cycle of a client order. The allowed moves are kept here so every
 * screen and service applies the same rule.
 */
public enum OrderStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    DISPATCHED("Dispatched"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public Set<OrderStatus> allowedNext() {
        switch (this) {
            case PENDING:
                return EnumSet.of(PROCESSING, CANCELLED);
            case PROCESSING:
                return EnumSet.of(DISPATCHED, CANCELLED);
            case DISPATCHED:
                // back to PROCESSING when a delivery attempt fails
                return EnumSet.of(DELIVERED, PROCESSING);
            default:
                return EnumSet.noneOf(OrderStatus.class);
        }
    }

    public boolean canChangeTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }

    @Override
    public String toString() {
        return label;
    }
}
