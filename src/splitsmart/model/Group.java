package splitsmart.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an expense-sharing group owned by a specific user.
 */
public class Group {
    private String id;
    private String ownerUserId;
    private String name;
    private List<Member> members;
    private List<Expense> expenses;

    public Group(String id, String ownerUserId, String name) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.members = new ArrayList<>();
        this.expenses = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        if (members != null) {
            this.members = members;
        } else {
            this.members = new ArrayList<>();
        }
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        if (expenses != null) {
            this.expenses = expenses;
        } else {
            this.expenses = new ArrayList<>();
        }
    }

    /**
     * Finds a member within this group by their unique identifier.
     */
    public Member findMemberById(String memberId) {
        if (memberId == null) {
            return null;
        }
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            if (memberId.equals(m.getId())) {
                return m;
            }
        }
        return null;
    }

    /**
     * Checks if a member appears as a payer or sharer in any group expense.
     */
    public boolean isMemberInExpenses(String memberId) {
        if (memberId == null) {
            return false;
        }
        for (int i = 0; i < expenses.size(); i++) {
            Expense e = expenses.get(i);
            if (memberId.equals(e.getPayerMemberId())) {
                return true;
            }
            if (e.getShares() != null && e.getShares().containsKey(memberId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}
