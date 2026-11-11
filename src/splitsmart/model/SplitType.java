package splitsmart.model;

/**
 * Defines the type of expense split among group members.
 */
public enum SplitType {
    EQUAL("Equal Split"),
    EXACT("Exact Amounts"),
    PERCENTAGE("Percentage Split");

    private final String displayName;

    SplitType(String displayName) {
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
