package econest.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One recorded price change of a product (additional feature - Senuji). */
public class PriceHistory {

    private int productId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String changedBy;
    private LocalDateTime changedAt;

    public PriceHistory() {
    }

    public PriceHistory(int productId, BigDecimal oldPrice, BigDecimal newPrice, String changedBy) {
        this.productId = productId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.changedBy = changedBy;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(BigDecimal oldPrice) {
        this.oldPrice = oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    /** Percentage change, e.g. +12.5 or -5.0. */
    public double getChangePercent() {
        if (oldPrice == null || oldPrice.signum() == 0) {
            return 0;
        }
        return newPrice.subtract(oldPrice).doubleValue() * 100.0 / oldPrice.doubleValue();
    }
}
