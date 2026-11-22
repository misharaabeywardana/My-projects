package econest.service;

import econest.dao.ClientDao;
import econest.dao.OrderDao;
import econest.model.Client;
import econest.model.ClientSummary;
import econest.model.ClientType;
import econest.model.Order;
import econest.util.Validator;
import java.util.List;

/** Business rules for household and office clients (Task 2 - Mishara). */
public class ClientService {

    private final ClientDao clientDao;
    private final OrderDao orderDao;

    public ClientService() {
        this(new ClientDao(), new OrderDao());
    }

    public ClientService(ClientDao clientDao, OrderDao orderDao) {
        this.clientDao = clientDao;
        this.orderDao = orderDao;
    }

    public List<Client> getAll() {
        return clientDao.findAll();
    }

    public Client getById(int id) {
        return clientDao.findById(id);
    }

    public void validate(Client c) {
        if (c.getClientType() == null) {
            throw new ValidationException("Please choose the client type (household or office).");
        }
        if (Validator.isBlank(c.getName())) {
            throw new ValidationException("Client name is required.");
        }
        if (c.getName().length() > 100) {
            throw new ValidationException("Client name must be 100 characters or less.");
        }
        if (c.getClientType() == ClientType.OFFICE && Validator.isBlank(c.getCompanyName())) {
            throw new ValidationException("Company name is required for office clients.");
        }
        if (!Validator.isValidPhone(c.getPhone())) {
            throw new ValidationException("Enter a valid phone number, e.g. 0771234567 or +94 77 123 4567.");
        }
        if (!Validator.isValidEmail(c.getEmail())) {
            throw new ValidationException("Enter a valid e-mail address, e.g. name@example.com.\n"
                    + "It is needed for dispatch notifications.");
        }
        if (Validator.isBlank(c.getAddress())) {
            throw new ValidationException("Delivery address is required.");
        }
        if (c.getAddress().length() > 255) {
            throw new ValidationException("Address must be 255 characters or less.");
        }
        if (clientDao.emailExists(c.getEmail().trim(), c.getId())) {
            throw new ValidationException("Another client already uses the e-mail " + c.getEmail().trim() + ".");
        }
    }

    private void clean(Client c) {
        c.setName(c.getName() == null ? null : c.getName().trim());
        c.setPhone(c.getPhone() == null ? null : c.getPhone().trim());
        c.setEmail(c.getEmail() == null ? null : c.getEmail().trim());
        c.setAddress(c.getAddress() == null ? null : c.getAddress().trim());
        c.setCompanyName(c.getClientType() == ClientType.OFFICE ? Validator.trimToNull(c.getCompanyName()) : null);
    }

    public int addClient(Client c) {
        clean(c);
        validate(c);
        int id = clientDao.insert(c);
        c.setId(id);
        return id;
    }

    public void updateClient(Client c) {
        clean(c);
        validate(c);
        clientDao.update(c);
    }

    /** Clients with orders are kept so order history and reports stay correct. */
    public void deleteClient(int clientId) {
        if (clientDao.hasOrders(clientId)) {
            throw new ValidationException("This client has orders and cannot be deleted.\n"
                    + "Their order history is needed for reports.");
        }
        clientDao.delete(clientId);
    }

    // ------------------------------------------------ extra feature: purchase history

    public ClientSummary getSummary(int clientId) {
        return clientDao.getSummary(clientId);
    }

    public List<Order> getOrders(int clientId) {
        return orderDao.findByClient(clientId);
    }
}
