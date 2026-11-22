package econest.model;

public enum EmailStatus {
    SENT("Sent"),
    FAILED("Failed"),
    SIMULATED("Simulated (not sent)");

    private final String label;

    EmailStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
