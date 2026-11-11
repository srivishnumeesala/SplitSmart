package splitsmart.ui;

import splitsmart.logic.Money;
import splitsmart.model.Category;
import splitsmart.model.Expense;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.model.User;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen displaying all shared expenses across all groups with live search
 * and filters by group and category.
 */
public class TransactionsPanel extends JPanel {

    private final DataStore dataStore;
    private JTextField searchField;
    private JComboBox<String> groupCombo;
    private JComboBox<String> categoryCombo;
    private JTable transactionsTable;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;

    private List<Expense> allExpenses = new ArrayList<>();
    private List<Expense> filteredExpenses = new ArrayList<>();

    public TransactionsPanel(DataStore dataStore) {
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

        JLabel titleLbl = new JLabel("All Transactions");
        titleLbl.setFont(Theme.fontTitle);
        titleLbl.setForeground(Theme.getText());
        headerRow.add(titleLbl, BorderLayout.WEST);
        root.add(headerRow);
        root.add(Box.createVerticalStrut(14));

        // Filter Bar Card
        GlossyPanel filterCard = new GlossyPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        filterCard.setPreferredSize(new Dimension(800, 65));

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(Theme.fontSmallBold);
        searchLbl.setForeground(Theme.getText());

        searchField = UIUtils.createTextField();
        searchField.setPreferredSize(new Dimension(180, 34));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        JLabel grpLbl = new JLabel("Group:");
        grpLbl.setFont(Theme.fontSmallBold);
        grpLbl.setForeground(Theme.getText());

        groupCombo = new JComboBox<>();
        groupCombo.setFont(Theme.fontBody);
        groupCombo.setBackground(Theme.getCard());
        groupCombo.setForeground(Theme.getText());
        groupCombo.setPreferredSize(new Dimension(150, 34));
        groupCombo.addActionListener(e -> applyFilters());

        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(Theme.fontSmallBold);
        catLbl.setForeground(Theme.getText());

        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(Theme.fontBody);
        categoryCombo.setBackground(Theme.getCard());
        categoryCombo.setForeground(Theme.getText());
        categoryCombo.setPreferredSize(new Dimension(150, 34));
        categoryCombo.addActionListener(e -> applyFilters());

        GlossyButton clearBtn = new GlossyButton("Clear", false);
        clearBtn.setPreferredSize(new Dimension(75, 34));
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            groupCombo.setSelectedIndex(0);
            categoryCombo.setSelectedIndex(0);
        });

        filterCard.add(searchLbl);
        filterCard.add(searchField);
        filterCard.add(grpLbl);
        filterCard.add(groupCombo);
        filterCard.add(catLbl);
        filterCard.add(categoryCombo);
        filterCard.add(clearBtn);

        root.add(filterCard);
        root.add(Box.createVerticalStrut(14));

        // Transactions Table Card
        GlossyPanel tableCard = new GlossyPanel();
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        String[] cols = {"Date", "Description", "Group", "Paid By", "Category", "Split Type", "Amount"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        transactionsTable = new JTable(tableModel);
        UIUtils.styleTable(transactionsTable);

        JScrollPane tableScroll = new JScrollPane(transactionsTable);
        UIUtils.styleScrollPane(tableScroll);
        tableScroll.setPreferredSize(new Dimension(800, 380));
        tableCard.add(tableScroll, BorderLayout.CENTER);

        // Summary Bar
        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(Theme.fontSmall);
        summaryLabel.setForeground(Theme.getMuted());
        tableCard.add(summaryLabel, BorderLayout.SOUTH);

        root.add(tableCard);

        add(root, BorderLayout.CENTER);
        refreshUI();
    }

    public void refreshUI() {
        User user = dataStore.getCurrentUser();
        allExpenses = user != null ? dataStore.getAllExpensesForUser(user.getId()) : new ArrayList<>();

        // Populate Group combo
        groupCombo.removeAllItems();
        groupCombo.addItem("All Groups");
        List<Group> userGroups = user != null ? dataStore.getGroupsForUser(user.getId()) : new ArrayList<>();
        for (int i = 0; i < userGroups.size(); i++) {
            groupCombo.addItem(userGroups.get(i).getName());
        }

        // Populate Category combo
        categoryCombo.removeAllItems();
        categoryCombo.addItem("All Categories");
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            categoryCombo.addItem(cats[i].getDisplayName());
        }

        applyFilters();
    }

    private void applyFilters() {
        if (searchField == null || groupCombo == null || categoryCombo == null) {
            return;
        }

        String search = searchField.getText().trim().toLowerCase();
        String selectedGroup = (String) groupCombo.getSelectedItem();
        String selectedCategory = (String) categoryCombo.getSelectedItem();

        filteredExpenses.clear();
        tableModel.setRowCount(0);

        long totalFiltered = 0;

        for (int i = 0; i < allExpenses.size(); i++) {
            Expense e = allExpenses.get(i);
            Group g = dataStore.findGroupById(e.getGroupId());
            String groupName = g != null ? g.getName() : "Unknown";
            Member payer = g != null ? g.findMemberById(e.getPayerMemberId()) : null;
            String payerName = payer != null ? payer.getName() : "Unknown";

            // Group filter
            if (selectedGroup != null && !selectedGroup.equals("All Groups")) {
                if (!groupName.equalsIgnoreCase(selectedGroup)) {
                    continue;
                }
            }

            // Category filter
            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                if (!e.getCategory().getDisplayName().equalsIgnoreCase(selectedCategory)) {
                    continue;
                }
            }

            // Search text filter
            if (!search.isEmpty()) {
                boolean matchDesc = e.getDescription().toLowerCase().contains(search);
                boolean matchPayer = payerName.toLowerCase().contains(search);
                boolean matchGroup = groupName.toLowerCase().contains(search);
                if (!matchDesc && !matchPayer && !matchGroup) {
                    continue;
                }
            }

            filteredExpenses.add(e);
            totalFiltered += e.getAmountPaise();

            tableModel.addRow(new Object[]{
                    e.getDate().toString(),
                    e.getDescription(),
                    groupName,
                    payerName,
                    e.getCategory().getDisplayName(),
                    e.getSplitType().getDisplayName(),
                    Money.format(e.getAmountPaise())
            });
        }

        summaryLabel.setText("Showing " + filteredExpenses.size() + " of " + allExpenses.size()
                + " transactions  •  Filtered Total: " + Money.format(totalFiltered));
    }
}
