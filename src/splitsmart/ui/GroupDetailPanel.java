package splitsmart.ui;

import splitsmart.logic.BalanceCalculator;
import splitsmart.logic.Money;
import splitsmart.logic.SettlementPlanner;
import splitsmart.model.Category;
import splitsmart.model.Expense;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.model.SplitType;
import splitsmart.model.Transfer;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed view for an individual group displaying members, expense transactions,
 * calculated balances, and interactive debt settlement plans.
 */
public class GroupDetailPanel extends JPanel {

    public interface BackListener {
        void onBack();
    }

    private final DataStore dataStore;
    private final Group group;
    private final BackListener backListener;

    public GroupDetailPanel(DataStore dataStore, Group group, BackListener backListener) {
        this.dataStore = dataStore;
        this.group = group;
        this.backListener = backListener;

        setLayout(new BorderLayout());
        setOpaque(false);
        refreshUI();
    }

    public void refreshUI() {
        removeAll();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(20, 24, 24, 24));

        // 1. Navigation & Header
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel leftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTitle.setOpaque(false);

        GlossyButton backBtn = new GlossyButton("← Back", false);
        backBtn.setPreferredSize(new Dimension(80, 34));
        backBtn.addActionListener(e -> {
            if (backListener != null) {
                backListener.onBack();
            }
        });

        JLabel groupNameLbl = new JLabel(group.getName());
        groupNameLbl.setFont(Theme.fontTitle);
        groupNameLbl.setForeground(Theme.getText());

        leftTitle.add(backBtn);
        leftTitle.add(groupNameLbl);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        GlossyButton addExpBtn = new GlossyButton("+ Add Expense", true);
        addExpBtn.setPreferredSize(new Dimension(140, 36));
        addExpBtn.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            ExpenseDialog dlg = new ExpenseDialog(frame, dataStore, group, null);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                refreshUI();
            }
        });

        rightActions.add(addExpBtn);

        topRow.add(leftTitle, BorderLayout.WEST);
        topRow.add(rightActions, BorderLayout.EAST);
        root.add(topRow);
        root.add(Box.createVerticalStrut(10));

        // Members chip bar
        JPanel membersBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        membersBar.setOpaque(false);
        membersBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel mTag = new JLabel("Members:");
        mTag.setFont(Theme.fontSmallBold);
        mTag.setForeground(Theme.getMuted());
        membersBar.add(mTag);

        for (int i = 0; i < group.getMembers().size(); i++) {
            Member m = group.getMembers().get(i);
            JLabel chip = new JLabel(" " + m.getName() + " ");
            chip.setFont(Theme.fontSmall);
            chip.setForeground(Theme.getText());
            chip.setOpaque(true);
            chip.setBackground(Theme.getRaised());
            chip.setBorder(BorderFactory.createLineBorder(Theme.getBorder(), 1));
            membersBar.add(chip);
        }
        root.add(membersBar);
        root.add(Box.createVerticalStrut(16));

        // 2. Balances & Settle-up Cards (Side-by-side or stacked)
        JPanel midSection = new JPanel(new GridLayout(1, 2, 16, 0));
        midSection.setOpaque(false);
        midSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        midSection.setPreferredSize(new Dimension(800, 260));

        // Balances Card
        GlossyPanel balancesCard = new GlossyPanel();
        balancesCard.setLayout(new BorderLayout(0, 8));
        balancesCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel balancesTitle = new JLabel("Member Balances");
        balancesTitle.setFont(Theme.fontSubtitle);
        balancesTitle.setForeground(Theme.getText());
        balancesCard.add(balancesTitle, BorderLayout.NORTH);

        List<BalanceCalculator.MemberBalance> balanceList = BalanceCalculator.calculateGroupBalances(group);
        String[] balCols = {"Member", "Paid", "Share", "Net"};
        DefaultTableModel balModel = new DefaultTableModel(balCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (int i = 0; i < balanceList.size(); i++) {
            BalanceCalculator.MemberBalance mb = balanceList.get(i);
            balModel.addRow(new Object[]{
                    mb.getMember().getName(),
                    Money.format(mb.getPaidPaise()),
                    Money.format(mb.getSharePaise()),
                    Money.format(mb.getNetPaise())
            });
        }

        JTable balTable = new JTable(balModel);
        UIUtils.styleTable(balTable);

        // Custom renderer for Net column: amber = gets back, rose = owes, gray = settled
        balTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                long net = balanceList.get(row).getNetPaise();
                if (net > 0) {
                    c.setForeground(Theme.getAccentText());
                } else if (net < 0) {
                    c.setForeground(Theme.getOweRose());
                } else {
                    c.setForeground(Theme.getMuted());
                }
                c.setBackground(row % 2 == 0 ? Theme.getCard() : Theme.getRaised());
                return c;
            }
        });

        JScrollPane balScroll = new JScrollPane(balTable);
        UIUtils.styleScrollPane(balScroll);
        balancesCard.add(balScroll, BorderLayout.CENTER);

        // Settle-up Plan Card
        GlossyPanel settleCard = new GlossyPanel();
        settleCard.setLayout(new BorderLayout(0, 8));
        settleCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        List<Transfer> transfers = SettlementPlanner.planSettlements(balanceList);
        JLabel settleTitle = new JLabel("Settle-Up Plan (" + transfers.size() + " transfers)");
        settleTitle.setFont(Theme.fontSubtitle);
        settleTitle.setForeground(Theme.getText());
        settleCard.add(settleTitle, BorderLayout.NORTH);

        if (transfers.isEmpty()) {
            settleCard.add(UIUtils.createEmptyState("Everyone is already settled up!"), BorderLayout.CENTER);
        } else {
            JPanel transferList = new JPanel();
            transferList.setLayout(new BoxLayout(transferList, BoxLayout.Y_AXIS));
            transferList.setOpaque(false);

            for (int i = 0; i < transfers.size(); i++) {
                Transfer t = transfers.get(i);
                JPanel tRow = new JPanel(new BorderLayout(8, 0));
                tRow.setOpaque(false);
                tRow.setBorder(new EmptyBorder(6, 4, 6, 4));

                String text = t.getFromMember().getName() + " pays " + t.getToMember().getName() + "  " + Money.format(t.getAmountPaise());
                JLabel tLbl = new JLabel(text);
                tLbl.setFont(Theme.fontBody);
                tLbl.setForeground(Theme.getText());

                GlossyButton markPaidBtn = new GlossyButton("Mark as Paid", false);
                markPaidBtn.setPreferredSize(new Dimension(110, 28));
                markPaidBtn.addActionListener(e -> {
                    handleMarkPaid(t);
                });

                tRow.add(tLbl, BorderLayout.CENTER);
                tRow.add(markPaidBtn, BorderLayout.EAST);

                transferList.add(tRow);
                if (i < transfers.size() - 1) {
                    transferList.add(Box.createVerticalStrut(4));
                }
            }

            JScrollPane transferScroll = new JScrollPane(transferList);
            UIUtils.styleScrollPane(transferScroll);
            settleCard.add(transferScroll, BorderLayout.CENTER);
        }

        midSection.add(balancesCard);
        midSection.add(settleCard);
        root.add(midSection);
        root.add(Box.createVerticalStrut(18));

        // 3. Expenses History Card
        GlossyPanel expensesCard = new GlossyPanel();
        expensesCard.setLayout(new BorderLayout(0, 10));
        expensesCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel expHeader = new JPanel(new BorderLayout());
        expHeader.setOpaque(false);
        JLabel expTitle = new JLabel("Expenses (" + group.getExpenses().size() + ")");
        expTitle.setFont(Theme.fontSubtitle);
        expTitle.setForeground(Theme.getText());
        expHeader.add(expTitle, BorderLayout.WEST);
        expensesCard.add(expHeader, BorderLayout.NORTH);

        if (group.getExpenses().isEmpty()) {
            expensesCard.add(UIUtils.createEmptyState("No expenses recorded yet. Click '+ Add Expense' to record one."), BorderLayout.CENTER);
        } else {
            String[] expCols = {"Date", "Description", "Paid By", "Category", "Split Type", "Amount", "Actions"};
            DefaultTableModel expModel = new DefaultTableModel(expCols, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return c == 6; // only actions column
                }
            };

            for (int i = 0; i < group.getExpenses().size(); i++) {
                Expense e = group.getExpenses().get(i);
                Member payer = group.findMemberById(e.getPayerMemberId());
                String payerName = payer != null ? payer.getName() : "Unknown";
                expModel.addRow(new Object[]{
                        e.getDate().toString(),
                        e.getDescription(),
                        payerName,
                        e.getCategory().getDisplayName(),
                        e.getSplitType().getDisplayName(),
                        Money.format(e.getAmountPaise()),
                        e.getId()
                });
            }

            JTable expTable = new JTable(expModel);
            UIUtils.styleTable(expTable);

            // Custom Action Buttons Column Renderer and Editor
            expTable.getColumnModel().getColumn(6).setPreferredWidth(140);
            expTable.getColumnModel().getColumn(6).setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
                p.setOpaque(false);
                GlossyButton edit = new GlossyButton("Edit", false);
                edit.setPreferredSize(new Dimension(55, 26));
                GlossyButton del = new GlossyButton("Delete", false);
                del.setPreferredSize(new Dimension(65, 26));
                p.add(edit);
                p.add(del);
                return p;
            });

            expTable.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int col = expTable.columnAtPoint(e.getPoint());
                    int row = expTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && col == 6) {
                        Expense exp = group.getExpenses().get(row);
                        int xInCell = e.getX() - expTable.getCellRect(row, col, false).x;
                        if (xInCell < 70) {
                            // Edit
                            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(GroupDetailPanel.this);
                            ExpenseDialog dlg = new ExpenseDialog(frame, dataStore, group, exp);
                            dlg.setVisible(true);
                            if (dlg.isSaved()) {
                                refreshUI();
                            }
                        } else {
                            // Delete
                            int confirm = JOptionPane.showConfirmDialog(GroupDetailPanel.this,
                                    "Are you sure you want to delete expense '" + exp.getDescription() + "'?",
                                    "Confirm Delete Expense",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE);
                            if (confirm == JOptionPane.YES_OPTION) {
                                dataStore.deleteExpense(group.getId(), exp.getId());
                                refreshUI();
                            }
                        }
                    }
                }
            });

            JScrollPane expScroll = new JScrollPane(expTable);
            expScroll.setPreferredSize(new Dimension(800, 220));
            UIUtils.styleScrollPane(expScroll);
            expensesCard.add(expScroll, BorderLayout.CENTER);
        }

        root.add(expensesCard);

        JScrollPane mainScroll = new JScrollPane(root);
        UIUtils.styleScrollPane(mainScroll);
        mainScroll.setBorder(null);
        add(mainScroll, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void handleMarkPaid(Transfer transfer) {
        String desc = "Settlement: " + transfer.getFromMember().getName() + " paid " + transfer.getToMember().getName();

        // The payer pays the exact amount, and the recipient shares 100% of it, which credits the payer and debits the recipient
        Map<String, Long> shares = new HashMap<>();
        shares.put(transfer.getToMember().getId(), transfer.getAmountPaise());

        String expId = "exp_settle_" + UUID.randomUUID().toString().substring(0, 8);
        Expense settleExp = new Expense(
                expId,
                group.getId(),
                desc,
                transfer.getAmountPaise(),
                LocalDate.now(),
                transfer.getFromMember().getId(),
                Category.SETTLEMENT,
                SplitType.EXACT,
                shares
        );

        dataStore.addExpense(group.getId(), settleExp);
        refreshUI();
    }
}
