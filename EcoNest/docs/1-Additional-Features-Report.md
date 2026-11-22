# EcoNest – Additional Features Report

**Project:** EcoNest Home & Garden Management (Java Swing desktop application)
**Module:** Object Oriented Programming (Java) – HDIT Year 1 Semester 2, 2026
**Technologies:** Java 17+, Java Swing, JDBC, MySQL (XAMPP), IntelliJ IDEA

> Note for the team: the task allocation document gives first names only. Add each member's surname to the "Full name" line before submitting.

## How to read this report

Every member section has two clearly separated parts:

- **ORIGINAL REQUIREMENT** – what the project specification and the task allocation asked for. These are implemented exactly as specified and were not changed.
- **ADDITIONAL FEATURE** – something extra we added that builds on the original requirement without replacing it.

All features open from the same application window (sidebar menu). Every additional feature uses the same database (`econest_db`) and the same model classes as the rest of the team.

| Member | Original task(s) | Additional feature(s) |
|---|---|---|
| Senuji | 1 – Manage Product Catalogue | Product Price-Change History; search & eco-rating filter |
| Mishara (Group Leader) | 2 – Manage Clients | Client Purchase History & Loyalty Tier |
| Zija | 3 – Stock/Inventory; 4 – Delivery Agents | Smart Reorder Suggestions with CSV purchase list; Agent Availability & Workload |
| Fahath | 5 – Process Orders; 6 – Assign Delivery Agents | Printable / savable Invoice; Delivery Schedule-Clash Warning |
| Rashmi | 7 – Monthly Sales & Inventory Reports | Daily Revenue Chart, CSV (Excel) export and printing |
| Rakshana | 8 – E-mail clients when an order is dispatched | Editable E-mail Templates with live preview |
| Sandeepa | 9 – E-mail agents when a delivery is assigned | Notification Centre (e-mail log) with Resend |
| Shared (integration) | – | Login with hashed passwords and an Admin Panel (Users, Settings) |

---

## 1. Senuji

**Full name:** Senuji ______________ (add surname)

### ORIGINAL REQUIREMENT – Task 1: Manage Product Catalogue
Add, view, update and remove plants, planters and gardening kits. Store product name, type (category), price and eco-rating. Validate required fields and prices.

*Implemented in:* `ProductPanel`, `ProductDialog`, `ProductService`, `ProductDao`, table `products`.

### ADDITIONAL FEATURE – Product Price-Change History

**Purpose.** In the original requirement, updating a price simply overwrites the old value, so nobody can see when a price changed, by how much, or who changed it. The history gives the shop an audit trail for pricing decisions and protects against mistaken price edits.

**How it works.**
1. When a product is saved, `ProductService.updateProduct()` first loads the current record inside the same database transaction.
2. If the new price is different, a row is written to `product_price_history` with the old price, new price, the logged-in username and the date/time.
3. Both the product update and the history row are committed together – if one fails, neither is saved.
4. The *Price History* window lists all changes with the percentage increase or decrease.

A second small addition is a **search box with category and minimum eco-rating filters**, plus a "Show inactive" option, so staff can quickly find, for example, "all gardening kits rated 4 stars or more".

**How to use it (step by step).**
1. Click **Products** in the sidebar.
2. Select a product (for example *Areca Palm*) and click **Edit**, change the price and click **Save**.
3. With the product still selected, click **Price History**.
4. The window shows the date, old price, new price, % change and who changed it.
5. To filter, type in the search box or choose a category / eco-rating in the drop-down lists.

**Java classes and Swing screens.** `ProductPanel` (buttons *Price History*, filters), `PriceHistoryDialog` (new window), `ProductService.updateProduct()`, `ProductService.getPriceHistory()`, `ProductService.filter()`, `ProductDao.insertPriceHistory()`, `ProductDao.findPriceHistory()`, model `PriceHistory`.

**Database tables.** `product_price_history` (new; FK `product_id` → `products`), `products`.

**Benefits.** Accountability for price changes, easier to answer client questions ("why did this cost less last month?"), and the price at the time of each order is still kept in `order_items.unit_price`, so reports remain correct.

---

## 2. Mishara (Group Leader)

**Full name:** Mishara ______________ (add surname)

### ORIGINAL REQUIREMENT – Task 2: Manage Clients
Add, view, update and remove household and office clients. Store client type, name, phone, e-mail and address. Coordinate shared class names, database design and final integration.

*Implemented in:* `ClientPanel`, `ClientDialog`, `ClientService`, `ClientDao`, table `clients`. Shared design work is visible in the common base classes (`BaseEntity`, `Person`, `BaseDao`, `CrudDao`, `AbstractCrudPanel`) that every member's screen uses.

### ADDITIONAL FEATURE – Client Purchase History & Loyalty Tier

**Purpose.** The original screen stores contact details only. Staff also need to know how valuable a client is and what they buy, for example to offer office clients a discount or to recommend products.

**How it works.**
1. `ClientDao.getSummary()` runs one SQL query over the client's orders and calculates: number of completed orders, number of cancelled orders, lifetime spend (cancelled orders excluded), first and last order date.
2. A second query finds the client's favourite product (most units bought).
3. The window shows four KPI cards (lifetime value, orders, average order value, days since last order) and the full order list.
4. A **loyalty tier** is assigned automatically: **Gold** from Rs. 50,000 lifetime spend, **Silver** from Rs. 20,000, otherwise **Green**.

**How to use it (step by step).**
1. Click **Clients** in the sidebar.
2. Select a client in the table (e.g. *Ruwan Jayasinghe*).
3. Click **Purchase History**.
4. Read the cards and the list of orders; close the window when done.

**Java classes and Swing screens.** `ClientPanel` (button *Purchase History*), `ClientHistoryDialog` (new window with `StatCard` components), `ClientService.getSummary()`, `ClientService.getOrders()`, `ClientDao.getSummary()`, `OrderDao.findByClient()`, model `ClientSummary`.

**Database tables.** `clients`, `orders`, `order_items`, `products` (read only – no new table needed).

**Benefits.** Better customer service, identifies top clients for promotions, and spots clients who have not ordered for a long time.

---

## 3. Zija

**Full name:** Zija ______________ (add surname)

### ORIGINAL REQUIREMENT – Task 3: Manage Stock / Inventory
Track quantity on hand, reorder level and stock received from suppliers; show low-stock alerts.

*Implemented in:* `InventoryPanel` (tabs *Stock Levels*, *Stock-In History*, *Suppliers*), `StockInDialog`, `SupplierPanel`, `SupplierDialog`, `InventoryService`, `InventoryDao`, `StockInDao`, `SupplierDao`; tables `inventory`, `stock_in`, `suppliers`. The low-stock alert appears as a yellow banner on the Inventory screen, on the Dashboard and in the reports.

### ORIGINAL REQUIREMENT – Task 4: Manage Delivery Agents
Add, view, update and remove delivery agents with contact and vehicle details, in a separate class/panel.

*Implemented in:* `AgentPanel`, `AgentDialog`, `AgentService`, `AgentDao`, table `delivery_agents`.

### ADDITIONAL FEATURE (Task 3) – Smart Reorder Suggestions with CSV Purchase List

**Purpose.** A low-stock alert says *what* is low but not *how much* to buy. Ordering too little causes stock-outs; ordering too much ties up money in plants that may wilt.

**How it works.**
1. `InventoryService.getReorderSuggestions()` reads units sold per product in the last 30 days (non-cancelled orders).
2. Average daily sales = units sold ÷ 30. Days of stock left = quantity on hand ÷ daily sales.
3. A product is listed if it is at/below its reorder level **or** will run out within 14 days.
4. Suggested order quantity = target − on hand, where target = the larger of (2 × reorder level) and (30 days of sales + reorder level).
5. The list can be exported to a CSV file that opens in Excel and sent to suppliers, or turned directly into a stock-in record.

**How to use it.**
1. Click **Inventory** → tab **Reorder Suggestions**.
2. Review the suggested quantities.
3. Click **Export Purchase List (CSV)** and choose where to save the file, **or**
4. When the stock arrives, select the row and click **Record Stock-In for Selected** – the product and suggested quantity are filled in for you.

**Classes / screens.** `InventoryPanel.buildReorderTab()`, `InventoryService.getReorderSuggestions()` and `suggestedQuantity()`, `InventoryDao.unitsSoldSince()`, model `ReorderSuggestion`, `CsvExporter`, `StockInDialog`.
**Tables.** `inventory`, `products`, `orders`, `order_items` (read), `stock_in` (write when stock is recorded).

### ADDITIONAL FEATURE (Task 4) – Agent Availability Switch & Live Workload

**Purpose.** Agents go on leave or have vehicle problems; deleting them would lose their delivery history. Dispatchers also need to see who is busy.

**How it works.** Each agent has an *available* flag. **Toggle Availability** switches it without deleting the agent; unavailable agents are not offered when assigning deliveries. The table shows an **Open deliveries** column (assigned or in transit), calculated live by SQL. Agents cannot be made unavailable while they still have open deliveries, and the assignment screen lists the least-busy agents first.

**How to use it.** Delivery Agents → select an agent → **Toggle Availability**. Use the *Available only* tick box and the vehicle filter to narrow the list.

**Classes / screens.** `AgentPanel`, `AgentService.setAvailable()`, `AgentDao.findActive()` (sorted by workload), `DeliveryAgent.openDeliveries`. **Tables.** `delivery_agents`, `deliveries`.

**Benefits.** Fewer stock-outs and less over-stocking; fair distribution of deliveries; agent history is never lost.

---

## 4. Fahath

**Full name:** Fahath ______________ (add surname)

### ORIGINAL REQUIREMENT – Task 5: Process Client Orders
Create an order for a client, add products and quantities, calculate line totals and order total.

*Implemented in:* `OrderPanel`, `NewOrderDialog`, `OrderService.createOrder()`, `OrderDao`, model `Order`/`OrderItem`; tables `orders`, `order_items`. Saving an order also reduces stock in the same transaction, and cancelling returns the stock.

### ORIGINAL REQUIREMENT – Task 6: Assign Delivery Agents to Orders
Assign an agent to an order, record the schedule and update delivery status.

*Implemented in:* `DeliveryPanel`, `AssignDeliveryDialog`, `DeliveryService`, `DeliveryDao`; table `deliveries`.

### ADDITIONAL FEATURE (Task 5) – Printable Invoice

**Purpose.** Clients, especially office clients, need a document for their records, and drivers need a delivery note.

**How it works.** `InvoiceGenerator` builds a neatly aligned invoice (order reference, date, bill-to details, every line with quantity, unit price and line total, grand total, delivery agent and schedule, notes). The window can send it to any printer using Swing's built-in printing (`JTextArea.print()`), or save it as a text file.

**How to use it.**
1. **Orders** → select an order → **View / Invoice** (or double-click the row).
2. Click **Print Invoice** to print, or **Save as Text File** to keep a copy.

**Classes / screens.** `OrderDetailsDialog`, `InvoiceGenerator`, `OrderService.buildInvoice()`. **Tables.** `orders`, `order_items`, `clients`, `deliveries`, `delivery_agents` (read).

### ADDITIONAL FEATURE (Task 6) – Delivery Schedule-Clash Warning

**Purpose.** Prevents booking the same driver at two addresses at the same time.

**How it works.** Before saving an assignment, `DeliveryService.findClashes()` searches that agent's open deliveries scheduled within ±60 minutes. If any are found, a warning lists them (order reference, time, address) and asks whether to assign anyway.

**How to use it.** Orders → **Assign Agent** (or Deliveries → **Assign Agent**), choose agent and time, click **Assign & notify agent**. If there is a clash, choose *No* to pick another time/agent, or *Yes* to continue.

**Classes / screens.** `AssignDeliveryDialog`, `DeliveryService.findClashes()`, `DeliveryDao.findClashes()`. **Tables.** `deliveries`, `orders`, `clients`.

**Benefits.** Professional paperwork for clients; realistic delivery schedules and fewer late deliveries.

---

## 5. Rashmi

**Full name:** Rashmi ______________ (add surname)

### ORIGINAL REQUIREMENT – Task 7: Monthly Sales & Inventory Reports
Monthly revenue/sales summaries and inventory reports including low-stock items, using real database data. Agree on how cancelled orders and order dates are handled.

*Implemented in:* `ReportPanel`, `ReportService`, `MonthlyReport`, `ReportDao`. **Rules agreed:** orders belong to the month of their *order date*; **cancelled orders are excluded** from revenue and units sold but counted separately. The report shows revenue, number of orders, average order value, units sold, new clients, sales by product, an inventory report (received this month, sold this month, on hand, reorder level, status) and a low-stock alert list.

### ADDITIONAL FEATURE – Daily Revenue Chart, CSV Export and Printing

**Purpose.** Numbers in a table are hard to read quickly. A chart shows busy and quiet days at a glance; the CSV file lets management open the report in Excel; printing gives a paper copy for meetings.

**How it works.**
1. `ReportService` fills in every day of the month (days without sales become Rs. 0) so the chart has one bar per day.
2. `BarChartPanel` is a custom Swing component drawn with Java2D – no external chart library. It calculates a "nice" axis maximum, draws grid lines, bars and day labels, and shows the exact amount when you hover over a bar.
3. **Export CSV** writes the whole report (summary, sales by product, daily revenue, inventory) to a UTF-8 CSV file.
4. **Print Table** prints the selected table with a title and page numbers using `JTable.print()`.

**How to use it.**
1. **Reports** → choose the month and year → **Generate**.
2. The *Revenue Chart* tab opens first; hover over a bar to see that day's revenue.
3. Click **Export CSV** to save the report, or open another tab and click **Print Table**.

**Classes / screens.** `ReportPanel`, `BarChartPanel`, `ReportService.toCsvRows()`, `ReportService.fillMissingDays()`, `ReportDao.dailyRevenue()`, `CsvExporter`, model `DailyRevenue`.
**Tables.** `orders`, `order_items`, `products`, `inventory`, `stock_in`, `clients` (read only).

**Benefits.** Faster decisions, easy sharing with people who do not use the application, and a visual element that strengthens the project demonstration.

---

## 6. Rakshana

**Full name:** Rakshana ______________ (add surname)

### ORIGINAL REQUIREMENT – Task 8: E-mail clients when an order is dispatched
Send a client notification when the order status changes to dispatched, using the client's saved e-mail address and a useful order reference. Keep SMTP credentials outside source code.

*Implemented in:* `NotificationService.notifyClientDispatched()`, triggered by `DeliveryService.updateStatus()` when a delivery becomes *In transit* (order becomes *Dispatched*). Sending uses `SmtpEmailSender` (STARTTLS/SSL, Gmail App Password) or `SimulatedEmailSender`. Credentials live in `config/email.properties`, which is excluded from Git.

### ADDITIONAL FEATURE – Editable E-mail Templates with Live Preview

**Purpose.** The wording of e-mails is a business decision (tone, promotions, holiday notices). Without templates, every wording change would need a programmer.

**How it works.**
1. Templates are stored in the `email_templates` table with placeholders such as `{clientName}`, `{orderRef}`, `{items}`, `{total}`, `{scheduledAt}`, `{agentName}`.
2. `TemplateEngine.render()` replaces each placeholder with real order data when the e-mail is sent.
3. The editor shows the template and a live preview filled with example data side by side; double-clicking a placeholder inserts it.
4. Validation stops a template from being saved if the subject or body is empty or if `{orderRef}` is missing (the spec requires a useful order reference).
5. **Reset to Default** restores the original wording. If the table is empty, built-in defaults are used, so e-mails always work.

**How to use it.**
1. **Notifications** → tab **E-mail Templates**.
2. Choose *Order dispatched – sent to the client* (or the agent template).
3. Edit the subject/body; watch the preview update.
4. Click **Save Template**. The next dispatch e-mail uses the new wording.

**Classes / screens.** `NotificationPanel.buildTemplateTab()`, `NotificationService.getTemplate()/saveTemplate()/resetTemplate()/preview()`, `EmailTemplateDao`, `TemplateEngine`, model `EmailTemplate`.
**Tables.** `email_templates` (new).

**Benefits.** Non-technical staff control client communication; consistent professional messages; the same mechanism also powers the agent e-mail template.

---

## 7. Sandeepa

**Full name:** Sandeepa ______________ (add surname)

### ORIGINAL REQUIREMENT – Task 9: E-mail delivery agents when a delivery is assigned
Notify the assigned agent when a new delivery is assigned, including the order reference, delivery schedule and necessary details. Keep SMTP credentials outside source code.

*Implemented in:* `NotificationService.notifyAgentAssigned()`, called by `DeliveryService.assignAgent()` after the assignment is committed. The e-mail contains order reference, schedule, vehicle, client name, phone, delivery address, item list, order value and notes.

### ADDITIONAL FEATURE – Notification Centre with Resend

**Purpose.** E-mail can fail (no internet, wrong password). Without a record, the shop would not know that a client or driver was never informed.

**How it works.**
1. Every e-mail attempt – client, agent or test – is stored in `email_log` with recipient, subject, full body, related order, status (*Sent*, *Failed*, *Simulated*) and the error message.
2. The Notification Centre lists the log with a status filter and shows the full e-mail when a row is selected.
3. **Resend Selected** sends the same e-mail again; **Resend All Failed** retries every failed one (for example after fixing the SMTP settings). Each retry is logged as a new entry.
4. The Dashboard shows the number of failed e-mails so problems are noticed.
5. **Simulation mode** (the default) records e-mails without sending them, which is safe for class demonstrations.

**How to use it.**
1. **Notifications** → tab **Notification Centre**.
2. Use *Show* to filter, e.g. *Failed*.
3. Select an e-mail to read it underneath.
4. Click **Resend Selected** or **Resend All Failed**.

**Classes / screens.** `NotificationPanel.buildLogTab()`, `NotificationService.resend()`, `resendAllFailed()`, `getLogs()`, `EmailLogDao`, model `EmailLog`, `EmailStatus`, `RecipientType`.
**Tables.** `email_log` (new; FK `order_id` → `orders`, `ON DELETE SET NULL`).

**Benefits.** No notification is lost silently; full audit trail of what each client and agent was told; easy recovery from e-mail outages.

---

## 8. Shared integration feature – Login and Admin Panel

The specification does not require an admin panel, so this is listed separately and not credited to one member. It protects the system and supports the other features:

- **Login screen** with passwords stored as salted PBKDF2 hashes (`PasswordUtil`), never as plain text.
- Two roles: **Administrator** and **Staff**. Staff use all business screens; only administrators see the **Admin Panel** (*Users* and *Settings*).
- **Users:** add/edit users, change role, activate/deactivate, reset passwords, delete. Safety rules stop an admin from disabling or deleting their own account and always keep at least one active administrator.
- **Settings:** e-mail mode (simulation or SMTP), SMTP server details, *Send Test* button, database connection details with *Test Connection*.
- **Change password** link for every user in the header.

Classes: `LoginDialog`, `UserPanel`, `UserDialog`, `SettingsPanel`, `DbSettingsDialog`, `ChangePasswordDialog`, `UserService`, `UserDao`, `Session`. Table: `users`.
