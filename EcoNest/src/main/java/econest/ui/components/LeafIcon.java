package econest.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/** The EcoNest leaf logo, drawn with Java2D so no image file is needed. */
public class LeafIcon implements Icon {

    private final int size;
    private final Color color;

    public LeafIcon(int size, Color color) {
        this.size = size;
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        double s = size;
        Path2D leaf = new Path2D.Double();
        leaf.moveTo(s * 0.15, s * 0.85);
        leaf.curveTo(s * 0.10, s * 0.35, s * 0.45, s * 0.10, s * 0.90, s * 0.10);
        leaf.curveTo(s * 0.90, s * 0.55, s * 0.65, s * 0.90, s * 0.15, s * 0.85);
        leaf.closePath();
        g2.setColor(color);
        g2.fill(leaf);
        g2.setColor(new Color(255, 255, 255, 170));
        g2.setStroke(new BasicStroke((float) Math.max(1.2, s / 14)));
        Path2D vein = new Path2D.Double();
        vein.moveTo(s * 0.18, s * 0.82);
        vein.curveTo(s * 0.40, s * 0.60, s * 0.60, s * 0.40, s * 0.82, s * 0.18);
        g2.draw(vein);
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
