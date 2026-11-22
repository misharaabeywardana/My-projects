package econest.service;

import econest.dao.AgentDao;
import econest.dao.ClientDao;
import econest.dao.DeliveryDao;
import econest.dao.EmailLogDao;
import econest.dao.EmailTemplateDao;
import econest.dao.OrderDao;
import econest.model.Client;
import econest.model.Delivery;
import econest.model.DeliveryAgent;
import econest.model.EmailLog;
import econest.model.EmailStatus;
import econest.model.EmailTemplate;
import econest.model.Order;
import econest.model.OrderItem;
import econest.model.RecipientType;
import econest.service.email.EmailConfig;
import econest.service.email.EmailException;
import econest.service.email.EmailMessage;
import econest.service.email.EmailSender;
import econest.util.Formatters;
import econest.util.TemplateEngine;
import econest.util.Validator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * E-mail notifications.
 * Task 8 (Rakshana): e-mail the client when an order is dispatched.
 * Task 9 (Sandeepa): e-mail the agent when a delivery is assigned.
 * Every attempt is written to email_log so nothing is lost silently.
 */
public class NotificationService {

    public static final String[] CLIENT_PLACEHOLDERS = {
        "clientName", "orderRef", "orderDate", "items", "total", "agentName", "agentPhone", "vehicle",
        "scheduledAt", "deliveryAddress"
    };
    public static final String[] AGENT_PLACEHOLDERS = {
        "agentName", "orderRef", "scheduledAt", "clientName", "clientPhone", "deliveryAddress", "items",
        "itemCount", "total", "vehicle", "notes"
    };

    private final EmailTemplateDao templateDao;
    private final EmailLogDao logDao;
    private final OrderDao orderDao;
    private final ClientDao clientDao;
    private final DeliveryDao deliveryDao;
    private final AgentDao agentDao;
    private final Supplier<EmailSender> senderFactory;

    public NotificationService() {
        this(new EmailTemplateDao(), new EmailLogDao(), new OrderDao(), new ClientDao(), new DeliveryDao(),
                new AgentDao(), () -> EmailConfig.load().createSender());
    }

    public NotificationService(EmailTemplateDao templateDao, EmailLogDao logDao, OrderDao orderDao,
                               ClientDao clientDao, DeliveryDao deliveryDao, AgentDao agentDao,
                               Supplier<EmailSender> senderFactory) {
        this.templateDao = templateDao;
        this.logDao = logDao;
        this.orderDao = orderDao;
        this.clientDao = clientDao;
        this.deliveryDao = deliveryDao;
        this.agentDao = agentDao;
        this.senderFactory = senderFactory;
    }

    // ------------------------------------------------------------ Task 8

    /** Sends the "your order is on its way" e-mail to the client's saved address. */
    public EmailLog notifyClientDispatched(int orderId) {
        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw new ValidationException("Order not found.");
        }
        Client client = clientDao.findById(order.getClientId());
        Delivery delivery = deliveryDao.findByOrder(orderId);
        DeliveryAgent agent = delivery == null ? null : agentDao.findById(delivery.getAgentId());
        Map<String, String> values = clientValues(order, client, delivery, agent);
        EmailTemplate t = getTemplate(EmailTemplate.ORDER_DISPATCHED);
        return deliver(client.getEmail(), RecipientType.CLIENT, TemplateEngine.render(t.getSubject(), values),
                TemplateEngine.render(t.getBody(), values), orderId);
    }

    // ------------------------------------------------------------ Task 9

    /** Sends the new-delivery e-mail with schedule and address to the assigned agent. */
    public EmailLog notifyAgentAssigned(int deliveryId) {
        Delivery delivery = deliveryDao.findById(deliveryId);
        if (delivery == null) {
            throw new ValidationException("Delivery not found.");
        }
        Order order = orderDao.findById(delivery.getOrderId());
        Client client = clientDao.findById(order.getClientId());
        DeliveryAgent agent = agentDao.findById(delivery.getAgentId());
        Map<String, String> values = agentValues(order, client, delivery, agent);
        EmailTemplate t = getTemplate(EmailTemplate.DELIVERY_ASSIGNED);
        return deliver(agent.getEmail(), RecipientType.AGENT, TemplateEngine.render(t.getSubject(), values),
                TemplateEngine.render(t.getBody(), values), order.getId());
    }

    // ------------------------------------------------------------ placeholders

    static Map<String, String> clientValues(Order order, Client client, Delivery delivery, DeliveryAgent agent) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("clientName", client.getName());
        v.put("orderRef", order.getOrderRef());
        v.put("orderDate", Formatters.dateTime(order.getOrderDate()));
        v.put("items", itemList(order.getItems()));
        v.put("total", Formatters.money(order.getTotalAmount()));
        v.put("agentName", agent == null ? "our delivery team" : agent.getName());
        v.put("agentPhone", agent == null ? "-" : agent.getPhone());
        v.put("vehicle", agent == null ? "-" : agent.getVehicleSummary());
        v.put("scheduledAt", delivery == null ? "soon" : Formatters.friendly(delivery.getScheduledAt()));
        v.put("deliveryAddress", client.getAddress());
        return v;
    }

    static Map<String, String> agentValues(Order order, Client client, Delivery delivery, DeliveryAgent agent) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("agentName", agent.getName());
        v.put("orderRef", order.getOrderRef());
        v.put("scheduledAt", Formatters.friendly(delivery.getScheduledAt()));
        v.put("clientName", client.getDisplayName());
        v.put("clientPhone", client.getPhone());
        v.put("deliveryAddress", client.getAddress());
        v.put("items", itemList(order.getItems()));
        v.put("itemCount", String.valueOf(order.getTotalQuantity()));
        v.put("total", Formatters.money(order.getTotalAmount()));
        v.put("vehicle", agent.getVehicleSummary());
        v.put("notes", Validator.isBlank(delivery.getNotes()) ? "-" : delivery.getNotes());
        return v;
    }

    private static String itemList(List<OrderItem> items) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : items) {
            sb.append("  - ").append(item.getQuantity()).append(" x ").append(item.getProductName())
                    .append(" (").append(Formatters.money(item.getLineTotal())).append(")\n");
        }
        return sb.toString().stripTrailing();
    }

    // ------------------------------------------------------------ sending + log

    private EmailLog deliver(String to, RecipientType type, String subject, String body, Integer orderId) {
        EmailLog log = new EmailLog();
        log.setRecipientEmail(to == null ? "" : to);
        log.setRecipientType(type);
        log.setSubject(subject.length() > 200 ? subject.substring(0, 200) : subject);
        log.setBody(body);
        log.setOrderId(orderId);
        try {
            EmailSender sender = senderFactory.get();
            sender.send(new EmailMessage(to, subject, body));
            log.setStatus(sender.isSimulation() ? EmailStatus.SIMULATED : EmailStatus.SENT);
        } catch (EmailException e) {
            log.setStatus(EmailStatus.FAILED);
            log.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.setStatus(EmailStatus.FAILED);
            log.setErrorMessage("Unexpected error: " + e.getMessage());
        }
        log.setId(logDao.insert(log));
        return log;
    }

    /** Sends a test e-mail from the settings screen. */
    public EmailLog sendTestEmail(String to) {
        if (!Validator.isValidEmail(to)) {
            throw new ValidationException("Enter a valid e-mail address for the test.");
        }
        return deliver(to.trim(), RecipientType.TEST, "EcoNest test e-mail",
                "Hello!\n\nThis is a test message from the EcoNest desktop application.\n"
                + "If you can read this, the e-mail settings are working.\n\nEcoNest", null);
    }

    // ------------------------------------------------------------ extra feature (Sandeepa): notification centre

    public List<EmailLog> getLogs(EmailStatus filter) {
        return filter == null ? logDao.findAll() : logDao.findByStatus(filter);
    }

    public int countFailed() {
        return logDao.countByStatus(EmailStatus.FAILED);
    }

    /** Sends a logged e-mail again (for example after fixing the SMTP settings). */
    public EmailLog resend(int logId) {
        EmailLog old = logDao.findById(logId);
        if (old == null) {
            throw new ValidationException("This notification no longer exists.");
        }
        if (old.getStatus() == EmailStatus.SENT) {
            throw new ValidationException("This e-mail was already sent successfully.");
        }
        return deliver(old.getRecipientEmail(), old.getRecipientType(), old.getSubject(), old.getBody(),
                old.getOrderId());
    }

    /** Sends every failed e-mail again; returns how many succeeded. */
    public int resendAllFailed() {
        int ok = 0;
        for (EmailLog log : logDao.findByStatus(EmailStatus.FAILED)) {
            EmailLog result = deliver(log.getRecipientEmail(), log.getRecipientType(), log.getSubject(),
                    log.getBody(), log.getOrderId());
            if (result.getStatus() != EmailStatus.FAILED) {
                ok++;
            }
        }
        return ok;
    }

    // ------------------------------------------------------------ extra feature (Rakshana): templates

    public EmailTemplate getTemplate(String key) {
        EmailTemplate t = templateDao.find(key);
        return t != null ? t : defaultTemplate(key);
    }

    public void saveTemplate(EmailTemplate t) {
        if (Validator.isBlank(t.getSubject())) {
            throw new ValidationException("The e-mail subject cannot be empty.");
        }
        if (t.getSubject().length() > 200) {
            throw new ValidationException("The subject must be 200 characters or less.");
        }
        if (Validator.isBlank(t.getBody())) {
            throw new ValidationException("The e-mail body cannot be empty.");
        }
        if (!t.getSubject().contains("{orderRef}") && !t.getBody().contains("{orderRef}")) {
            throw new ValidationException("Please keep {orderRef} in the subject or body so the "
                    + "recipient knows which order the e-mail is about.");
        }
        templateDao.save(t);
    }

    public EmailTemplate resetTemplate(String key) {
        EmailTemplate t = defaultTemplate(key);
        templateDao.save(t);
        return t;
    }

    /** Fills a template with example data so the user can preview it. */
    public static String[] preview(EmailTemplate t) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("clientName", "Nimali Perera");
        v.put("orderRef", "ECN-20261113-00042");
        v.put("orderDate", "2026-11-13 10:30");
        v.put("items", "  - 2 x Snake Plant (Rs. 3,000.00)\n  - 1 x Terracotta Pot 8in (Rs. 950.00)");
        v.put("itemCount", "3");
        v.put("total", "Rs. 3,950.00");
        v.put("agentName", "Kasun Silva");
        v.put("agentPhone", "0771234567");
        v.put("vehicle", "Motorbike WP BCD-4521");
        v.put("scheduledAt", "Fri, 14 Nov 2026 at 10:00 AM");
        v.put("deliveryAddress", "45 Flower Road, Colombo 07");
        v.put("clientPhone", "0712345678");
        v.put("notes", "Call before arriving");
        return new String[] {TemplateEngine.render(t.getSubject(), v), TemplateEngine.render(t.getBody(), v)};
    }

    public static EmailTemplate defaultTemplate(String key) {
        if (EmailTemplate.DELIVERY_ASSIGNED.equals(key)) {
            return new EmailTemplate(key, "New delivery assigned: {orderRef}",
                    "Hello {agentName},\n\n"
                    + "A new EcoNest delivery has been assigned to you.\n\n"
                    + "Order reference : {orderRef}\n"
                    + "Scheduled       : {scheduledAt}\n"
                    + "Vehicle         : {vehicle}\n\n"
                    + "Deliver to      : {clientName}\n"
                    + "Phone           : {clientPhone}\n"
                    + "Address         : {deliveryAddress}\n\n"
                    + "Items ({itemCount}):\n{items}\n\n"
                    + "Order value     : {total}\n"
                    + "Notes           : {notes}\n\n"
                    + "Please confirm with the office if you cannot make this time.\n\n"
                    + "EcoNest Operations");
        }
        return new EmailTemplate(EmailTemplate.ORDER_DISPATCHED, "Your EcoNest order {orderRef} is on its way!",
                "Dear {clientName},\n\n"
                + "Good news - your order {orderRef} (placed {orderDate}) has been dispatched.\n\n"
                + "Items:\n{items}\n\n"
                + "Order total : {total}\n"
                + "Delivery to : {deliveryAddress}\n"
                + "Expected    : {scheduledAt}\n"
                + "Driver      : {agentName} ({agentPhone}), {vehicle}\n\n"
                + "Please keep your phone nearby so our driver can reach you.\n"
                + "Quote {orderRef} if you need to contact us.\n\n"
                + "Thank you for growing greener with EcoNest!");
    }
}
