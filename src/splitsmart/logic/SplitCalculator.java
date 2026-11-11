package splitsmart.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates expense distributions among members for Equal, Exact, and Percentage splits.
 * Ensures that the sum of individual shares always matches the total expense amount to the exact paisa.
 */
public class SplitCalculator {

    private SplitCalculator() {
        // Utility class
    }

    /**
     * Splits an amount equally among a list of members.
     * Leftover paise (amount % n) are distributed one paisa each to the first members
     * so that the sum of all shares exactly equals the total amount.
     */
    public static Map<String, Long> calculateEqualSplit(long totalPaise, List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException("No members selected for equal split.");
        }
        if (totalPaise <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
        }

        int count = memberIds.size();
        long baseShare = totalPaise / count;
        long remainder = totalPaise % count;

        Map<String, Long> shares = new HashMap<>();
        for (int i = 0; i < count; i++) {
            long share = baseShare + (i < remainder ? 1 : 0);
            shares.put(memberIds.get(i), share);
        }
        return shares;
    }

    /**
     * Splits an amount based on percentage allocations (expressed in basis points: 1% = 100 basis points, 100% = 10000).
     * Percentages must sum to exactly 10000 (100.00%).
     * Leftover rounding paise are distributed to the first members to guarantee exact sum.
     */
    public static Map<String, Long> calculatePercentageSplit(long totalPaise, List<String> memberIds, Map<String, Integer> basisPointsMap) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException("No members selected for percentage split.");
        }
        if (basisPointsMap == null || basisPointsMap.isEmpty()) {
            throw new IllegalArgumentException("Percentages map cannot be empty.");
        }
        if (totalPaise <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
        }

        int totalBasisPoints = 0;
        for (int i = 0; i < memberIds.size(); i++) {
            String mid = memberIds.get(i);
            Integer bp = basisPointsMap.get(mid);
            if (bp == null || bp < 0) {
                throw new IllegalArgumentException("Invalid percentage for member ID: " + mid);
            }
            totalBasisPoints += bp;
        }

        if (totalBasisPoints != 10000) {
            throw new IllegalArgumentException("Percentages must sum to exactly 100%. Current sum: "
                    + (totalBasisPoints / 100) + "." + String.format("%02d", totalBasisPoints % 100) + "%");
        }

        Map<String, Long> shares = new HashMap<>();
        long distributed = 0;
        for (int i = 0; i < memberIds.size(); i++) {
            String mid = memberIds.get(i);
            int bp = basisPointsMap.get(mid);
            long share = (totalPaise * (long) bp) / 10000L;
            shares.put(mid, share);
            distributed += share;
        }

        // Remainder fix: distribute leftover paise one by one
        long remainder = totalPaise - distributed;
        for (int i = 0; i < remainder; i++) {
            String mid = memberIds.get(i % memberIds.size());
            long current = shares.get(mid);
            shares.put(mid, current + 1);
        }

        return shares;
    }

    /**
     * Validates exact shares provided by user.
     * Ensures that every share is non-negative and their total matches totalPaise.
     */
    public static void validateExactSplit(long totalPaise, Map<String, Long> shares) {
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Shares cannot be empty.");
        }
        if (totalPaise <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
        }

        long sum = 0;
        for (Long share : shares.values()) {
            if (share == null || share < 0) {
                throw new IllegalArgumentException("Individual shares cannot be negative.");
            }
            sum += share;
        }

        if (sum != totalPaise) {
            throw new IllegalArgumentException("Exact shares sum (" + Money.format(sum)
                    + ") does not equal total amount (" + Money.format(totalPaise) + ").");
        }
    }
}
