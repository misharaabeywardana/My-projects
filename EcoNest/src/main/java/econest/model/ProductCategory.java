package econest.model;

public enum ProductCategory {
    PLANT("Plant"),
    PLANTER("Planter"),
    GARDEN_KIT("Gardening Kit");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
