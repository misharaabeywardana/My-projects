package econest.ui.dialogs;

import econest.dao.DataAccessException;
import econest.service.ValidationException;
import econest.ui.components.AppButton;
import econest.ui.components.Banner;
import econest.ui.components.FormBuilder;
import econest.ui.components.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Base class for add/edit dialogs: a form, an error banner and Save / Cancel
 * buttons. Subclasses build the form and implement {@link #save()}.
 */
public abstract class AbstractFormDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    protected final FormBuilder form = new FormBuilder();
    protected final Banner errorBanner = new Banner();
    protected final AppButton saveButton;
    private boolean saved;

    protected AbstractFormDialog(Component parent, String title, String saveText) {
        super(parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent), title,
                ModalityType.APPLICATION_MODAL);
        saveButton = new AppButton(saveText, AppButton.Variant.PRIMARY);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        root.add(errorBanner, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        AppButton cancel = new AppButton("Cancel");
        cancel.addActionListener(e -> dispose());
        buttons.add(cancel);
        buttons.add(saveButton);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        saveButton.addActionListener(e -> trySave());
        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Reads the form and saves it. Throw ValidationException to show a message. */
    protected abstract void save();

    private void trySave() {
        try {
            save();
            saved = true;
            dispose();
        } catch (ValidationException | DataAccessException e) {
            showError(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            showError("Unexpected error: " + e);
        }
    }

    protected void showError(String message) {
        errorBanner.show(Banner.Kind.ERROR, message);
        pack();
    }

    /** Shows the dialog and waits; returns true if the user saved. */
    public boolean showDialog() {
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return saved;
    }
}
