package splitsmart.model;

/**
 * Represents a suggested money transfer between two members to settle balances.
 */
public class Transfer {
    private Member fromMember;
    private Member toMember;
    private long amountPaise;

    public Transfer(Member fromMember, Member toMember, long amountPaise) {
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.amountPaise = amountPaise;
    }

    public Member getFromMember() {
        return fromMember;
    }

    public void setFromMember(Member fromMember) {
        this.fromMember = fromMember;
    }

    public Member getToMember() {
        return toMember;
    }

    public void setToMember(Member toMember) {
        this.toMember = toMember;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(long amountPaise) {
        this.amountPaise = amountPaise;
    }
}
