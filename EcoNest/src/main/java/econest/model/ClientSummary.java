package econest.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClientSummary {

    private int totalOrders;
    private int cancelledOrders;
    private BigDecimal lifetimeValue = BigDecimal.ZERO;
    private LocalDateTime firstOrder;
    private LocalDateTime lastOrder;
    private String favouriteProduct;

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public int getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(int cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public BigDecimal getLifetimeValue() {
        return lifetimeValue;
    }

    public void setLifetimeValue(BigDecimal lifetimeValue) {
        this.lifetimeValue = lifetimeValue;
    }

    public LocalDateTime getFirstOrder() {
        return firstOrder;
    }

    public void setFirstOrder(LocalDateTime firstOrder) {
        this.firstOrder = firstOrder;
    }

    public LocalDateTime getLastOrder() {
        return lastOrder;
    }

    public void setLastOrder(LocalDateTime lastOrder) {
        this.lastOrder = lastOrder;
    }

    public String getFavouriteProduct() {
        return favouriteProduct;
    }

    public void setFavouriteProduct(String favouriteProduct) {
        this.favouriteProduct = favouriteProduct;
    }

    /** Days since the last non-cancelled order, or -1 if the client never ordered. */
    public long getDaysSinceLastOrder() {
        if (lastOrder == null) {
            return -1;
        }
        return java.time.Duration.between(lastOrder, LocalDateTime.now()).toDays();
    }
}
