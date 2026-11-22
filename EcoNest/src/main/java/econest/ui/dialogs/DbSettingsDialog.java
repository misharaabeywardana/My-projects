package econest.ui.dialogs;

import econest.dao.DBConnection;
import econest.service.ValidationException;
import econest.ui.components.AppButton;
import econest.ui.components.Banner;
import java.awt.Component;
import java.io.IOException;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Edit config/db.properties from inside the app (handy for beginners). */
public class DbSettingsDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField url = new JTextField(DBConnection.getUrl(), 34);
    private final JTextField user = new JTextField(DBConnection.getUser(), 14);
    private final JPasswordField password = new JPasswordField(DBConnection.getPassword(), 14);

    public DbSettingsDialog(Component parent) {
        super(parent, "Database connection", "Save");
        form.add("JDBC URL", url, true).hint("XAMPP default: " + DBConnection.DEFAULT_URL);
        form.add("MySQL user", user, true).hint("XAMPP default: root");
        form.add("MySQL password", password).hint("XAMPP default: empty");
        AppButton test = new AppButton("Test connection");
        test.addActionListener(e -> test());
        form.full(test);
        form.hint("Saved in " + DBConnection.getConfigPath());
    }

    private void test() {
        String oldUrl = DBConnection.getUrl();
        String oldUser = DBConnection.getUser();
        String oldPw = DBConnection.getPassword();
        try {
            DBConnection.saveSettings(url.getText(), user.getText(), new String(password.getPassword()));
            errorBanner.show(Banner.Kind.SUCCESS, DBConnection.testConnection());
        } catch (IOException e) {
            errorBanner.show(Banner.Kind.ERROR, "Could not save settings: " + e.getMessage());
        } catch (RuntimeException e) {
            errorBanner.show(Banner.Kind.ERROR, e.getMessage());
            try {
                DBConnection.saveSettings(oldUrl, oldUser, oldPw);
            } catch (IOException ignored) {
                // keep the new values if the old ones cannot be written back
            }
        }
        pack();
    }

    @Override
    protected void save() {
        if (url.getText().isBlank() || !url.getText().trim().startsWith("jdbc:")) {
            throw new ValidationException("The URL must start with jdbc:, e.g. " + DBConnection.DEFAULT_URL);
        }
        try {
            DBConnection.saveSettings(url.getText(), user.getText(), new String(password.getPassword()));
        } catch (IOException e) {
            throw new ValidationException("Could not save settings: " + e.getMessage());
        }
    }
}
