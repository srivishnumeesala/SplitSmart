package splitsmart.ui;

import splitsmart.model.Group;
import splitsmart.model.User;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window coordinating navigation, sidebar controls,
 * active screen swapping via CardLayout, and live theme updates.
 */
public class MainFrame extends JFrame {

    private final DataStore dataStore;

    // View names
    private static final String VIEW_LOGIN = "LOGIN";
    private static final String VIEW_DASHBOARD = "DASHBOARD";
    private static final String VIEW_GROUPS = "GROUPS";
    private static final String VIEW_GROUP_DETAIL = "GROUP_DETAIL";
    private static final String VIEW_TRANSACTIONS = "TRANSACTIONS";
    private static final String VIEW_ANALYTICS = "ANALYTICS";
    private static final String VIEW_SETTINGS = "SETTINGS";

    private JPanel rootContainer;
    private CardLayout rootCardLayout;

    // Authenticated Layout Components
    private JPanel appShell;
    private JPanel sidebar;
    private JPanel contentContainer;
    private CardLayout contentCardLayout;

    // Screen Panels
    private DashboardPanel dashboardPanel;
    private GroupsPanel groupsPanel;
    private GroupDetailPanel groupDetailPanel;
    private TransactionsPanel transactionsPanel;
    private AnalyticsPanel analyticsPanel;
    private SettingsPanel settingsPanel;
    private LoginPanel loginPanel;

    private String currentNav = VIEW_DASHBOARD;
    private final List<NavItemButton> navButtons = new ArrayList<>();
    private JLabel userDisplayLabel;
    private JLabel userHandleLabel;

    public MainFrame() {
        super("SplitSmart — Split expenses. Settle fairly. Stay friends.");
        this.dataStore = DataStore.getInstance();

        // Restore saved theme
        String savedTheme = dataStore.getSetting("theme", "LIGHT");
        Theme.isDark = "DARK".equalsIgnoreCase(savedTheme);

        initFrame();
    }

    private void initFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);

        rootCardLayout = new CardLayout();
        rootContainer = new JPanel(rootCardLayout);

        // 1. Login Screen
        loginPanel = new LoginPanel(dataStore, this::handleLoginSuccess);
        rootContainer.add(loginPanel, VIEW_LOGIN);

        // 2. App Shell Screen
        buildAppShell();
        rootContainer.add(appShell, "APP");

        getContentPane().add(rootContainer);

        // Check if user is already authenticated
        if (dataStore.getCurrentUser() != null) {
            rootCardLayout.show(rootContainer, "APP");
            navigateTo(VIEW_DASHBOARD);
        } else {
            rootCardLayout.show(rootContainer, VIEW_LOGIN);
        }
    }

    private void buildAppShell() {
        appShell = new JPanel(new BorderLayout());
        appShell.setBackground(Theme.getBackground());

        // Left Sidebar (230 px)
        sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.getCard());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.getBorder());
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(230, getHeight()));
        sidebar.setOpaque(false);

        // Sidebar Top: Wordmark & Slogan
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(24, 20, 20, 20));

        JLabel logoLbl = new JLabel("SplitSmart");
        logoLbl.setFont(Theme.fontTitle);
        logoLbl.setForeground(Theme.getAccent());
        logoLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sloganLbl = new JLabel("Split expenses fairly");
        sloganLbl.setFont(Theme.fontSmall);
        sloganLbl.setForeground(Theme.getMuted());
        sloganLbl.setAlignmentX(LEFT_ALIGNMENT);

        brandPanel.add(logoLbl);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(sloganLbl);
        brandPanel.add(Box.createVerticalStrut(20));

        // Sidebar Middle: Nav Buttons
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);

        navButtons.clear();
        addNavButton(navList, "Dashboard", VIEW_DASHBOARD);
        addNavButton(navList, "My Groups", VIEW_GROUPS);
        addNavButton(navList, "Transactions", VIEW_TRANSACTIONS);
        addNavButton(navList, "Analytics", VIEW_ANALYTICS);
        addNavButton(navList, "Settings", VIEW_SETTINGS);

        brandPanel.add(navList);
        sidebar.add(brandPanel, BorderLayout.NORTH);

        // Sidebar Bottom: User Info & Logout
        JPanel bottomUserPanel = new JPanel();
        bottomUserPanel.setLayout(new BoxLayout(bottomUserPanel, BoxLayout.Y_AXIS));
        bottomUserPanel.setOpaque(false);
        bottomUserPanel.setBorder(new EmptyBorder(16, 16, 20, 16));

        userDisplayLabel = new JLabel("User");
        userDisplayLabel.setFont(Theme.fontBodyBold);
        userDisplayLabel.setForeground(Theme.getText());
        userDisplayLabel.setAlignmentX(LEFT_ALIGNMENT);

        userHandleLabel = new JLabel("@user");
        userHandleLabel.setFont(Theme.fontSmall);
        userHandleLabel.setForeground(Theme.getMuted());
        userHandleLabel.setAlignmentX(LEFT_ALIGNMENT);

        GlossyButton logoutBtn = new GlossyButton("Log Out", false);
        logoutBtn.setPreferredSize(new Dimension(190, 32));
        logoutBtn.setAlignmentX(LEFT_ALIGNMENT);
        logoutBtn.addActionListener(e -> handleLogout());

        bottomUserPanel.add(userDisplayLabel);
        bottomUserPanel.add(Box.createVerticalStrut(2));
        bottomUserPanel.add(userHandleLabel);
        bottomUserPanel.add(Box.createVerticalStrut(10));
        bottomUserPanel.add(logoutBtn);

        sidebar.add(bottomUserPanel, BorderLayout.SOUTH);
        appShell.add(sidebar, BorderLayout.WEST);

        // Right Content Area
        contentCardLayout = new CardLayout();
        contentContainer = new JPanel(contentCardLayout);
        contentContainer.setOpaque(false);

        // Initialize screens
        dashboardPanel = new DashboardPanel(dataStore, new DashboardPanel.NavigationListener() {
            @Override
            public void openGroupDetail(Group group) {
                showGroupDetail(group);
            }

            @Override
            public void openGroups() {
                navigateTo(VIEW_GROUPS);
            }
        });

        groupsPanel = new GroupsPanel(dataStore, this::showGroupDetail);
        transactionsPanel = new TransactionsPanel(dataStore);
        analyticsPanel = new AnalyticsPanel(dataStore);
        settingsPanel = new SettingsPanel(dataStore, new SettingsPanel.SettingsListener() {
            @Override
            public void onThemeToggled() {
                applyThemeChanges();
            }

            @Override
            public void onLogout() {
                handleLogout();
            }

            @Override
            public void onDataChanged() {
                refreshAllScreens();
            }
        });

        contentContainer.add(dashboardPanel, VIEW_DASHBOARD);
        contentContainer.add(groupsPanel, VIEW_GROUPS);
        contentContainer.add(transactionsPanel, VIEW_TRANSACTIONS);
        contentContainer.add(analyticsPanel, VIEW_ANALYTICS);
        contentContainer.add(settingsPanel, VIEW_SETTINGS);

        appShell.add(contentContainer, BorderLayout.CENTER);
    }

    private void addNavButton(JPanel container, String text, String viewKey) {
        NavItemButton btn = new NavItemButton(text, viewKey);
        navButtons.add(btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(6));
    }

    private void navigateTo(String viewKey) {
        currentNav = viewKey;
        contentCardLayout.show(contentContainer, viewKey);

        for (int i = 0; i < navButtons.size(); i++) {
            navButtons.get(i).repaint();
        }

        if (VIEW_DASHBOARD.equals(viewKey)) {
            dashboardPanel.refreshUI();
        } else if (VIEW_GROUPS.equals(viewKey)) {
            groupsPanel.refreshUI();
        } else if (VIEW_TRANSACTIONS.equals(viewKey)) {
            transactionsPanel.refreshUI();
        } else if (VIEW_ANALYTICS.equals(viewKey)) {
            analyticsPanel.refreshUI();
        } else if (VIEW_SETTINGS.equals(viewKey)) {
            settingsPanel.initUI();
        }
    }

    public void showGroupDetail(Group group) {
        if (groupDetailPanel != null) {
            contentContainer.remove(groupDetailPanel);
        }
        groupDetailPanel = new GroupDetailPanel(dataStore, group, () -> navigateTo(VIEW_GROUPS));
        contentContainer.add(groupDetailPanel, VIEW_GROUP_DETAIL);
        currentNav = VIEW_GROUPS;
        for (int i = 0; i < navButtons.size(); i++) {
            navButtons.get(i).repaint();
        }
        contentCardLayout.show(contentContainer, VIEW_GROUP_DETAIL);
    }

    private void handleLoginSuccess(User user) {
        userDisplayLabel.setText(user.getDisplayName());
        userHandleLabel.setText("@" + user.getUsername());
        rootCardLayout.show(rootContainer, "APP");
        navigateTo(VIEW_DASHBOARD);
    }

    private void handleLogout() {
        dataStore.setCurrentUser(null);
        loginPanel = new LoginPanel(dataStore, this::handleLoginSuccess);
        rootContainer.add(loginPanel, VIEW_LOGIN);
        rootCardLayout.show(rootContainer, VIEW_LOGIN);
    }

    private void refreshAllScreens() {
        dashboardPanel.refreshUI();
        groupsPanel.refreshUI();
        transactionsPanel.refreshUI();
        analyticsPanel.refreshUI();
    }

    private void applyThemeChanges() {
        getContentPane().setBackground(Theme.getBackground());
        appShell.setBackground(Theme.getBackground());
        loginPanel.setBackground(Theme.getBackground());
        sidebar.repaint();
        refreshAllScreens();
        SwingUtilities.updateComponentTreeUI(this);
    }

    /**
     * Custom navigation item button with soft amber active highlight and hover state.
     */
    private class NavItemButton extends JPanel {
        private final String text;
        private final String viewKey;
        private boolean hovered = false;

        NavItemButton(String text, String viewKey) {
            this.text = text;
            this.viewKey = viewKey;
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(190, 36));
            setMaximumSize(new Dimension(190, 36));
            setAlignmentX(LEFT_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    navigateTo(viewKey);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean active = viewKey.equals(currentNav);
            int w = getWidth();
            int h = getHeight();

            if (active) {
                // Soft amber highlight for active item
                g2.setColor(Theme.getSelectionBackground());
                g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 8, 8));

                // Left active amber bar
                g2.setColor(Theme.getAccent());
                g2.fill(new RoundRectangle2D.Float(2, 6, 3, h - 12, 2, 2));

                g2.setColor(Theme.getAccentText());
                g2.setFont(Theme.fontBodyBold);
            } else if (hovered) {
                g2.setColor(Theme.getRaised());
                g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 8, 8));
                g2.setColor(Theme.getText());
                g2.setFont(Theme.fontBody);
            } else {
                g2.setColor(Theme.getMuted());
                g2.setFont(Theme.fontBody);
            }

            g2.drawString(text, 16, (h / 2) + 5);
            g2.dispose();
        }
    }
}
