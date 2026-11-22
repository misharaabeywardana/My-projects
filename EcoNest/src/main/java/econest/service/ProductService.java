package econest.service;

import econest.dao.DBConnection;
import econest.dao.InventoryDao;
import econest.dao.ProductDao;
import econest.model.PriceHistory;
import econest.model.Product;
import econest.model.ProductCategory;
import econest.util.Session;
import econest.util.Validator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Business rules for the product catalogue (Task 1 - Senuji). */
public class ProductService {

    public static final BigDecimal MAX_PRICE = new BigDecimal("1000000");

    private final ProductDao productDao;
    private final InventoryDao inventoryDao;

    public ProductService() {
        this(new ProductDao(), new InventoryDao());
    }

    public ProductService(ProductDao productDao, InventoryDao inventoryDao) {
        this.productDao = productDao;
        this.inventoryDao = inventoryDao;
    }

    public List<Product> getAll() {
        return productDao.findAll();
    }

    public List<Product> getActive() {
        return productDao.findActive();
    }

    public Product getById(int id) {
        return productDao.findById(id);
    }

    /** Checks every field and throws ValidationException with a clear message. */
    public void validate(Product p) {
        if (Validator.isBlank(p.getName())) {
            throw new ValidationException("Product name is required.");
        }
        if (p.getName().trim().length() > 100) {
            throw new ValidationException("Product name must be 100 characters or less.");
        }
        if (p.getCategory() == null) {
            throw new ValidationException("Please choose a category (plant, planter or gardening kit).");
        }
        if (p.getPrice() == null || p.getPrice().signum() <= 0) {
            throw new ValidationException("Price must be greater than zero.");
        }
        if (p.getPrice().compareTo(MAX_PRICE) > 0) {
            throw new ValidationException("Price looks too high (maximum Rs. 1,000,000).");
        }
        if (p.getPrice().scale() > 2) {
            throw new ValidationException("Price can have at most 2 decimal places.");
        }
        if (p.getEcoRating() < 1 || p.getEcoRating() > 5) {
            throw new ValidationException("Eco-rating must be between 1 and 5.");
        }
        if (p.getDescription() != null && p.getDescription().length() > 255) {
            throw new ValidationException("Description must be 255 characters or less.");
        }
        if (productDao.nameExists(p.getName().trim(), p.getId())) {
            throw new ValidationException("A product called \"" + p.getName().trim() + "\" already exists.");
        }
    }

    /** Adds a product and creates its inventory record in one transaction. */
    public int addProduct(Product p, int openingStock, int reorderLevel) {
        p.setName(p.getName() == null ? null : p.getName().trim());
        p.setDescription(Validator.trimToNull(p.getDescription()));
        validate(p);
        if (openingStock < 0) {
            throw new ValidationException("Opening stock cannot be negative.");
        }
        if (reorderLevel < 0) {
            throw new ValidationException("Reorder level cannot be negative.");
        }
        int id = DBConnection.inTransaction(con -> {
            int newId = productDao.insert(con, p);
            inventoryDao.createRow(con, newId, openingStock, reorderLevel);
            return newId;
        });
        p.setId(id);
        return id;
    }

    /** Saves changes; a price change is also written to the price history. */
    public void updateProduct(Product p) {
        p.setName(p.getName() == null ? null : p.getName().trim());
        p.setDescription(Validator.trimToNull(p.getDescription()));
        validate(p);
        DBConnection.inTransaction(con -> {
            Product old = productDao.findById(con, p.getId());
            if (old == null) {
                throw new ValidationException("This product no longer exists.");
            }
            productDao.update(con, p);
            if (old.getPrice().compareTo(p.getPrice()) != 0) {
                productDao.insertPriceHistory(con,
                        new PriceHistory(p.getId(), old.getPrice(), p.getPrice(), Session.username()));
            }
            return null;
        });
    }

    /**
     * Removes a product. Products that were already ordered are kept for the
     * reports and just marked inactive (they disappear from new orders).
     *
     * @return a message describing what happened
     */
    public String removeProduct(int productId) {
        Product p = productDao.findById(productId);
        if (p == null) {
            throw new ValidationException("This product no longer exists.");
        }
        if (productDao.isUsedInOrders(productId)) {
            productDao.setActive(productId, false);
            return "\"" + p.getName() + "\" appears in past orders, so it was marked INACTIVE instead of deleted.\n"
                    + "It is hidden from new orders but kept in reports.";
        }
        productDao.delete(productId);
        return "\"" + p.getName() + "\" was deleted.";
    }

    public void reactivate(int productId) {
        productDao.setActive(productId, true);
    }

    public List<PriceHistory> getPriceHistory(int productId) {
        return productDao.findPriceHistory(productId);
    }

    /** Search by text plus optional category and minimum eco-rating filter. */
    public static List<Product> filter(List<Product> products, String text, ProductCategory category,
                                       int minEcoRating, boolean includeInactive) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        List<Product> result = new ArrayList<>();
        for (Product p : products) {
            if (!includeInactive && !p.isActive()) {
                continue;
            }
            if (category != null && p.getCategory() != category) {
                continue;
            }
            if (p.getEcoRating() < minEcoRating) {
                continue;
            }
            String haystack = (p.getName() + " " + (p.getDescription() == null ? "" : p.getDescription()))
                    .toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !haystack.contains(q)) {
                continue;
            }
            result.add(p);
        }
        return result;
    }
}
