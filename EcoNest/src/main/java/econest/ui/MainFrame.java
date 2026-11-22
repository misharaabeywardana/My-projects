package econest.ui;

import econest.dao.DBConnection;
import econest.service.email.EmailConfig;
import econest.ui.components.LeafIcon;
import econest.ui.components.Theme;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.ChangePasswordDialog;
import econest.ui.panels.AgentPanel;
import econest.ui.panels.ClientPanel;
import econest.ui.panels.DashboardPanel;
import econest.ui.panels.DeliveryPanel;
import econest.ui.panels.InventoryPanel;
import econest.ui.panels.NotificationPanel;
import econest.ui.panels.OrderPanel;
import econest.ui.panels.ProductPanel;
import econest.ui.panels.ReportPanel;
import econest.ui.panels.SettingsPanel;
import econest.ui.panels.UserPanel;
import econest.util.Session;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/** Main window: header, sidebar navigation and the screen area (CardLayout). */
public class MainFrame extends JFrame implements Navigator {

    private static final long serialVersionUID = 1L;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Component> pages = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Runnable onLogout;
    private String current;

    public MainFrame(Runnable onLogout) {
        super("EcoNest - Home & Garden Management");
        this.onLogout = onLogout;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImage(iconImage());
        content.setBackground(Theme.BACKGROUND);

        addPage(DASHBOARD, new DashboardPanel(this));
        addPage(PRODUCTS, new ProductPanel());
        addPage(CLIENTS, new ClientPanel());
        addPage(INVENTORY, new InventoryPanel());
        addPage(AGENTS, new AgentPanel());
        addPage(ORDERS, new OrderPanel());
        addPage(DELIVERIES, new DeliveryPanel());
        addPage(REPORTS, new ReportPanel());
        addPage(NOTIFICATIONS, new NotificationPanel());
        if (Session.isAdmin()) {
            addPage(USERS, new UserPanel());
            addPage(SETTINGS, new SettingsPanel());
        }

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1360, 820);
        setLocationRelativeTo(null);
        navigate(DASHBOARD);
    }

    private void addPage(String name, Component page) {
        pages.put(name, page);
        content.add(page, name);
    }

    private static java.awt.Image iconImage() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(32, 32,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        new LeafIcon(32, Theme.PRIMARY).paintIcon(null, g, 0, 0);
        g.dispose();
        return img;
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(Theme.PRIMARY_DARK);
        h.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        JLabel brand = new JLabel("EcoNest", new LeafIcon(26, new Color(0xA5D6A7)), SwingConstants.LEFT);
        brand.setIconTextGap(10);
        brand.setFont(Theme.font(Font.BOLD, 20));
        brand.setForeground(Color.WHITE);
        h.add(brand, BorderLayout.WEST);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        JLabel who = new JLabel(Session.getCurrentUser().getFullName() + "  (" + Session.getCurrentUser().getRole()
                + ")");
        who.setForeground(new Color(0xC8E6C9));
        right.add(who);
        right.add(Box.createHorizontalStrut(16));
        right.add(headerLink("Change password", () -> new ChangePasswordDialog(this).showDialog()));
        right.add(Box.createHorizontalStrut(12));
        right.add(headerLink("Log out", () -> {
            if (UiUtil.confirm(this, "Log out of EcoNest?")) {
                Session.clear();
                dispose();
                onLogout.run();
            }
        }));
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JLabel headerLink(String text, Runnable action) {
        JLabel l = new JLabel("<html><u>" + text + "</u></html>");
        l.setForeground(Color.WHITE);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return l;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.SIDEBAR);
        side.setPreferredSize(new Dimension(212, 100));
        side.setBorder(BorderFactory.createEmptyBorder(14, 10, 10, 10));
        side.add(sectionLabel("MAIN"));
        for (String name : new String[] {DASHBOARD, ORDERS, DELIVERIES}) {
            side.add(navButton(name));
        }
        side.add(sectionLabel("CATALOGUE & PEOPLE"));
        for (String name : new String[] {PRODUCTS, INVENTORY, CLIENTS, AGENTS}) {
            side.add(navButton(name));
        }
        side.add(sectionLabel("INSIGHTS"));
        side.add(navButton(REPORTS));
        side.add(navButton(NOTIFICATIONS));
        if (Session.isAdmin()) {
            side.add(sectionLabel("ADMIN PANEL"));
            side.add(navButton(USERS));
            side.add(navButton(SETTINGS));
        }
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 10));
        l.setForeground(new Color(0x7FA88D));
        l.setBorder(BorderFactory.createEmptyBorder(14, 10, 6, 0));
        l.setAlignmentX(0f);
        return l;
    }

    private JButton navButton(String name) {
        JButton b = new JButton(name);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(Theme.font(Font.PLAIN, 14));
        b.setForeground(new Color(0xDCEFE2));
        b.setBackground(Theme.SIDEBAR);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 10));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(0f);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.addActionListener(e -> navigate(name));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!name.equals(current)) {
                    b.setBackground(Theme.SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!name.equals(current)) {
                    b.setBackground(Theme.SIDEBAR);
                }
            }
        });
        navButtons.put(name, b);
        return b;
    }

    private JPanel buildStatusBar() {
        JPanel s = new JPanel(new BorderLayout());
        s.setBackground(new Color(0xE6ECE7));
        s.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        JLabel db = new JLabel("Database: " + DBConnection.getUrl());
        db.setFont(Theme.font(Font.PLAIN, 11));
        db.setForeground(Theme.MUTED);
        JLabel mail = new JLabel("E-mail: " + (EmailConfig.load().getMode() == EmailConfig.Mode.SMTP
                ? "SMTP (real sending)" : "simulation mode"));
        mail.setFont(Theme.font(Font.PLAIN, 11));
        mail.setForeground(Theme.MUTED);
        s.add(db, BorderLayout.WEST);
        s.add(mail, BorderLayout.EAST);
        return s;
    }

    /**
     * Shows a screen and reloads its data. "Orders:new" and "Inventory:stockin"
     * open the page and then the matching dialog (dashboard shortcuts).
     */
    @Override
    public void navigate(String target) {
        String[] parts = target.split(":", 2);
        String page = parts[0];
        if (!pages.containsKey(page)) {
            return;
        }
        current = page;
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean active = e.getKey().equals(page);
            e.getValue().setBackground(active ? Theme.PRIMARY : Theme.SIDEBAR);
            e.getValue().setForeground(active ? Color.WHITE : new Color(0xDCEFE2));
            e.getValue().setFont(Theme.font(active ? Font.BOLD : Font.PLAIN, 14));
        }
        cards.show(content, page);
        Component c = pages.get(page);
        if (c instanceof Refreshable) {
            ((Refreshable) c).refresh();
        }
        if (parts.length == 2) {
            if ("new".equals(parts[1]) && c instanceof OrderPanel) {
                ((OrderPanel) c).openNewOrder();
            } else if ("stockin".equals(parts[1]) && c instanceof InventoryPanel) {
                ((InventoryPanel) c).openStockIn();
            }
        }
    }

    /** For automated UI tests. */
    public Component getPage(String name) {
        return pages.get(name);
    }

    public String getCurrentPage() {
        return current;
    }
}
