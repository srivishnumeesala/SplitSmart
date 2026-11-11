package splitsmart.ui;

import splitsmart.model.User;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Settings screen providing Light/Dark theme toggling, demo data creation,
 * account data reset, and user logout.
 */
public class SettingsPanel extends JPanel {

    public interface SettingsListener {
        void onThemeToggled();
        void onLogout();
        void onDataChanged();
    }

    private final DataStore dataStore;
    private final SettingsListener settingsListener;

    public SettingsPanel(DataStore dataStore, SettingsListener settingsListener) {
        this.dataStore = dataStore;
        this.settingsListener = settingsListener;
        setLayout(new BorderLayout());
        setOpaque(false);
        initUI();
    }

    public void initUI() {
        removeAll();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Header Row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel titleLbl = new JLabel("Settings");
        titleLbl.setFont(Theme.fontTitle);
        titleLbl.setForeground(Theme.getText());
        headerRow.add(titleLbl, BorderLayout.WEST);
        root.add(headerRow);
        root.add(Box.createVerticalStrut(18));

        User user = dataStore.getCurrentUser();

        // 1. Theme Card
        GlossyPanel themeCard = new GlossyPanel();
        themeCard.setLayout(new BorderLayout(0, 10));
        themeCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        themeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        themeCard.setPreferredSize(new Dimension(800, 90));

        JPanel themeText = new JPanel();
        themeText.setLayout(new BoxLayout(themeText, BoxLayout.Y_AXIS));
        themeText.setOpaque(false);

        JLabel themeTitle = new JLabel("Visual Appearance");
        themeTitle.setFont(Theme.fontSubtitle);
        themeTitle.setForeground(Theme.getText());

        JLabel themeDesc = new JLabel("Switch between Warm Amber Light and Dark themes.");
        themeDesc.setFont(Theme.fontSmall);
        themeDesc.setForeground(Theme.getMuted());

        themeText.add(themeTitle);
        themeText.add(Box.createVerticalStrut(4));
        themeText.add(themeDesc);

        GlossyButton toggleThemeBtn = new GlossyButton(Theme.isDark ? "Switch to Light Mode" : "Switch to Dark Mode", false);
        toggleThemeBtn.setPreferredSize(new Dimension(170, 36));
        toggleThemeBtn.addActionListener(e -> {
            Theme.isDark = !Theme.isDark;
            dataStore.setSetting("theme", Theme.isDark ? "DARK" : "LIGHT");
            if (settingsListener != null) {
                settingsListener.onThemeToggled();
            }
            initUI();
        });

        themeCard.add(themeText, BorderLayout.WEST);
        themeCard.add(toggleThemeBtn, BorderLayout.EAST);
        root.add(themeCard);
        root.add(Box.createVerticalStrut(16));

        // 2. Demo Data Card
        GlossyPanel demoCard = new GlossyPanel();
        demoCard.setLayout(new BorderLayout(0, 10));
        demoCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        demoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        demoCard.setPreferredSize(new Dimension(800, 95));

        JPanel demoText = new JPanel();
        demoText.setLayout(new BoxLayout(demoText, BoxLayout.Y_AXIS));
        demoText.setOpaque(false);

        JLabel demoTitle = new JLabel("Sample Demo Data");
        demoTitle.setFont(Theme.fontSubtitle);
        demoTitle.setForeground(Theme.getText());

        JLabel demoDesc = new JLabel("Populate a Goa Trip group with Aman, Bina, Charan, and Diya to test settlements.");
        demoDesc.setFont(Theme.fontSmall);
        demoDesc.setForeground(Theme.getMuted());

        demoText.add(demoTitle);
        demoText.add(Box.createVerticalStrut(4));
        demoText.add(demoDesc);

        GlossyButton loadDemoBtn = new GlossyButton("Load Demo Data", true);
        loadDemoBtn.setPreferredSize(new Dimension(150, 36));
        loadDemoBtn.addActionListener(e -> {
            if (user != null) {
                dataStore.loadDemoData(user.getId());
                JOptionPane.showMessageDialog(this,
                        "Demo data loaded successfully with members: Aman, Bina, Charan, Diya.",
                        "Demo Data Created",
                        JOptionPane.INFORMATION_MESSAGE);
                if (settingsListener != null) {
                    settingsListener.onDataChanged();
                }
            }
        });

        demoCard.add(demoText, BorderLayout.WEST);
        demoCard.add(loadDemoBtn, BorderLayout.EAST);
        root.add(demoCard);
        root.add(Box.createVerticalStrut(16));

        // 3. Reset All Data Card
        GlossyPanel resetCard = new GlossyPanel();
        resetCard.setLayout(new BorderLayout(0, 10));
        resetCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        resetCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        resetCard.setPreferredSize(new Dimension(800, 95));

        JPanel resetText = new JPanel();
        resetText.setLayout(new BoxLayout(resetText, BoxLayout.Y_AXIS));
        resetText.setOpaque(false);

        JLabel resetTitle = new JLabel("Delete All My Data");
        resetTitle.setFont(Theme.fontSubtitle);
        resetTitle.setForeground(Theme.getOweRose());

        JLabel resetDesc = new JLabel("Permanently delete all your groups, members, and recorded expenses.");
        resetDesc.setFont(Theme.fontSmall);
        resetDesc.setForeground(Theme.getMuted());

        resetText.add(resetTitle);
        resetText.add(Box.createVerticalStrut(4));
        resetText.add(resetDesc);

        GlossyButton deleteDataBtn = new GlossyButton("Delete Data", false);
        deleteDataBtn.setPreferredSize(new Dimension(120, 36));
        deleteDataBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete ALL groups and expenses? This cannot be undone.",
                    "Confirm Delete All Data",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                if (user != null) {
                    dataStore.deleteAllUserData(user.getId());
                    JOptionPane.showMessageDialog(this, "All user data has been cleared.");
                    if (settingsListener != null) {
                        settingsListener.onDataChanged();
                    }
                }
            }
        });

        resetCard.add(resetText, BorderLayout.WEST);
        resetCard.add(deleteDataBtn, BorderLayout.EAST);
        root.add(resetCard);
        root.add(Box.createVerticalStrut(16));

        // 4. Account Information & Logout Card
        GlossyPanel accountCard = new GlossyPanel();
        accountCard.setLayout(new BorderLayout(0, 10));
        accountCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        accountCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        accountCard.setPreferredSize(new Dimension(800, 90));

        JPanel accText = new JPanel();
        accText.setLayout(new BoxLayout(accText, BoxLayout.Y_AXIS));
        accText.setOpaque(false);

        String accName = user != null ? user.getDisplayName() + " (@" + user.getUsername() + ")" : "Logged out";
        JLabel accTitle = new JLabel("Signed in as: " + accName);
        accTitle.setFont(Theme.fontSubtitle);
        accTitle.setForeground(Theme.getText());

        JLabel accDesc = new JLabel("End your current session safely.");
        accDesc.setFont(Theme.fontSmall);
        accDesc.setForeground(Theme.getMuted());

        accText.add(accTitle);
        accText.add(Box.createVerticalStrut(4));
        accText.add(accDesc);

        GlossyButton logoutBtn = new GlossyButton("Log Out", false);
        logoutBtn.setPreferredSize(new Dimension(100, 36));
        logoutBtn.addActionListener(e -> {
            if (settingsListener != null) {
                settingsListener.onLogout();
            }
        });

        accountCard.add(accText, BorderLayout.WEST);
        accountCard.add(logoutBtn, BorderLayout.EAST);
        root.add(accountCard);

        JScrollPane scroll = new JScrollPane(root);
        UIUtils.styleScrollPane(scroll);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        revalidate();
        repaint();
    }
}
