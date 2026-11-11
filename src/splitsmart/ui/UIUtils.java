package splitsmart.ui;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Styling helpers for standard Swing components (tables, text fields, headers, scroll panes).
 */
public class UIUtils {

    private UIUtils() {
        // Utility class
    }

    /**
     * Creates a styled text field that conforms to the active Theme palette.
     */
    public static JTextField createTextField() {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.getCard());
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 8, 8));
                g2.setColor(hasFocus() ? Theme.getAccent() : Theme.getBorder());
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setOpaque(false);
        tf.setFont(Theme.fontBody);
        tf.setForeground(Theme.getText());
        tf.setCaretColor(Theme.getAccent());
        tf.setBorder(new EmptyBorder(8, 12, 8, 12));
        tf.setPreferredSize(new Dimension(200, 38));
        return tf;
    }

    /**
     * Styles a JTable with theme colors, 40px row height, soft amber row selection,
     * and custom headers.
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(Theme.fontBody);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(Theme.getSelectionBackground());
        table.setSelectionForeground(Theme.getText());
        table.setBackground(Theme.getCard());
        table.setForeground(Theme.getText());

        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.fontSmallBold);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
        header.setBackground(Theme.getRaised());
        header.setForeground(Theme.getMuted());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.getBorder()));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (isSelected) {
                    c.setBackground(Theme.getSelectionBackground());
                    c.setForeground(Theme.getText());
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.getCard() : Theme.getRaised());
                    c.setForeground(Theme.getText());
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    /**
     * Styles a JScrollPane to match cards and background cleanly.
     */
    public static void styleScrollPane(JScrollPane sp) {
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createLineBorder(Theme.getBorder(), 1));
        sp.getVerticalScrollBar().setUnitIncrement(16);
    }

    /**
     * Creates an empty state panel with an informative muted message.
     */
    public static JPanel createEmptyState(String message) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(40, 20, 40, 20));
        JLabel label = new JLabel(message);
        label.setFont(Theme.fontBody);
        label.setForeground(Theme.getMuted());
        panel.add(label);
        return panel;
    }
}
