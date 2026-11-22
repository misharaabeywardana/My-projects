package econest.ui.dialogs;

import econest.dao.DataAccessException;
import econest.model.User;
import econest.service.UserService;
import econest.service.ValidationException;
import econest.ui.components.AppButton;
import econest.ui.components.Banner;
import econest.ui.components.FormBuilder;
import econest.ui.components.LeafIcon;
import econest.ui.components.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** First screen: username and password. */
public class LoginDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField username = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);
    private final Banner banner = new Banner();
    private final AppButton loginButton = new AppButton("Log in", AppButton.Variant.PRIMARY);
    private User user;

    public LoginDialog() {
        super((java.awt.Frame) null, "EcoNest - Log in", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());

        JPanel brand = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, 0, getHeight(), Theme.SIDEBAR));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setPreferredSize(new Dimension(250, 360));
        brand.setBorder(BorderFactory.createEmptyBorder(70, 24, 24, 24));
        JLabel logo = new JLabel(new LeafIcon(64, new Color(0xA5D6A7)));
        logo.setAlignmentX(0f);
        JLabel name = new JLabel("EcoNest");
        name.setFont(Theme.font(Font.BOLD, 32));
        name.setForeground(Color.WHITE);
        name.setAlignmentX(0f);
        JLabel tag = new JLabel("<html>Sustainable plants, planters<br>and gardening kits</html>");
        tag.setForeground(new Color(0xC8E6C9));
        tag.setAlignmentX(0f);
        brand.add(logo);
        brand.add(javax.swing.Box.createVerticalStrut(12));
        brand.add(name);
        brand.add(javax.swing.Box.createVerticalStrut(6));
        brand.add(tag);
        root.add(brand, BorderLayout.WEST);

        JPanel formSide = new JPanel(new BorderLayout(0, 12));
        formSide.setBackground(Color.WHITE);
        formSide.setBorder(BorderFactory.createEmptyBorder(48, 34, 30, 34));
        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(Theme.font(Font.BOLD, 22));
        JLabel sub = new JLabel("Log in to manage the shop");
        sub.setForeground(Theme.MUTED);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(welcome, BorderLayout.NORTH);
        head.add(sub, BorderLayout.CENTER);
        head.add(banner, BorderLayout.SOUTH);
        formSide.add(head, BorderLayout.NORTH);
        FormBuilder form = new FormBuilder();
        form.add("Username", username);
        form.add("Password", password);
        loginButton.addActionListener(e -> login());
        form.full(loginButton);
        JLabel hint = new JLabel("First time? Use admin / admin123, then change the password.",
                SwingConstants.LEFT);
        hint.setFont(Theme.font(Font.PLAIN, 11));
        hint.setForeground(Theme.MUTED);
        form.full(hint);
        formSide.add(form, BorderLayout.CENTER);
        AppButton dbButton = new AppButton("Database settings...");
        dbButton.addActionListener(e -> new DbSettingsDialog(this).showDialog());
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(dbButton, BorderLayout.EAST);
        formSide.add(south, BorderLayout.SOUTH);
        root.add(formSide, BorderLayout.CENTER);

        setContentPane(root);
        getRootPane().setDefaultButton(loginButton);
        setSize(720, 420);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void login() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            user = new UserService().login(username.getText(), new String(password.getPassword()));
            dispose();
        } catch (ValidationException e) {
            banner.show(Banner.Kind.ERROR, e.getMessage());
            password.setText("");
        } catch (DataAccessException e) {
            banner.show(Banner.Kind.ERROR, e.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /** Shows the dialog; returns the logged-in user or null if the window was closed. */
    public User showLogin() {
        setVisible(true);
        return user;
    }

    // used by automated UI tests
    public void fill(String user, String pass) {
        username.setText(user);
        password.setText(pass);
    }

    public void clickLogin() {
        loginButton.doClick();
    }
}
