package econest.model;

public enum ClientType {
    HOUSEHOLD("Household"),
    OFFICE("Office");

    private final String label;

    ClientType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
