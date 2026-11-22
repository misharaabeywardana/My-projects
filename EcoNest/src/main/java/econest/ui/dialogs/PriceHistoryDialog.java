package econest.ui.dialogs;

import econest.model.PriceHistory;
import econest.model.Product;
import econest.ui.components.AppButton;
import econest.ui.components.DataTable;
import econest.ui.components.Theme;
import econest.util.Formatters;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Shows every recorded price change of a product (additional feature - Senuji). */
public class PriceHistoryDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public PriceHistoryDialog(Component parent, Product product, List<PriceHistory> history) {
        super(SwingUtilities.getWindowAncestor(parent), "Price history - " + product.getName(),
                ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        JLabel title = new JLabel("<html><b>" + product.getName() + "</b> - current price "
                + Formatters.money(product.getPrice()) + "<br>" + history.size() + " price change(s) recorded</html>");
        title.setFont(Theme.regular());
        root.add(title, BorderLayout.NORTH);
        DataTable table = new DataTable(new String[] {"Changed on", "Old price", "New price", "Change", "Changed by"});
        List<Object[]> rows = new ArrayList<>();
        for (PriceHistory h : history) {
            double pct = h.getChangePercent();
            rows.add(new Object[] {Formatters.dateTime(h.getChangedAt()), Formatters.money(h.getOldPrice()),
                Formatters.money(h.getNewPrice()), String.format("%+.1f%%", pct), h.getChangedBy()});
        }
        table.setRows(rows);
        root.add(table.inScrollPane(), BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        AppButton close = new AppButton("Close", AppButton.Variant.PRIMARY);
        close.addActionListener(e -> dispose());
        south.add(close);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(new Dimension(620, 360));
        setLocationRelativeTo(getOwner());
    }
}
