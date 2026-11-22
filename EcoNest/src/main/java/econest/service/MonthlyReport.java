package econest.service;

import econest.model.DailyRevenue;
import econest.model.InventoryItem;
import econest.model.MonthlySalesSummary;
import econest.model.ProductSales;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Everything shown on the monthly report screen, built from real database data. */
public class MonthlyReport {

    private final YearMonth month;
    private final MonthlySalesSummary summary;
    private final List<ProductSales> productSales;
    private final List<DailyRevenue> dailyRevenue;
    private final List<InventoryItem> inventory;
    private final Map<Integer, Integer> unitsReceived;
    private final Map<Integer, Integer> unitsSold;
    private final BigDecimal stockPurchaseCost;

    public MonthlyReport(YearMonth month, MonthlySalesSummary summary, List<ProductSales> productSales,
                         List<DailyRevenue> dailyRevenue, List<InventoryItem> inventory,
                         Map<Integer, Integer> unitsReceived, Map<Integer, Integer> unitsSold,
                         BigDecimal stockPurchaseCost) {
        this.month = month;
        this.summary = summary;
        this.productSales = productSales;
        this.dailyRevenue = dailyRevenue;
        this.inventory = inventory;
        this.unitsReceived = unitsReceived;
        this.unitsSold = unitsSold;
        this.stockPurchaseCost = stockPurchaseCost;
    }

    public YearMonth getMonth() {
        return month;
    }

    public MonthlySalesSummary getSummary() {
        return summary;
    }

    public List<ProductSales> getProductSales() {
        return productSales;
    }

    /** One entry per day of the month, including days with no sales. */
    public List<DailyRevenue> getDailyRevenue() {
        return dailyRevenue;
    }

    public List<InventoryItem> getInventory() {
        return inventory;
    }

    public int getUnitsReceived(int productId) {
        return unitsReceived.getOrDefault(productId, 0);
    }

    public int getUnitsSold(int productId) {
        return unitsSold.getOrDefault(productId, 0);
    }

    public BigDecimal getStockPurchaseCost() {
        return stockPurchaseCost;
    }

    public long getLowStockCount() {
        return inventory.stream().filter(i -> i.isProductActive() && i.isLowStock()).count();
    }

    public ProductSales getBestSeller() {
        ProductSales best = null;
        for (ProductSales p : productSales) {
            if (best == null || p.getQuantity() > best.getQuantity()) {
                best = p;
            }
        }
        return best;
    }
}
