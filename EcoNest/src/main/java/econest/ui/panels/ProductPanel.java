package econest.ui.panels;

import econest.model.Product;
import econest.model.ProductCategory;
import econest.service.ProductService;
import econest.ui.components.AppButton;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.PriceHistoryDialog;
import econest.ui.dialogs.ProductDialog;
import econest.util.Formatters;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

/** Task 1 (Senuji): manage plants, planters and gardening kits. */
public class ProductPanel extends AbstractCrudPanel<Product> {

    private static final long serialVersionUID = 1L;

    private final ProductService service = new ProductService();
    private final JComboBox<Object> categoryFilter = new JComboBox<>();
    private final JComboBox<String> ratingFilter = new JComboBox<>(new String[] {
        "Any eco-rating", "2 stars or more", "3 stars or more", "4 stars or more", "5 stars only"});
    private final JCheckBox showInactive = new JCheckBox("Show inactive");
    private final AppButton historyButton = new AppButton("Price History");
    private final AppButton reactivateButton = new AppButton("Reactivate");

    public ProductPanel() {
        super("Product Catalogue", "Plants, planters and gardening kits with pricing and eco-rating",
                new String[] {"ID", "Name", "Category", "Price", "Eco-rating", "In stock", "Status"},
                new Class<?>[] {Integer.class, null, null, null, null, Integer.class, null}, true);
        table.setColumnWidths(50, 240, 120, 110, 110, 80, 80);
        addButton.setText("Add Product");
        deleteButton.setText("Remove");
        categoryFilter.addItem("All categories");
        for (ProductCategory c : ProductCategory.values()) {
            categoryFilter.addItem(c);
        }
        categoryFilter.addActionListener(e -> applyFilter());
        ratingFilter.addActionListener(e -> applyFilter());
        showInactive.setOpaque(false);
        showInactive.addActionListener(e -> applyFilter());
        addFilter(categoryFilter);
        addFilter(ratingFilter);
        addFilter(showInactive);
        historyButton.addActionListener(e -> showHistory());
        reactivateButton.addActionListener(e -> reactivate());
        addButton(historyButton);
        addButton(reactivateButton);
    }

    @Override
    protected List<Product> loadItems() {
        return service.getAll();
    }

    @Override
    protected Object[] toRow(Product p) {
        return new Object[] {p.getId(), p.getName(), p.getCategory(), Formatters.money(p.getPrice()), p.getEcoStars(),
            p.getQuantityOnHand(), p.isActive() ? "Active" : "Inactive"};
    }

    @Override
    protected String searchText(Product p) {
        return p.getName() + " " + (p.getDescription() == null ? "" : p.getDescription());
    }

    @Override
    protected boolean passesFilters(Product p) {
        Object cat = categoryFilter.getSelectedItem();
        int minRating = Math.max(1, ratingFilter.getSelectedIndex() + 1);
        return !ProductService.filter(List.of(p), "", cat instanceof ProductCategory ? (ProductCategory) cat : null,
                minRating, showInactive.isSelected()).isEmpty();
    }

    @Override
    protected void selectionChanged(Product p) {
        reactivateButton.setEnabled(p != null && !p.isActive());
        historyButton.setEnabled(p != null);
    }

    @Override
    protected boolean onAdd() {
        return new ProductDialog(this, service, null).showDialog();
    }

    @Override
    protected boolean onEdit(Product p) {
        return new ProductDialog(this, service, service.getById(p.getId())).showDialog();
    }

    @Override
    protected boolean onDelete(Product p) {
        if (!UiUtil.confirm(this, "Remove \"" + p.getName() + "\" from the catalogue?")) {
            return false;
        }
        try {
            UiUtil.info(this, service.removeProduct(p.getId()));
            return true;
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
            return false;
        }
    }

    private void showHistory() {
        Product p = requireSelection();
        if (p == null) {
            return;
        }
        try {
            new PriceHistoryDialog(this, p, service.getPriceHistory(p.getId())).setVisible(true);
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }

    private void reactivate() {
        Product p = requireSelection();
        if (p != null) {
            service.reactivate(p.getId());
            refresh();
        }
    }
}
