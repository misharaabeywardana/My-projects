package econest.service;

import econest.dao.ClientDao;
import econest.dao.DBConnection;
import econest.dao.DeliveryDao;
import econest.dao.InventoryDao;
import econest.dao.OrderDao;
import econest.dao.ProductDao;
import econest.model.Delivery;
import econest.model.DeliveryStatus;
import econest.model.Order;
import econest.model.OrderItem;
import econest.model.OrderStatus;
import econest.model.Product;
import econest.util.Session;
import econest.util.Validator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Business rules for client orders (Task 5 - Fahath). */
public class OrderService {

    public static final int MAX_QTY_PER_LINE = 10_000;

    private final OrderDao orderDao;
    private final ProductDao productDao;
    private final InventoryDao inventoryDao;
    private final DeliveryDao deliveryDao;
    private final ClientDao clientDao;

    public OrderService() {
        this(new OrderDao(), new ProductDao(), new InventoryDao(), new DeliveryDao(), new ClientDao());
    }

    public OrderService(OrderDao orderDao, ProductDao productDao, InventoryDao inventoryDao,
                        DeliveryDao deliveryDao, ClientDao clientDao) {
        this.orderDao = orderDao;
        this.productDao = productDao;
        this.inventoryDao = inventoryDao;
        this.deliveryDao = deliveryDao;
        this.clientDao = clientDao;
    }

    public List<Order> getAll() {
        return orderDao.findAll();
    }

    public List<Order> getRecent(int limit) {
        return orderDao.findRecent(limit);
    }

    public Order getById(int orderId) {
        return orderDao.findById(orderId);
    }

    public List<Order> getAwaitingDelivery() {
        return orderDao.findAwaitingDelivery();
    }

    /** Order reference shown to clients, e.g. ECN-20261113-00042. */
    public static String buildReference(int orderId, LocalDateTime date) {
        return "ECN-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%05d", orderId);
    }

    /**
     * Saves a new order. In ONE transaction it:
     * 1) re-reads each product's current price, 2) saves the order and its lines,
     * 3) takes the quantities out of stock. If any product does not have enough
     * stock, nothing is saved.
     */
    public Order createOrder(Order draft) {
        if (draft.getClientId() <= 0) {
            throw new ValidationException("Please choose a client for this order.");
        }
        if (draft.getItems().isEmpty()) {
            throw new ValidationException("Add at least one product to the order.");
        }
        for (OrderItem item : draft.getItems()) {
            if (item.getQuantity() <= 0 || item.getQuantity() > MAX_QTY_PER_LINE) {
                throw new ValidationException("Quantity for " + item.getProductName()
                        + " must be between 1 and " + MAX_QTY_PER_LINE + ".");
            }
        }
        if (draft.getNotes() != null && draft.getNotes().length() > 255) {
            throw new ValidationException("Order notes must be 255 characters or less.");
        }
        draft.setNotes(Validator.trimToNull(draft.getNotes()));
        draft.setOrderDate(LocalDateTime.now().withNano(0));
        draft.setStatus(OrderStatus.PENDING);
        draft.setCreatedBy(Session.username());

        return DBConnection.inTransaction(con -> {
            // use the price stored in the database right now
            for (OrderItem item : draft.getItems()) {
                Product p = productDao.findById(con, item.getProductId());
                if (p == null || !p.isActive()) {
                    throw new ValidationException(item.getProductName() + " is no longer available.");
                }
                item.setUnitPrice(p.getPrice());
                item.setProductName(p.getName());
            }
            draft.recalculateTotal();

            int orderId = orderDao.insertOrder(con, draft);
            String ref = buildReference(orderId, draft.getOrderDate());
            orderDao.updateRef(con, orderId, ref);
            for (OrderItem item : draft.getItems()) {
                orderDao.insertItem(con, orderId, item);
                if (!inventoryDao.decrease(con, item.getProductId(), item.getQuantity())) {
                    int available = inventoryDao.findByProduct(con, item.getProductId()).getQuantityOnHand();
                    throw new ValidationException("Not enough stock for " + item.getProductName()
                            + ": ordered " + item.getQuantity() + ", only " + available + " available.");
                }
            }
            draft.setId(orderId);
            draft.setOrderRef(ref);
            return draft;
        });
    }

    /**
     * Cancels a pending or processing order, puts the products back into stock
     * and closes any open delivery.
     */
    public void cancelOrder(int orderId, String reason) {
        DBConnection.inTransaction(con -> {
            Order order = orderDao.findById(con, orderId);
            if (order == null) {
                throw new ValidationException("This order no longer exists.");
            }
            if (!order.getStatus().isCancellable()) {
                throw new ValidationException("Only Pending or Processing orders can be cancelled. "
                        + order.getOrderRef() + " is " + order.getStatus() + ".");
            }
            for (OrderItem item : order.getItems()) {
                inventoryDao.increase(con, item.getProductId(), item.getQuantity());
            }
            orderDao.updateStatus(con, orderId, OrderStatus.CANCELLED);
            Delivery delivery = deliveryDao.findByOrder(con, orderId);
            if (delivery != null && delivery.getStatus().isOpen()) {
                String note = "Order cancelled" + (Validator.isBlank(reason) ? "" : ": " + reason.trim());
                deliveryDao.updateStatus(con, delivery.getId(), DeliveryStatus.FAILED, null,
                        note.length() > 255 ? note.substring(0, 255) : note);
            }
            return null;
        });
    }

    /** Extra feature (Fahath): printable invoice text for an order. */
    public String buildInvoice(int orderId) {
        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw new ValidationException("This order no longer exists.");
        }
        return InvoiceGenerator.generate(order, clientDao.findById(order.getClientId()),
                deliveryDao.findByOrder(orderId));
    }
}
