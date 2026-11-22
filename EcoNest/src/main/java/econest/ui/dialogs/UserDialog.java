package econest.ui.dialogs;

import econest.model.Role;
import econest.model.User;
import econest.service.UserService;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Admin panel: add / edit a user account. */
public class UserDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final UserService service;
    private final User user;
    private final JTextField username = new JTextField(20);
    private final JTextField fullName = new JTextField(24);
    private final JComboBox<Role> role = new JComboBox<>(Role.values());
    private final JCheckBox active = new JCheckBox("Account active (can log in)", true);
    private final JPasswordField password = new JPasswordField(20);

    public UserDialog(Component parent, UserService service, User existing) {
        super(parent, existing == null ? "Add user" : "Edit user", "Save");
        this.service = service;
        this.user = existing == null ? new User() : existing;
        active.setOpaque(false);
        form.add("Username", username, true);
        form.add("Full name", fullName, true);
        form.add("Role", role, true).hint("Administrators can manage users and settings");
        form.add("Status", active);
        if (existing == null) {
            role.setSelectedItem(Role.STAFF);
            form.add("Password", password, true).hint("At least " + UserService.MIN_PASSWORD_LENGTH + " characters");
        } else {
            username.setText(existing.getUsername());
            fullName.setText(existing.getFullName());
            role.setSelectedItem(existing.getRole());
            active.setSelected(existing.isActive());
        }
    }

    @Override
    protected void save() {
        user.setUsername(username.getText());
        user.setFullName(fullName.getText());
        user.setRole((Role) role.getSelectedItem());
        user.setActive(active.isSelected());
        if (user.isNew()) {
            service.createUser(user, new String(password.getPassword()));
        } else {
            service.updateUser(user);
        }
    }
}
