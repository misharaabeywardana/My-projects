package econest.model;

public enum StockStatus {
    OUT_OF_STOCK("Out of stock"),
    LOW("Low stock"),
    OK("In stock");

    private final String label;

    StockStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
