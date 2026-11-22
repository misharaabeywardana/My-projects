package econest.ui.dialogs;

import econest.service.UserService;
import econest.service.ValidationException;
import java.awt.Component;
import java.util.Arrays;
import javax.swing.JPasswordField;

/** Lets the logged-in user change their own password. */
public class ChangePasswordDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final JPasswordField current = new JPasswordField(18);
    private final JPasswordField next = new JPasswordField(18);
    private final JPasswordField repeat = new JPasswordField(18);

    public ChangePasswordDialog(Component parent) {
        super(parent, "Change my password", "Change password");
        form.add("Current password", current, true);
        form.add("New password", next, true).hint("At least " + UserService.MIN_PASSWORD_LENGTH + " characters");
        form.add("Repeat new password", repeat, true);
    }

    @Override
    protected void save() {
        if (!Arrays.equals(next.getPassword(), repeat.getPassword())) {
            throw new ValidationException("The two new passwords are not the same.");
        }
        new UserService().changeOwnPassword(new String(current.getPassword()), new String(next.getPassword()));
    }
}
