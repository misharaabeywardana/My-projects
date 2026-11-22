package econest.model;

public enum Role {
    ADMIN("Administrator"),
    STAFF("Staff");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
