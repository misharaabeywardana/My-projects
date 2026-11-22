package econest.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Coloured message strip for alerts (low stock, simulation mode, form errors). */
public class Banner extends JPanel {

    private static final long serialVersionUID = 1L;

    public enum Kind { INFO, WARNING, ERROR, SUCCESS }

    private final JLabel label = new JLabel();

    public Banner() {
        super(new BorderLayout());
        label.setFont(Theme.regular());
        add(label, BorderLayout.CENTER);
        setVisible(false);
    }

    public void show(Kind kind, String message) {
        Color bg;
        Color fg;
        switch (kind) {
            case WARNING:
                bg = Theme.WARNING_BG;
                fg = new Color(0x7A4F00);
                break;
            case ERROR:
                bg = Theme.ERROR_BG;
                fg = new Color(0x8E1B1B);
                break;
            case SUCCESS:
                bg = Theme.PRIMARY_LIGHT;
                fg = Theme.PRIMARY_DARK;
                break;
            default:
                bg = new Color(0xE3F2FD);
                fg = new Color(0x0D47A1);
        }
        setBackground(bg);
        label.setForeground(fg);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bg.darker()),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        label.setText("<html>" + escape(message).replace("\n", "<br>") + "</html>");
        setVisible(true);
        revalidate();
    }

    public void hideBanner() {
        setVisible(false);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
