package splitsmart.ui;

import splitsmart.logic.Validator;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modal dialog for creating or editing groups and managing group members.
 */
public class GroupDialog extends JDialog {

    private final Group existingGroup;
    private final String currentUserId;
    private final DataStore dataStore;
    private boolean saved = false;

    private JTextField nameField;
    private JTextField newMemberField;
    private DefaultListModel<String> membersModel;
    private JList<String> membersList;
    private JLabel errorLabel;

    public GroupDialog(JFrame parent, DataStore dataStore, String currentUserId, Group existingGroup) {
        super(parent, existingGroup == null ? "Create New Group" : "Edit Group", true);
        this.dataStore = dataStore;
        this.currentUserId = currentUserId;
        this.existingGroup = existingGroup;

        initUI();
    }

    private void initUI() {
        setSize(460, 520);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(Theme.getCard());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));
        root.setBackground(Theme.getCard());

        JLabel titleLabel = new JLabel(existingGroup == null ? "Create Group" : "Edit Group");
        titleLabel.setFont(Theme.fontSubtitle);
        titleLabel.setForeground(Theme.getText());
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Group Name
        JLabel nameLbl = new JLabel("Group Name");
        nameLbl.setFont(Theme.fontSmallBold);
        nameLbl.setForeground(Theme.getText());
        nameLbl.setAlignmentX(LEFT_ALIGNMENT);

        nameField = UIUtils.createTextField();
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        if (existingGroup != null) {
            nameField.setText(existingGroup.getName());
        }

        // Members Section
        JLabel membersLbl = new JLabel("Group Members (minimum 2)");
        membersLbl.setFont(Theme.fontSmallBold);
        membersLbl.setForeground(Theme.getText());
        membersLbl.setAlignmentX(LEFT_ALIGNMENT);

        membersModel = new DefaultListModel<>();
        if (existingGroup != null) {
            for (int i = 0; i < existingGroup.getMembers().size(); i++) {
                membersModel.addElement(existingGroup.getMembers().get(i).getName());
            }
        }

        membersList = new JList<>(membersModel);
        membersList.setFont(Theme.fontBody);
        membersList.setBackground(Theme.getRaised());
        membersList.setForeground(Theme.getText());
        membersList.setSelectionBackground(Theme.getSelectionBackground());
        membersList.setSelectionForeground(Theme.getText());

        JScrollPane scrollPane = new JScrollPane(membersList);
        scrollPane.setPreferredSize(new Dimension(400, 140));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        UIUtils.styleScrollPane(scrollPane);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);

        // Add Member Row
        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        addRow.setAlignmentX(LEFT_ALIGNMENT);

        newMemberField = UIUtils.createTextField();
        GlossyButton addMemberBtn = new GlossyButton("Add Member", false);
        addMemberBtn.setPreferredSize(new Dimension(110, 36));
        addMemberBtn.addActionListener(e -> handleAddMember());

        newMemberField.addActionListener(e -> handleAddMember());

        addRow.add(newMemberField, BorderLayout.CENTER);
        addRow.add(addMemberBtn, BorderLayout.EAST);

        // Remove Member Row
        JPanel removeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        removeRow.setOpaque(false);
        removeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        removeRow.setAlignmentX(LEFT_ALIGNMENT);

        GlossyButton removeMemberBtn = new GlossyButton("Remove Selected", false);
        removeMemberBtn.setPreferredSize(new Dimension(130, 32));
        removeMemberBtn.addActionListener(e -> handleRemoveMember());
        removeRow.add(removeMemberBtn);

        // Error message
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.fontSmall);
        errorLabel.setForeground(Theme.getOweRose());
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        actions.setAlignmentX(LEFT_ALIGNMENT);

        GlossyButton cancelBtn = new GlossyButton("Cancel", false);
        cancelBtn.addActionListener(e -> dispose());

        GlossyButton saveBtn = new GlossyButton(existingGroup == null ? "Create Group" : "Save Changes", true);
        saveBtn.addActionListener(e -> handleSave());

        actions.add(cancelBtn);
        actions.add(saveBtn);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(nameLbl);
        root.add(Box.createVerticalStrut(4));
        root.add(nameField);
        root.add(Box.createVerticalStrut(14));
        root.add(membersLbl);
        root.add(Box.createVerticalStrut(4));
        root.add(scrollPane);
        root.add(Box.createVerticalStrut(8));
        root.add(addRow);
        root.add(Box.createVerticalStrut(4));
        root.add(removeRow);
        root.add(Box.createVerticalStrut(10));
        root.add(errorLabel);
        root.add(Box.createVerticalStrut(10));
        root.add(actions);

        getContentPane().add(root);
    }

    private void handleAddMember() {
        errorLabel.setText(" ");
        String name = newMemberField.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        for (int i = 0; i < membersModel.size(); i++) {
            if (membersModel.get(i).equalsIgnoreCase(name)) {
                errorLabel.setText("Member '" + name + "' already exists in this list.");
                return;
            }
        }
        membersModel.addElement(name);
        newMemberField.setText("");
        newMemberField.requestFocus();
    }

    private void handleRemoveMember() {
        errorLabel.setText(" ");
        int selected = membersList.getSelectedIndex();
        if (selected == -1) {
            errorLabel.setText("Select a member from the list to remove.");
            return;
        }

        String memberName = membersModel.get(selected);

        // If editing existing group, check if member is part of any expenses
        if (existingGroup != null) {
            Member m = null;
            for (int i = 0; i < existingGroup.getMembers().size(); i++) {
                if (existingGroup.getMembers().get(i).getName().equalsIgnoreCase(memberName)) {
                    m = existingGroup.getMembers().get(i);
                    break;
                }
            }
            if (m != null && existingGroup.isMemberInExpenses(m.getId())) {
                errorLabel.setText("Cannot remove " + memberName + ": recorded in expenses.");
                return;
            }
        }

        membersModel.remove(selected);
    }

    private void handleSave() {
        errorLabel.setText(" ");
        String groupName = nameField.getText().trim();
        if (groupName.isEmpty()) {
            errorLabel.setText("Group name cannot be empty.");
            return;
        }
        if (membersModel.size() < 2) {
            errorLabel.setText("A group must have at least 2 members.");
            return;
        }

        if (existingGroup == null) {
            // Create new group
            String groupId = "grp_" + UUID.randomUUID().toString().substring(0, 8);
            Group group = new Group(groupId, currentUserId, groupName);
            for (int i = 0; i < membersModel.size(); i++) {
                String memberId = "mem_" + UUID.randomUUID().toString().substring(0, 8);
                group.getMembers().add(new Member(memberId, membersModel.get(i)));
            }
            dataStore.addGroup(group);
        } else {
            // Update existing group
            existingGroup.setName(groupName);

            // Add new members
            List<Member> updatedMembers = new ArrayList<>();
            for (int i = 0; i < membersModel.size(); i++) {
                String name = membersModel.get(i);
                Member existingMember = null;
                for (int j = 0; j < existingGroup.getMembers().size(); j++) {
                    if (existingGroup.getMembers().get(j).getName().equalsIgnoreCase(name)) {
                        existingMember = existingGroup.getMembers().get(j);
                        break;
                    }
                }
                if (existingMember != null) {
                    updatedMembers.add(existingMember);
                } else {
                    String newId = "mem_" + UUID.randomUUID().toString().substring(0, 8);
                    updatedMembers.add(new Member(newId, name));
                }
            }
            existingGroup.setMembers(updatedMembers);
            dataStore.updateGroup(existingGroup);
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}
