package econest.ui.components;

import econest.dao.DataAccessException;
import econest.model.EmailLog;
import econest.service.ValidationException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Message boxes and background tasks used by all screens. */
public final class UiUtil {

    private UiUtil() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "EcoNest", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Shows the right kind of message for any exception. */
    public static void handle(Component parent, Throwable t) {
        Throwable e = t;
        while ((e instanceof ExecutionException || e instanceof RuntimeException && !(e instanceof ValidationException)
                && !(e instanceof DataAccessException)) && e.getCause() != null) {
            e = e.getCause();
        }
        if (e instanceof ValidationException) {
            warn(parent, e.getMessage());
        } else if (e instanceof DataAccessException) {
            error(parent, e.getMessage());
        } else {
            e.printStackTrace();
            error(parent, "Unexpected error: " + e);
        }
    }

    /** One sentence describing what happened to an automatic e-mail. */
    public static String emailOutcome(EmailLog log) {
        if (log == null) {
            return "";
        }
        switch (log.getStatus()) {
            case SENT:
                return "E-mail sent to " + log.getRecipientEmail() + ".";
            case SIMULATED:
                return "E-mail to " + log.getRecipientEmail() + " recorded in the Notification Centre "
                        + "(simulation mode - nothing was sent).";
            default:
                return "WARNING: the e-mail to " + log.getRecipientEmail() + " could not be sent:\n"
                        + log.getErrorMessage() + "\nYou can resend it from the Notifications screen.";
        }
    }

    /** Runs slow work (database + e-mail) off the screen thread, then calls onSuccess on it. */
    public static <T> void runAsync(Component parent, Callable<T> work, Consumer<T> onSuccess) {
        Window w = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (w != null) {
            w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                if (w != null) {
                    w.setCursor(Cursor.getDefaultCursor());
                }
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    handle(parent, e.getCause());
                } catch (RuntimeException e) {
                    handle(parent, e);
                }
            }
        }.execute();
    }
}
