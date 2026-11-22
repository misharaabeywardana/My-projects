package econest;

import econest.dao.DBConnection;
import econest.dao.UserDao;
import econest.model.Client;
import econest.model.ClientSummary;
import econest.model.ClientType;
import econest.model.Delivery;
import econest.model.DeliveryAgent;
import econest.model.DeliveryStatus;
import econest.model.EmailLog;
import econest.model.EmailStatus;
import econest.model.EmailTemplate;
import econest.model.Order;
import econest.model.OrderItem;
import econest.model.OrderStatus;
import econest.model.PriceHistory;
import econest.model.Product;
import econest.model.ProductCategory;
import econest.model.ProductSales;
import econest.model.RecipientType;
import econest.model.ReorderSuggestion;
import econest.model.Role;
import econest.model.StockIn;
import econest.model.Supplier;
import econest.model.User;
import econest.model.VehicleType;
import econest.service.AgentService;
import econest.service.ClientService;
import econest.service.DeliveryService;
import econest.service.InventoryService;
import econest.service.MonthlyReport;
import econest.service.NotificationService;
import econest.service.OrderService;
import econest.service.ProductService;
import econest.service.ReportService;
import econest.service.UserService;
import econest.service.ValidationException;
import econest.service.email.EmailException;
import econest.service.email.EmailSender;
import econest.service.email.SimulatedEmailSender;
import econest.util.PasswordUtil;
import econest.util.Session;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end test of every feature through the real service and DAO classes,
 * against the database configured in config/db.properties.
 *
 * Run it in IntelliJ (right-click > Run 'SystemTest.main()') AFTER importing
 * schema.sql. It creates its own records (names start with "ZZT"), checks
 * the results, prints PASS/FAIL for each check and deletes its records again.
 */
public class SystemTest {

    private static int passed;
    private static final List<String> failures = new ArrayList<>();
    private static final String TAG = "ZZT" + (System.currentTimeMillis() % 100000);

    // ids created by the test, removed in cleanup()
    private static final List<Integer> productIds = new ArrayList<>();
    private static final List<Integer> clientIds = new ArrayList<>();
    private static final List<Integer> agentIds = new ArrayList<>();
    private static final List<Integer> supplierIds = new ArrayList<>();
    private static final List<Integer> userIds = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("EcoNest system test - tag " + TAG);
        try {
            run();
        } catch (Throwable t) {
            failures.add("Unexpected error: " + t);
            t.printStackTrace();
        } finally {
            try {
                cleanup();
                System.out.println("Cleanup done.");
            } catch (Exception e) {
                System.out.println("Cleanup problem: " + e.getMessage());
            }
        }
        System.out.println();
        System.out.println("PASSED: " + passed + "   FAILED: " + failures.size());
        for (String f : failures) {
            System.out.println("  FAIL: " + f);
        }
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    // ------------------------------------------------------------------ helpers

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failures.add(name);
            System.out.println("FAIL  " + name);
        }
    }

    private interface Action {
        void run();
    }

    private static void expectRejected(String name, Action action) {
        try {
            action.run();
            check(name + " (should be rejected)", false);
        } catch (ValidationException e) {
            check(name + " -> \"" + e.getMessage().split("\n")[0] + "\"", true);
        }
    }

    private static boolean eq(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) == 0;
    }

    // ------------------------------------------------------------------ tests

    private static void run() {
        check("Database connection: " + DBConnection.testConnection(), true);

        // ---------- users / login / admin
        UserDao userDao = new UserDao();
        User admin = new User();
        admin.setUsername(TAG.toLowerCase() + "_admin");
        admin.setFullName("Test Admin");
        admin.setRole(Role.ADMIN);
        admin.setSalt(PasswordUtil.newSalt());
        admin.setPasswordHash(PasswordUtil.hash("secret99", admin.getSalt()));
        admin.setId(userDao.insert(admin));
        userIds.add(admin.getId());

        UserService users = new UserService();
        expectRejected("Login with wrong password", () -> users.login(admin.getUsername(), "nope"));
        User logged = users.login(admin.getUsername(), "secret99");
        check("Login with correct password", logged != null && Session.isAdmin());

        User staff = new User();
        staff.setUsername(TAG.toLowerCase() + "_staff");
        staff.setFullName("Test Staff");
        staff.setRole(Role.STAFF);
        expectRejected("Create user with short password", () -> users.createUser(staff, "123"));
        users.createUser(staff, "staffpw1");
        userIds.add(staff.getId());
        check("Admin creates staff user", staff.getId() > 0);
        User dup = new User();
        dup.setUsername(staff.getUsername());
        dup.setFullName("Dup");
        dup.setRole(Role.STAFF);
        expectRejected("Duplicate username", () -> users.createUser(dup, "abcdef1"));
        users.resetPassword(staff.getId(), "newpass1");
        check("Reset password then login", new UserService().login(staff.getUsername(), "newpass1") != null);
        Session.setCurrentUser(logged);
        expectRejected("Admin cannot disable own account", () -> {
            logged.setActive(false);
            try {
                users.updateUser(logged);
            } finally {
                logged.setActive(true);
            }
        });

        // ---------- Task 1: products
        ProductService products = new ProductService();
        expectRejected("Product with empty name", () -> products.addProduct(product("", "100", 3), 0, 5));
        expectRejected("Product with zero price", () -> products.addProduct(product(TAG + " X", "0", 3), 0, 5));
        expectRejected("Product with eco-rating 6", () -> products.addProduct(product(TAG + " X", "10", 6), 0, 5));
        Product fern = product(TAG + " Fern", "1000.00", 5);
        products.addProduct(fern, 20, 5);
        productIds.add(fern.getId());
        Product kit = product(TAG + " Kit", "2500.50", 4);
        kit.setCategory(ProductCategory.GARDEN_KIT);
        products.addProduct(kit, 3, 5);
        productIds.add(kit.getId());
        check("Add products (Fern, Kit)", fern.getId() > 0 && kit.getId() > 0);
        expectRejected("Duplicate product name", () -> products.addProduct(product(TAG + " Fern", "5", 3), 0, 1));
        check("Product appears in list", products.getAll().stream().anyMatch(p -> p.getId() == fern.getId()));
        fern.setPrice(new BigDecimal("1200.00"));
        products.updateProduct(fern);
        List<PriceHistory> history = products.getPriceHistory(fern.getId());
        check("Price change recorded in history (extra)", history.size() == 1
                && eq(history.get(0).getOldPrice(), new BigDecimal("1000")) && eq(history.get(0).getNewPrice(),
                new BigDecimal("1200")) && Math.abs(history.get(0).getChangePercent() - 20.0) < 0.001);
        fern.setDescription("Updated description");
        products.updateProduct(fern);
        check("Update without price change adds no history", products.getPriceHistory(fern.getId()).size() == 1);
        check("Search/filter by category and rating", ProductService.filter(products.getAll(), TAG, ProductCategory.GARDEN_KIT,
                4, false).size() == 1);

        // ---------- Task 2: clients
        ClientService clients = new ClientService();
        expectRejected("Client with invalid e-mail", () -> clients.addClient(client("A", "bad-email", ClientType.HOUSEHOLD)));
        expectRejected("Client with invalid phone", () -> {
            Client c = client("A", TAG.toLowerCase() + "@example.com", ClientType.HOUSEHOLD);
            c.setPhone("12ab");
            clients.addClient(c);
        });
        expectRejected("Office client without company", () -> clients.addClient(client("A",
                TAG.toLowerCase() + "o@example.com", ClientType.OFFICE)));
        Client home = client(TAG + " Home", TAG.toLowerCase() + ".home@example.com", ClientType.HOUSEHOLD);
        clients.addClient(home);
        clientIds.add(home.getId());
        Client office = client(TAG + " Office", TAG.toLowerCase() + ".office@example.com", ClientType.OFFICE);
        office.setCompanyName("Test Corp");
        clients.addClient(office);
        clientIds.add(office.getId());
        check("Add household and office clients", home.getId() > 0 && office.getId() > 0);
        expectRejected("Duplicate client e-mail", () -> clients.addClient(client("B", home.getEmail(),
                ClientType.HOUSEHOLD)));
        home.setAddress("99 Updated Street, Colombo");
        clients.updateClient(home);
        check("Update client", "99 Updated Street, Colombo".equals(clients.getById(home.getId()).getAddress()));

        // ---------- Task 3: inventory + suppliers
        InventoryService inventory = new InventoryService();
        Supplier sup = new Supplier();
        sup.setName(TAG + " Supplier");
        sup.setPhone("0112223344");
        inventory.saveSupplier(sup);
        supplierIds.add(sup.getId());
        check("Add supplier", sup.getId() > 0);
        StockIn in = new StockIn();
        in.setProductId(fern.getId());
        in.setSupplierId(sup.getId());
        in.setQuantity(10);
        in.setUnitCost(new BigDecimal("600"));
        inventory.recordStockIn(in);
        check("Stock-in adds to quantity (20 + 10 = 30)", inventory.getByProduct(fern.getId()).getQuantityOnHand() == 30);
        expectRejected("Stock-in with zero quantity", () -> {
            StockIn bad = new StockIn();
            bad.setProductId(fern.getId());
            bad.setSupplierId(sup.getId());
            bad.setQuantity(0);
            inventory.recordStockIn(bad);
        });
        expectRejected("Stock-in dated in the future", () -> {
            StockIn bad = new StockIn();
            bad.setProductId(fern.getId());
            bad.setSupplierId(sup.getId());
            bad.setQuantity(1);
            bad.setReceivedDate(LocalDate.now().plusDays(3));
            inventory.recordStockIn(bad);
        });
        check("Low-stock alert lists Kit (3 <= 5)", inventory.getLowStock().stream()
                .anyMatch(i -> i.getProductId() == kit.getId()));
        check("Low-stock alert does not list Fern", inventory.getLowStock().stream()
                .noneMatch(i -> i.getProductId() == fern.getId()));
        inventory.updateReorderLevel(fern.getId(), 8);
        check("Update reorder level", inventory.getByProduct(fern.getId()).getReorderLevel() == 8);
        expectRejected("Negative reorder level", () -> inventory.updateReorderLevel(fern.getId(), -1));

        // ---------- Task 4: agents
        AgentService agents = new AgentService();
        expectRejected("Agent with invalid vehicle number", () -> agents.addAgent(agent("A", "!!")));
        DeliveryAgent a1 = agent(TAG + " Rider", TAG + "-1");
        agents.addAgent(a1);
        agentIds.add(a1.getId());
        DeliveryAgent a2 = agent(TAG + " Driver", TAG + "-2");
        a2.setVehicleType(VehicleType.VAN);
        agents.addAgent(a2);
        agentIds.add(a2.getId());
        check("Add agents", a1.getId() > 0 && a2.getId() > 0);
        expectRejected("Duplicate vehicle number", () -> agents.addAgent(agent("C", TAG + "-1")));
        a2.setPhone("0779998887");
        agents.updateAgent(a2);
        check("Update agent", "0779998887".equals(agents.getById(a2.getId()).getPhone()));

        // ---------- Task 5: orders
        OrderService orders = new OrderService();
        expectRejected("Order without items", () -> {
            Order o = new Order();
            o.setClientId(home.getId());
            orders.createOrder(o);
        });
        Order draft = new Order();
        draft.setClientId(home.getId());
        draft.addItem(new OrderItem(products.getById(fern.getId()), 2));
        draft.addItem(new OrderItem(products.getById(kit.getId()), 1));
        draft.addItem(new OrderItem(products.getById(fern.getId()), 1)); // merges into 3 x Fern
        check("Same product added twice merges into one line", draft.getItems().size() == 2
                && draft.getItems().get(0).getQuantity() == 3);
        BigDecimal expected = new BigDecimal("1200.00").multiply(BigDecimal.valueOf(3)).add(new BigDecimal("2500.50"));
        check("Draft total = 3 x 1200.00 + 2500.50 = 6100.50", eq(draft.getTotalAmount(), expected));
        Order o1 = orders.createOrder(draft);
        Order saved = orders.getById(o1.getId());
        check("Order saved with total 6100.50", saved != null && eq(saved.getTotalAmount(), expected)
                && saved.getItems().size() == 2 && saved.getStatus() == OrderStatus.PENDING);
        check("Order reference format ECN-yyyyMMdd-nnnnn", saved.getOrderRef().matches("ECN-\\d{8}-\\d{5}"));
        check("Line totals saved", eq(saved.getItems().get(0).getLineTotal(), new BigDecimal("3600.00")));
        check("Stock decreased (Fern 30 - 3 = 27, Kit 3 - 1 = 2)",
                inventory.getByProduct(fern.getId()).getQuantityOnHand() == 27
                && inventory.getByProduct(kit.getId()).getQuantityOnHand() == 2);
        int ordersBefore = orders.getAll().size();
        expectRejected("Order more than stock", () -> {
            Order big = new Order();
            big.setClientId(office.getId());
            big.addItem(new OrderItem(products.getById(fern.getId()), 1));
            big.addItem(new OrderItem(products.getById(kit.getId()), 50));
            orders.createOrder(big);
        });
        check("Failed order rolled back (no order saved, Fern stock still 27)",
                orders.getAll().size() == ordersBefore && inventory.getByProduct(fern.getId()).getQuantityOnHand() == 27);

        // ---------- Task 6 + Task 9: assign agent -> agent e-mail
        DeliveryService deliveries = new DeliveryService();
        LocalDateTime when = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        expectRejected("Delivery time in the past", () -> deliveries.assignAgent(o1.getId(), a1.getId(),
                LocalDateTime.now().minusDays(1), null));
        DeliveryService.Result assigned = deliveries.assignAgent(o1.getId(), a1.getId(), when, "Gate code 1234");
        Delivery d1 = assigned.getDelivery();
        check("Agent assigned, delivery ASSIGNED", d1 != null && d1.getStatus() == DeliveryStatus.ASSIGNED
                && d1.getAgentId() == a1.getId());
        check("Order moved to PROCESSING", orders.getById(o1.getId()).getStatus() == OrderStatus.PROCESSING);
        EmailLog agentMail = assigned.getEmail();
        check("Task 9: agent e-mail logged to agent address", agentMail != null
                && agentMail.getRecipientType() == RecipientType.AGENT
                && a1.getEmail().equals(agentMail.getRecipientEmail()) && agentMail.getStatus() != EmailStatus.FAILED);
        check("Task 9: agent e-mail has order ref, address and schedule",
                agentMail.getBody().contains(saved.getOrderRef()) && agentMail.getBody().contains("99 Updated Street")
                && agentMail.getBody().contains("Gate code 1234"));
        check("Agent open delivery count = 1 (workload)", agents.getById(a1.getId()).getOpenDeliveries() == 1);
        expectRejected("Cannot make agent unavailable with open deliveries",
                () -> agents.setAvailable(a1.getId(), false));

        // second order for clash + cancel tests
        Order draft2 = new Order();
        draft2.setClientId(office.getId());
        draft2.addItem(new OrderItem(products.getById(fern.getId()), 4));
        Order o2 = orders.createOrder(draft2);
        check("Clash warning finds delivery 30 min apart (extra)",
                deliveries.findClashes(a1.getId(), when.plusMinutes(30), 0).size() == 1);
        check("No clash 3 hours apart", deliveries.findClashes(a1.getId(), when.plusHours(3), 0).isEmpty());
        deliveries.assignAgent(o2.getId(), a2.getId(), when.plusHours(2), null);
        agents.setAvailable(a2.getId(), true);

        // ---------- Task 8: dispatch -> client e-mail
        expectRejected("Invalid status jump ASSIGNED -> DELIVERED",
                () -> deliveries.updateStatus(d1.getId(), DeliveryStatus.DELIVERED, null));
        DeliveryService.Result dispatched = deliveries.updateStatus(d1.getId(), DeliveryStatus.IN_TRANSIT, null);
        check("Order DISPATCHED when delivery in transit",
                orders.getById(o1.getId()).getStatus() == OrderStatus.DISPATCHED);
        EmailLog clientMail = dispatched.getEmail();
        check("Task 8: client e-mail sent to saved client address", clientMail != null
                && clientMail.getRecipientType() == RecipientType.CLIENT
                && home.getEmail().equals(clientMail.getRecipientEmail()));
        check("Task 8: client e-mail includes order reference and total",
                clientMail.getSubject().contains(saved.getOrderRef()) && clientMail.getBody().contains("6,100.50"));
        expectRejected("Cannot cancel a dispatched order", () -> orders.cancelOrder(o1.getId(), "test"));
        deliveries.updateStatus(d1.getId(), DeliveryStatus.DELIVERED, "Signed by client");
        check("Order DELIVERED", orders.getById(o1.getId()).getStatus() == OrderStatus.DELIVERED);
        check("Delivered time recorded", deliveries.getByOrder(o1.getId()).getDeliveredAt() != null);

        // ---------- cancel restores stock
        int fernBefore = inventory.getByProduct(fern.getId()).getQuantityOnHand();
        orders.cancelOrder(o2.getId(), "Client request");
        check("Cancel: order CANCELLED", orders.getById(o2.getId()).getStatus() == OrderStatus.CANCELLED);
        check("Cancel: 4 Fern returned to stock", inventory.getByProduct(fern.getId()).getQuantityOnHand() == fernBefore + 4);
        check("Cancel: open delivery closed as FAILED",
                deliveries.getByOrder(o2.getId()).getStatus() == DeliveryStatus.FAILED);

        // ---------- Task 7: reports
        ReportService reports = new ReportService();
        MonthlyReport r = reports.buildMonthlyReport(YearMonth.now());
        check("Report revenue includes the new order", r.getSummary().getRevenue().compareTo(expected) >= 0);
        check("Report counts cancelled orders", r.getSummary().getCancelledOrders() >= 1);
        ProductSales fernSales = r.getProductSales().stream().filter(p -> p.getProductId() == fern.getId())
                .findFirst().orElse(null);
        check("Product sales: Fern 3 units, Rs 3600 (cancelled order excluded)", fernSales != null
                && fernSales.getQuantity() == 3 && eq(fernSales.getRevenue(), new BigDecimal("3600")));
        check("Daily revenue has one entry per day", r.getDailyRevenue().size() == YearMonth.now().lengthOfMonth());
        check("Inventory report shows 10 Fern received", r.getUnitsReceived(fern.getId()) == 10);
        check("CSV export rows built (extra)", reports.toCsvRows(r).size() > 20);
        check("Dashboard stats load", reports.getDashboardStats().activeProducts >= 2);

        // ---------- extra features
        List<ReorderSuggestion> suggestions = inventory.getReorderSuggestions();
        ReorderSuggestion kitSug = suggestions.stream().filter(s -> s.getItem().getProductId() == kit.getId())
                .findFirst().orElse(null);
        check("Reorder suggestion for Kit (extra)", kitSug != null && kitSug.getSuggestedQuantity() >= 8);

        ClientSummary cs = clients.getSummary(home.getId());
        check("Client history: 1 order, value 6100.50, favourite Fern (extra)", cs.getTotalOrders() == 1
                && eq(cs.getLifetimeValue(), expected) && (TAG + " Fern").equals(cs.getFavouriteProduct()));
        check("Client order list", clients.getOrders(home.getId()).size() == 1);

        String invoice = orders.buildInvoice(o1.getId());
        check("Invoice contains reference and total (extra)", invoice.contains(saved.getOrderRef())
                && invoice.contains("6,100.50"));

        NotificationService notes = new NotificationService();
        EmailTemplate original = notes.getTemplate(EmailTemplate.ORDER_DISPATCHED);
        expectRejected("Template without {orderRef}", () -> notes.saveTemplate(
                new EmailTemplate(EmailTemplate.ORDER_DISPATCHED, "Hi", "Hello")));
        notes.saveTemplate(new EmailTemplate(EmailTemplate.ORDER_DISPATCHED, "Shipped {orderRef}",
                "Hi {clientName}, {orderRef} is coming."));
        EmailLog custom = notes.notifyClientDispatched(o1.getId());
        check("Edited template is used (extra)", custom.getSubject().equals("Shipped " + saved.getOrderRef())
                && custom.getBody().startsWith("Hi " + home.getName()));
        notes.saveTemplate(original);

        EmailSender broken = new EmailSender() {
            @Override
            public void send(econest.service.email.EmailMessage m) throws EmailException {
                throw new EmailException("Simulated network failure");
            }

            @Override
            public boolean isSimulation() {
                return false;
            }

            @Override
            public String describe() {
                return "broken";
            }
        };
        NotificationService failing = new NotificationService(new econest.dao.EmailTemplateDao(),
                new econest.dao.EmailLogDao(), new econest.dao.OrderDao(), new econest.dao.ClientDao(),
                new econest.dao.DeliveryDao(), new econest.dao.AgentDao(), () -> broken);
        EmailLog failed = failing.notifyClientDispatched(o1.getId());
        check("Failed e-mail is logged as FAILED, not lost", failed.getStatus() == EmailStatus.FAILED
                && failed.getErrorMessage().contains("Simulated network failure"));
        NotificationService working = new NotificationService(new econest.dao.EmailTemplateDao(),
                new econest.dao.EmailLogDao(), new econest.dao.OrderDao(), new econest.dao.ClientDao(),
                new econest.dao.DeliveryDao(), new econest.dao.AgentDao(), SimulatedEmailSender::new);
        EmailLog retried = working.resend(failed.getId());
        check("Resend failed e-mail (extra)", retried.getStatus() == EmailStatus.SIMULATED
                && retried.getBody().equals(failed.getBody()));

        // ---------- delete protections
        expectRejected("Delete client with orders", () -> clients.deleteClient(home.getId()));
        String msg = products.removeProduct(fern.getId());
        check("Remove ordered product -> marked inactive", msg.contains("INACTIVE") && !products.getById(fern.getId()).isActive());
        expectRejected("Inactive product cannot be ordered", () -> {
            Order o = new Order();
            o.setClientId(office.getId());
            o.addItem(new OrderItem(products.getById(fern.getId()), 1));
            orders.createOrder(o);
        });
        expectRejected("Delete agent with delivery history", () -> agents.deleteAgent(a1.getId()));
        expectRejected("Delete supplier with stock-in records", () -> inventory.deleteSupplier(sup.getId()));
        Product temp = product(TAG + " Temp", "10", 2);
        products.addProduct(temp, 0, 0);
        String msg2 = products.removeProduct(temp.getId());
        check("Remove never-ordered product -> deleted", msg2.contains("deleted") && products.getById(temp.getId()) == null);
        Client tempClient = client(TAG + " Temp", TAG.toLowerCase() + ".temp@example.com", ClientType.HOUSEHOLD);
        clients.addClient(tempClient);
        clients.deleteClient(tempClient.getId());
        check("Delete client without orders", clients.getById(tempClient.getId()) == null);
        users.deleteUser(staff.getId());
        check("Admin deletes user", userDao.findById(staff.getId()) == null);
    }

    // ------------------------------------------------------------------ builders

    private static Product product(String name, String price, int rating) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(ProductCategory.PLANT);
        p.setPrice(new BigDecimal(price));
        p.setEcoRating(rating);
        return p;
    }

    private static Client client(String name, String email, ClientType type) {
        Client c = new Client();
        c.setName(name);
        c.setClientType(type);
        c.setEmail(email);
        c.setPhone("0771234567");
        c.setAddress("1 Test Road, Colombo");
        return c;
    }

    private static DeliveryAgent agent(String name, String vehicle) {
        DeliveryAgent a = new DeliveryAgent();
        a.setName(name);
        a.setPhone("0711234567");
        a.setEmail(name.toLowerCase().replaceAll("[^a-z0-9]", "") + "@example.com");
        a.setVehicleType(VehicleType.MOTORBIKE);
        a.setVehicleNumber(vehicle);
        return a;
    }

    // ------------------------------------------------------------------ cleanup

    private static void cleanup() throws Exception {
        try (Connection con = DBConnection.getConnection()) {
            for (int clientId : clientIds) {
                exec(con, "DELETE FROM email_log WHERE order_id IN (SELECT order_id FROM orders WHERE client_id = ?)", clientId);
                exec(con, "DELETE FROM deliveries WHERE order_id IN (SELECT order_id FROM orders WHERE client_id = ?)", clientId);
                exec(con, "DELETE FROM order_items WHERE order_id IN (SELECT order_id FROM orders WHERE client_id = ?)", clientId);
                exec(con, "DELETE FROM orders WHERE client_id = ?", clientId);
                exec(con, "DELETE FROM clients WHERE client_id = ?", clientId);
            }
            exec(con, "DELETE FROM email_log WHERE recipient_email LIKE ?", TAG.toLowerCase() + "%");
            for (int id : productIds) {
                exec(con, "DELETE FROM stock_in WHERE product_id = ?", id);
                exec(con, "DELETE FROM products WHERE product_id = ?", id);
            }
            for (int id : agentIds) {
                exec(con, "DELETE FROM delivery_agents WHERE agent_id = ?", id);
            }
            for (int id : supplierIds) {
                exec(con, "DELETE FROM suppliers WHERE supplier_id = ?", id);
            }
            for (int id : userIds) {
                exec(con, "DELETE FROM users WHERE user_id = ?", id);
            }
        }
    }

    private static void exec(Connection con, String sql, int id) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static void exec(Connection con, String sql, String value) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.executeUpdate();
        }
    }
}
