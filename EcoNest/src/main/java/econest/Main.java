package econest;

import econest.model.User;
import econest.ui.MainFrame;
import econest.ui.components.Theme;
import econest.ui.dialogs.LoginDialog;
import javax.swing.SwingUtilities;

/**
 * Starting point of the EcoNest desktop application.
 * Run this class in IntelliJ IDEA (green triangle next to main).
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.apply();
            showLogin();
        });
    }

    /** Shows the login window; after a successful login opens the main window. */
    public static void showLogin() {
        User user = new LoginDialog().showLogin();
        if (user == null) {
            System.exit(0);
        }
        new MainFrame(Main::showLogin).setVisible(true);
    }
}
