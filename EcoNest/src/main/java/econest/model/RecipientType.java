package econest.model;

public enum RecipientType {
    CLIENT("Client"),
    AGENT("Delivery agent"),
    TEST("Test");

    private final String label;

    RecipientType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
