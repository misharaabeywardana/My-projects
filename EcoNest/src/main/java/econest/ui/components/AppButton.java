package econest.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;

/** Rounded, coloured button used on every screen. */
public class AppButton extends JButton {

    private static final long serialVersionUID = 1L;

    public enum Variant { PRIMARY, SECONDARY, DANGER, ACCENT }

    private final Variant variant;
    private boolean hover;

    public AppButton(String text) {
        this(text, Variant.SECONDARY);
    }

    public AppButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setUI(new BasicButtonUI());
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(Theme.bold());
        setForeground(variant == Variant.SECONDARY ? Theme.PRIMARY_DARK : Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    private Color baseColor() {
        switch (variant) {
            case PRIMARY:
                return Theme.PRIMARY;
            case DANGER:
                return Theme.DANGER;
            case ACCENT:
                return new Color(0xE08E0B);
            default:
                return Color.WHITE;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 84), Math.max(d.height, 34));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = baseColor();
        if (!isEnabled()) {
            bg = new Color(0xE3E8E4);
        } else if (getModel().isPressed()) {
            bg = variant == Variant.SECONDARY ? Theme.PRIMARY_LIGHT.darker() : bg.darker();
        } else if (hover) {
            bg = variant == Variant.SECONDARY ? Theme.PRIMARY_LIGHT : bg.brighter();
        }
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        if (variant == Variant.SECONDARY) {
            g2.setColor(isEnabled() ? new Color(0xA5C8AD) : Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setForeground(!enabled ? new Color(0x9AA8A0)
                : variant == Variant.SECONDARY ? Theme.PRIMARY_DARK : Color.WHITE);
    }
}
