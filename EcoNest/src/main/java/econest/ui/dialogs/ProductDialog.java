package econest.ui.dialogs;

import econest.model.Product;
import econest.model.ProductCategory;
import econest.service.ProductService;
import econest.service.ValidationException;
import econest.util.Formatters;
import econest.util.Validator;
import java.awt.Component;
import java.math.BigDecimal;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** Add / edit a product (Task 1). */
public class ProductDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final ProductService service;
    private final Product product;
    private final JTextField name = new JTextField(26);
    private final JComboBox<ProductCategory> category = new JComboBox<>(ProductCategory.values());
    private final JTextArea description = new JTextArea();
    private final JTextField price = new JTextField(12);
    private final JComboBox<String> rating = new JComboBox<>(new String[] {
        "1 - basic", "2 - fair", "3 - good", "4 - very good", "5 - excellent"});
    private final JSpinner openingStock = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
    private final JSpinner reorderLevel = new JSpinner(new SpinnerNumberModel(10, 0, 100000, 1));

    public ProductDialog(Component parent, ProductService service, Product existing) {
        super(parent, existing == null ? "Add product" : "Edit product", existing == null ? "Add product" : "Save");
        this.service = service;
        this.product = existing == null ? new Product() : existing;
        form.add("Name", name, true);
        form.add("Category", category, true);
        form.add("Description", econest.ui.components.FormBuilder.area(description, 3));
        form.add("Price (Rs.)", price, true).hint("e.g. 1500 or 1250.50");
        form.add("Eco-rating", rating, true).hint("How sustainable the product is (5 = most eco-friendly)");
        if (existing == null) {
            form.add("Opening stock", openingStock).hint("Units already in the shop");
            form.add("Reorder level", reorderLevel).hint("A low-stock alert appears at or below this level");
        } else {
            name.setText(existing.getName());
            category.setSelectedItem(existing.getCategory());
            description.setText(existing.getDescription() == null ? "" : existing.getDescription());
            price.setText(Formatters.plainMoney(existing.getPrice()));
            rating.setSelectedIndex(existing.getEcoRating() - 1);
        }
    }

    @Override
    protected void save() {
        if (Validator.isBlank(name.getText())) {
            throw new ValidationException("Product name is required.");
        }
        BigDecimal p = Validator.parseMoney(price.getText());
        if (p == null) {
            throw new ValidationException("Price must be a number with up to 2 decimals, e.g. 1250.50");
        }
        product.setName(name.getText());
        product.setCategory((ProductCategory) category.getSelectedItem());
        product.setDescription(description.getText());
        product.setPrice(p);
        product.setEcoRating(rating.getSelectedIndex() + 1);
        if (product.isNew()) {
            service.addProduct(product, (Integer) openingStock.getValue(), (Integer) reorderLevel.getValue());
        } else {
            service.updateProduct(product);
        }
    }
}
