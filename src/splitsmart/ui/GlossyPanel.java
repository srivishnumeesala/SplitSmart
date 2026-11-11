package splitsmart.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A glossy card panel featuring rounded corners (radius 14), layered drop shadows,
 * subtle vertical gradients, a top glass highlight band, and theme-adaptive borders.
 */
public class GlossyPanel extends JPanel {

    private int cornerRadius = 14;
    private int shadowOffset = 3;

    public GlossyPanel() {
        super();
        setOpaque(false);
    }

    public GlossyPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public GlossyPanel(int cornerRadius) {
        super();
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int pad = shadowOffset;

        int cardX = pad;
        int cardY = 1;
        int cardW = width - (pad * 2);
        int cardH = height - (pad * 2);

        if (cardW <= 0 || cardH <= 0) {
            g2.dispose();
            return;
        }

        // Draw 3-layered soft drop shadow
        int shadowAlpha = Theme.isDark ? 18 : 12;
        for (int i = shadowOffset; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, shadowAlpha));
            g2.fill(new RoundRectangle2D.Float(
                    cardX - i + 1,
                    cardY + i,
                    cardW + (i * 2) - 2,
                    cardH,
                    cornerRadius + i,
                    cornerRadius + i
            ));
        }

        // Card vertical subtle gradient
        Color cardBase = Theme.getCard();
        Color topGrad = Theme.isDark ? new Color(0x23, 0x20, 0x1C) : new Color(0xFF, 0xFF, 0xFF);
        Color btmGrad = Theme.isDark ? new Color(0x1B, 0x18, 0x15) : new Color(0xFB, 0xF9, 0xF5);

        GradientPaint fillGradient = new GradientPaint(0, cardY, topGrad, 0, cardY + cardH, btmGrad);
        g2.setPaint(fillGradient);
        RoundRectangle2D.Float cardShape = new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, cornerRadius, cornerRadius);
        g2.fill(cardShape);

        // Thin top-half glass shine highlight band
        int shineAlpha = Theme.isDark ? 12 : 40;
        g2.setColor(new Color(255, 255, 255, shineAlpha));
        g2.setClip(cardShape);
        g2.fill(new RoundRectangle2D.Float(cardX, cardY, cardW, cardH / 2.2f, cornerRadius, cornerRadius));
        g2.setClip(null);

        // 1 px subtle border
        g2.setColor(Theme.getBorder());
        g2.draw(cardShape);

        g2.dispose();
        super.paintComponent(g);
    }
}
