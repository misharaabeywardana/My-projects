package econest.model;

/** A suggested purchase quantity for a product (additional feature - Zija). */
public class ReorderSuggestion {

    private final InventoryItem item;
    private final int soldLast30Days;
    private final double averageDailySales;
    private final int suggestedQuantity;

    public ReorderSuggestion(InventoryItem item, int soldLast30Days, int suggestedQuantity) {
        this.item = item;
        this.soldLast30Days = soldLast30Days;
        this.averageDailySales = soldLast30Days / 30.0;
        this.suggestedQuantity = suggestedQuantity;
    }

    public InventoryItem getItem() {
        return item;
    }

    public int getSoldLast30Days() {
        return soldLast30Days;
    }

    public double getAverageDailySales() {
        return averageDailySales;
    }

    public int getSuggestedQuantity() {
        return suggestedQuantity;
    }

    /** How many days the current stock will last at the current sales speed. -1 = no recent sales. */
    public int getDaysOfStockLeft() {
        if (averageDailySales <= 0) {
            return -1;
        }
        return (int) Math.floor(item.getQuantityOnHand() / averageDailySales);
    }
}
