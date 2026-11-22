# EcoNest – Installation and Implementation Guide (Beginner Friendly)

This guide assumes you have never installed Java, IntelliJ IDEA or XAMPP before. Follow the sections **in order**. Screens and menu names are for **Windows 10/11**; macOS notes are given where different.

**What you will have at the end:** the EcoNest Java Swing application running from IntelliJ IDEA, connected to a MySQL database in XAMPP, with sample data loaded.

**You need:** a 64-bit computer, about 4 GB free disk space, internet for the downloads, and the project folder `EcoNest` (unzip `EcoNest.zip` to a simple path such as `C:\Projects\EcoNest`).

---

## A. Install the Java JDK

**What is the JDK?** The Java Development Kit is the set of tools that turns your `.java` files into a running program (compiler `javac` + Java runtime `java`).

**Which version?** Use **JDK 21 (LTS)**. The EcoNest code is written for Java 17 or newer and was compiled and tested with JDK 21. JDK 17 or 25 also work. Do **not** use JDK 8 or 11 – the project needs 17+.

**Official download:** Eclipse Temurin (free, open source build of OpenJDK): https://adoptium.net/temurin/releases/?version=21
(Oracle's JDK from https://www.oracle.com/java/technologies/downloads/ also works.)

**Install – Windows**
1. On the Adoptium page choose **Operating System: Windows**, **Architecture: x64**, **Package Type: JDK**, version **21**, and download the **.msi** file.
2. Double-click the `.msi`. Click **Next**.
3. On the *Custom Setup* screen, click the options **"Set JAVA_HOME variable"** and **"Add to PATH"** and choose *"Will be installed on local hard drive"* for both. Click **Next → Install → Finish**.
4. Check it: press the **Windows key**, type `cmd`, press Enter, then type:
   ```
   java -version
   ```
   You should see something like `openjdk version "21.0.x"`. If you see *"'java' is not recognized"*, restart the computer and try again.

*macOS:* download the `.pkg` for your chip (aarch64 for Apple Silicon, x64 for Intel) and double-click it.

**Configure the JDK in IntelliJ IDEA** – done in step B.4 below (IntelliJ must be installed first).

---

## B. Install IntelliJ IDEA

**Official download:** https://www.jetbrains.com/idea/download/

Since version 2025.3 there is just **one IntelliJ IDEA** download (the separate "Community Edition" was merged into it). Everything needed for this project is **free** – you do not need to buy or activate anything. If it offers a 30-day trial of extra features you can ignore it.

### B.1 Install
1. Download the Windows **.exe** installer and double-click it.
2. Click **Next** until *Installation Options*. Tick **Create Desktop Shortcut** and **Add "Open Folder as Project"**. Click **Next → Install**.
3. When it finishes, tick **Run IntelliJ IDEA** and click **Finish**. Accept the user agreement. You may choose not to send data.

### B.2 Open the EcoNest project (recommended way)
The project already contains a `pom.xml` file (a *Maven* project). Maven is built into IntelliJ and **downloads the MySQL JDBC driver automatically** – this is the easiest way.
1. On the IntelliJ welcome screen click **Open**.
2. Choose the folder `C:\Projects\EcoNest` (the folder that contains `pom.xml`) and click **OK**.
3. If asked *"Trust and Open Project?"* click **Trust Project**.
4. Wait until the progress bar at the bottom finishes ("Indexing…", "Downloading…"). This needs internet the first time.

### B.3 Understand the folders and packages
```
EcoNest/
├── pom.xml                         ← Maven settings + MySQL driver dependency
├── README.md
├── config/                         ← db.properties and email.properties (created on first run)
├── database/
│   ├── schema.sql                  ← creates the database and tables
│   └── sample-data.sql             ← optional test data
├── docs/                           ← these documents
└── src/
    ├── main/java/econest/
    │   ├── Main.java               ← START HERE (main method)
    │   ├── model/                  ← data classes: Product, Client, Order, DeliveryAgent ...
    │   ├── dao/                    ← database code (JDBC + SQL): DBConnection, ProductDao ...
    │   ├── service/                ← business rules and validation: OrderService ...
    │   │   └── email/              ← SMTP e-mail sending
    │   ├── ui/                     ← Swing: MainFrame
    │   │   ├── panels/             ← one screen per feature (ProductPanel, OrderPanel ...)
    │   │   ├── dialogs/            ← pop-up forms (ProductDialog, NewOrderDialog ...)
    │   │   └── components/         ← shared styled widgets (AppButton, DataTable, Theme ...)
    │   └── util/                   ← helpers: Validator, Formatters, PasswordUtil ...
    └── test/java/econest/
        └── SystemTest.java         ← automatic test of all features
```
**Package** = folder of related classes. The first line of every Java file (e.g. `package econest.dao;`) must match its folder. To add a class yourself: right-click a package in the *Project* panel → **New → Java Class** → type the name → Enter.

*If you ever create a project from scratch:* **File → New → Project → Java**, Build system **Maven**, JDK **21**, then put your packages under `src/main/java`.

### B.4 Configure the JDK in IntelliJ
1. **File → Project Structure…** (or press `Ctrl+Alt+Shift+S`).
2. Click **Project** on the left.
3. **SDK:** choose **21** (temurin-21). If it is not in the list choose **Add SDK → JDK…** and select `C:\Program Files\Eclipse Adoptium\jdk-21…`. (IntelliJ can also download one: **Add SDK → Download JDK… → Version 21 → Eclipse Temurin**.)
4. **Language level:** "SDK default". Click **OK**.

### B.5 Add the MySQL JDBC driver
**JDBC driver** = a JAR file that lets Java talk to MySQL.

- **Using Maven (already done):** `pom.xml` contains:
  ```xml
  <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <version>8.4.0</version>
  </dependency>
  ```
  If you change `pom.xml`, click the small **Maven reload** icon (circular arrows "Load Maven Changes") that appears at the top-right of the editor. To check: open the **Maven** tool window (right edge) → *Dependencies* → you should see `com.mysql:mysql-connector-j:8.4.0`.
- **Manual way (only if Maven cannot download, e.g. no internet in the lab):**
  1. Download the platform-independent ZIP from https://dev.mysql.com/downloads/connector/j/ (choose "Platform Independent"; use *Archives* for 8.4.0) and unzip it.
  2. Create a folder `lib` inside the project and copy `mysql-connector-j-8.4.0.jar` into it.
  3. **File → Project Structure → Modules → Dependencies → + → JARs or Directories…** → select the JAR → **OK**.

---

## C. Install XAMPP (MySQL database + phpMyAdmin)

**Official download:** https://www.apachefriends.org/download.html

XAMPP bundles Apache (web server), **MariaDB** (the free MySQL-compatible database – the XAMPP panel still calls it "MySQL") and **phpMyAdmin** (a web page for managing the database). EcoNest only needs the database; Apache is needed only to open phpMyAdmin.

### C.1 Install
1. Download the latest **XAMPP for Windows** installer and run it. If Windows warns about *User Account Control*, click **OK** – this is why you should install to `C:\xampp` (the default), not to *Program Files*.
2. On *Select Components*, keep at least **MySQL** and **phpMyAdmin** (Apache is selected automatically). You can untick FileZilla, Mercury, Tomcat, Perl, Webalizer, Fake Sendmail.
3. Folder: `C:\xampp`. Click **Next** until it installs, then **Finish**. Choose a language for the Control Panel.
4. If the Windows Firewall asks about Apache or mysqld, allow **Private networks**.

*macOS:* download the OS X installer, open the `.dmg`, run it, then open **XAMPP → manager-osx** and use the *Manage Servers* tab.

### C.2 Start Apache and MySQL
1. Open **XAMPP Control Panel** (Start menu → *XAMPP Control Panel*; run it **as administrator** the first time).
2. Click **Start** next to **Apache** and **Start** next to **MySQL**. Both module names turn **green** and show port numbers (Apache 80/443, MySQL 3306).
3. Keep the Control Panel open (you can minimise it) whenever you use EcoNest.

### C.3 Open phpMyAdmin
- Click **Admin** next to MySQL in the Control Panel, **or** open a browser at **http://localhost/phpmyadmin/**.
- You are logged in automatically as user `root` (XAMPP's default user has **no password**).

### C.4 Create the database and import the SQL files
The file `schema.sql` creates the database `econest_db` for you.
1. In phpMyAdmin click the **Import** tab at the top (with no database selected on the left).
2. **Choose File** → select `C:\Projects\EcoNest\database\schema.sql`.
3. Leave the other options as they are (Character set utf-8, Format SQL). Scroll down and click **Import** (or **Go**).
4. You should see a green message *"Import has been successfully finished"*. On the left, **econest_db** appears with 13 tables.
5. Click **econest_db** on the left, click **Import** again, choose `database\sample-data.sql`, and click **Import**. (Optional but recommended – it adds products, clients, agents and orders for testing.)
6. Check: click **econest_db → products → Browse** – you should see 12 products.

**Re-importing later** (to reset the data): import `schema.sql` again – it drops and recreates all tables – then `sample-data.sql`.

**Login accounts created by the scripts**

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Administrator (sees the Admin Panel) |
| `staff` | `staff123` | Staff (from sample-data.sql) |

Change these passwords after the first login (header → *Change password*).

---

## D. Connecting Java Swing to MySQL

### D.1 JDBC in simple words
**JDBC (Java Database Connectivity)** is Java's standard way of talking to a database. Think of it as a telephone call:
1. **Driver** – the phone line provider (the MySQL Connector/J JAR).
2. **Connection** – dialling the number: `DriverManager.getConnection(url, user, password)`.
3. **PreparedStatement** – what you say: an SQL command with `?` blanks, e.g. `SELECT * FROM products WHERE product_id = ?`, then you fill the blanks with `setInt(1, 5)`.
4. **ResultSet** – the answer: rows you read one by one with `next()` and `getString("name")`.
5. **Close** – hang up (`try-with-resources` does this automatically).

The **URL** says where the database is: `jdbc:mysql://localhost:3306/econest_db` = MySQL protocol, on this computer (`localhost`), port `3306`, database `econest_db`.

### D.2 Configure the connection
Settings are in **`config/db.properties`** in the project folder (kept out of the Java code so passwords are never committed to GitHub). It is created automatically the first time you run the program with XAMPP's defaults:
```
db.url=jdbc:mysql://localhost:3306/econest_db
db.user=root
db.password=
```
- If you gave MySQL's root user a password, write it after `db.password=`.
- If MySQL uses a different port (Control Panel shows it), change `3306`.
- You can also edit these values inside the program: login screen → **Database settings…** → **Test connection** → **Save** (or *Settings → Edit Connection…* as admin).

### D.3 The complete connection code
File location: **`src/main/java/econest/dao/DBConnection.java`** (package `econest.dao`). It is already in the project – the full listing is in *2-Database-and-JDBC.md*, section 5. The important part:
```java
public static Connection getConnection() {
    try {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    } catch (SQLException e) {
        throw new DataAccessException(explain(e), e, DataAccessException.Kind.CONNECTION);
    }
}
```
Every DAO uses it like this (example from `BaseDao`):
```java
try (Connection con = DBConnection.getConnection();
     PreparedStatement ps = con.prepareStatement("SELECT name, price FROM products WHERE product_id = ?")) {
    ps.setInt(1, 5);
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            System.out.println(rs.getString("name") + " costs " + rs.getBigDecimal("price"));
        }
    }
}
```
Other files involved: `config/db.properties` (settings), `util/ConfigFile.java` (reads/writes the settings file), `dao/BaseDao.java` (shared JDBC helper), `dao/DataAccessException.java` (friendly errors).

### D.4 Test the database connection
Choose any one:
1. **Inside the app:** on the login window click **Database settings… → Test connection**. A green message starting with *"Connected to …"* followed by the server name/version and the URL means it works. (With XAMPP the version usually mentions MariaDB – that is normal.)
2. **As admin:** *Settings → Test Connection*.
3. **Full automatic test:** in IntelliJ open `src/test/java/econest/SystemTest.java`, click the green ▶ next to `public static void main` → **Run 'SystemTest.main()'**. It tests every feature against your database (about 86 checks), prints PASS/FAIL, and deletes its own test records at the end. Expected last line: `PASSED: 86   FAILED: 0`.

---

## E. Running the Application

### E.1 Run from IntelliJ
1. Make sure **MySQL is green** in the XAMPP Control Panel.
2. In the *Project* panel open `src/main/java/econest/Main.java`.
3. Click the green ▶ beside `public class Main` (or beside `main`) → **Run 'Main.main()'**. Next time you can just press the green ▶ at the top right, or `Shift+F10`.
4. The **login window** appears. Log in with `admin` / `admin123`.

**Important – working directory:** the program looks for the `config` folder in its working directory. IntelliJ uses the project folder by default, which is correct. If you see a *config* folder appear somewhere else, open **Run → Edit Configurations… → Main → Working directory** and set it to the project folder.

### E.2 Verify the database operations work (5-minute check)
| Step | Do this | You should see |
|---|---|---|
| 1 | **Products → Add Product**, name *Test Fern*, price *500*, opening stock *10* → **Add product** | New row in the table. In phpMyAdmin *products → Browse* the row is there (**Create**). |
| 2 | Close and reopen the program, open **Products** | *Test Fern* is still there (**Read**, data is saved). |
| 3 | Select it → **Edit** → price *550* → **Save** → **Price History** | One change 500 → 550 (**Update** + extra feature). |
| 4 | **Orders → New Order**, choose a client, add 2 × *Test Fern*, **Place order** | Total Rs. 1,100.00; *Inventory* shows 8 left. |
| 5 | Answer **Yes** to "Assign a delivery agent now?", choose an agent, **Assign & notify agent** | Message that the agent e-mail was recorded; *Notifications* lists it. |
| 6 | **Deliveries** → select the order → **Mark Dispatched** → **Yes** | Client e-mail recorded; order status *Dispatched*. |
| 7 | **Reports** → current month → **Generate** | The new order is included in revenue and the chart. |
| 8 | **Clients → Add Client** (any test data) → select it → **Delete** | Row disappears (**Delete**). |

### E.3 Sending real e-mails (optional)
By default e-mails are **simulated** (recorded in *Notifications* but not sent) – ideal for demonstrations. To send real e-mails with Gmail:
1. Your Google account needs **2-Step Verification** turned on. Then create an **App Password** at https://myaccount.google.com/apppasswords (name it "EcoNest"). Copy the 16-character password.
2. In EcoNest log in as admin → **Settings**:
   Mode **Send real e-mails**, SMTP host `smtp.gmail.com`, Port `587`, Security `STARTTLS`, Username = your Gmail address, Password = the App Password, From name `EcoNest`. Click **Save E-mail Settings**.
3. Type your own address next to *Send test e-mail to* → **Send Test**. Check your inbox.
4. The settings are stored in `config/email.properties`, which is listed in `.gitignore` – never upload it to GitHub.
5. Use your own/test addresses only. The sample clients use `example.com`, which never receives mail – those e-mails will show *Failed* or bounce in real mode.

### E.4 Troubleshooting common errors
| Problem / message | Cause | Fix |
|---|---|---|
| "Cannot reach the MySQL server… click Start next to MySQL" / *Communications link failure* | MySQL not running | Open XAMPP Control Panel → **Start** MySQL. |
| MySQL does not start in XAMPP; log says *port 3306 in use* | Another MySQL (e.g. MySQL Installer, Workbench service) uses the port | Stop the other service (Windows *Services* → MySQL80 → Stop) or change XAMPP's port in *Config → my.ini* (`port=3307`) and in `db.url`. |
| MySQL stops right after starting ("shutdown unexpectedly") | Damaged data files, often after a power cut | Back up `C:\xampp\mysql\data`, then follow XAMPP's *mysql\backup* restore steps or reinstall XAMPP and re-import the SQL files. |
| Apache does not start (port 80/443 in use) | Skype/IIS/other web server | Only needed for phpMyAdmin: stop the other program, or change Apache's port in *Config → httpd.conf* (`Listen 8080`) and open http://localhost:8080/phpmyadmin. |
| "The database econest_db does not exist yet" | SQL not imported | Section C.4. |
| "MySQL refused the username/password" | Password in `db.properties` wrong | Section D.2. |
| "The MySQL JDBC driver was not found" | Driver JAR missing | Reload Maven (B.5) or add the JAR manually. |
| *Incorrect username or password* at login | Wrong login | Use `admin`/`admin123`. If you changed and forgot it, re-import `schema.sql` (resets everything) or in phpMyAdmin copy the admin `password_hash` and `salt` values from `schema.sql` into the `users` row. |
| Red underlines everywhere in IntelliJ / *package does not exist* | JDK not set or Maven not loaded | B.4 and the Maven reload icon. Also **File → Invalidate Caches → Restart**. |
| *java: error: release version 17 not supported* | Project JDK older than 17 | Set SDK 21 (B.4). |
| `#1046 No database selected` in phpMyAdmin | Imported sample-data first | Import `schema.sql` first. |
| `#1452 Cannot add or update a child row` during import | Files imported in the wrong order | Import `schema.sql` again, then `sample-data.sql`. |
| E-mail *Login rejected (535)* | Normal Gmail password used | Use an App Password (E.3). |
| E-mail *Could not send… timed out* | Firewall/campus network blocks SMTP | Try another network or port 465 with SSL; keep simulation mode for the demo. |
| Reports show zero for this month | The sample orders are dated Oct–Nov 2026 | Choose **November 2026** in Reports, or create new orders. |
| *Server time zone value … is unrecognized* (older drivers only) | Old Connector/J | Use the 8.4.0 driver from `pom.xml`. |

### E.5 Packaging the application (optional)
To give the program to someone without IntelliJ:

**With Maven (easiest)**
1. Open the **Maven** tool window (right edge) → **EcoNest → Lifecycle** → double-click **package**.
2. The file `target/EcoNest-1.0-jar-with-dependencies.jar` is created. It contains your code **and** the MySQL driver.
3. Copy the JAR to a folder, and next to it create a `config` folder (the program will create `db.properties` on first run).
4. Run it by double-clicking, or in Command Prompt: `java -jar EcoNest-1.0-jar-with-dependencies.jar`. The computer needs Java 17+ and XAMPP MySQL running with `econest_db` imported.

**With IntelliJ artifacts (alternative)**
**File → Project Structure → Artifacts → + → JAR → From modules with dependencies…** → Main Class `econest.Main` → **OK** → **Build → Build Artifacts… → Build**. The JAR appears under `out/artifacts/`.

---

## F. Working as a team on GitHub (summary)
1. The leader pushes this project to the group repository (the `.gitignore` already excludes `config/*.properties`, `target/`, `.idea/`).
2. Each member: **Git → Clone…** in IntelliJ, then **Git → New Branch** (`feature/products`, `feature/clients`, …).
3. Commit (`Ctrl+K`) and push (`Ctrl+Shift+K`) your branch, open a pull request, the leader merges into `main`.
4. After every merge run `SystemTest` and the 5-minute check in E.2.
