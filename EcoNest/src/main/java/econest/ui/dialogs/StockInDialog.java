package econest.ui.dialogs;

import econest.model.Product;
import econest.model.StockIn;
import econest.model.Supplier;
import econest.service.InventoryService;
import econest.service.ValidationException;
import econest.util.Validator;
import java.awt.Component;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;

/** Record stock received from a supplier (Task 3). */
public class StockInDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final InventoryService service;
    private final JComboBox<Product> product = new JComboBox<>();
    private final JComboBox<Supplier> supplier = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
    private final JTextField unitCost = new JTextField("0.00", 10);
    private final JSpinner date = new JSpinner(new SpinnerDateModel());
    private final JTextField note = new JTextField(26);

    public StockInDialog(Component parent, InventoryService service, Integer productId, Integer suggestedQty) {
        super(parent, "Record stock received", "Save stock-in");
        this.service = service;
        for (Product p : service.getProducts()) {
            if (p.isActive()) {
                product.addItem(p);
                if (productId != null && p.getId() == productId) {
                    product.setSelectedItem(p);
                }
            }
        }
        for (Supplier s : service.getSuppliers()) {
            supplier.addItem(s);
        }
        if (suggestedQty != null && suggestedQty > 0) {
            quantity.setValue(suggestedQty);
        }
        date.setEditor(new JSpinner.DateEditor(date, "yyyy-MM-dd"));
        form.add("Product", product, true);
        form.add("Supplier", supplier, true);
        if (supplier.getItemCount() == 0) {
            form.hint("No suppliers yet - add one in the Suppliers tab first");
        }
        form.add("Quantity received", quantity, true);
        form.add("Unit cost (Rs.)", unitCost).hint("Purchase price per unit (used in the monthly report)");
        form.add("Date received", date, true);
        form.add("Note", note);
    }

    @Override
    protected void save() {
        BigDecimal cost = Validator.parseMoney(unitCost.getText());
        if (cost == null) {
            throw new ValidationException("Unit cost must be a number, e.g. 450.00 (use 0 if unknown).");
        }
        Product p = (Product) product.getSelectedItem();
        Supplier s = (Supplier) supplier.getSelectedItem();
        StockIn in = new StockIn();
        in.setProductId(p == null ? 0 : p.getId());
        in.setSupplierId(s == null ? 0 : s.getId());
        in.setQuantity((Integer) quantity.getValue());
        in.setUnitCost(cost);
        in.setReceivedDate(((Date) date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        in.setNote(note.getText());
        service.recordStockIn(in);
    }
}
