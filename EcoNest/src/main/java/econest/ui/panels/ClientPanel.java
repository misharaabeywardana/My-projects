package econest.ui.panels;

import econest.model.Client;
import econest.model.ClientType;
import econest.service.ClientService;
import econest.ui.components.AppButton;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.ClientDialog;
import econest.ui.dialogs.ClientHistoryDialog;
import java.util.List;
import javax.swing.JComboBox;

/** Task 2 (Mishara): manage household and office clients. */
public class ClientPanel extends AbstractCrudPanel<Client> {

    private static final long serialVersionUID = 1L;

    private final ClientService service = new ClientService();
    private final JComboBox<Object> typeFilter = new JComboBox<>(new Object[] {
        "All clients", ClientType.HOUSEHOLD, ClientType.OFFICE});

    public ClientPanel() {
        super("Clients", "Household and office clients and their contact details",
                new String[] {"ID", "Type", "Name", "Company", "Phone", "E-mail", "Address"},
                new Class<?>[] {Integer.class}, true);
        table.setColumnWidths(45, 90, 170, 160, 110, 220, 260);
        addButton.setText("Add Client");
        typeFilter.addActionListener(e -> applyFilter());
        addFilter(typeFilter);
        AppButton history = new AppButton("Purchase History");
        history.addActionListener(e -> showHistory());
        addButton(history);
    }

    @Override
    protected List<Client> loadItems() {
        return service.getAll();
    }

    @Override
    protected Object[] toRow(Client c) {
        return new Object[] {c.getId(), c.getClientType(), c.getName(),
            c.getCompanyName() == null ? "" : c.getCompanyName(), c.getPhone(), c.getEmail(), c.getAddress()};
    }

    @Override
    protected String searchText(Client c) {
        return c.getName() + " " + c.getCompanyName() + " " + c.getPhone() + " " + c.getEmail() + " " + c.getAddress();
    }

    @Override
    protected boolean passesFilters(Client c) {
        Object t = typeFilter.getSelectedItem();
        return !(t instanceof ClientType) || c.getClientType() == t;
    }

    @Override
    protected boolean onAdd() {
        return new ClientDialog(this, service, null).showDialog();
    }

    @Override
    protected boolean onEdit(Client c) {
        return new ClientDialog(this, service, service.getById(c.getId())).showDialog();
    }

    @Override
    protected boolean onDelete(Client c) {
        if (!UiUtil.confirm(this, "Delete client \"" + c.getDisplayName() + "\"?")) {
            return false;
        }
        try {
            service.deleteClient(c.getId());
            return true;
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
            return false;
        }
    }

    private void showHistory() {
        Client c = requireSelection();
        if (c == null) {
            return;
        }
        try {
            new ClientHistoryDialog(this, c, service.getSummary(c.getId()), service.getOrders(c.getId()))
                    .setVisible(true);
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }
}
