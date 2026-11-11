package splitsmart.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Validates user input across forms and actions to ensure data integrity.
 */
public class Validator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Validator() {
        // Utility class
    }

    /**
     * Validates that a string is not null or whitespace only.
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Validates username requirements: non-empty, 3-20 chars, alphanumeric or underscore.
     */
    public static String validateUsername(String username) {
        if (!isNotEmpty(username)) {
            return "Username cannot be empty.";
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3 || trimmed.length() > 20) {
            return "Username must be between 3 and 20 characters.";
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_';
            if (!valid) {
                return "Username can only contain letters, numbers, and underscores.";
            }
        }
        return null;
    }

    /**
     * Validates password requirements: minimum 6 characters.
     */
    public static String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters long.";
        }
        return null;
    }

    /**
     * Validates display name requirements.
     */
    public static String validateDisplayName(String name) {
        if (!isNotEmpty(name)) {
            return "Display name cannot be empty.";
        }
        if (name.trim().length() > 50) {
            return "Display name is too long (max 50 characters).";
        }
        return null;
    }

    /**
     * Validates a date string in yyyy-MM-dd format.
     */
    public static LocalDate parseAndValidateDate(String dateText) {
        if (!isNotEmpty(dateText)) {
            throw new IllegalArgumentException("Date cannot be empty.");
        }
        try {
            return LocalDate.parse(dateText.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy-MM-dd (e.g. 2026-11-11).");
        }
    }

    /**
     * Formats a LocalDate to standard yyyy-MM-dd string.
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * Validates that total shares in paise sum up exactly to the total expense amount.
     */
    public static String validateSharesSum(long totalAmountPaise, Map<String, Long> shares) {
        if (shares == null || shares.isEmpty()) {
            return "At least one member must share the expense.";
        }
        long sum = 0;
        for (Long share : shares.values()) {
            if (share == null || share < 0) {
                return "Individual shares cannot be negative.";
            }
            sum += share;
        }
        if (sum != totalAmountPaise) {
            long diff = totalAmountPaise - sum;
            return "Shares sum (" + Money.format(sum) + ") does not equal total amount ("
                    + Money.format(totalAmountPaise) + "). Difference: " + Money.format(diff) + ".";
        }
        return null;
    }
}
