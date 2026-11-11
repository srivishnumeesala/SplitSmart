package splitsmart.ui;

import splitsmart.logic.Validator;
import splitsmart.model.User;
import splitsmart.storage.DataStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Authentication screen supporting user login and registration with validation
 * and salted SHA-256 hashed password storage.
 */
public class LoginPanel extends JPanel {

    public interface LoginListener {
        void onLoginSuccess(User user);
    }

    private final LoginListener loginListener;
    private final DataStore dataStore;

    private boolean isRegisterMode = false;

    private JTextField displayNameField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JLabel errorLabel;
    private JPanel fieldsPanel;
    private GlossyButton submitButton;
    private JLabel toggleModeLabel;

    public LoginPanel(DataStore dataStore, LoginListener loginListener) {
        this.dataStore = dataStore;
        this.loginListener = loginListener;

        setLayout(new GridBagLayout());
        setBackground(Theme.getBackground());

        initUI();
    }

    private void initUI() {
        removeAll();

        // Main card panel
        GlossyPanel card = new GlossyPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(32, 36, 32, 36));
        card.setPreferredSize(new Dimension(380, isRegisterMode ? 480 : 380));

        // Brand Wordmark & Slogan
        JLabel titleLabel = new JLabel("SplitSmart");
        titleLabel.setFont(Theme.fontTitle);
        titleLabel.setForeground(Theme.getAccent());
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sloganLabel = new JLabel("Split expenses. Settle fairly. Stay friends.");
        sloganLabel.setFont(Theme.fontSmall);
        sloganLabel.setForeground(Theme.getMuted());
        sloganLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Error message label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.fontSmall);
        errorLabel.setForeground(Theme.getOweRose());
        errorLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Fields panel
        fieldsPanel = new JPanel();
        fieldsPanel.setOpaque(false);
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleSubmit();
                }
            }
        };

        if (isRegisterMode) {
            JLabel nameLbl = createFieldLabel("Display Name");
            displayNameField = UIUtils.createTextField();
            displayNameField.addKeyListener(enterSubmit);

            JLabel userLbl = createFieldLabel("Username");
            usernameField = UIUtils.createTextField();
            usernameField.addKeyListener(enterSubmit);

            JLabel passLbl = createFieldLabel("Password (min 6 chars)");
            passwordField = createPasswordField();
            passwordField.addKeyListener(enterSubmit);

            JLabel confirmLbl = createFieldLabel("Confirm Password");
            confirmPasswordField = createPasswordField();
            confirmPasswordField.addKeyListener(enterSubmit);

            fieldsPanel.add(nameLbl);
            fieldsPanel.add(Box.createVerticalStrut(4));
            fieldsPanel.add(displayNameField);
            fieldsPanel.add(Box.createVerticalStrut(10));
            fieldsPanel.add(userLbl);
            fieldsPanel.add(Box.createVerticalStrut(4));
            fieldsPanel.add(usernameField);
            fieldsPanel.add(Box.createVerticalStrut(10));
            fieldsPanel.add(passLbl);
            fieldsPanel.add(Box.createVerticalStrut(4));
            fieldsPanel.add(passwordField);
            fieldsPanel.add(Box.createVerticalStrut(10));
            fieldsPanel.add(confirmLbl);
            fieldsPanel.add(Box.createVerticalStrut(4));
            fieldsPanel.add(confirmPasswordField);
        } else {
            JLabel userLbl = createFieldLabel("Username");
            usernameField = UIUtils.createTextField();
            usernameField.addKeyListener(enterSubmit);

            JLabel passLbl = createFieldLabel("Password");
            passwordField = createPasswordField();
            passwordField.addKeyListener(enterSubmit);

            fieldsPanel.add(userLbl);
            fieldsPanel.add(Box.createVerticalStrut(4));
            fieldsPanel.add(usernameField);
            fieldsPanel.add(Box.createVerticalStrut(12));
            fieldsPanel.add(passLbl);
            fieldsPanel.add(Box.createVerticalStrut(4));
            fieldsPanel.add(passwordField);
        }

        // Submit Button
        submitButton = new GlossyButton(isRegisterMode ? "Create Account" : "Log In", true);
        submitButton.setAlignmentX(CENTER_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(380, 40));
        submitButton.addActionListener(e -> handleSubmit());

        // Toggle link
        toggleModeLabel = new JLabel(isRegisterMode ? "Already have an account? Log In" : "Don't have an account? Sign Up");
        toggleModeLabel.setFont(Theme.fontSmall);
        toggleModeLabel.setForeground(Theme.getAccentText());
        toggleModeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleModeLabel.setAlignmentX(CENTER_ALIGNMENT);
        toggleModeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isRegisterMode = !isRegisterMode;
                initUI();
                revalidate();
                repaint();
            }
        });

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(sloganLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(fieldsPanel);
        card.add(Box.createVerticalStrut(8));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(submitButton);
        card.add(Box.createVerticalStrut(12));
        card.add(toggleModeLabel);

        add(card);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.fontSmallBold);
        lbl.setForeground(Theme.getText());
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JPasswordField createPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setOpaque(true);
        pf.setBackground(Theme.getCard());
        pf.setForeground(Theme.getText());
        pf.setCaretColor(Theme.getAccent());
        pf.setFont(Theme.fontBody);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.getBorder(), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        pf.setPreferredSize(new Dimension(200, 38));
        return pf;
    }

    private void handleSubmit() {
        errorLabel.setText(" ");
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (isRegisterMode) {
            String displayName = displayNameField.getText().trim();
            String confirm = new String(confirmPasswordField.getPassword());

            String nameErr = Validator.validateDisplayName(displayName);
            if (nameErr != null) {
                errorLabel.setText(nameErr);
                return;
            }

            String userErr = Validator.validateUsername(username);
            if (userErr != null) {
                errorLabel.setText(userErr);
                return;
            }

            String passErr = Validator.validatePassword(password);
            if (passErr != null) {
                errorLabel.setText(passErr);
                return;
            }

            if (!password.equals(confirm)) {
                errorLabel.setText("Passwords do not match.");
                return;
            }

            try {
                User user = dataStore.registerUser(username, displayName, password);
                dataStore.setCurrentUser(user);
                if (loginListener != null) {
                    loginListener.onLoginSuccess(user);
                }
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage());
            }
        } else {
            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter username and password.");
                return;
            }

            User user = dataStore.authenticate(username, password);
            if (user == null) {
                errorLabel.setText("Invalid username or password.");
                return;
            }

            dataStore.setCurrentUser(user);
            if (loginListener != null) {
                loginListener.onLoginSuccess(user);
            }
        }
    }
}
