package splitsmart.model;

/**
 * Categories for classifying shared expenses.
 */
public enum Category {
    FOOD("Food & Dining"),
    GROCERIES("Groceries"),
    TRAVEL("Travel & Transport"),
    ENTERTAINMENT("Entertainment"),
    UTILITIES("Utilities & Bills"),
    RENT("Rent & Stay"),
    SETTLEMENT("Settlement"),
    OTHER("Other");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
