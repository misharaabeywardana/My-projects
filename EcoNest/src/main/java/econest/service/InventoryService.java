package econest.service;

import econest.dao.DBConnection;
import econest.dao.InventoryDao;
import econest.dao.ProductDao;
import econest.dao.StockInDao;
import econest.dao.SupplierDao;
import econest.model.InventoryItem;
import econest.model.Product;
import econest.model.ReorderSuggestion;
import econest.model.StockIn;
import econest.model.StockStatus;
import econest.model.Supplier;
import econest.util.Session;
import econest.util.Validator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Business rules for stock, suppliers and stock-in (Task 3 - Zija). */
public class InventoryService {

    /** Products with less than this many days of stock left are suggested for reorder. */
    public static final int COVER_DAYS_WARNING = 14;
    /** Suggestions aim to cover this many days of sales. */
    public static final int TARGET_COVER_DAYS = 30;

    private final InventoryDao inventoryDao;
    private final StockInDao stockInDao;
    private final SupplierDao supplierDao;
    private final ProductDao productDao;

    public InventoryService() {
        this(new InventoryDao(), new StockInDao(), new SupplierDao(), new ProductDao());
    }

    public InventoryService(InventoryDao inventoryDao, StockInDao stockInDao, SupplierDao supplierDao,
                            ProductDao productDao) {
        this.inventoryDao = inventoryDao;
        this.stockInDao = stockInDao;
        this.supplierDao = supplierDao;
        this.productDao = productDao;
    }

    public List<InventoryItem> getAll() {
        return inventoryDao.findAll();
    }

    public InventoryItem getByProduct(int productId) {
        return inventoryDao.findByProduct(productId);
    }

    /** Low-stock alert list: active products at or below their reorder level. */
    public List<InventoryItem> getLowStock() {
        return inventoryDao.findLowStock();
    }

    public void updateReorderLevel(int productId, int reorderLevel) {
        if (reorderLevel < 0 || reorderLevel > 100_000) {
            throw new ValidationException("Reorder level must be between 0 and 100000.");
        }
        inventoryDao.updateReorderLevel(productId, reorderLevel);
    }

    /** Stock count correction, e.g. after a physical count or damaged plants. */
    public void adjustQuantity(int productId, int newQuantity) {
        if (newQuantity < 0 || newQuantity > 1_000_000) {
            throw new ValidationException("Quantity must be between 0 and 1000000.");
        }
        inventoryDao.setQuantity(productId, newQuantity);
    }

    /** Records stock received from a supplier and adds it to the quantity on hand. */
    public void recordStockIn(StockIn s) {
        if (s.getProductId() <= 0) {
            throw new ValidationException("Please choose the product that was received.");
        }
        if (s.getSupplierId() <= 0) {
            throw new ValidationException("Please choose the supplier. Add suppliers in the Suppliers tab.");
        }
        if (s.getQuantity() <= 0) {
            throw new ValidationException("Quantity received must be more than zero.");
        }
        if (s.getQuantity() > 100_000) {
            throw new ValidationException("Quantity received looks too large (maximum 100000).");
        }
        if (s.getUnitCost() == null || s.getUnitCost().signum() < 0) {
            throw new ValidationException("Unit cost cannot be negative.");
        }
        if (s.getReceivedDate() == null || s.getReceivedDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Received date cannot be in the future.");
        }
        if (s.getNote() != null && s.getNote().length() > 255) {
            throw new ValidationException("Note must be 255 characters or less.");
        }
        s.setNote(Validator.trimToNull(s.getNote()));
        s.setRecordedBy(Session.username());
        DBConnection.inTransaction(con -> {
            int id = stockInDao.insert(con, s);
            inventoryDao.increase(con, s.getProductId(), s.getQuantity());
            s.setId(id);
            return id;
        });
    }

    public List<StockIn> getStockInHistory() {
        return stockInDao.findAll();
    }

    // ------------------------------------------------ suppliers

    public List<Supplier> getSuppliers() {
        return supplierDao.findAll();
    }

    public void saveSupplier(Supplier s) {
        s.setName(s.getName() == null ? null : s.getName().trim());
        s.setPhone(s.getPhone() == null ? null : s.getPhone().trim());
        s.setEmail(Validator.trimToNull(s.getEmail()));
        if (Validator.isBlank(s.getName())) {
            throw new ValidationException("Supplier name is required.");
        }
        if (!Validator.isValidPhone(s.getPhone())) {
            throw new ValidationException("Enter a valid supplier phone number.");
        }
        if (s.getEmail() != null && !Validator.isValidEmail(s.getEmail())) {
            throw new ValidationException("Supplier e-mail is not valid (leave it empty if unknown).");
        }
        try {
            if (s.isNew()) {
                s.setId(supplierDao.insert(s));
            } else {
                supplierDao.update(s);
            }
        } catch (econest.dao.DataAccessException e) {
            if (e.isDuplicate()) {
                throw new ValidationException("A supplier called \"" + s.getName() + "\" already exists.");
            }
            throw e;
        }
    }

    public void deleteSupplier(int supplierId) {
        if (supplierDao.hasStockRecords(supplierId)) {
            throw new ValidationException("This supplier has stock-in records and cannot be deleted.");
        }
        supplierDao.delete(supplierId);
    }

    // ------------------------------------------------ extra feature: reorder suggestions

    /**
     * Looks at the last 30 days of sales and suggests how much of each product
     * to buy so that stock lasts about {@value #TARGET_COVER_DAYS} days above
     * the reorder level.
     */
    public List<ReorderSuggestion> getReorderSuggestions() {
        Map<Integer, Integer> sold = inventoryDao.unitsSoldSince(LocalDateTime.now().minusDays(30));
        List<ReorderSuggestion> list = new ArrayList<>();
        for (InventoryItem item : inventoryDao.findAll()) {
            if (!item.isProductActive()) {
                continue;
            }
            int sold30 = sold.getOrDefault(item.getProductId(), 0);
            ReorderSuggestion probe = new ReorderSuggestion(item, sold30, 0);
            int daysLeft = probe.getDaysOfStockLeft();
            boolean low = item.getStockStatus() != StockStatus.OK;
            boolean runningOut = daysLeft >= 0 && daysLeft < COVER_DAYS_WARNING;
            if (!low && !runningOut) {
                continue;
            }
            int suggested = suggestedQuantity(item.getQuantityOnHand(), item.getReorderLevel(), sold30);
            list.add(new ReorderSuggestion(item, sold30, suggested));
        }
        list.sort(Comparator.comparingInt((ReorderSuggestion r) -> r.getItem().getQuantityOnHand()
                - r.getItem().getReorderLevel()).thenComparing(r -> r.getItem().getProductName()));
        return list;
    }

    /** target = max(2 x reorder level, 30 days of sales + reorder level); buy the difference. */
    static int suggestedQuantity(int onHand, int reorderLevel, int soldLast30Days) {
        double perDay = soldLast30Days / 30.0;
        int target = Math.max(reorderLevel * 2, (int) Math.ceil(perDay * TARGET_COVER_DAYS) + reorderLevel);
        return Math.max(1, target - onHand);
    }

    /** Products for the stock-in form. */
    public List<Product> getProducts() {
        return productDao.findAll();
    }
}
