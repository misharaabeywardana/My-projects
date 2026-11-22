package econest.ui.components;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Title, subtitle and optional buttons at the top of each screen. */
public class PageHeader extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();

    public PageHeader(String title, String subtitle) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        titleLabel.setFont(Theme.title());
        titleLabel.setForeground(Theme.TEXT);
        subtitleLabel.setFont(Theme.font(java.awt.Font.PLAIN, 13));
        subtitleLabel.setForeground(Theme.MUTED);
        setTitle(title, subtitle);
        text.add(titleLabel);
        text.add(subtitleLabel);
        add(text, BorderLayout.WEST);
        actions.setOpaque(false);
        add(actions, BorderLayout.EAST);
    }

    public final void setTitle(String title, String subtitle) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle == null || subtitle.isEmpty() ? " " : subtitle);
    }

    public void addAction(JComponent c) {
        actions.add(c);
    }
}
