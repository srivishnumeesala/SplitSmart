package splitsmart.ui;

import splitsmart.logic.BalanceCalculator;
import splitsmart.logic.Money;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.model.User;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Screen presenting all groups owned by the active user with options to create,
 * rename, view, or delete groups.
 */
public class GroupsPanel extends JPanel {

    public interface GroupNavigationListener {
        void openGroupDetail(Group group);
    }

    private final DataStore dataStore;
    private final GroupNavigationListener navListener;

    public GroupsPanel(DataStore dataStore, GroupNavigationListener navListener) {
        this.dataStore = dataStore;
        this.navListener = navListener;
        setLayout(new BorderLayout());
        setOpaque(false);
        refreshUI();
    }

    public void refreshUI() {
        removeAll();

        User user = dataStore.getCurrentUser();
        List<Group> userGroups = user != null ? dataStore.getGroupsForUser(user.getId()) : java.util.Collections.emptyList();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Header Row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel titleLbl = new JLabel("My Groups (" + userGroups.size() + ")");
        titleLbl.setFont(Theme.fontTitle);
        titleLbl.setForeground(Theme.getText());

        GlossyButton createBtn = new GlossyButton("+ Create Group", true);
        createBtn.setPreferredSize(new Dimension(140, 36));
        createBtn.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            GroupDialog dlg = new GroupDialog(frame, dataStore, user != null ? user.getId() : "", null);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                refreshUI();
            }
        });

        headerRow.add(titleLbl, BorderLayout.WEST);
        headerRow.add(createBtn, BorderLayout.EAST);
        root.add(headerRow);
        root.add(Box.createVerticalStrut(18));

        if (userGroups.isEmpty()) {
            JPanel empty = UIUtils.createEmptyState("No groups yet. Click '+ Create Group' to get started.");
            root.add(empty);
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 2, 18, 18));
            grid.setOpaque(false);

            for (int i = 0; i < userGroups.size(); i++) {
                Group g = userGroups.get(i);
                grid.add(createGroupCard(g));
            }
            root.add(grid);
        }

        JScrollPane scroll = new JScrollPane(root);
        UIUtils.styleScrollPane(scroll);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private GlossyPanel createGroupCard(Group group) {
        GlossyPanel card = new GlossyPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setPreferredSize(new Dimension(360, 175));

        // Top: Group Name & Total Spent
        long totalExpenses = 0;
        for (int i = 0; i < group.getExpenses().size(); i++) {
            totalExpenses += group.getExpenses().get(i).getAmountPaise();
        }

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel nameLbl = new JLabel(group.getName());
        nameLbl.setFont(Theme.fontSubtitle);
        nameLbl.setForeground(Theme.getText());

        JLabel spentLbl = new JLabel(Money.format(totalExpenses));
        spentLbl.setFont(Theme.fontSubtitle);
        spentLbl.setForeground(Theme.getAccentText());

        top.add(nameLbl, BorderLayout.WEST);
        top.add(spentLbl, BorderLayout.EAST);

        // Center: Members names chip
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < group.getMembers().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(group.getMembers().get(i).getName());
        }

        JLabel membersLbl = new JLabel(group.getMembers().size() + " Members: " + sb.toString());
        membersLbl.setFont(Theme.fontSmall);
        membersLbl.setForeground(Theme.getMuted());

        JLabel countLbl = new JLabel(group.getExpenses().size() + " recorded expenses");
        countLbl.setFont(Theme.fontSmall);
        countLbl.setForeground(Theme.getMuted());

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        centerPanel.setOpaque(false);
        centerPanel.add(membersLbl);
        centerPanel.add(countLbl);

        // Bottom: Action Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        GlossyButton openBtn = new GlossyButton("Open", true);
        openBtn.setPreferredSize(new Dimension(75, 30));
        openBtn.addActionListener(e -> {
            if (navListener != null) {
                navListener.openGroupDetail(group);
            }
        });

        GlossyButton editBtn = new GlossyButton("Edit", false);
        editBtn.setPreferredSize(new Dimension(65, 30));
        editBtn.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            GroupDialog dlg = new GroupDialog(frame, dataStore, group.getOwnerUserId(), group);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                refreshUI();
            }
        });

        GlossyButton deleteBtn = new GlossyButton("Delete", false);
        deleteBtn.setPreferredSize(new Dimension(75, 30));
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete group '" + group.getName() + "' and all its expenses?",
                    "Confirm Delete Group",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                dataStore.deleteGroup(group.getId());
                refreshUI();
            }
        });

        actions.add(deleteBtn);
        actions.add(editBtn);
        actions.add(openBtn);

        card.add(top, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }
}
