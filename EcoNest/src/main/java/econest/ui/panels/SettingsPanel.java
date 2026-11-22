package econest.ui.panels;

import econest.dao.DBConnection;
import econest.model.EmailLog;
import econest.service.NotificationService;
import econest.service.ValidationException;
import econest.service.email.EmailConfig;
import econest.ui.Refreshable;
import econest.ui.components.AppButton;
import econest.ui.components.Banner;
import econest.ui.components.FormBuilder;
import econest.ui.components.PageHeader;
import econest.ui.components.Theme;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.DbSettingsDialog;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** Admin panel: e-mail (SMTP) and database settings. Visible to administrators only. */
public class SettingsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final JRadioButton simulate = new JRadioButton("Simulation - record e-mails only (safe for demos)");
    private final JRadioButton smtp = new JRadioButton("Send real e-mails through an SMTP server");
    private final JTextField host = new JTextField(22);
    private final JTextField port = new JTextField(6);
    private final JComboBox<EmailConfig.Security> security = new JComboBox<>(EmailConfig.Security.values());
    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JTextField fromAddress = new JTextField(22);
    private final JTextField fromName = new JTextField(22);
    private final JTextField testTo = new JTextField(22);
    private final Banner emailBanner = new Banner();
    private final JLabel dbInfo = new JLabel();
    private final Banner dbBanner = new Banner();

    public SettingsPanel() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(new PageHeader("Settings", "Administrator only - e-mail sending and database connection"),
                BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 14, 0);
        content.add(buildEmailCard(), c);
        content.add(buildDbCard(), c);
        c.weighty = 1;
        content.add(new JPanel() {
            private static final long serialVersionUID = 1L;

            {
                setOpaque(false);
            }
        }, c);
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        add(sp, BorderLayout.CENTER);
    }

    private JPanel card(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.SURFACE);
        card.setBorder(Theme.card());
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(java.awt.Font.BOLD, 16));
        card.add(t, BorderLayout.NORTH);
        return card;
    }

    private JPanel buildEmailCard() {
        JPanel card = card("E-mail notifications (Tasks 8 and 9)");
        ButtonGroup g = new ButtonGroup();
        g.add(simulate);
        g.add(smtp);
        simulate.setOpaque(false);
        smtp.setOpaque(false);
        simulate.addActionListener(e -> enableSmtp());
        smtp.addActionListener(e -> enableSmtp());
        FormBuilder f = new FormBuilder();
        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modes.setOpaque(false);
        modes.add(simulate);
        modes.add(smtp);
        f.add("Mode", modes);
        f.add("SMTP host", host).hint("Gmail: smtp.gmail.com");
        f.add("Port", port).hint("587 with STARTTLS, or 465 with SSL");
        f.add("Security", security);
        f.add("Username", username).hint("Your full e-mail address");
        f.add("Password", password).hint("Gmail: create an App Password (Google Account > Security > App passwords)");
        f.add("From address", fromAddress).hint("Leave empty to use the username");
        f.add("From name", fromName);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        AppButton save = new AppButton("Save E-mail Settings", AppButton.Variant.PRIMARY);
        save.addActionListener(e -> saveEmail());
        buttons.add(save);
        buttons.add(new JLabel("   Send test e-mail to:"));
        buttons.add(testTo);
        AppButton test = new AppButton("Send Test");
        test.addActionListener(e -> sendTest());
        buttons.add(test);
        f.full(buttons);
        f.full(emailBanner);
        JLabel where = new JLabel("Stored in " + EmailConfig.getFilePath() + " (outside the source code - "
                + "do not upload this file to GitHub).");
        where.setForeground(Theme.MUTED);
        f.full(where);
        card.add(f, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildDbCard() {
        JPanel card = card("Database (MySQL via XAMPP)");
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(dbInfo, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        AppButton test = new AppButton("Test Connection", AppButton.Variant.PRIMARY);
        test.addActionListener(e -> UiUtil.runAsync(this, DBConnection::testConnection,
                msg -> dbBanner.show(Banner.Kind.SUCCESS, msg)));
        AppButton edit = new AppButton("Edit Connection...");
        edit.addActionListener(e -> {
            if (new DbSettingsDialog(this).showDialog()) {
                refresh();
            }
        });
        buttons.add(test);
        buttons.add(edit);
        body.add(buttons, BorderLayout.CENTER);
        body.add(dbBanner, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void enableSmtp() {
        boolean on = smtp.isSelected();
        for (java.awt.Component comp : new java.awt.Component[] {host, port, security, username, password,
            fromAddress, fromName}) {
            comp.setEnabled(on);
        }
    }

    private void saveEmail() {
        try {
            EmailConfig cfg = new EmailConfig();
            cfg.setMode(smtp.isSelected() ? EmailConfig.Mode.SMTP : EmailConfig.Mode.SIMULATE);
            cfg.setHost(host.getText().trim());
            int p;
            try {
                p = Integer.parseInt(port.getText().trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("Port must be a number such as 587.");
            }
            if (p < 1 || p > 65535) {
                throw new ValidationException("Port must be between 1 and 65535.");
            }
            cfg.setPort(p);
            cfg.setSecurity((EmailConfig.Security) security.getSelectedItem());
            cfg.setUsername(username.getText().trim());
            cfg.setPassword(new String(password.getPassword()));
            cfg.setFromAddress(fromAddress.getText().trim());
            cfg.setFromName(fromName.getText().trim());
            if (cfg.getMode() == EmailConfig.Mode.SMTP && (cfg.getHost().isEmpty() || cfg.getEffectiveFrom().isEmpty())) {
                throw new ValidationException("For SMTP mode enter at least the host and the username.");
            }
            cfg.save();
            emailBanner.show(Banner.Kind.SUCCESS, "E-mail settings saved. Mode: " + cfg.createSender().describe());
        } catch (ValidationException e) {
            emailBanner.show(Banner.Kind.ERROR, e.getMessage());
        } catch (IOException e) {
            emailBanner.show(Banner.Kind.ERROR, "Could not save: " + e.getMessage());
        }
    }

    private void sendTest() {
        String to = testTo.getText();
        UiUtil.runAsync(this, () -> new NotificationService().sendTestEmail(to), (EmailLog log) -> {
            emailBanner.show(log.getStatus() == econest.model.EmailStatus.FAILED ? Banner.Kind.ERROR : Banner.Kind.SUCCESS,
                    UiUtil.emailOutcome(log));
        });
    }

    @Override
    public void refresh() {
        EmailConfig cfg = EmailConfig.load();
        simulate.setSelected(cfg.getMode() == EmailConfig.Mode.SIMULATE);
        smtp.setSelected(cfg.getMode() == EmailConfig.Mode.SMTP);
        host.setText(cfg.getHost());
        port.setText(String.valueOf(cfg.getPort()));
        security.setSelectedItem(cfg.getSecurity());
        username.setText(cfg.getUsername());
        password.setText(cfg.getPassword());
        fromAddress.setText(cfg.getFromAddress());
        fromName.setText(cfg.getFromName());
        enableSmtp();
        dbInfo.setText("<html>URL: <b>" + DBConnection.getUrl() + "</b><br>User: <b>" + DBConnection.getUser()
                + "</b><br>Settings file: " + DBConnection.getConfigPath() + "</html>");
    }
}
