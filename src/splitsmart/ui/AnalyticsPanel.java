package splitsmart.ui;

import splitsmart.logic.Money;
import splitsmart.model.Category;
import splitsmart.model.Expense;
import splitsmart.model.User;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Visual analytics dashboard presenting spending distributions across categories
 * and weekly spending timelines rendered with Java2D bar charts.
 */
public class AnalyticsPanel extends JPanel {

    private final DataStore dataStore;
    private BarChartPanel categoryChart;
    private BarChartPanel weeklyChart;
    private JLabel totalSpendVal;
    private JLabel topCatVal;
    private JLabel groupsCountVal;

    public AnalyticsPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setLayout(new BorderLayout());
        setOpaque(false);
        initUI();
    }

    private void initUI() {
        removeAll();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Header Row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel titleLbl = new JLabel("Spending Analytics");
        titleLbl.setFont(Theme.fontTitle);
        titleLbl.setForeground(Theme.getText());
        headerRow.add(titleLbl, BorderLayout.WEST);
        root.add(headerRow);
        root.add(Box.createVerticalStrut(16));

        // KPI Summary Cards
        GlossyPanel kpiCard = new GlossyPanel();
        kpiCard.setLayout(new GridLayout(1, 3, 20, 0));
        kpiCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        kpiCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        kpiCard.setPreferredSize(new Dimension(800, 90));

        totalSpendVal = new JLabel("₹0.00");
        totalSpendVal.setFont(Theme.fontTitle);
        totalSpendVal.setForeground(Theme.getAccentText());

        topCatVal = new JLabel("None");
        topCatVal.setFont(Theme.fontTitle);
        topCatVal.setForeground(Theme.getText());

        groupsCountVal = new JLabel("0");
        groupsCountVal.setFont(Theme.fontTitle);
        groupsCountVal.setForeground(Theme.getText());

        kpiCard.add(createKpi("Total Group Spending", totalSpendVal));
        kpiCard.add(createKpi("Top Category", topCatVal));
        kpiCard.add(createKpi("Active Groups", groupsCountVal));

        root.add(kpiCard);
        root.add(Box.createVerticalStrut(18));

        // 1. Spending by Category Card
        GlossyPanel catCard = new GlossyPanel();
        catCard.setLayout(new BorderLayout());
        catCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        catCard.setPreferredSize(new Dimension(800, 240));
        catCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        categoryChart = new BarChartPanel();
        catCard.add(categoryChart, BorderLayout.CENTER);
        root.add(catCard);
        root.add(Box.createVerticalStrut(18));

        // 2. Spending by Week Card
        GlossyPanel weekCard = new GlossyPanel();
        weekCard.setLayout(new BorderLayout());
        weekCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        weekCard.setPreferredSize(new Dimension(800, 240));
        weekCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        weeklyChart = new BarChartPanel();
        weekCard.add(weeklyChart, BorderLayout.CENTER);
        root.add(weekCard);

        JScrollPane scroll = new JScrollPane(root);
        UIUtils.styleScrollPane(scroll);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        refreshUI();
    }

    public void refreshUI() {
        User user = dataStore.getCurrentUser();
        List<Expense> allExp = user != null ? dataStore.getAllExpensesForUser(user.getId()) : new ArrayList<>();

        long totalSpend = 0;
        Map<String, Long> catMap = new HashMap<>();
        TreeMap<LocalDate, Long> weekMap = new TreeMap<>();

        DateTimeFormatter weekFmt = DateTimeFormatter.ofPattern("d MMM");

        for (int i = 0; i < allExp.size(); i++) {
            Expense e = allExp.get(i);
            totalSpend += e.getAmountPaise();

            // Category breakdown
            String cat = e.getCategory().getDisplayName();
            long cSum = catMap.containsKey(cat) ? catMap.get(cat) : 0L;
            catMap.put(cat, cSum + e.getAmountPaise());

            // Week breakdown (Monday of expense week)
            LocalDate monday = e.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long wSum = weekMap.containsKey(monday) ? weekMap.get(monday) : 0L;
            weekMap.put(monday, wSum + e.getAmountPaise());
        }

        // Update KPIs
        totalSpendVal.setText(Money.format(totalSpend));
        int groupCount = user != null ? dataStore.getGroupsForUser(user.getId()).size() : 0;
        groupsCountVal.setText(String.valueOf(groupCount));

        String topCat = "None";
        long maxCat = 0;
        List<BarChartPanel.BarItem> catItems = new ArrayList<>();
        for (Map.Entry<String, Long> entry : catMap.entrySet()) {
            if (entry.getValue() > maxCat) {
                maxCat = entry.getValue();
                topCat = entry.getKey();
            }
            catItems.add(new BarChartPanel.BarItem(entry.getKey(), entry.getValue()));
        }
        topCatVal.setText(topCat);

        // Sort categories by descending amount
        for (int i = 0; i < catItems.size() - 1; i++) {
            for (int j = i + 1; j < catItems.size(); j++) {
                if (catItems.get(i).amountPaise < catItems.get(j).amountPaise) {
                    BarChartPanel.BarItem temp = catItems.get(i);
                    catItems.set(i, catItems.get(j));
                    catItems.set(j, temp);
                }
            }
        }
        categoryChart.setData("Spending by Category", catItems);

        // Populate weekly chart
        List<BarChartPanel.BarItem> weekItems = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : weekMap.entrySet()) {
            String label = entry.getKey().format(weekFmt);
            weekItems.add(new BarChartPanel.BarItem(label, entry.getValue()));
        }
        weeklyChart.setData("Spending by Week", weekItems);

        revalidate();
        repaint();
    }

    private JPanel createKpi(String title, JLabel valLabel) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(Theme.fontSmallBold);
        t.setForeground(Theme.getMuted());
        t.setAlignmentX(LEFT_ALIGNMENT);

        valLabel.setAlignmentX(LEFT_ALIGNMENT);

        p.add(t);
        p.add(Box.createVerticalStrut(4));
        p.add(valLabel);
        return p;
    }
}
