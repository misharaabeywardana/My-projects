package econest.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Headline figures of the monthly sales report (Task 7). */
public class MonthlySalesSummary {

    private int completedOrders;
    private int cancelledOrders;
    private int itemsSold;
    private BigDecimal revenue = BigDecimal.ZERO;
    private int newClients;

    public int getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(int completedOrders) {
        this.completedOrders = completedOrders;
    }

    public int getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(int cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public int getItemsSold() {
        return itemsSold;
    }

    public void setItemsSold(int itemsSold) {
        this.itemsSold = itemsSold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public int getNewClients() {
        return newClients;
    }

    public void setNewClients(int newClients) {
        this.newClients = newClients;
    }

    public BigDecimal getAverageOrderValue() {
        if (completedOrders == 0) {
            return BigDecimal.ZERO;
        }
        return revenue.divide(BigDecimal.valueOf(completedOrders), 2, RoundingMode.HALF_UP);
    }
}
