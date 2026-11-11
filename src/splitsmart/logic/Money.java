package splitsmart.logic;

/**
 * Utility class for monetary calculations and formatting.
 * All monetary values are represented internally as long integers in paise (1 Rupee = 100 paise).
 * Floating-point arithmetic (float/double) is strictly avoided to prevent rounding errors.
 */
public class Money {

    private Money() {
        // Utility class
    }

    /**
     * Formats an amount in paise to a human-readable Rupee string.
     * Uses Indian number grouping (e.g. ₹1,284.50, ₹12,34,567.89).
     */
    public static String format(long paise) {
        boolean negative = paise < 0;
        long absPaise = Math.abs(paise);
        long rupees = absPaise / 100;
        long paiseRemainder = absPaise % 100;

        String groupedRupees = formatIndianGrouping(rupees);
        String paiseStr = paiseRemainder < 10 ? "0" + paiseRemainder : String.valueOf(paiseRemainder);

        if (negative) {
            return "-₹" + groupedRupees + "." + paiseStr;
        } else {
            return "₹" + groupedRupees + "." + paiseStr;
        }
    }

    /**
     * Formats paise as a clean numeric string with 2 decimal places (no currency symbol).
     * Useful for pre-filling input fields when editing expenses.
     */
    public static String formatPlain(long paise) {
        boolean negative = paise < 0;
        long absPaise = Math.abs(paise);
        long rupees = absPaise / 100;
        long paiseRemainder = absPaise % 100;
        String paiseStr = paiseRemainder < 10 ? "0" + paiseRemainder : String.valueOf(paiseRemainder);
        return (negative ? "-" : "") + rupees + "." + paiseStr;
    }

    /**
     * Converts a user-entered text string (e.g. "1284.50", "₹1,284.50", "500") to paise.
     * Rejects invalid input with an IllegalArgumentException.
     */
    public static long parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        String clean = text.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be empty");
        }

        // Strip currency symbols and formatting commas
        clean = clean.replace("₹", "").replace("Rs", "").replace("rs", "").replace(",", "").trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Amount is invalid");
        }

        boolean negative = false;
        if (clean.startsWith("-")) {
            negative = true;
            clean = clean.substring(1).trim();
        } else if (clean.startsWith("+")) {
            clean = clean.substring(1).trim();
        }

        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Amount contains no numbers");
        }

        int dotIndex = clean.indexOf('.');
        String rupeePart;
        String paisePart;

        if (dotIndex == -1) {
            rupeePart = clean;
            paisePart = "00";
        } else {
            rupeePart = clean.substring(0, dotIndex).trim();
            paisePart = clean.substring(dotIndex + 1).trim();

            // Verify no other dots
            if (paisePart.indexOf('.') != -1) {
                throw new IllegalArgumentException("Invalid amount with multiple decimal points");
            }
        }

        if (rupeePart.isEmpty()) {
            rupeePart = "0";
        }

        if (paisePart.isEmpty()) {
            paisePart = "00";
        } else if (paisePart.length() == 1) {
            paisePart = paisePart + "0";
        } else if (paisePart.length() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }

        // Validate all characters are digits
        for (int i = 0; i < rupeePart.length(); i++) {
            char c = rupeePart.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Invalid character in amount: " + c);
            }
        }
        for (int i = 0; i < paisePart.length(); i++) {
            char c = paisePart.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Invalid character in paise: " + c);
            }
        }

        long rupees;
        try {
            rupees = Long.parseLong(rupeePart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is too large");
        }

        long paiseValue = Long.parseLong(paisePart);
        long totalPaise = rupees * 100L + paiseValue;

        return negative ? -totalPaise : totalPaise;
    }

    /**
     * Formats integer rupees according to the Indian numbering system.
     */
    private static String formatIndianGrouping(long rupees) {
        String s = String.valueOf(rupees);
        if (s.length() <= 3) {
            return s;
        }

        // Last 3 digits
        String lastThree = s.substring(s.length() - 3);
        String remaining = s.substring(0, s.length() - 3);

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = remaining.length() - 1; i >= 0; i--) {
            sb.append(remaining.charAt(i));
            count++;
            if (count % 2 == 0 && i > 0) {
                sb.append(',');
            }
        }

        return sb.reverse().toString() + "," + lastThree;
    }
}
