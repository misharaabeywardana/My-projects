package econest.ui.components;

import econest.model.DailyRevenue;
import econest.util.Formatters;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * Daily revenue bar chart drawn with Java2D (additional feature - Rashmi).
 * Hover over a bar to see the exact amount.
 */
public class BarChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int LEFT = 86;
    private static final int RIGHT = 20;
    private static final int TOP = 36;
    private static final int BOTTOM = 40;

    private List<DailyRevenue> data = new ArrayList<>();
    private String title = "Daily revenue";

    public BarChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(700, 320));
        setToolTipText("");
    }

    public void setData(String title, List<DailyRevenue> data) {
        this.title = title;
        this.data = data == null ? new ArrayList<>() : data;
        repaint();
    }

    /** Rounds the axis maximum up to a "nice" number such as 5,000 or 20,000. */
    static double niceMax(double max) {
        if (max <= 0) {
            return 1000;
        }
        double exp = Math.pow(10, Math.floor(Math.log10(max)));
        for (double m : new double[] {1, 2, 2.5, 5, 10}) {
            if (m * exp >= max) {
                return m * exp;
            }
        }
        return 10 * exp;
    }

    private double maxValue() {
        double max = 0;
        for (DailyRevenue d : data) {
            max = Math.max(max, d.getRevenue().doubleValue());
        }
        return niceMax(max);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int plotW = w - LEFT - RIGHT;
        int plotH = h - TOP - BOTTOM;
        g2.setFont(Theme.font(Font.BOLD, 14));
        g2.setColor(Theme.TEXT);
        g2.drawString(title, LEFT, 22);
        if (data.isEmpty() || plotW <= 0 || plotH <= 0) {
            g2.dispose();
            return;
        }
        double max = maxValue();
        g2.setFont(Theme.font(Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        // horizontal grid lines with labels
        for (int i = 0; i <= 4; i++) {
            int y = TOP + plotH - (int) Math.round(plotH * i / 4.0);
            g2.setColor(new Color(0xEDF1EE));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(LEFT, y, LEFT + plotW, y);
            String label = Formatters.money(BigDecimal.valueOf(max * i / 4.0)).replace(".00", "");
            g2.setColor(Theme.MUTED);
            g2.drawString(label, LEFT - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
        }
        int n = data.size();
        double slot = plotW / (double) n;
        int barW = Math.max(2, (int) (slot * 0.68));
        boolean anySales = false;
        for (int i = 0; i < n; i++) {
            double v = data.get(i).getRevenue().doubleValue();
            int bh = (int) Math.round(plotH * (v / max));
            int x = LEFT + (int) Math.round(i * slot + (slot - barW) / 2);
            int y = TOP + plotH - bh;
            if (v > 0) {
                anySales = true;
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(x, y, barW, bh, 4, 4);
            }
            int day = data.get(i).getDay().getDayOfMonth();
            if (n <= 12 || day == 1 || day % 5 == 0) {
                String d = String.valueOf(day);
                g2.setColor(Theme.MUTED);
                g2.drawString(d, x + (barW - fm.stringWidth(d)) / 2, TOP + plotH + 16);
            }
        }
        g2.setColor(Theme.BORDER);
        g2.drawLine(LEFT, TOP + plotH, LEFT + plotW, TOP + plotH);
        g2.setColor(Theme.MUTED);
        g2.drawString("Day of month", LEFT + plotW / 2 - 30, h - 8);
        if (!anySales) {
            g2.setFont(Theme.font(Font.PLAIN, 14));
            String msg = "No sales recorded in this month";
            g2.drawString(msg, LEFT + (plotW - g2.getFontMetrics().stringWidth(msg)) / 2, TOP + plotH / 2);
        }
        g2.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int plotW = getWidth() - LEFT - RIGHT;
        if (data.isEmpty() || e.getX() < LEFT || e.getX() > LEFT + plotW) {
            return null;
        }
        int i = (int) ((e.getX() - LEFT) / (plotW / (double) data.size()));
        if (i < 0 || i >= data.size()) {
            return null;
        }
        DailyRevenue d = data.get(i);
        return Formatters.date(d.getDay()) + ": " + Formatters.money(d.getRevenue());
    }
}
