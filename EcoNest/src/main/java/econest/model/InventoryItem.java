package econest.model;

import java.time.LocalDateTime;

/** Stock level of one product (Task 3). */
public class InventoryItem {

    private int productId;
    private String productName;
    private ProductCategory category;
    private boolean productActive = true;
    private int quantityOnHand;
    private int reorderLevel;
    private LocalDateTime updatedAt;

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public boolean isProductActive() {
        return productActive;
    }

    public void setProductActive(boolean productActive) {
        this.productActive = productActive;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Business rule: 0 = out of stock, at or below reorder level = low. */
    public StockStatus getStockStatus() {
        if (quantityOnHand <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (quantityOnHand <= reorderLevel) {
            return StockStatus.LOW;
        }
        return StockStatus.OK;
    }

    public boolean isLowStock() {
        return getStockStatus() != StockStatus.OK;
    }
}
