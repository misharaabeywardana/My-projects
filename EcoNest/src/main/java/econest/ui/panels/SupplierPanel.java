package econest.ui.panels;

import econest.model.Supplier;
import econest.service.InventoryService;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.SupplierDialog;
import java.util.List;

/** Suppliers tab inside the Inventory screen. */
public class SupplierPanel extends AbstractCrudPanel<Supplier> {

    private static final long serialVersionUID = 1L;

    private final InventoryService service;

    public SupplierPanel(InventoryService service) {
        super("Suppliers", "", new String[] {"ID", "Supplier", "Phone", "E-mail"},
                new Class<?>[] {Integer.class}, false);
        this.service = service;
        addButton.setText("Add Supplier");
        table.setColumnWidths(50, 280, 140, 260);
    }

    @Override
    protected List<Supplier> loadItems() {
        return service.getSuppliers();
    }

    @Override
    protected Object[] toRow(Supplier s) {
        return new Object[] {s.getId(), s.getName(), s.getPhone(), s.getEmail() == null ? "" : s.getEmail()};
    }

    @Override
    protected String searchText(Supplier s) {
        return s.getName() + " " + s.getPhone() + " " + s.getEmail();
    }

    @Override
    protected boolean onAdd() {
        return new SupplierDialog(this, service, null).showDialog();
    }

    @Override
    protected boolean onEdit(Supplier s) {
        return new SupplierDialog(this, service, s).showDialog();
    }

    @Override
    protected boolean onDelete(Supplier s) {
        if (!UiUtil.confirm(this, "Delete supplier \"" + s.getName() + "\"?")) {
            return false;
        }
        try {
            service.deleteSupplier(s.getId());
            return true;
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
            return false;
        }
    }
}
