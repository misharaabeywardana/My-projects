package econest.service;

import econest.dao.AgentDao;
import econest.dao.ClientDao;
import econest.dao.DeliveryDao;
import econest.dao.EmailLogDao;
import econest.dao.InventoryDao;
import econest.dao.OrderDao;
import econest.dao.ProductDao;
import econest.dao.ReportDao;
import econest.dao.StockInDao;
import econest.model.DailyRevenue;
import econest.model.EmailStatus;
import econest.model.InventoryItem;
import econest.model.MonthlySalesSummary;
import econest.model.OrderStatus;
import econest.model.ProductSales;
import econest.util.Formatters;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Monthly sales and inventory reports (Task 7 - Rashmi) and dashboard figures. */
public class ReportService {

    private final ReportDao reportDao;
    private final InventoryDao inventoryDao;
    private final StockInDao stockInDao;

    public ReportService() {
        this(new ReportDao(), new InventoryDao(), new StockInDao());
    }

    public ReportService(ReportDao reportDao, InventoryDao inventoryDao, StockInDao stockInDao) {
        this.reportDao = reportDao;
        this.inventoryDao = inventoryDao;
        this.stockInDao = stockInDao;
    }

    /**
     * Builds the report for one calendar month. Orders are counted by their
     * order date; CANCELLED orders are excluded from revenue and units sold
     * but shown as a separate count.
     */
    public MonthlyReport buildMonthlyReport(YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        MonthlySalesSummary summary = reportDao.salesSummary(from, to);
        List<ProductSales> productSales = reportDao.productSales(from, to);
        Map<Integer, Integer> sold = new HashMap<>();
        for (ProductSales ps : productSales) {
            sold.put(ps.getProductId(), ps.getQuantity());
        }
        List<DailyRevenue> daily = fillMissingDays(month, reportDao.dailyRevenue(from, to));
        List<InventoryItem> inventory = inventoryDao.findAll();
        Map<Integer, Integer> received = stockInDao.unitsReceived(from.toLocalDate(), to.toLocalDate());
        BigDecimal cost = reportDao.stockPurchaseCost(from.toLocalDate(), to.toLocalDate());
        return new MonthlyReport(month, summary, productSales, daily, inventory, received, sold, cost);
    }

    /** The chart needs a bar for every day, even when nothing was sold. */
    static List<DailyRevenue> fillMissingDays(YearMonth month, List<DailyRevenue> fromDb) {
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (DailyRevenue d : fromDb) {
            byDay.put(d.getDay(), d.getRevenue());
        }
        List<DailyRevenue> all = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            all.add(new DailyRevenue(date, byDay.getOrDefault(date, BigDecimal.ZERO)));
        }
        return all;
    }

    /** Extra feature (Rashmi): the whole report as rows for a CSV/Excel file. */
    public List<String[]> toCsvRows(MonthlyReport r) {
        List<String[]> rows = new ArrayList<>();
        MonthlySalesSummary s = r.getSummary();
        rows.add(new String[] {"EcoNest Monthly Report", r.getMonth().toString()});
        rows.add(new String[] {"Generated", Formatters.dateTime(LocalDateTime.now())});
        rows.add(new String[] {});
        rows.add(new String[] {"SALES SUMMARY"});
        rows.add(new String[] {"Orders (excluding cancelled)", String.valueOf(s.getCompletedOrders())});
        rows.add(new String[] {"Cancelled orders", String.valueOf(s.getCancelledOrders())});
        rows.add(new String[] {"Units sold", String.valueOf(s.getItemsSold())});
        rows.add(new String[] {"Revenue (Rs.)", Formatters.plainMoney(s.getRevenue())});
        rows.add(new String[] {"Average order value (Rs.)", Formatters.plainMoney(s.getAverageOrderValue())});
        rows.add(new String[] {"New clients", String.valueOf(s.getNewClients())});
        rows.add(new String[] {"Stock purchases (Rs.)", Formatters.plainMoney(r.getStockPurchaseCost())});
        rows.add(new String[] {});
        rows.add(new String[] {"SALES BY PRODUCT"});
        rows.add(new String[] {"Product", "Category", "Units sold", "Revenue (Rs.)"});
        for (ProductSales p : r.getProductSales()) {
            rows.add(new String[] {p.getProductName(), p.getCategory().toString(), String.valueOf(p.getQuantity()),
                Formatters.plainMoney(p.getRevenue())});
        }
        rows.add(new String[] {});
        rows.add(new String[] {"DAILY REVENUE"});
        rows.add(new String[] {"Date", "Revenue (Rs.)"});
        for (DailyRevenue d : r.getDailyRevenue()) {
            rows.add(new String[] {Formatters.date(d.getDay()), Formatters.plainMoney(d.getRevenue())});
        }
        rows.add(new String[] {});
        rows.add(new String[] {"INVENTORY"});
        rows.add(new String[] {"Product", "Category", "Received this month", "Sold this month", "On hand",
            "Reorder level", "Status"});
        for (InventoryItem i : r.getInventory()) {
            rows.add(new String[] {i.getProductName(), i.getCategory().toString(),
                String.valueOf(r.getUnitsReceived(i.getProductId())), String.valueOf(r.getUnitsSold(i.getProductId())),
                String.valueOf(i.getQuantityOnHand()), String.valueOf(i.getReorderLevel()),
                i.isProductActive() ? i.getStockStatus().toString() : "Inactive product"});
        }
        return rows;
    }

    // ------------------------------------------------------------ dashboard

    /** Headline numbers for the dashboard. */
    public static class DashboardStats {
        public int activeProducts;
        public int clients;
        public int lowStock;
        public int pendingOrders;
        public int openDeliveries;
        public int availableAgents;
        public int failedEmails;
        public BigDecimal monthRevenue = BigDecimal.ZERO;
        public int monthOrders;
    }

    public DashboardStats getDashboardStats() {
        DashboardStats st = new DashboardStats();
        st.activeProducts = new ProductDao().countActive();
        st.clients = new ClientDao().count();
        st.lowStock = inventoryDao.countLowStock();
        OrderDao orderDao = new OrderDao();
        st.pendingOrders = orderDao.countByStatus(OrderStatus.PENDING) + orderDao.countByStatus(OrderStatus.PROCESSING);
        st.openDeliveries = new DeliveryDao().countOpen();
        st.availableAgents = new AgentDao().countActive();
        st.failedEmails = new EmailLogDao().countByStatus(EmailStatus.FAILED);
        YearMonth now = YearMonth.now();
        MonthlySalesSummary s = reportDao.salesSummary(now.atDay(1).atStartOfDay(),
                now.plusMonths(1).atDay(1).atStartOfDay());
        st.monthRevenue = s.getRevenue();
        st.monthOrders = s.getCompletedOrders();
        return st;
    }
}
