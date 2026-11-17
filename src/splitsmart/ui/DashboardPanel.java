package splitsmart.ui;

import splitsmart.logic.BalanceCalculator;
import splitsmart.logic.Money;
import splitsmart.model.Expense;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.model.User;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard screen presenting personalized greetings, balance summaries,
 * user group status, a spending chart, and recent shared expenses.
 */
public class DashboardPanel extends JPanel {

    public interface NavigationListener {
        void openGroupDetail(Group group);
        void openGroups();
    }

    private final DataStore dataStore;
    private final NavigationListener navListener;

    public DashboardPanel(DataStore dataStore, NavigationListener navListener) {
        this.dataStore = dataStore;
        this.navListener = navListener;
        setLayout(new BorderLayout());
        setOpaque(false);
        refreshUI();
    }

    private BalanceCalculator.MemberBalance findUserMemberBalance(List<BalanceCalculator.MemberBalance> bList, User user) {
        if (bList == null || bList.isEmpty()) {
            return null;
        }
        if (user != null) {
            String dn = user.getDisplayName() != null ? user.getDisplayName().trim() : "";
            String un = user.getUsername() != null ? user.getUsername().trim() : "";
            for (BalanceCalculator.MemberBalance b : bList) {
                String mName = b.getMember().getName().trim();
                if ((!dn.isEmpty() && mName.equalsIgnoreCase(dn)) ||
                    (!un.isEmpty() && mName.equalsIgnoreCase(un))) {
                    return b;
                }
            }
        }
        for (BalanceCalculator.MemberBalance b : bList) {
            if (b.getNetPaise() > 0) {
                return b;
            }
        }
        return bList.get(0);
    }

    public void refreshUI() {
        removeAll();

        User user = dataStore.getCurrentUser();
        String displayName = user != null ? user.getDisplayName() : "User";

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 28, 28, 28));

        // 1. Header: Greeting & Date
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel greetingLbl = new JLabel("Welcome back, " + displayName);
        greetingLbl.setFont(Theme.fontTitle);
        greetingLbl.setForeground(Theme.getText());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy");
        JLabel dateLbl = new JLabel(LocalDate.now().format(dtf));
        dateLbl.setFont(Theme.fontBody);
        dateLbl.setForeground(Theme.getMuted());

        headerRow.add(greetingLbl, BorderLayout.WEST);
        headerRow.add(dateLbl, BorderLayout.EAST);
        content.add(headerRow);
        content.add(Box.createVerticalStrut(18));

        // 2. Wide Balance Summary Card
        List<Group> userGroups = user != null ? dataStore.getGroupsForUser(user.getId()) : new ArrayList<>();
        long totalReceivable = 0;
        long totalPayable = 0;

        for (int i = 0; i < userGroups.size(); i++) {
            Group g = userGroups.get(i);
            List<BalanceCalculator.MemberBalance> bList = BalanceCalculator.calculateGroupBalances(g);
            BalanceCalculator.MemberBalance userBal = findUserMemberBalance(bList, user);
            long userNet = userBal != null ? userBal.getNetPaise() : 0L;
            if (userNet > 0) {
                totalReceivable += userNet;
            } else if (userNet < 0) {
                totalPayable += (-userNet);
            }
        }
        long netOverall = totalReceivable - totalPayable;

        GlossyPanel summaryCard = new GlossyPanel();
        summaryCard.setLayout(new GridLayout(1, 3, 20, 0));
        summaryCard.setBorder(new EmptyBorder(18, 24, 18, 24));
        summaryCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        summaryCard.setPreferredSize(new Dimension(800, 100));

        summaryCard.add(createSummaryColumn("Total You Are Owed", Money.format(totalReceivable), Theme.getAccentText()));
        summaryCard.add(createSummaryColumn("Total You Owe", Money.format(totalPayable), Theme.getOweRose()));
        Color netColor = netOverall > 0 ? Theme.getAccentText() : (netOverall < 0 ? Theme.getOweRose() : Theme.getMuted());
        summaryCard.add(createSummaryColumn("Net Balance", Money.format(netOverall), netColor));

        content.add(summaryCard);
        content.add(Box.createVerticalStrut(18));

        // 3. Middle Row: Groups Card (Left) & Spending Chart Card (Right)
        JPanel midRow = new JPanel(new GridLayout(1, 2, 18, 0));
        midRow.setOpaque(false);
        midRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        midRow.setPreferredSize(new Dimension(800, 260));

        // Left: Groups Overview
        GlossyPanel groupsCard = new GlossyPanel();
        groupsCard.setLayout(new BorderLayout(0, 10));
        groupsCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel groupsTop = new JPanel(new BorderLayout());
        groupsTop.setOpaque(false);
        JLabel grpTitle = new JLabel("Your Groups (" + userGroups.size() + ")");
        grpTitle.setFont(Theme.fontSubtitle);
        grpTitle.setForeground(Theme.getText());
        GlossyButton viewAllBtn = new GlossyButton("View All", false);
        viewAllBtn.setPreferredSize(new Dimension(90, 30));
        viewAllBtn.addActionListener(e -> {
            if (navListener != null) {
                navListener.openGroups();
            }
        });
        groupsTop.add(grpTitle, BorderLayout.WEST);
        groupsTop.add(viewAllBtn, BorderLayout.EAST);
        groupsCard.add(groupsTop, BorderLayout.NORTH);

        if (userGroups.isEmpty()) {
            groupsCard.add(UIUtils.createEmptyState("No groups yet. Create your first group!"), BorderLayout.CENTER);
        } else {
            JPanel groupList = new JPanel();
            groupList.setLayout(new BoxLayout(groupList, BoxLayout.Y_AXIS));
            groupList.setOpaque(false);

            int maxShow = Math.min(4, userGroups.size());
            for (int i = 0; i < maxShow; i++) {
                Group g = userGroups.get(i);
                JPanel item = new JPanel(new BorderLayout(8, 0));
                item.setOpaque(false);
                item.setBorder(new EmptyBorder(6, 4, 6, 4));

                JLabel name = new JLabel(g.getName());
                name.setFont(Theme.fontBodyBold);
                name.setForeground(Theme.getText());

                JLabel meta = new JLabel(g.getMembers().size() + " members • " + g.getExpenses().size() + " expenses");
                meta.setFont(Theme.fontSmall);
                meta.setForeground(Theme.getMuted());

                JPanel textPanel = new JPanel(new GridLayout(2, 1));
                textPanel.setOpaque(false);
                textPanel.add(name);
                textPanel.add(meta);

                GlossyButton openBtn = new GlossyButton("Open", false);
                openBtn.setPreferredSize(new Dimension(70, 28));
                openBtn.addActionListener(e -> {
                    if (navListener != null) {
                        navListener.openGroupDetail(g);
                    }
                });

                item.add(textPanel, BorderLayout.CENTER);
                item.add(openBtn, BorderLayout.EAST);

                groupList.add(item);
                if (i < maxShow - 1) {
                    groupList.add(Box.createVerticalStrut(4));
                }
            }
            groupsCard.add(groupList, BorderLayout.CENTER);
        }

        // Right: Spending Chart
        GlossyPanel chartCard = new GlossyPanel();
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        BarChartPanel chart = new BarChartPanel();
        List<Expense> allExp = user != null ? dataStore.getAllExpensesForUser(user.getId()) : new ArrayList<>();
        Map<String, Long> catSpending = new HashMap<>();
        for (int i = 0; i < allExp.size(); i++) {
            Expense e = allExp.get(i);
            String catName = e.getCategory().getDisplayName();
            long curr = catSpending.containsKey(catName) ? catSpending.get(catName) : 0L;
            catSpending.put(catName, curr + e.getAmountPaise());
        }
        List<BarChartPanel.BarItem> chartItems = new ArrayList<>();
        for (Map.Entry<String, Long> entry : catSpending.entrySet()) {
            chartItems.add(new BarChartPanel.BarItem(entry.getKey(), entry.getValue()));
        }
        chart.setData("Spending by Category", chartItems);
        chartCard.add(chart, BorderLayout.CENTER);

        midRow.add(groupsCard);
        midRow.add(chartCard);
        content.add(midRow);
        content.add(Box.createVerticalStrut(18));

        // 4. Bottom Row: Recent Activity Card (Latest 5 Expenses)
        GlossyPanel recentCard = new GlossyPanel();
        recentCard.setLayout(new BorderLayout(0, 10));
        recentCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel recentTitle = new JLabel("Recent Activity");
        recentTitle.setFont(Theme.fontSubtitle);
        recentTitle.setForeground(Theme.getText());
        recentCard.add(recentTitle, BorderLayout.NORTH);

        if (allExp.isEmpty()) {
            recentCard.add(UIUtils.createEmptyState("No recent activity recorded yet."), BorderLayout.CENTER);
        } else {
            String[] cols = {"Date", "Description", "Group", "Paid By", "Category", "Amount"};
            DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

            int maxRecent = Math.min(5, allExp.size());
            for (int i = 0; i < maxRecent; i++) {
                Expense e = allExp.get(i);
                Group g = dataStore.findGroupById(e.getGroupId());
                String groupName = g != null ? g.getName() : "Unknown";
                Member payer = g != null ? g.findMemberById(e.getPayerMemberId()) : null;
                String payerName = payer != null ? payer.getName() : "Unknown";

                tableModel.addRow(new Object[]{
                        e.getDate().toString(),
                        e.getDescription(),
                        groupName,
                        payerName,
                        e.getCategory().getDisplayName(),
                        Money.format(e.getAmountPaise())
                });
            }

            JTable recentTable = new JTable(tableModel);
            UIUtils.styleTable(recentTable);
            JScrollPane tableScroll = new JScrollPane(recentTable);
            tableScroll.setPreferredSize(new Dimension(800, 140));
            UIUtils.styleScrollPane(tableScroll);
            recentCard.add(tableScroll, BorderLayout.CENTER);
        }

        content.add(recentCard);

        JScrollPane mainScroll = new JScrollPane(content);
        UIUtils.styleScrollPane(mainScroll);
        mainScroll.setBorder(null);
        add(mainScroll, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel createSummaryColumn(String title, String value, Color valueColor) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JLabel tLbl = new JLabel(title);
        tLbl.setFont(Theme.fontSmallBold);
        tLbl.setForeground(Theme.getMuted());
        tLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel vLbl = new JLabel(value);
        vLbl.setFont(Theme.fontTitle);
        vLbl.setForeground(valueColor);
        vLbl.setAlignmentX(LEFT_ALIGNMENT);

        col.add(tLbl);
        col.add(Box.createVerticalStrut(4));
        col.add(vLbl);
        return col;
    }
}
