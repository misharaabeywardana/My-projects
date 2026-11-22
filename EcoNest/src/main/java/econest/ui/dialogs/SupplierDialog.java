package econest.ui.dialogs;

import econest.model.Supplier;
import econest.service.InventoryService;
import java.awt.Component;
import javax.swing.JTextField;

/** Add / edit a supplier. */
public class SupplierDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final InventoryService service;
    private final Supplier supplier;
    private final JTextField name = new JTextField(26);
    private final JTextField phone = new JTextField(16);
    private final JTextField email = new JTextField(26);

    public SupplierDialog(Component parent, InventoryService service, Supplier existing) {
        super(parent, existing == null ? "Add supplier" : "Edit supplier", "Save");
        this.service = service;
        this.supplier = existing == null ? new Supplier() : existing;
        form.add("Supplier name", name, true);
        form.add("Phone", phone, true);
        form.add("E-mail", email).hint("Optional");
        if (existing != null) {
            name.setText(existing.getName());
            phone.setText(existing.getPhone());
            email.setText(existing.getEmail() == null ? "" : existing.getEmail());
        }
    }

    @Override
    protected void save() {
        supplier.setName(name.getText());
        supplier.setPhone(phone.getText());
        supplier.setEmail(email.getText());
        service.saveSupplier(supplier);
    }
}
