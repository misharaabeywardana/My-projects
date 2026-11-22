package econest.model;

import java.math.BigDecimal;

/** A plant, planter or gardening kit sold by EcoNest (Task 1). */
public class Product extends BaseEntity {

    private String name;
    private ProductCategory category = ProductCategory.PLANT;
    private String description;
    private BigDecimal price = BigDecimal.ZERO;
    private int ecoRating = 3;
    private boolean active = true;
    /** Read-only copy of the stock level, filled when products are listed. */
    private int quantityOnHand;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getEcoRating() {
        return ecoRating;
    }

    public void setEcoRating(int ecoRating) {
        this.ecoRating = ecoRating;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    /** Eco-rating shown as stars, e.g. 3 -> three filled and two empty stars. */
    public String getEcoStars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= ecoRating ? '★' : '☆');
        }
        return sb.toString();
    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
