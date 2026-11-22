package econest.ui.dialogs;

import econest.model.Client;
import econest.model.Order;
import econest.model.OrderItem;
import econest.model.Product;
import econest.service.ClientService;
import econest.service.OrderService;
import econest.service.ProductService;
import econest.service.ValidationException;
import econest.ui.components.AppButton;
import econest.ui.components.DataTable;
import econest.ui.components.Theme;
import econest.util.Formatters;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** Create a client order: choose products and quantities, totals are calculated live (Task 5). */
public class NewOrderDialog extends AbstractFormDialog {

    private static final long serialVersionUID = 1L;

    private final OrderService orderService;
    private final Order draft = new Order();
    private final JComboBox<Client> client = new JComboBox<>();
    private final JComboBox<Product> product = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, OrderService.MAX_QTY_PER_LINE, 1));
    private final DataTable itemsTable = new DataTable(new String[] {"Product", "Unit price", "Qty", "Line total"},
            new Class<?>[] {null, null, Integer.class});
    private final JLabel totalLabel = new JLabel();
    private final JLabel stockHint = new JLabel(" ");
    private final JTextField notes = new JTextField(30);
    private Order created;

    public NewOrderDialog(Component parent, OrderService orderService, ClientService clientService,
                          ProductService productService) {
        super(parent, "New client order", "Place order");
        this.orderService = orderService;
        for (Client c : clientService.getAll()) {
            client.addItem(c);
        }
        for (Product p : productService.getActive()) {
            product.addItem(p);
        }
        product.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel,
                                                          boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof Product) {
                    Product p = (Product) value;
                    setText(p.getName() + "  -  " + Formatters.money(p.getPrice()) + "  -  "
                            + p.getQuantityOnHand() + " in stock");
                }
                return this;
            }
        });
        product.addActionListener(e -> updateStockHint());

        form.add("Client", client, true);
        if (client.getItemCount() == 0) {
            form.hint("No clients yet - add one on the Clients screen first");
        }

        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.add(product, BorderLayout.CENTER);
        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        qtyPanel.setOpaque(false);
        qtyPanel.add(new JLabel("Qty"));
        quantity.setPreferredSize(new Dimension(70, 30));
        qtyPanel.add(quantity);
        AppButton add = new AppButton("Add to order", AppButton.Variant.ACCENT);
        add.addActionListener(e -> addLine());
        qtyPanel.add(add);
        addRow.add(qtyPanel, BorderLayout.EAST);
        form.add("Product", addRow, true);
        stockHint.setForeground(Theme.MUTED);
        form.full(stockHint);

        JPanel itemsPanel = new JPanel(new BorderLayout(0, 6));
        itemsPanel.setOpaque(false);
        itemsTable.setColumnWidths(300, 120, 60, 130);
        javax.swing.JScrollPane sp = itemsTable.inScrollPane();
        sp.setPreferredSize(new Dimension(640, 190));
        itemsPanel.add(sp, BorderLayout.CENTER);
        JPanel under = new JPanel(new BorderLayout());
        under.setOpaque(false);
        AppButton remove = new AppButton("Remove selected line");
        remove.addActionListener(e -> removeLine());
        under.add(remove, BorderLayout.WEST);
        totalLabel.setFont(Theme.font(Font.BOLD, 20));
        totalLabel.setForeground(Theme.PRIMARY_DARK);
        under.add(totalLabel, BorderLayout.EAST);
        itemsPanel.add(under, BorderLayout.SOUTH);
        form.full(itemsPanel);
        form.add("Notes", notes).hint("Optional, e.g. delivery instructions");
        refreshItems();
        updateStockHint();
    }

    private void updateStockHint() {
        Product p = (Product) product.getSelectedItem();
        if (p == null) {
            stockHint.setText("No active products. Add products on the Product Catalogue screen.");
            return;
        }
        int inOrder = 0;
        for (OrderItem i : draft.getItems()) {
            if (i.getProductId() == p.getId()) {
                inOrder += i.getQuantity();
            }
        }
        stockHint.setText("Available: " + (p.getQuantityOnHand() - inOrder) + " units of " + p.getName()
                + "  |  Eco-rating " + p.getEcoStars());
    }

    private void addLine() {
        Product p = (Product) product.getSelectedItem();
        if (p == null) {
            return;
        }
        int qty = (Integer) quantity.getValue();
        int already = 0;
        for (OrderItem i : draft.getItems()) {
            if (i.getProductId() == p.getId()) {
                already += i.getQuantity();
            }
        }
        if (already + qty > p.getQuantityOnHand()) {
            showError("Only " + (p.getQuantityOnHand() - already) + " units of " + p.getName() + " are available.");
            return;
        }
        errorBanner.hideBanner();
        draft.addItem(new OrderItem(p, qty));
        quantity.setValue(1);
        refreshItems();
        updateStockHint();
    }

    private void removeLine() {
        int row = itemsTable.getSelectedModelRow();
        if (row >= 0) {
            draft.removeItem(row);
            refreshItems();
            updateStockHint();
        }
    }

    private void refreshItems() {
        List<Object[]> rows = new ArrayList<>();
        for (OrderItem i : draft.getItems()) {
            rows.add(new Object[] {i.getProductName(), Formatters.money(i.getUnitPrice()), i.getQuantity(),
                Formatters.money(i.getLineTotal())});
        }
        itemsTable.setRows(rows);
        totalLabel.setText("Total: " + Formatters.money(draft.calculateTotal()) + "  (" + draft.getTotalQuantity()
                + " items)");
    }

    @Override
    protected void save() {
        Client c = (Client) client.getSelectedItem();
        if (c == null) {
            throw new ValidationException("Please choose a client.");
        }
        draft.setClientId(c.getId());
        draft.setClientName(c.getName());
        draft.setNotes(notes.getText());
        created = orderService.createOrder(draft);
    }

    /** The saved order (after showDialog() returned true). */
    public Order getCreatedOrder() {
        return created;
    }
}
