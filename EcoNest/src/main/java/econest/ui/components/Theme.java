package econest.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;

/** Colours, fonts and look-and-feel shared by every screen (consistent styling). */
public final class Theme {

    public static final Color PRIMARY = new Color(0x2E7D32);
    public static final Color PRIMARY_DARK = new Color(0x1B5E20);
    public static final Color PRIMARY_LIGHT = new Color(0xE8F5E9);
    public static final Color ACCENT = new Color(0xF9A825);
    public static final Color DANGER = new Color(0xC62828);
    public static final Color INFO = new Color(0x1565C0);
    public static final Color WARNING_BG = new Color(0xFFF8E1);
    public static final Color ERROR_BG = new Color(0xFDECEA);
    public static final Color BACKGROUND = new Color(0xF3F6F3);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SIDEBAR = new Color(0x183D2A);
    public static final Color SIDEBAR_HOVER = new Color(0x245A3E);
    public static final Color TEXT = new Color(0x1F2D24);
    public static final Color MUTED = new Color(0x667A6D);
    public static final Color BORDER = new Color(0xDCE5DE);

    private static final String FAMILY = pickFamily();

    private Theme() {
    }

    private static String pickFamily() {
        List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : new String[] {"Segoe UI", "Inter", "Helvetica Neue", "Arial"}) {
            if (installed.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, int size) {
        return new Font(FAMILY, style, size);
    }

    public static Font regular() {
        return font(Font.PLAIN, 13);
    }

    public static Font bold() {
        return font(Font.BOLD, 13);
    }

    public static Font title() {
        return font(Font.BOLD, 22);
    }

    public static Font mono() {
        return new Font(Font.MONOSPACED, Font.PLAIN, 13);
    }

    public static Border padding(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    public static Border card() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), padding(14, 16));
    }

    /** Nimbus look-and-feel recoloured with the EcoNest green palette. */
    public static void apply() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Nimbus look and feel not available: " + e.getMessage());
        }
        UIManager.put("nimbusBase", PRIMARY_DARK);
        UIManager.put("nimbusBlueGrey", new Color(0xB9C9BE));
        UIManager.put("control", BACKGROUND);
        UIManager.put("nimbusSelectionBackground", PRIMARY);
        UIManager.put("nimbusFocus", new Color(0x66BB6A));
        UIManager.put("nimbusLightBackground", Color.WHITE);
        UIManager.put("text", TEXT);
        UIManager.put("Table.alternateRowColor", new Color(0xF7FAF7));
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("ToolTip.background", Color.WHITE);
        FontUIResource f = new FontUIResource(regular());
        UIManager.getLookAndFeelDefaults().put("defaultFont", f);
        UIManager.put("defaultFont", f);
    }
}
