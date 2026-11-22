package econest.ui;

/** Lets one screen open another (e.g. Dashboard quick actions). */
public interface Navigator {

    String DASHBOARD = "Dashboard";
    String PRODUCTS = "Products";
    String CLIENTS = "Clients";
    String INVENTORY = "Inventory";
    String AGENTS = "Delivery Agents";
    String ORDERS = "Orders";
    String DELIVERIES = "Deliveries";
    String REPORTS = "Reports";
    String NOTIFICATIONS = "Notifications";
    String USERS = "Users";
    String SETTINGS = "Settings";

    void navigate(String page);
}
