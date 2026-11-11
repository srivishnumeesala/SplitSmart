package splitsmart.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single shared expense recorded within a group.
 */
public class Expense {
    private String id;
    private String groupId;
    private String description;
    private long amountPaise;
    private LocalDate date;
    private String payerMemberId;
    private Category category;
    private SplitType splitType;
    // Map of member ID to their share in paise
    private Map<String, Long> shares;

    public Expense(String id, String groupId, String description, long amountPaise,
                   LocalDate date, String payerMemberId, Category category,
                   SplitType splitType, Map<String, Long> shares) {
        this.id = id;
        this.groupId = groupId;
        this.description = description;
        this.amountPaise = amountPaise;
        this.date = date;
        this.payerMemberId = payerMemberId;
        this.category = category;
        this.splitType = splitType;
        if (shares != null) {
            this.shares = new HashMap<>(shares);
        } else {
            this.shares = new HashMap<>();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getPayerMemberId() {
        return payerMemberId;
    }

    public void setPayerMemberId(String payerMemberId) {
        this.payerMemberId = payerMemberId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public Map<String, Long> getShares() {
        return shares;
    }

    public void setShares(Map<String, Long> shares) {
        if (shares != null) {
            this.shares = new HashMap<>(shares);
        } else {
            this.shares = new HashMap<>();
        }
    }
}
