package econest.ui.components;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Builds tidy two-column forms (label | field) with GridBagLayout. */
public class FormBuilder extends JPanel {

    private static final long serialVersionUID = 1L;

    private int row;

    public FormBuilder() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    /** Adds a labelled field. A "*" is shown for required fields. */
    public FormBuilder add(String label, JComponent field, boolean required) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = field instanceof JScrollPane ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 6, 14);
        JLabel l = new JLabel(required ? "<html>" + label + " <font color='#C62828'>*</font></html>" : label);
        l.setFont(Theme.bold());
        l.setForeground(Theme.TEXT);
        add(l, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        add(field, c);
        row++;
        return this;
    }

    public FormBuilder add(String label, JComponent field) {
        return add(label, field, false);
    }

    /** Small grey help text under the previous field. */
    public FormBuilder hint(String text) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row++;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(-4, 0, 6, 0);
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.PLAIN, 11));
        l.setForeground(Theme.MUTED);
        add(l, c);
        return this;
    }

    /** A component spanning both columns. */
    public FormBuilder full(JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        add(comp, c);
        return this;
    }

    public static JScrollPane area(JTextArea area, int rows) {
        area.setRows(rows);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.regular());
        return new JScrollPane(area);
    }
}
