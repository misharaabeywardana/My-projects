package econest.ui.panels;

import econest.model.User;
import econest.service.UserService;
import econest.ui.components.AppButton;
import econest.ui.components.UiUtil;
import econest.ui.dialogs.UserDialog;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/** Admin panel: manage who can log in. Visible to administrators only. */
public class UserPanel extends AbstractCrudPanel<User> {

    private static final long serialVersionUID = 1L;

    private final UserService service = new UserService();

    public UserPanel() {
        super("User Management", "Administrator only - staff accounts, roles and passwords",
                new String[] {"ID", "Username", "Full name", "Role", "Status"}, new Class<?>[] {Integer.class}, true);
        table.setColumnWidths(50, 160, 240, 140, 100);
        addButton.setText("Add User");
        AppButton reset = new AppButton("Reset Password");
        reset.addActionListener(e -> resetPassword());
        addButton(reset);
    }

    @Override
    protected List<User> loadItems() {
        return service.getAll();
    }

    @Override
    protected Object[] toRow(User u) {
        return new Object[] {u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
            u.isActive() ? "Active" : "Inactive"};
    }

    @Override
    protected String searchText(User u) {
        return u.getUsername() + " " + u.getFullName();
    }

    @Override
    protected boolean onAdd() {
        return new UserDialog(this, service, null).showDialog();
    }

    @Override
    protected boolean onEdit(User u) {
        return new UserDialog(this, service, u).showDialog();
    }

    @Override
    protected boolean onDelete(User u) {
        if (!UiUtil.confirm(this, "Delete user \"" + u.getUsername() + "\"?")) {
            return false;
        }
        try {
            service.deleteUser(u.getId());
            return true;
        } catch (RuntimeException e) {
            UiUtil.handle(this, e);
            return false;
        }
    }

    private void resetPassword() {
        User u = requireSelection();
        if (u == null) {
            return;
        }
        JPasswordField field = new JPasswordField(16);
        int ok = JOptionPane.showConfirmDialog(this, new Object[] {"New password for " + u.getUsername() + ":", field},
                "Reset password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                service.resetPassword(u.getId(), new String(field.getPassword()));
                UiUtil.info(this, "Password changed for " + u.getUsername() + ".");
            } catch (RuntimeException e) {
                UiUtil.handle(this, e);
            }
        }
    }
}
