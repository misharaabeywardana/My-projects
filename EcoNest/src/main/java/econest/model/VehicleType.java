package econest.model;

public enum VehicleType {
    BICYCLE("Bicycle"),
    MOTORBIKE("Motorbike"),
    THREE_WHEELER("Three-wheeler"),
    VAN("Van"),
    LORRY("Lorry");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
