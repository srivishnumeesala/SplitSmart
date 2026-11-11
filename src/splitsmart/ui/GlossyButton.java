package splitsmart.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A styled button with primary (amber gradient, glossy highlight, hover glow)
 * and secondary (card fill with border) modes, plus focus rings and pressed states.
 */
public class GlossyButton extends JButton {

    private boolean primary;
    private boolean hovered = false;
    private boolean pressed = false;
    private int cornerRadius = 8;

    public GlossyButton(String text) {
        this(text, true);
    }

    public GlossyButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFont(Theme.fontBodyBold);
        setPreferredSize(new Dimension(getPreferredSize().width + 24, 38));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int pad = 2;
        int btnW = w - (pad * 2);
        int btnH = h - (pad * 2);

        if (primary) {
            // Primary Amber Button
            Color topAmber = Theme.isDark ? new Color(0xF5, 0xA2, 0x4D) : new Color(0xE8, 0x7E, 0x26);
            Color btmAmber = Theme.isDark ? new Color(0xDC, 0x7F, 0x22) : new Color(0xC7, 0x62, 0x12);

            if (hovered) {
                topAmber = topAmber.brighter();
                btmAmber = btmAmber.brighter();
            }
            if (pressed) {
                topAmber = topAmber.darker();
                btmAmber = btmAmber.darker();
            }

            // Subtle amber glow behind button
            if (hovered || hasFocus()) {
                g2.setColor(new Color(217, 112, 26, 45));
                g2.fill(new RoundRectangle2D.Float(pad - 1, pad - 1, btnW + 2, btnH + 2, cornerRadius + 2, cornerRadius + 2));
            }

            // Gradient body
            GradientPaint gp = new GradientPaint(0, pad, topAmber, 0, pad + btnH, btmAmber);
            g2.setPaint(gp);
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(pad, pad, btnW, btnH, cornerRadius, cornerRadius);
            g2.fill(shape);

            // Glass top shine strip
            g2.setClip(shape);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.fill(new RoundRectangle2D.Float(pad, pad, btnW, btnH * 0.45f, cornerRadius, cornerRadius));
            g2.setClip(null);

            // Outline
            g2.setColor(btmAmber.darker());
            g2.draw(shape);

            // Text
            g2.setColor(Color.WHITE);
        } else {
            // Secondary Neutral Button
            Color fill = hovered ? Theme.getRaised() : Theme.getCard();
            if (pressed) {
                fill = Theme.getBorder();
            }

            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(pad, pad, btnW, btnH, cornerRadius, cornerRadius);
            g2.setColor(fill);
            g2.fill(shape);

            g2.setColor(Theme.getBorder());
            g2.draw(shape);

            g2.setColor(Theme.getText());
        }

        // Focus Ring
        if (hasFocus()) {
            g2.setColor(Theme.getAccent());
            g2.draw(new RoundRectangle2D.Float(pad - 1, pad - 1, btnW + 2, btnH + 2, cornerRadius + 2, cornerRadius + 2));
        }

        // Centered Button Label
        g2.setFont(getFont() != null ? getFont() : Theme.fontBodyBold);
        FontMetrics fm = g2.getFontMetrics();
        int strW = fm.stringWidth(getText());
        int strH = fm.getAscent() - fm.getDescent();
        int textX = (w - strW) / 2;
        int textY = (h + strH) / 2 + (pressed ? 1 : 0);

        g2.drawString(getText(), textX, textY);
        g2.dispose();
    }
}
