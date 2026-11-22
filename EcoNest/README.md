# EcoNest – Home & Garden Management (Java Swing)

Desktop application for EcoNest: products, clients, stock, suppliers, delivery agents, orders,
deliveries, monthly reports and e-mail notifications. Java 17+, Swing, JDBC, MySQL (XAMPP).

## Quick start
1. Install **JDK 21**, **IntelliJ IDEA** and **XAMPP** (see `docs/3-Installation-and-Setup-Guide.md`).
2. Start **MySQL** in the XAMPP Control Panel.
3. In phpMyAdmin → **Import** `database/schema.sql`, then `database/sample-data.sql`.
4. IntelliJ → **Open** this folder (Maven project, the MySQL driver downloads automatically).
5. Run `src/main/java/econest/Main.java`. Log in with **admin / admin123** (or staff / staff123).

Database settings: `config/db.properties` (created on first run; XAMPP default user `root`, no password).
E-mail: simulation mode by default; real SMTP can be set in **Settings** (admin).

## Verify everything
Run `src/test/java/econest/SystemTest.java` → expect `PASSED: 86   FAILED: 0`.

## Team allocation
| Member | Task(s) | Main classes |
|---|---|---|
| Senuji | 1 Products | ProductPanel, ProductService, ProductDao |
| Mishara (leader) | 2 Clients, integration | ClientPanel, ClientService, ClientDao, base classes |
| Zija | 3 Inventory, 4 Agents | InventoryPanel, InventoryService, AgentPanel, AgentService |
| Fahath | 5 Orders, 6 Deliveries | OrderPanel, NewOrderDialog, OrderService, DeliveryPanel, DeliveryService |
| Rashmi | 7 Reports | ReportPanel, ReportService, ReportDao, BarChartPanel |
| Rakshana | 8 Client dispatch e-mail | NotificationService, SmtpEmailSender, template editor |
| Sandeepa | 9 Agent assignment e-mail | NotificationService, Notification Centre |

## Documents (`docs/`)
1. Additional Features Report  2. Database and JDBC  3. Installation and Setup Guide  4. Testing and Completion Report

Do **not** commit `config/db.properties` or `config/email.properties` (already in `.gitignore`).
