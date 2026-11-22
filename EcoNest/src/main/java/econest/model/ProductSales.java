package econest.model;

import java.math.BigDecimal;

/** Units and revenue of one product within a period. */
public class ProductSales {

    private final int productId;
    private final String productName;
    private final ProductCategory category;
    private final int quantity;
    private final BigDecimal revenue;

    public ProductSales(int productId, String productName, ProductCategory category,
                        int quantity, BigDecimal revenue) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
