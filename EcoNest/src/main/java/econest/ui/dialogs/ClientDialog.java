package econest.ui.dialogs;

import econest.model.Client;
import econest.model.ClientType;
import econest.service.ClientService;
import econest.ui.components.FormBuilder;
import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Add / edit a household or office client (Task 2). */
public class ClientDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final ClientService service;
    private final Client client;
    private final JComboBox<ClientType> type = new JComboBox<>(ClientType.values());
    private final JTextField name = new JTextField(26);
    private final JTextField company = new JTextField(26);
    private final JTextField phone = new JTextField(16);
    private final JTextField email = new JTextField(26);
    private final JTextArea address = new JTextArea();

    public ClientDialog(Component parent, ClientService service, Client existing) {
        super(parent, existing == null ? "Add client" : "Edit client", existing == null ? "Add client" : "Save");
        this.service = service;
        this.client = existing == null ? new Client() : existing;
        form.add("Client type", type, true);
        form.add("Contact name", name, true);
        form.add("Company name", company).hint("Required for office clients");
        form.add("Phone", phone, true).hint("e.g. 0771234567 or +94 77 123 4567");
        form.add("E-mail", email, true).hint("Dispatch notifications are sent to this address");
        form.add("Delivery address", FormBuilder.area(address, 3), true);
        type.addActionListener(e -> company.setEnabled(type.getSelectedItem() == ClientType.OFFICE));
        if (existing != null) {
            type.setSelectedItem(existing.getClientType());
            name.setText(existing.getName());
            company.setText(existing.getCompanyName() == null ? "" : existing.getCompanyName());
            phone.setText(existing.getPhone());
            email.setText(existing.getEmail());
            address.setText(existing.getAddress());
        }
        company.setEnabled(type.getSelectedItem() == ClientType.OFFICE);
    }

    @Override
    protected void save() {
        client.setClientType((ClientType) type.getSelectedItem());
        client.setName(name.getText());
        client.setCompanyName(company.getText());
        client.setPhone(phone.getText());
        client.setEmail(email.getText());
        client.setAddress(address.getText());
        if (client.isNew()) {
            service.addClient(client);
        } else {
            service.updateClient(client);
        }
    }
}
