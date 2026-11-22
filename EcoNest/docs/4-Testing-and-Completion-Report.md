# EcoNest – Final Testing and Development Completion Report

**Date:** 13 November 2026  **Version:** 1.0

## 1. Summary

| Item | Result |
|---|---|
| All 9 specified business requirements implemented | **Yes** |
| One or more additional features per team member (7 members) | **Yes – 10 additional features** |
| Admin panel | Included (Users, Settings) although the specification does not require it |
| Java source files | 114 (113 application + 1 system test), about 10,700 lines |
| Compilation (`javac --release 17 -Xlint`) | **0 errors, 0 warnings** (Swing serialisation / this-escape lint categories excluded) |
| Automated system test (services + DAOs + SQL) | **86 passed, 0 failed** |
| Automated GUI test (real Swing screens clicked by a robot) | **19 passed, 0 failed**, 34 screenshots reviewed |
| Extra checks (roles, error messages, CSV, October totals) | **6 passed, 0 failed** |
| SMTP e-mail sender (plain, STARTTLS, SSL, wrong password, untrusted certificate) | **5 of 5 scenarios behaved correctly** |
| Runnable JAR (manifest `Main-Class: econest.Main`) | Starts and waits at the login window |
| Screenshots | 34 in `docs/screenshots` (taken on the SQLite test bridge, so the status bar shows `jdbc:fakesqlite`) |

**Conclusion:** the application is fully implemented and every feature was exercised by automated tests. The tests could **not** be run against a real MySQL/MariaDB server in the development environment (see section 2), so the team must run the 15-minute verification in section 6 on a computer with XAMPP before submission.

## 2. Test environment – what was and was not available

| Component | Used for testing | Note |
|---|---|---|
| JDK | OpenJDK 21.0.12, compiled with `--release 17` | Same code runs on JDK 17, 21, 25 |
| GUI | Real Swing windows on a virtual display (Xvfb, 1600×1000), Nimbus look-and-feel | Fonts on Windows will be Segoe UI, so spacing may differ slightly |
| Database | **SQLite through a test-only JDBC bridge** | MySQL/XAMPP could not be installed in the sandbox (no network access). `schema.sql` and `sample-data.sql` were translated mechanically (ENUM→CHECK, AUTO_INCREMENT→AUTOINCREMENT) and loaded, and the **real DAO SQL** of the application ran against them through JDBC. The bridge maps SQLite errors to the MySQL error codes 1062/1451/1452 that the application handles. The bridge is not part of the delivered project. |
| MySQL JDBC driver | Not available offline | `pom.xml` downloads `mysql-connector-j 8.4.0`; the Maven build itself was not run here. |
| E-mail | Local fake SMTP server (Python) with a self-signed TLS certificate | Real Gmail sending not tried. |

The SQL in the DAO classes was written to be standard (no SQLite- or MySQL-only functions except `DATE()`, `COALESCE`, `LIMIT`, which both support), and dates are passed from Java, so behaviour on MySQL is expected to be the same – but this is **expected, not yet observed**.

## 3. Requirement coverage (original specification)

| # | Requirement | Owner | Screens | Verified by tests | Status |
|---|---|---|---|---|---|
| 1 | Manage Product Catalogue (add, update, remove; price; eco-rating) | Senuji | Products | add/validate/duplicate/update/remove→inactive/delete; GUI add with validation | Implemented & tested |
| 2 | Manage Clients (household & office; contact details) | Mishara | Clients | add both types, validation (e-mail, phone, company), duplicate e-mail, update, delete protection | Implemented & tested |
| 3 | Stock / Inventory (quantity, reorder level, stock-in from suppliers) | Zija | Inventory (4 tabs) | stock-in adds quantity, future date / zero rejected, reorder level, low-stock alert list, GUI stock-in | Implemented & tested |
| 4 | Manage Delivery Agents (vehicle details) | Zija | Delivery Agents | add, invalid/duplicate vehicle number, update, delete protection | Implemented & tested |
| 5 | Process Client Orders (products, totals) | Fahath | Orders, New Order | line totals, order total 6,100.50, merging duplicate lines, stock decrease, over-stock rejected with full rollback, cancel restores stock; GUI order Rs. 22,000 | Implemented & tested |
| 6 | Assign Delivery Agents, schedule and track status | Fahath | Deliveries, Assign dialog | assign → PROCESSING, past time rejected, invalid status jump rejected, in transit → DISPATCHED, delivered → DELIVERED + time, failed → re-assign | Implemented & tested |
| 7 | Monthly Sales & Inventory Reports, low-stock alerts | Rashmi | Reports | November and October 2026 totals checked by hand (Oct = 50,100.00; cancelled excluded), units per product, inventory received/sold, low-stock list | Implemented & tested |
| 8 | E-mail clients when order dispatched | Rakshana | Deliveries → Mark Dispatched; Notifications | sent to the saved client e-mail, subject contains order reference, body contains total; GUI dispatch message | Implemented & tested (simulated + fake SMTP) |
| 9 | E-mail agents when delivery assigned | Sandeepa | Assign dialog; Notifications | sent to agent e-mail with order ref, schedule, address, notes | Implemented & tested (simulated + fake SMTP) |

**Team-guide checklist** (from the EcoNest group guide, section 9)

| Check | Result |
|---|---|
| Starts from `Main.java` without errors | Yes (GUI test and JAR) |
| Each feature can add, view, update and remove | Yes (protected deletes explained to the user) |
| Data remains after closing and reopening | Yes – data is in the database; GUI test reopened screens after saving |
| Invalid prices, empty required fields, invalid quantities rejected | Yes – 28 rejection tests |
| Order total calculated from prices × quantities | Yes |
| Stock decreases when an order is processed (rule: at order creation; returned on cancel) | Yes |
| Agent can be assigned and delivery status updated | Yes |
| Monthly reports use saved orders and inventory data | Yes – no hard-coded totals |
| E-mails triggered by the correct events without exposing credentials | Yes – `config/email.properties` excluded from Git; simulation mode by default |
| Teammate can clone and run using the README | README and guide provided; to be confirmed by a teammate |

## 4. Additional features – test results

| Member | Feature | Test evidence | Status |
|---|---|---|---|
| Senuji | Price-change history; search & eco-rating filter | history row with old/new price and +20 %; no row when price unchanged; filter returns 1 kit; GUI dialog shows 2,950 → 3,200 | Tested |
| Mishara | Purchase history & loyalty tier | 1 order / 6,100.50 / favourite product; GUI shows Gold tier, Rs. 61,100.00 for Ruwan | Tested |
| Zija | Reorder suggestions + CSV | suggestion for low kit ≥ 8 units; GUI tab and CSV export routine | Tested |
| Zija | Availability & workload | open deliveries = 1; cannot make busy agent unavailable; unavailable agents excluded from assignment | Tested |
| Fahath | Printable invoice | invoice contains reference and total; GUI preview (printing to a physical printer not tried) | Tested |
| Fahath | Schedule-clash warning | clash 30 min apart found; none at 3 h; GUI warning shown and confirmed | Tested |
| Rashmi | Revenue chart, CSV export, printing | one bar per day; CSV contains summary & product rows; chart screenshot checked | Tested (printing dialog not tried) |
| Rakshana | E-mail templates | template without {orderRef} rejected; edited template used in the next e-mail; preview rendered | Tested |
| Sandeepa | Notification centre & resend | failed e-mail logged as FAILED with the error; resend creates a new successful entry | Tested |
| Shared | Login & admin panel | wrong password rejected; admin login; staff cannot see Users/Settings or call user management; cannot disable own account; last admin protected | Tested |

## 5. Detailed automated test log (system test)

All 86 checks passed (`SystemTest.java`, included in the project so the team can rerun it against MySQL):

```
PASS  Login with wrong password -> "Incorrect username or password."
PASS  Login with correct password
PASS  Create user with short password -> "Password must be at least 6 characters."
PASS  Admin creates staff user / Duplicate username / Reset password then login
PASS  Admin cannot disable own account
PASS  Product with empty name / zero price / eco-rating 6 rejected
PASS  Add products, duplicate name rejected, appears in list
PASS  Price change recorded in history (extra) / no history without price change
PASS  Search/filter by category and rating
PASS  Client invalid e-mail / invalid phone / office without company rejected
PASS  Add household and office clients, duplicate e-mail rejected, update client
PASS  Add supplier; stock-in 20 + 10 = 30; zero quantity and future date rejected
PASS  Low-stock alert lists Kit (3 <= 5) and not Fern; reorder level update; negative rejected
PASS  Agent invalid / duplicate vehicle number rejected; add and update agents
PASS  Order without items rejected; same product merges; total 6,100.50 saved; reference ECN-yyyyMMdd-nnnnn
PASS  Stock decreased (30 - 3 = 27, 3 - 1 = 2)
PASS  Order more than stock -> "Not enough stock for … only 2 available."; rolled back
PASS  Delivery time in the past rejected; agent assigned; order PROCESSING
PASS  Task 9: agent e-mail to agent address with order ref, address and schedule
PASS  Workload = 1; cannot make agent unavailable; clash warning 30 min; no clash 3 h
PASS  Invalid status jump ASSIGNED -> DELIVERED rejected
PASS  Order DISPATCHED; Task 8: client e-mail to saved address with ref and total
PASS  Cannot cancel a dispatched order; DELIVERED with delivered time
PASS  Cancel: CANCELLED, 4 units returned, open delivery closed as FAILED
PASS  Report revenue, cancelled count, product sales 3 units / Rs 3600, daily entries, 10 received
PASS  CSV rows, dashboard stats, reorder suggestion, client history, invoice
PASS  Template without {orderRef} rejected; edited template used
PASS  Failed e-mail logged as FAILED; resend succeeds
PASS  Delete protections (client with orders, ordered product -> inactive, agent with history,
      supplier with stock-in); inactive product cannot be ordered; normal deletes work
PASSED: 86   FAILED: 0      (test records removed afterwards - table counts verified)
```

GUI test (19 checks): login error and success, dashboard, products (12 rows, validation message, add → 13 rows, price history), client history (Gold, 61,100.00), inventory low-stock banner (4 products) and all tabs, stock-in, agents, new order (two products, total label), save confirmation, assignment with clash warning, agent e-mail message, order list +1, invoice (33,500.00), dispatch e-mail to *sachini.w@example.com*, November report, template preview, users, settings.

SMTP sender: plain SMTP → sent; STARTTLS with trusted certificate → sent; SSL (port 465 style) → sent; wrong password → *"Login rejected by mail server (535). For Gmail, use an App Password…"*; untrusted certificate → refused (certificate checking is on). The received message decoded correctly (UTF-8 subject with ✔, body with a line containing only ".").

## 6. Pending – must be done by the team on a real PC (about 15 minutes)

| # | Task | How | Expected |
|---|---|---|---|
| 1 | Open the Maven project in IntelliJ | Guide B.2 | Driver 8.4.0 downloaded, no red errors |
| 2 | Import `schema.sql` and `sample-data.sql` into XAMPP | Guide C.4 | 13 tables, 12 products |
| 3 | Run `SystemTest` against MySQL | Guide D.4 | `PASSED: 86   FAILED: 0` |
| 4 | Run `Main`, log in, do the 5-minute check | Guide E.2 | All 8 steps as described |
| 5 | (Optional) Real e-mail with a Gmail App Password | Guide E.3 | Test e-mail arrives |
| 6 | (Optional) Print an invoice / report table | Orders → View / Invoice → Print | Printer dialog opens |
| 7 | Add members' surnames to the Additional Features Report | – | – |

Known limitations (by design, documented): one delivery record per order (a failed delivery is re-assigned, not duplicated); report stock levels show the current quantity rather than a month-end snapshot; e-mails are sent synchronously with a 20-second timeout; the sample data is dated October–November 2026.

## 7. Development completion statement

The EcoNest Java Swing desktop application has been **developed completely**: all nine specified requirements, every task in the team allocation, at least one additional feature for each of the seven members, a login and admin panel, the MySQL database scripts, the JDBC layer, documentation and an automated test suite. All features were **tested automatically** in the development environment with the results above. Verification against a real XAMPP MySQL server and real e-mail sending (section 6) is the only outstanding step, because those services were not available where the application was built.
