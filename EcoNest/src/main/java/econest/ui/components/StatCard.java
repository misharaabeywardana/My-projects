package econest.ui.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A KPI tile: small caption, big value, coloured accent bar. */
public class StatCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel value = new JLabel("-");
    private final JLabel caption;
    private final JLabel note = new JLabel(" ");
    private final Color accent;

    public StatCard(String title, Color accent) {
        super(new BorderLayout(0, 4));
        this.accent = accent;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 16));
        caption = new JLabel(title.toUpperCase());
        caption.setFont(Theme.font(Font.BOLD, 11));
        caption.setForeground(Theme.MUTED);
        value.setFont(Theme.font(Font.BOLD, 24));
        value.setForeground(Theme.TEXT);
        note.setFont(Theme.font(Font.PLAIN, 12));
        note.setForeground(Theme.MUTED);
        add(caption, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
        add(note, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(200, 104));
    }

    public void setValue(String text, String noteText) {
        value.setText(text);
        note.setText(noteText == null || noteText.isEmpty() ? " " : noteText);
    }

    public void setValueColor(Color c) {
        value.setForeground(c);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.setColor(accent);
        g2.fillRoundRect(0, 0, 6, getHeight() - 1, 6, 6);
        g2.dispose();
        super.paintComponent(g);
    }
}
