package splitsmart.logic;

import splitsmart.model.Expense;
import splitsmart.model.Group;
import splitsmart.model.Member;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates member balances from the list of expenses.
 * Balances are never persisted directly; they are always dynamically derived
 * so additions, edits, and deletions of expenses are immediately reflected.
 */
public class BalanceCalculator {

    /**
     * Holds the calculated balance summary for a single member.
     */
    public static class MemberBalance {
        private final Member member;
        private final long paidPaise;
        private final long sharePaise;
        private final long netPaise;

        public MemberBalance(Member member, long paidPaise, long sharePaise, long netPaise) {
            this.member = member;
            this.paidPaise = paidPaise;
            this.sharePaise = sharePaise;
            this.netPaise = netPaise;
        }

        public Member getMember() {
            return member;
        }

        public long getPaidPaise() {
            return paidPaise;
        }

        public long getSharePaise() {
            return sharePaise;
        }

        public long getNetPaise() {
            return netPaise;
        }
    }

    private BalanceCalculator() {
        // Utility class
    }

    /**
     * Calculates the balance summary (paid, share, net) for each member in a group.
     * The sum of all net balances will always equal zero.
     */
    public static List<MemberBalance> calculateGroupBalances(Group group) {
        if (group == null) {
            return new ArrayList<>();
        }
        return calculateBalances(group.getMembers(), group.getExpenses());
    }

    /**
     * Calculates the balance summary for a specified list of members and expenses.
     */
    public static List<MemberBalance> calculateBalances(List<Member> members, List<Expense> expenses) {
        List<MemberBalance> result = new ArrayList<>();
        if (members == null || members.isEmpty()) {
            return result;
        }

        Map<String, Long> paidMap = new HashMap<>();
        Map<String, Long> shareMap = new HashMap<>();

        // Initialize maps for all members
        for (int i = 0; i < members.size(); i++) {
            String mid = members.get(i).getId();
            paidMap.put(mid, 0L);
            shareMap.put(mid, 0L);
        }

        // Tally paid amounts and shared amounts from all expenses
        if (expenses != null) {
            for (int i = 0; i < expenses.size(); i++) {
                Expense expense = expenses.get(i);
                String payerId = expense.getPayerMemberId();
                if (payerId != null && paidMap.containsKey(payerId)) {
                    long currentPaid = paidMap.get(payerId);
                    paidMap.put(payerId, currentPaid + expense.getAmountPaise());
                }

                Map<String, Long> shares = expense.getShares();
                if (shares != null) {
                    for (Map.Entry<String, Long> entry : shares.entrySet()) {
                        String memberId = entry.getKey();
                        Long share = entry.getValue();
                        if (memberId != null && share != null && shareMap.containsKey(memberId)) {
                            long currentShare = shareMap.get(memberId);
                            shareMap.put(memberId, currentShare + share);
                        }
                    }
                }
            }
        }

        // Build result list
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            long paid = paidMap.get(m.getId());
            long share = shareMap.get(m.getId());
            long net = paid - share;
            result.add(new MemberBalance(m, paid, share, net));
        }

        return result;
    }
}
