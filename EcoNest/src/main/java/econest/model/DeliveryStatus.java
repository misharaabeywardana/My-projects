package econest.model;

public enum DeliveryStatus {
    ASSIGNED("Assigned"),
    IN_TRANSIT("In transit (dispatched)"),
    DELIVERED("Delivered"),
    FAILED("Failed");

    private final String label;

    DeliveryStatus(String label) {
        this.label = label;
    }

    public boolean canChangeTo(DeliveryStatus next) {
        switch (this) {
            case ASSIGNED:
                return next == IN_TRANSIT || next == FAILED;
            case IN_TRANSIT:
                return next == DELIVERED || next == FAILED;
            default:
                return false;
        }
    }

    /** Deliveries that still occupy an agent. */
    public boolean isOpen() {
        return this == ASSIGNED || this == IN_TRANSIT;
    }

    @Override
    public String toString() {
        return label;
    }
}
