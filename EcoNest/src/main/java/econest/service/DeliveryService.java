package econest.service;

import econest.dao.AgentDao;
import econest.dao.DBConnection;
import econest.dao.DeliveryDao;
import econest.dao.OrderDao;
import econest.model.Delivery;
import econest.model.DeliveryAgent;
import econest.model.DeliveryStatus;
import econest.model.EmailLog;
import econest.model.Order;
import econest.model.OrderStatus;
import econest.util.Validator;
import java.time.LocalDateTime;
import java.util.List;

/** Business rules for assigning agents and tracking deliveries (Task 6 - Fahath). */
public class DeliveryService {

    /** Two deliveries of the same agent closer than this are reported as a clash. */
    public static final int CLASH_WINDOW_MINUTES = 60;

    private final DeliveryDao deliveryDao;
    private final OrderDao orderDao;
    private final AgentDao agentDao;
    private final NotificationService notificationService;

    public DeliveryService() {
        this(new DeliveryDao(), new OrderDao(), new AgentDao(), new NotificationService());
    }

    public DeliveryService(DeliveryDao deliveryDao, OrderDao orderDao, AgentDao agentDao,
                           NotificationService notificationService) {
        this.deliveryDao = deliveryDao;
        this.orderDao = orderDao;
        this.agentDao = agentDao;
        this.notificationService = notificationService;
    }

    /** Result of an action that may also have sent an e-mail. */
    public static class Result {
        private final Delivery delivery;
        private final EmailLog email;

        public Result(Delivery delivery, EmailLog email) {
            this.delivery = delivery;
            this.email = email;
        }

        public Delivery getDelivery() {
            return delivery;
        }

        /** The e-mail that was sent, or null if none was needed. */
        public EmailLog getEmail() {
            return email;
        }
    }

    public List<Delivery> getAll() {
        return deliveryDao.findAll();
    }

    public Delivery getByOrder(int orderId) {
        return deliveryDao.findByOrder(orderId);
    }

    /** Extra feature: other deliveries of this agent within +/- 60 minutes. */
    public List<Delivery> findClashes(int agentId, LocalDateTime when, int excludeDeliveryId) {
        return deliveryDao.findClashes(agentId, when.minusMinutes(CLASH_WINDOW_MINUTES),
                when.plusMinutes(CLASH_WINDOW_MINUTES), excludeDeliveryId);
    }

    /**
     * Assigns (or re-assigns) an agent to an order, schedules it and then
     * e-mails the agent (Task 9).
     */
    public Result assignAgent(int orderId, int agentId, LocalDateTime scheduledAt, String notes) {
        if (scheduledAt == null) {
            throw new ValidationException("Please choose the delivery date and time.");
        }
        if (scheduledAt.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new ValidationException("The delivery time cannot be in the past.");
        }
        if (scheduledAt.isAfter(LocalDateTime.now().plusDays(90))) {
            throw new ValidationException("Deliveries can be scheduled up to 90 days ahead.");
        }
        if (notes != null && notes.length() > 255) {
            throw new ValidationException("Delivery notes must be 255 characters or less.");
        }
        DeliveryAgent agent = agentDao.findById(agentId);
        if (agent == null) {
            throw new ValidationException("Please choose a delivery agent.");
        }
        if (!agent.isActive()) {
            throw new ValidationException(agent.getName() + " is marked unavailable.");
        }
        int deliveryId = DBConnection.inTransaction(con -> {
            Order order = orderDao.findById(con, orderId);
            if (order == null) {
                throw new ValidationException("This order no longer exists.");
            }
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
                throw new ValidationException("Only Pending or Processing orders can be assigned. "
                        + order.getOrderRef() + " is " + order.getStatus() + ".");
            }
            Delivery d = new Delivery();
            d.setOrderId(orderId);
            d.setAgentId(agentId);
            d.setScheduledAt(scheduledAt.withSecond(0).withNano(0));
            d.setNotes(Validator.trimToNull(notes));
            Delivery existing = deliveryDao.findByOrder(con, orderId);
            int id;
            if (existing == null) {
                id = deliveryDao.insert(con, d);
            } else if (existing.getStatus() == DeliveryStatus.ASSIGNED
                    || existing.getStatus() == DeliveryStatus.FAILED) {
                d.setId(existing.getId());
                deliveryDao.reassign(con, d);
                id = existing.getId();
            } else {
                throw new ValidationException("This order's delivery is already " + existing.getStatus() + ".");
            }
            if (order.getStatus() == OrderStatus.PENDING) {
                orderDao.updateStatus(con, orderId, OrderStatus.PROCESSING);
            }
            return id;
        });
        EmailLog email = notificationService.notifyAgentAssigned(deliveryId);
        return new Result(deliveryDao.findById(deliveryId), email);
    }

    /**
     * Moves a delivery to its next status and keeps the order status in step:
     * IN_TRANSIT  -> order DISPATCHED and the client is e-mailed (Task 8)
     * DELIVERED   -> order DELIVERED
     * FAILED      -> order back to PROCESSING so it can be reassigned
     */
    public Result updateStatus(int deliveryId, DeliveryStatus newStatus, String notes) {
        if (notes != null && notes.length() > 255) {
            throw new ValidationException("Notes must be 255 characters or less.");
        }
        Delivery updated = DBConnection.inTransaction(con -> {
            Delivery d = deliveryDao.findById(con, deliveryId);
            if (d == null) {
                throw new ValidationException("This delivery no longer exists.");
            }
            if (!d.getStatus().canChangeTo(newStatus)) {
                throw new ValidationException("A delivery that is " + d.getStatus() + " cannot be changed to "
                        + newStatus + ".");
            }
            OrderStatus orderStatus = d.getOrderStatus();
            OrderStatus nextOrderStatus;
            switch (newStatus) {
                case IN_TRANSIT:
                    nextOrderStatus = OrderStatus.DISPATCHED;
                    break;
                case DELIVERED:
                    nextOrderStatus = OrderStatus.DELIVERED;
                    break;
                case FAILED:
                    nextOrderStatus = orderStatus == OrderStatus.DISPATCHED ? OrderStatus.PROCESSING : orderStatus;
                    break;
                default:
                    nextOrderStatus = orderStatus;
            }
            if (nextOrderStatus != orderStatus && !orderStatus.canChangeTo(nextOrderStatus)) {
                throw new ValidationException("Order " + d.getOrderRef() + " is " + orderStatus
                        + " and cannot become " + nextOrderStatus + ".");
            }
            LocalDateTime deliveredAt = newStatus == DeliveryStatus.DELIVERED ? LocalDateTime.now().withNano(0) : null;
            deliveryDao.updateStatus(con, deliveryId, newStatus, deliveredAt, Validator.trimToNull(notes));
            if (nextOrderStatus != orderStatus) {
                orderDao.updateStatus(con, d.getOrderId(), nextOrderStatus);
            }
            return deliveryDao.findById(con, deliveryId);
        });
        EmailLog email = null;
        if (newStatus == DeliveryStatus.IN_TRANSIT) {
            email = notificationService.notifyClientDispatched(updated.getOrderId());
        }
        return new Result(updated, email);
    }
}
