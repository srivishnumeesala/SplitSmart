package splitsmart.ui;

import splitsmart.logic.Money;
import splitsmart.logic.SplitCalculator;
import splitsmart.logic.Validator;
import splitsmart.model.Category;
import splitsmart.model.Expense;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.model.SplitType;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
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
 * Modal dialog for creating and editing shared expenses with Equal, Exact, or Percentage splits.
 * Validates descriptions, dates, amounts, payer, and exact share sums.
 */
public class ExpenseDialog extends JDialog {

    private final Group group;
    private final Expense existingExpense;
    private final DataStore dataStore;
    private boolean saved = false;

    private JTextField descField;
    private JTextField amountField;
    private JTextField dateField;
    private JComboBox<Member> payerCombo;
    private JComboBox<Category> categoryCombo;
    private JComboBox<SplitType> splitTypeCombo;

    private JPanel sharesContainer;
    private JLabel errorLabel;

    // Per-member UI elements for splitting
    private final List<JCheckBox> memberChecks = new ArrayList<>();
    private final List<JTextField> shareInputFields = new ArrayList<>();
    private final List<Member> groupMembers = new ArrayList<>();

    public ExpenseDialog(JFrame parent, DataStore dataStore, Group group, Expense existingExpense) {
        super(parent, existingExpense == null ? "Add Expense" : "Edit Expense", true);
        this.dataStore = dataStore;
        this.group = group;
        this.existingExpense = existingExpense;
        this.groupMembers.addAll(group.getMembers());

        initUI();
    }

    private void initUI() {
        setSize(520, 680);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(Theme.getCard());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.setBackground(Theme.getCard());

        JLabel titleLabel = new JLabel(existingExpense == null ? "Record New Expense" : "Edit Expense");
        titleLabel.setFont(Theme.fontSubtitle);
        titleLabel.setForeground(Theme.getText());
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Form Fields
        descField = UIUtils.createTextField();
        amountField = UIUtils.createTextField();
        dateField = UIUtils.createTextField();
        dateField.setText(LocalDate.now().toString());

        payerCombo = new JComboBox<>(groupMembers.toArray(new Member[0]));
        payerCombo.setFont(Theme.fontBody);
        payerCombo.setBackground(Theme.getCard());
        payerCombo.setForeground(Theme.getText());

        categoryCombo = new JComboBox<>(Category.values());
        categoryCombo.setFont(Theme.fontBody);
        categoryCombo.setBackground(Theme.getCard());
        categoryCombo.setForeground(Theme.getText());

        splitTypeCombo = new JComboBox<>(SplitType.values());
        splitTypeCombo.setFont(Theme.fontBody);
        splitTypeCombo.setBackground(Theme.getCard());
        splitTypeCombo.setForeground(Theme.getText());
        splitTypeCombo.addActionListener(e -> updateSharesUI());

        if (existingExpense != null) {
            descField.setText(existingExpense.getDescription());
            amountField.setText(Money.formatPlain(existingExpense.getAmountPaise()));
            dateField.setText(existingExpense.getDate().toString());
            categoryCombo.setSelectedItem(existingExpense.getCategory());
            splitTypeCombo.setSelectedItem(existingExpense.getSplitType());

            Member payer = group.findMemberById(existingExpense.getPayerMemberId());
            if (payer != null) {
                payerCombo.setSelectedItem(payer);
            }
        }

        // Setup shares panel
        sharesContainer = new JPanel();
        sharesContainer.setLayout(new BoxLayout(sharesContainer, BoxLayout.Y_AXIS));
        sharesContainer.setOpaque(false);

        JScrollPane sharesScroll = new JScrollPane(sharesContainer);
        sharesScroll.setPreferredSize(new Dimension(460, 160));
        sharesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        UIUtils.styleScrollPane(sharesScroll);
        sharesScroll.setAlignmentX(LEFT_ALIGNMENT);

        updateSharesUI();

        // Error message
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.fontSmall);
        errorLabel.setForeground(Theme.getOweRose());
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        actions.setAlignmentX(LEFT_ALIGNMENT);

        GlossyButton cancelBtn = new GlossyButton("Cancel", false);
        cancelBtn.addActionListener(e -> dispose());

        GlossyButton saveBtn = new GlossyButton(existingExpense == null ? "Save Expense" : "Update Expense", true);
        saveBtn.addActionListener(e -> handleSave());

        actions.add(cancelBtn);
        actions.add(saveBtn);

        // Assemble Form
        root.add(titleLabel);
        root.add(Box.createVerticalStrut(12));

        root.add(createLabel("Description"));
        root.add(Box.createVerticalStrut(2));
        root.add(descField);
        root.add(Box.createVerticalStrut(8));

        JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row1.setAlignmentX(LEFT_ALIGNMENT);

        JPanel pAmount = new JPanel(new BorderLayout(0, 2));
        pAmount.setOpaque(false);
        pAmount.add(createLabel("Amount (₹)"), BorderLayout.NORTH);
        pAmount.add(amountField, BorderLayout.CENTER);

        JPanel pDate = new JPanel(new BorderLayout(0, 2));
        pDate.setOpaque(false);
        pDate.add(createLabel("Date (yyyy-MM-dd)"), BorderLayout.NORTH);
        pDate.add(dateField, BorderLayout.CENTER);

        row1.add(pAmount);
        row1.add(pDate);
        root.add(row1);
        root.add(Box.createVerticalStrut(8));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row2.setAlignmentX(LEFT_ALIGNMENT);

        JPanel pPayer = new JPanel(new BorderLayout(0, 2));
        pPayer.setOpaque(false);
        pPayer.add(createLabel("Paid by"), BorderLayout.NORTH);
        pPayer.add(payerCombo, BorderLayout.CENTER);

        JPanel pCat = new JPanel(new BorderLayout(0, 2));
        pCat.setOpaque(false);
        pCat.add(createLabel("Category"), BorderLayout.NORTH);
        pCat.add(categoryCombo, BorderLayout.CENTER);

        row2.add(pPayer);
        row2.add(pCat);
        root.add(row2);
        root.add(Box.createVerticalStrut(8));

        root.add(createLabel("Split Type"));
        root.add(Box.createVerticalStrut(2));
        root.add(splitTypeCombo);
        root.add(Box.createVerticalStrut(10));

        root.add(createLabel("Split Between Members"));
        root.add(Box.createVerticalStrut(4));
        root.add(sharesScroll);
        root.add(Box.createVerticalStrut(8));

        root.add(errorLabel);
        root.add(Box.createVerticalStrut(8));
        root.add(actions);

        getContentPane().add(root);
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.fontSmallBold);
        lbl.setForeground(Theme.getText());
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private void updateSharesUI() {
        sharesContainer.removeAll();
        memberChecks.clear();
        shareInputFields.clear();

        SplitType splitType = (SplitType) splitTypeCombo.getSelectedItem();
        if (splitType == null) {
            splitType = SplitType.EQUAL;
        }

        for (int i = 0; i < groupMembers.size(); i++) {
            Member m = groupMembers.get(i);
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            row.setBorder(new EmptyBorder(2, 6, 2, 6));

            JCheckBox cb = new JCheckBox(m.getName());
            cb.setFont(Theme.fontBody);
            cb.setForeground(Theme.getText());
            cb.setOpaque(false);

            // Default checked unless editing an expense where member wasn't in shares
            boolean checked = true;
            if (existingExpense != null && existingExpense.getShares() != null) {
                checked = existingExpense.getShares().containsKey(m.getId());
            }
            cb.setSelected(checked);
            memberChecks.add(cb);

            row.add(cb, BorderLayout.WEST);

            if (splitType == SplitType.EXACT) {
                JTextField txt = UIUtils.createTextField();
                txt.setPreferredSize(new Dimension(100, 30));
                if (existingExpense != null && existingExpense.getShares() != null && existingExpense.getShares().containsKey(m.getId())) {
                    txt.setText(Money.formatPlain(existingExpense.getShares().get(m.getId())));
                }
                shareInputFields.add(txt);
                row.add(txt, BorderLayout.EAST);
            } else if (splitType == SplitType.PERCENTAGE) {
                JPanel pctPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
                pctPanel.setOpaque(false);
                JTextField txt = UIUtils.createTextField();
                txt.setPreferredSize(new Dimension(70, 30));
                if (existingExpense != null && existingExpense.getAmountPaise() > 0 && existingExpense.getShares() != null && existingExpense.getShares().containsKey(m.getId())) {
                    long share = existingExpense.getShares().get(m.getId());
                    double pct = (double) share * 100.0 / existingExpense.getAmountPaise();
                    txt.setText(String.format("%.2f", pct).replace(".00", ""));
                }
                shareInputFields.add(txt);
                JLabel pctSign = new JLabel("%");
                pctSign.setFont(Theme.fontBody);
                pctSign.setForeground(Theme.getMuted());
                pctPanel.add(txt);
                pctPanel.add(pctSign);
                row.add(pctPanel, BorderLayout.EAST);
            }

            sharesContainer.add(row);
            sharesContainer.add(Box.createVerticalStrut(2));
        }

        sharesContainer.revalidate();
        sharesContainer.repaint();
    }

    private void handleSave() {
        errorLabel.setText(" ");
        String desc = descField.getText().trim();
        if (desc.isEmpty()) {
            errorLabel.setText("Description cannot be empty.");
            return;
        }

        long amountPaise;
        try {
            amountPaise = Money.parse(amountField.getText());
            if (amountPaise <= 0) {
                errorLabel.setText("Amount must be greater than ₹0.00.");
                return;
            }
        } catch (IllegalArgumentException e) {
            errorLabel.setText("Invalid amount: " + e.getMessage());
            return;
        }

        LocalDate date;
        try {
            date = Validator.parseAndValidateDate(dateField.getText());
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
            return;
        }

        Member payer = (Member) payerCombo.getSelectedItem();
        if (payer == null) {
            errorLabel.setText("Please select a payer.");
            return;
        }

        Category category = (Category) categoryCombo.getSelectedItem();
        SplitType splitType = (SplitType) splitTypeCombo.getSelectedItem();

        // Calculate shares according to split type
        Map<String, Long> shares = new HashMap<>();
        List<String> selectedMemberIds = new ArrayList<>();
        for (int i = 0; i < groupMembers.size(); i++) {
            if (memberChecks.get(i).isSelected()) {
                selectedMemberIds.add(groupMembers.get(i).getId());
            }
        }

        if (selectedMemberIds.isEmpty()) {
            errorLabel.setText("Select at least one member to share this expense.");
            return;
        }

        try {
            if (splitType == SplitType.EQUAL) {
                shares = SplitCalculator.calculateEqualSplit(amountPaise, selectedMemberIds);
            } else if (splitType == SplitType.EXACT) {
                for (int i = 0; i < groupMembers.size(); i++) {
                    if (memberChecks.get(i).isSelected()) {
                        String mid = groupMembers.get(i).getId();
                        String text = shareInputFields.get(i).getText().trim();
                        if (text.isEmpty()) {
                            errorLabel.setText("Enter share amount for " + groupMembers.get(i).getName());
                            return;
                        }
                        long sharePaise = Money.parse(text);
                        shares.put(mid, sharePaise);
                    }
                }
                SplitCalculator.validateExactSplit(amountPaise, shares);
            } else if (splitType == SplitType.PERCENTAGE) {
                Map<String, Integer> bpMap = new HashMap<>();
                for (int i = 0; i < groupMembers.size(); i++) {
                    if (memberChecks.get(i).isSelected()) {
                        String mid = groupMembers.get(i).getId();
                        String text = shareInputFields.get(i).getText().trim();
                        if (text.isEmpty()) {
                            errorLabel.setText("Enter percentage for " + groupMembers.get(i).getName());
                            return;
                        }
                        // Parse percentage into basis points (e.g. 50% = 5000 bp, 33.33% = 3333 bp)
                        double val = Double.parseDouble(text);
                        int bp = (int) Math.round(val * 100.0);
                        bpMap.put(mid, bp);
                    }
                }
                shares = SplitCalculator.calculatePercentageSplit(amountPaise, selectedMemberIds, bpMap);
            }
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
            return;
        }

        String validationErr = Validator.validateSharesSum(amountPaise, shares);
        if (validationErr != null) {
            errorLabel.setText(validationErr);
            return;
        }

        if (existingExpense == null) {
            String expId = "exp_" + UUID.randomUUID().toString().substring(0, 8);
            Expense expense = new Expense(expId, group.getId(), desc, amountPaise, date,
                    payer.getId(), category, splitType, shares);
            dataStore.addExpense(group.getId(), expense);
        } else {
            existingExpense.setDescription(desc);
            existingExpense.setAmountPaise(amountPaise);
            existingExpense.setDate(date);
            existingExpense.setPayerMemberId(payer.getId());
            existingExpense.setCategory(category);
            existingExpense.setSplitType(splitType);
            existingExpense.setShares(shares);
            dataStore.updateExpense(group.getId(), existingExpense);
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}
