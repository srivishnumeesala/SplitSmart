package splitsmart.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;

/**
 * Global theme configuration for Warm Amber Light and Dark visual modes.
 * Provides dynamic color getters that evaluate active mode at paint time,
 * as well as custom font loaders with fallback support for the Rupee (₹) symbol.
 */
public class Theme {

    public static boolean isDark = false;

    // Light Theme Palette
    private static final Color LIGHT_BACKGROUND = new Color(0xF7, 0xF4, 0xEF);
    private static final Color LIGHT_CARD = new Color(0xFF, 0xFF, 0xFF);
    private static final Color LIGHT_RAISED = new Color(0xF1, 0xEC, 0xE4);
    private static final Color LIGHT_BORDER = new Color(0xE9, 0xE3, 0xDA);
    private static final Color LIGHT_TEXT = new Color(0x21, 0x1C, 0x17);
    private static final Color LIGHT_MUTED = new Color(0x7A, 0x71, 0x67);
    private static final Color LIGHT_ACCENT = new Color(0xD9, 0x70, 0x1A);
    private static final Color LIGHT_ACCENT_TEXT = new Color(0xB3, 0x5A, 0x0F);
    private static final Color LIGHT_OWE_ROSE = new Color(0xC7, 0x5B, 0x4E);

    // Dark Theme Palette
    private static final Color DARK_BACKGROUND = new Color(0x14, 0x12, 0x10);
    private static final Color DARK_CARD = new Color(0x1C, 0x19, 0x16);
    private static final Color DARK_RAISED = new Color(0x24, 0x21, 0x1D);
    private static final Color DARK_BORDER = new Color(0x2D, 0x29, 0x24);
    private static final Color DARK_TEXT = new Color(0xF3, 0xEF, 0xE9);
    private static final Color DARK_MUTED = new Color(0xA5, 0x9E, 0x94);
    private static final Color DARK_ACCENT = new Color(0xF0, 0x96, 0x3F);
    private static final Color DARK_ACCENT_TEXT = new Color(0xF0, 0x96, 0x3F);
    private static final Color DARK_OWE_ROSE = new Color(0xE2, 0x7D, 0x70);

    // Fonts
    public static Font fontTitle;
    public static Font fontSubtitle;
    public static Font fontBody;
    public static Font fontBodyBold;
    public static Font fontSmall;
    public static Font fontSmallBold;
    public static Font fontMoney;

    static {
        initializeFonts();
    }

    private Theme() {
        // Utility class
    }

    /**
     * Initializes custom Plus Jakarta Sans fonts if present in the fonts/ folder,
     * otherwise falling back to Segoe UI or SansSerif, ensuring the Rupee (₹) symbol renders.
     */
    public static void initializeFonts() {
        Font regularBase = null;
        Font semiBoldBase = null;

        File regFile = new File("fonts/PlusJakartaSans-Regular.ttf");
        File semiFile = new File("fonts/PlusJakartaSans-SemiBold.ttf");

        try {
            if (regFile.exists()) {
                regularBase = Font.createFont(Font.TRUETYPE_FONT, regFile);
            }
            if (semiFile.exists()) {
                semiBoldBase = Font.createFont(Font.TRUETYPE_FONT, semiFile);
            }
        } catch (Exception ignored) {
        }

        String fallbackFamily = "Segoe UI";
        Font testFallback = new Font(fallbackFamily, Font.PLAIN, 14);
        if (!testFallback.canDisplay('₹')) {
            fallbackFamily = "SansSerif";
        }

        if (regularBase != null && regularBase.canDisplay('₹')) {
            fontBody = regularBase.deriveFont(Font.PLAIN, 14f);
            fontSmall = regularBase.deriveFont(Font.PLAIN, 12f);
        } else {
            fontBody = new Font(fallbackFamily, Font.PLAIN, 14);
            fontSmall = new Font(fallbackFamily, Font.PLAIN, 12);
        }

        if (semiBoldBase != null && semiBoldBase.canDisplay('₹')) {
            fontTitle = semiBoldBase.deriveFont(Font.BOLD, 22f);
            fontSubtitle = semiBoldBase.deriveFont(Font.BOLD, 16f);
            fontBodyBold = semiBoldBase.deriveFont(Font.BOLD, 14f);
            fontSmallBold = semiBoldBase.deriveFont(Font.BOLD, 12f);
            fontMoney = semiBoldBase.deriveFont(Font.BOLD, 14f);
        } else {
            fontTitle = new Font(fallbackFamily, Font.BOLD, 22);
            fontSubtitle = new Font(fallbackFamily, Font.BOLD, 16);
            fontBodyBold = new Font(fallbackFamily, Font.BOLD, 14);
            fontSmallBold = new Font(fallbackFamily, Font.BOLD, 12);
            fontMoney = new Font(fallbackFamily, Font.BOLD, 14);
        }
    }

    // Dynamic Color Getters
    public static Color getBackground() {
        return isDark ? DARK_BACKGROUND : LIGHT_BACKGROUND;
    }

    public static Color getCard() {
        return isDark ? DARK_CARD : LIGHT_CARD;
    }

    public static Color getRaised() {
        return isDark ? DARK_RAISED : LIGHT_RAISED;
    }

    public static Color getBorder() {
        return isDark ? DARK_BORDER : LIGHT_BORDER;
    }

    public static Color getText() {
        return isDark ? DARK_TEXT : LIGHT_TEXT;
    }

    public static Color getMuted() {
        return isDark ? DARK_MUTED : LIGHT_MUTED;
    }

    public static Color getAccent() {
        return isDark ? DARK_ACCENT : LIGHT_ACCENT;
    }

    public static Color getAccentText() {
        return isDark ? DARK_ACCENT_TEXT : LIGHT_ACCENT_TEXT;
    }

    public static Color getOweRose() {
        return isDark ? DARK_OWE_ROSE : LIGHT_OWE_ROSE;
    }

    public static Color getSettledGray() {
        return isDark ? DARK_MUTED : LIGHT_MUTED;
    }

    public static Color getSelectionBackground() {
        return isDark ? new Color(240, 150, 63, 50) : new Color(217, 112, 26, 35);
    }
}
