package econest.ui.dialogs;

import econest.model.Order;
import econest.ui.components.AppButton;
import econest.ui.components.Theme;
import econest.ui.components.UiUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** Order details shown as a printable invoice (additional feature - Fahath). */
public class OrderDetailsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextArea text = new JTextArea();

    public OrderDetailsDialog(Component parent, Order order, String invoice) {
        super(SwingUtilities.getWindowAncestor(parent), "Order " + order.getOrderRef(), ModalityType.APPLICATION_MODAL);
        text.setText(invoice);
        text.setEditable(false);
        text.setFont(Theme.mono());
        text.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        text.setCaretPosition(0);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        JScrollPane sp = new JScrollPane(text);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        root.add(sp, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        AppButton print = new AppButton("Print Invoice", AppButton.Variant.PRIMARY);
        print.addActionListener(e -> print());
        AppButton save = new AppButton("Save as Text File");
        save.addActionListener(e -> save(order));
        AppButton close = new AppButton("Close");
        close.addActionListener(e -> dispose());
        buttons.add(save);
        buttons.add(print);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(new Dimension(640, 620));
        setLocationRelativeTo(getOwner());
    }

    private void print() {
        try {
            text.print();
        } catch (PrinterException e) {
            UiUtil.error(this, "Printing failed: " + e.getMessage());
        }
    }

    private void save(Order order) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("invoice-" + order.getOrderRef() + ".txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), text.getText(), StandardCharsets.UTF_8);
                UiUtil.info(this, "Invoice saved to\n" + chooser.getSelectedFile());
            } catch (IOException e) {
                UiUtil.error(this, "Could not save: " + e.getMessage());
            }
        }
    }
}
