package splitsmart.ui;

import splitsmart.logic.Money;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom bar chart component drawn using Java2D.
 * Displays category or weekly spending distributions with values, labels,
 * and adaptive theme styling.
 */
public class BarChartPanel extends JPanel {

    public static class BarItem {
        final String label;
        final long amountPaise;

        public BarItem(String label, long amountPaise) {
            this.label = label;
            this.amountPaise = amountPaise;
        }
    }

    private List<BarItem> items = new ArrayList<>();
    private String title = "";

    public BarChartPanel() {
        setOpaque(false);
    }

    public void setData(String title, List<BarItem> items) {
        this.title = title;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Chart Title
        if (title != null && !title.isEmpty()) {
            g2.setFont(Theme.fontSmallBold);
            g2.setColor(Theme.getText());
            g2.drawString(title, 14, 20);
        }

        if (items.isEmpty()) {
            g2.setFont(Theme.fontSmall);
            g2.setColor(Theme.getMuted());
            String empty = "No spending recorded yet";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(empty, (w - fm.stringWidth(empty)) / 2, h / 2);
            g2.dispose();
            return;
        }

        long maxAmount = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).amountPaise > maxAmount) {
                maxAmount = items.get(i).amountPaise;
            }
        }

        int paddingBottom = 40;
        int paddingTop = 36;
        int paddingLeft = 16;
        int paddingRight = 16;

        int plotW = w - paddingLeft - paddingRight;
        int plotH = h - paddingTop - paddingBottom;
        int baselineY = h - paddingBottom;

        // Baseline
        g2.setColor(Theme.getBorder());
        g2.drawLine(paddingLeft, baselineY, w - paddingRight, baselineY);

        if (maxAmount <= 0) {
            g2.setFont(Theme.fontSmall);
            g2.setColor(Theme.getMuted());
            String zero = "All expenses sum to ₹0.00";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(zero, (w - fm.stringWidth(zero)) / 2, h / 2);
            g2.dispose();
            return;
        }

        int n = items.size();
        int slotW = plotW / n;
        int barW = Math.max(16, Math.min(48, slotW - 16));

        Color barTop = Theme.getAccent();
        Color barBtm = Theme.isDark ? new Color(0xC7, 0x62, 0x12) : new Color(0xB3, 0x5A, 0x0F);

        for (int i = 0; i < n; i++) {
            BarItem item = items.get(i);
            int centerX = paddingLeft + (i * slotW) + (slotW / 2);
            int barX = centerX - (barW / 2);

            int barHeight = (int) (((double) item.amountPaise / maxAmount) * (plotH - 20));
            barHeight = Math.max(4, barHeight);
            int barY = baselineY - barHeight;

            // Bar Gradient
            GradientPaint gp = new GradientPaint(barX, barY, barTop, barX, baselineY, barBtm);
            g2.setPaint(gp);
            RoundRectangle2D.Float rect = new RoundRectangle2D.Float(barX, barY, barW, barHeight, 6, 6);
            g2.fill(rect);

            // Amount label on top
            g2.setFont(Theme.fontSmall);
            g2.setColor(Theme.getText());
            String amtStr = Money.format(item.amountPaise);
            FontMetrics fm = g2.getFontMetrics();
            int amtW = fm.stringWidth(amtStr);
            g2.drawString(amtStr, centerX - (amtW / 2), barY - 6);

            // Category/Week label below baseline
            g2.setColor(Theme.getMuted());
            String lbl = item.label;
            if (lbl.length() > 10) {
                lbl = lbl.substring(0, 9) + "…";
            }
            int lblW = fm.stringWidth(lbl);
            g2.drawString(lbl, centerX - (lblW / 2), baselineY + 18);
        }

        g2.dispose();
    }
}
