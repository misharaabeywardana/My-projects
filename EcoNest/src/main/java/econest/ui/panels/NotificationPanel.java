package econest.ui.panels;

import econest.model.EmailLog;
import econest.model.EmailStatus;
import econest.model.EmailTemplate;
import econest.service.NotificationService;
import econest.service.email.EmailConfig;
import econest.ui.Refreshable;
import econest.ui.components.AppButton;
import econest.ui.components.Banner;
import econest.ui.components.DataTable;
import econest.ui.components.PageHeader;
import econest.ui.components.Theme;
import econest.ui.components.UiUtil;
import econest.util.Formatters;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Tasks 8 and 9 - e-mail notifications.
 * Extra (Sandeepa): Notification Centre with resend.
 * Extra (Rakshana): editable e-mail templates with live preview.
 */
public class NotificationPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final NotificationService service = new NotificationService();
    private final Banner modeBanner = new Banner();

    // notification centre
    private final DataTable logTable = new DataTable(new String[] {"Time", "Recipient", "Type", "Order", "Subject",
        "Status", "Error"});
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final JTextArea logBody = new JTextArea();
    private List<EmailLog> logs = new ArrayList<>();

    // template editor
    private final JComboBox<String> templateChoice = new JComboBox<>(new String[] {
        "Order dispatched - sent to the client (Task 8)", "Delivery assigned - sent to the agent (Task 9)"});
    private final JTextField subject = new JTextField();
    private final JTextArea body = new JTextArea();
    private final JTextArea preview = new JTextArea();
    private final JList<String> placeholders = new JList<>();

    public NotificationPanel() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        PageHeader header = new PageHeader("E-mail Notifications",
                "Dispatch e-mails to clients and assignment e-mails to delivery agents");
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(modeBanner, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.bold());
        tabs.addTab("Notification Centre", buildLogTab());
        tabs.addTab("E-mail Templates", buildTemplateTab());
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        center.add(tabs);
        add(center, BorderLayout.CENTER);
    }

    // ------------------------------------------------ notification centre

    private JPanel buildLogTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        statusFilter.addItem("All e-mails");
        for (EmailStatus s : EmailStatus.values()) {
            statusFilter.addItem(s);
        }
        statusFilter.addActionListener(e -> loadLogs());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel("Show:"));
        left.add(statusFilter);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        AppButton resend = new AppButton("Resend Selected", AppButton.Variant.PRIMARY);
        resend.addActionListener(e -> resendSelected());
        AppButton resendAll = new AppButton("Resend All Failed");
        resendAll.addActionListener(e -> resendAll());
        AppButton reload = new AppButton("Refresh");
        reload.addActionListener(e -> refresh());
        right.add(resend);
        right.add(resendAll);
        right.add(reload);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        p.add(bar, BorderLayout.NORTH);

        logTable.setColumnWidths(120, 210, 90, 150, 280, 130, 200);
        logTable.getSelectionModel().addListSelectionListener(e -> {
            int row = logTable.getSelectedModelRow();
            logBody.setText(row < 0 ? "" : "To: " + logs.get(row).getRecipientEmail() + "\nSubject: "
                    + logs.get(row).getSubject() + "\n\n" + logs.get(row).getBody());
            logBody.setCaretPosition(0);
        });
        logBody.setEditable(false);
        logBody.setFont(Theme.mono());
        logBody.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logTable.inScrollPane(), new JScrollPane(logBody));
        split.setResizeWeight(0.6);
        split.setBorder(null);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private void loadLogs() {
        Object f = statusFilter.getSelectedItem();
        logs = service.getLogs(f instanceof EmailStatus ? (EmailStatus) f : null);
        List<Object[]> rows = new ArrayList<>();
        for (EmailLog l : logs) {
            rows.add(new Object[] {Formatters.dateTime(l.getCreatedAt()), l.getRecipientEmail(), l.getRecipientType(),
                l.getOrderRef() == null ? "-" : l.getOrderRef(), l.getSubject(), l.getStatus(),
                l.getErrorMessage() == null ? "" : l.getErrorMessage()});
        }
        logTable.setRows(rows);
        logBody.setText("Select an e-mail above to read it.");
    }

    private void resendSelected() {
        int row = logTable.getSelectedModelRow();
        if (row < 0) {
            UiUtil.warn(this, "Please select an e-mail first.");
            return;
        }
        int id = logs.get(row).getId();
        UiUtil.runAsync(this, () -> service.resend(id), log -> {
            UiUtil.info(this, UiUtil.emailOutcome(log));
            loadLogs();
        });
    }

    private void resendAll() {
        int failed = service.countFailed();
        if (failed == 0) {
            UiUtil.info(this, "There are no failed e-mails.");
            return;
        }
        if (!UiUtil.confirm(this, "Try to send all " + failed + " failed e-mail(s) again?")) {
            return;
        }
        UiUtil.runAsync(this, service::resendAllFailed, ok -> {
            UiUtil.info(this, ok + " of " + failed + " e-mail(s) were delivered (or recorded in simulation mode).");
            loadLogs();
        });
    }

    // ------------------------------------------------ template editor (extra feature - Rakshana)

    private String selectedKey() {
        return templateChoice.getSelectedIndex() == 0 ? EmailTemplate.ORDER_DISPATCHED : EmailTemplate.DELIVERY_ASSIGNED;
    }

    private JPanel buildTemplateTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Theme.BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        JPanel top = new JPanel(new BorderLayout(8, 6));
        top.setOpaque(false);
        top.add(templateChoice, BorderLayout.NORTH);
        JPanel subj = new JPanel(new BorderLayout(8, 0));
        subj.setOpaque(false);
        JLabel sl = new JLabel("Subject:");
        sl.setFont(Theme.bold());
        subj.add(sl, BorderLayout.WEST);
        subj.add(subject, BorderLayout.CENTER);
        top.add(subj, BorderLayout.CENTER);
        p.add(top, BorderLayout.NORTH);

        body.setFont(Theme.mono());
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        preview.setFont(Theme.mono());
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setBackground(new java.awt.Color(0xFAFCFA));
        JPanel editors = new JPanel(new GridLayout(1, 2, 10, 0));
        editors.setOpaque(false);
        editors.add(titled("Template (edit me)", new JScrollPane(body)));
        editors.add(titled("Preview with example data", new JScrollPane(preview)));

        placeholders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        placeholders.setFont(Theme.mono());
        placeholders.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && placeholders.getSelectedValue() != null) {
                    body.insert(placeholders.getSelectedValue(), body.getCaretPosition());
                    updatePreview();
                }
            }
        });
        JPanel side = titled("Placeholders", new JScrollPane(placeholders));
        JLabel tip = new JLabel("Double-click to insert");
        tip.setForeground(Theme.MUTED);
        side.add(tip, BorderLayout.SOUTH);
        side.setPreferredSize(new java.awt.Dimension(200, 100));

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.add(editors, BorderLayout.CENTER);
        center.add(side, BorderLayout.EAST);
        p.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        AppButton reset = new AppButton("Reset to Default");
        reset.addActionListener(e -> {
            if (UiUtil.confirm(this, "Replace this template with the original EcoNest wording?")) {
                try {
                    showTemplate(service.resetTemplate(selectedKey()));
                } catch (RuntimeException ex) {
                    UiUtil.handle(this, ex);
                }
            }
        });
        AppButton save = new AppButton("Save Template", AppButton.Variant.PRIMARY);
        save.addActionListener(e -> {
            try {
                service.saveTemplate(new EmailTemplate(selectedKey(), subject.getText(), body.getText()));
                UiUtil.info(this, "Template saved. New e-mails will use it.");
            } catch (RuntimeException ex) {
                UiUtil.handle(this, ex);
            }
        });
        buttons.add(reset);
        buttons.add(save);
        p.add(buttons, BorderLayout.SOUTH);

        templateChoice.addActionListener(e -> loadTemplate());
        javax.swing.event.DocumentListener live = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }
        };
        subject.getDocument().addDocumentListener(live);
        body.getDocument().addDocumentListener(live);
        return p;
    }

    private static JPanel titled(String title, java.awt.Component c) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(title);
        l.setFont(Theme.bold());
        p.add(l, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void loadTemplate() {
        try {
            showTemplate(service.getTemplate(selectedKey()));
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }

    private void showTemplate(EmailTemplate t) {
        String[] names = EmailTemplate.ORDER_DISPATCHED.equals(t.getKey()) ? NotificationService.CLIENT_PLACEHOLDERS
                : NotificationService.AGENT_PLACEHOLDERS;
        String[] items = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            items[i] = "{" + names[i] + "}";
        }
        placeholders.setListData(items);
        subject.setText(t.getSubject());
        body.setText(t.getBody());
        body.setCaretPosition(0);
        updatePreview();
    }

    private void updatePreview() {
        String[] p = NotificationService.preview(new EmailTemplate(selectedKey(), subject.getText(), body.getText()));
        preview.setText("Subject: " + p[0] + "\n\n" + p[1]);
        preview.setCaretPosition(0);
    }

    @Override
    public void refresh() {
        EmailConfig cfg = EmailConfig.load();
        if (cfg.getMode() == EmailConfig.Mode.SIMULATE) {
            modeBanner.show(Banner.Kind.INFO, "Simulation mode: e-mails are recorded here but not sent. An "
                    + "administrator can switch to real sending in Settings > E-mail (SMTP).");
        } else {
            modeBanner.show(Banner.Kind.SUCCESS, "Real e-mail sending is ON (" + cfg.createSender().describe() + ").");
        }
        try {
            loadLogs();
            loadTemplate();
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
        }
    }
}
