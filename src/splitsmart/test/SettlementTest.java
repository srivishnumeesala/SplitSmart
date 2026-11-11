package splitsmart.test;

import splitsmart.logic.BalanceCalculator;
import splitsmart.logic.Money;
import splitsmart.logic.SettlementPlanner;
import splitsmart.logic.SplitCalculator;
import splitsmart.model.Category;
import splitsmart.model.Expense;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.model.SplitType;
import splitsmart.model.Transfer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Console-based test suite verifying core logic:
 * SplitCalculator, BalanceCalculator, SettlementPlanner, and expense edit/delete integrity.
 */
public class SettlementTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  SplitSmart Core Logic Test Suite");
        System.out.println("==================================================");

        test1ProposalExample();
        test2RoundingRemainder();
        test3TwoPayersMixedExpenses();
        test4ExactAndPercentageSplits();
        test5AlreadySettledZeroTransfers();
        test6EditAndDeleteExpense();
        test7DataStoreRoundTrip();

        System.out.println("==================================================");
        System.out.println("TEST SUMMARY: " + testsPassed + " PASSED, " + testsFailed + " FAILED");
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void pass(String testName) {
        System.out.println("[PASS] " + testName);
        testsPassed++;
    }

    private static void fail(String testName, String reason) {
        System.out.println("[FAIL] " + testName + " -> " + reason);
        testsFailed++;
    }

    /**
     * Test 1: Proposal example
     * Aman pays ₹1,200 dinner split equally by Aman, Bina, Charan, Diya.
     * Expected: Aman +₹900, others -₹300 each, 3 transfers of ₹300 to Aman.
     */
    private static void test1ProposalExample() {
        String name = "Test 1: Proposal Example (Aman pays ₹1,200 split equally among 4)";
        try {
            Member aman = new Member("m1", "Aman");
            Member bina = new Member("m2", "Bina");
            Member charan = new Member("m3", "Charan");
            Member diya = new Member("m4", "Diya");

            List<Member> members = new ArrayList<>();
            members.add(aman);
            members.add(bina);
            members.add(charan);
            members.add(diya);

            long amountPaise = 120000; // ₹1,200.00
            List<String> memberIds = new ArrayList<>();
            for (int i = 0; i < members.size(); i++) {
                memberIds.add(members.get(i).getId());
            }

            Map<String, Long> shares = SplitCalculator.calculateEqualSplit(amountPaise, memberIds);

            Expense dinner = new Expense("e1", "g1", "Dinner", amountPaise,
                    LocalDate.now(), aman.getId(), Category.FOOD, SplitType.EQUAL, shares);

            List<Expense> expenses = new ArrayList<>();
            expenses.add(dinner);

            List<BalanceCalculator.MemberBalance> balances = BalanceCalculator.calculateBalances(members, expenses);

            // Check balances
            for (int i = 0; i < balances.size(); i++) {
                BalanceCalculator.MemberBalance mb = balances.get(i);
                if (mb.getMember().getId().equals("m1")) {
                    if (mb.getNetPaise() != 90000) {
                        fail(name, "Aman net should be +90000 but was " + mb.getNetPaise());
                        return;
                    }
                } else {
                    if (mb.getNetPaise() != -30000) {
                        fail(name, mb.getMember().getName() + " net should be -30000 but was " + mb.getNetPaise());
                        return;
                    }
                }
            }

            // Check transfers
            List<Transfer> transfers = SettlementPlanner.planSettlements(balances);
            if (transfers.size() != 3) {
                fail(name, "Expected 3 transfers but got " + transfers.size());
                return;
            }

            for (int i = 0; i < transfers.size(); i++) {
                Transfer t = transfers.get(i);
                if (!t.getToMember().getId().equals("m1")) {
                    fail(name, "Transfer recipient should be Aman but was " + t.getToMember().getName());
                    return;
                }
                if (t.getAmountPaise() != 30000) {
                    fail(name, "Transfer amount should be 30000 paise but was " + t.getAmountPaise());
                    return;
                }
            }

            pass(name);
        } catch (Exception e) {
            fail(name, "Exception: " + e.getMessage());
        }
    }

    /**
     * Test 2: Rounding remainder fix
     * ₹1,000 among 3 people: shares 333.34, 333.33, 333.33 summing exactly to 100000 paise.
     */
    private static void test2RoundingRemainder() {
        String name = "Test 2: Rounding Remainder (₹1,000 among 3 people)";
        try {
            List<String> memberIds = new ArrayList<>();
            memberIds.add("m1");
            memberIds.add("m2");
            memberIds.add("m3");

            long total = 100000; // ₹1,000.00
            Map<String, Long> shares = SplitCalculator.calculateEqualSplit(total, memberIds);

            long share1 = shares.get("m1");
            long share2 = shares.get("m2");
            long share3 = shares.get("m3");

            if (share1 != 33334 || share2 != 33333 || share3 != 33333) {
                fail(name, "Expected shares 33334, 33333, 33333 but got: " + share1 + ", " + share2 + ", " + share3);
                return;
            }

            long sum = share1 + share2 + share3;
            if (sum != total) {
                fail(name, "Sum of shares (" + sum + ") does not equal total (" + total + ")");
                return;
            }

            pass(name);
        } catch (Exception e) {
            fail(name, "Exception: " + e.getMessage());
        }
    }

    /**
     * Test 3: Two payers and mixed expenses; transfers fully clear every balance.
     */
    private static void test3TwoPayersMixedExpenses() {
        String name = "Test 3: Two Payers and Mixed Expenses";
        try {
            Member aman = new Member("m1", "Aman");
            Member bina = new Member("m2", "Bina");
            Member charan = new Member("m3", "Charan");

            List<Member> members = new ArrayList<>();
            members.add(aman);
            members.add(bina);
            members.add(charan);

            // Expense 1: Aman pays ₹600 (60000 paise) split equally among 3 (20000 each)
            List<String> all3 = new ArrayList<>();
            all3.add("m1");
            all3.add("m2");
            all3.add("m3");
            Map<String, Long> shares1 = SplitCalculator.calculateEqualSplit(60000, all3);
            Expense exp1 = new Expense("e1", "g1", "Cab", 60000, LocalDate.now(),
                    aman.getId(), Category.TRAVEL, SplitType.EQUAL, shares1);

            // Expense 2: Bina pays ₹900 (90000 paise) split equally among 3 (30000 each)
            Map<String, Long> shares2 = SplitCalculator.calculateEqualSplit(90000, all3);
            Expense exp2 = new Expense("e2", "g1", "Lunch", 90000, LocalDate.now(),
                    bina.getId(), Category.FOOD, SplitType.EQUAL, shares2);

            List<Expense> expenses = new ArrayList<>();
            expenses.add(exp1);
            expenses.add(exp2);

            // Balances:
            // Aman: paid 60000, share 50000 -> net +10000
            // Bina: paid 90000, share 50000 -> net +40000
            // Charan: paid 0, share 50000 -> net -50000
            List<BalanceCalculator.MemberBalance> balances = BalanceCalculator.calculateBalances(members, expenses);
            List<Transfer> transfers = SettlementPlanner.planSettlements(balances);

            // Simulate transfers on member balances
            Map<String, Long> simNet = new HashMap<>();
            for (int i = 0; i < balances.size(); i++) {
                simNet.put(balances.get(i).getMember().getId(), balances.get(i).getNetPaise());
            }

            for (int i = 0; i < transfers.size(); i++) {
                Transfer t = transfers.get(i);
                String from = t.getFromMember().getId();
                String to = t.getToMember().getId();
                long amt = t.getAmountPaise();

                simNet.put(from, simNet.get(from) + amt); // payer reduces negative debt
                simNet.put(to, simNet.get(to) - amt);     // receiver reduces positive credit
            }

            // Verify all balances are exactly 0
            for (Map.Entry<String, Long> entry : simNet.entrySet()) {
                if (entry.getValue() != 0) {
                    fail(name, "Member " + entry.getKey() + " has remaining unsettled balance: " + entry.getValue());
                    return;
                }
            }

            pass(name);
        } catch (Exception e) {
            fail(name, "Exception: " + e.getMessage());
        }
    }

    /**
     * Test 4: Exact-amount and percentage splits, including invalid input rejection.
     */
    private static void test4ExactAndPercentageSplits() {
        String name = "Test 4: Exact and Percentage Splits with Validation";
        try {
            List<String> memberIds = new ArrayList<>();
            memberIds.add("m1");
            memberIds.add("m2");

            // 1. Valid exact split
            Map<String, Long> exactShares = new HashMap<>();
            exactShares.put("m1", 4000L);
            exactShares.put("m2", 6000L);
            SplitCalculator.validateExactSplit(10000L, exactShares);

            // 2. Invalid exact split (sum mismatch)
            boolean caughtExact = false;
            try {
                exactShares.put("m2", 5000L); // sum = 9000 != 10000
                SplitCalculator.validateExactSplit(10000L, exactShares);
            } catch (IllegalArgumentException e) {
                caughtExact = true;
            }
            if (!caughtExact) {
                fail(name, "Mismatched exact split should have thrown IllegalArgumentException");
                return;
            }

            // 3. Valid percentage split: 60% + 40% = 100% (6000 + 4000 bp)
            Map<String, Integer> bpMap = new HashMap<>();
            bpMap.put("m1", 6000);
            bpMap.put("m2", 4000);
            Map<String, Long> pctShares = SplitCalculator.calculatePercentageSplit(10000L, memberIds, bpMap);
            if (pctShares.get("m1") != 6000L || pctShares.get("m2") != 4000L) {
                fail(name, "Percentage split shares incorrect");
                return;
            }

            // 4. Invalid percentage split (sum != 100%)
            boolean caughtPct = false;
            try {
                bpMap.put("m2", 3000); // sum = 90%
                SplitCalculator.calculatePercentageSplit(10000L, memberIds, bpMap);
            } catch (IllegalArgumentException e) {
                caughtPct = true;
            }
            if (!caughtPct) {
                fail(name, "Percentages not summing to 100% should have thrown IllegalArgumentException");
                return;
            }

            pass(name);
        } catch (Exception e) {
            fail(name, "Exception: " + e.getMessage());
        }
    }

    /**
     * Test 5: Everyone already settled -> zero transfers.
     */
    private static void test5AlreadySettledZeroTransfers() {
        String name = "Test 5: Already Settled Produces Zero Transfers";
        try {
            Member aman = new Member("m1", "Aman");
            Member bina = new Member("m2", "Bina");

            List<Member> members = new ArrayList<>();
            members.add(aman);
            members.add(bina);

            // Offsetting expenses: Aman pays ₹50 for Bina, Bina pays ₹50 for Aman
            Map<String, Long> shares1 = new HashMap<>();
            shares1.put("m2", 5000L);
            Expense e1 = new Expense("e1", "g1", "Snack 1", 5000L, LocalDate.now(),
                    aman.getId(), Category.FOOD, SplitType.EXACT, shares1);

            Map<String, Long> shares2 = new HashMap<>();
            shares2.put("m1", 5000L);
            Expense e2 = new Expense("e2", "g1", "Snack 2", 5000L, LocalDate.now(),
                    bina.getId(), Category.FOOD, SplitType.EXACT, shares2);

            List<Expense> expenses = new ArrayList<>();
            expenses.add(e1);
            expenses.add(e2);

            List<BalanceCalculator.MemberBalance> balances = BalanceCalculator.calculateBalances(members, expenses);
            for (int i = 0; i < balances.size(); i++) {
                if (balances.get(i).getNetPaise() != 0) {
                    fail(name, "Net balance should be 0 for " + balances.get(i).getMember().getName());
                    return;
                }
            }

            List<Transfer> transfers = SettlementPlanner.planSettlements(balances);
            if (!transfers.isEmpty()) {
                fail(name, "Expected 0 transfers but got " + transfers.size());
                return;
            }

            pass(name);
        } catch (Exception e) {
            fail(name, "Exception: " + e.getMessage());
        }
    }

    /**
     * Test 6: Edit and delete an expense -> balances change correctly.
     */
    private static void test6EditAndDeleteExpense() {
        String name = "Test 6: Edit and Delete Expense Updates Balances";
        try {
            Member aman = new Member("m1", "Aman");
            Member bina = new Member("m2", "Bina");

            Group group = new Group("g1", "u1", "Flatmates");
            group.getMembers().add(aman);
            group.getMembers().add(bina);

            // Add initial expense: ₹100 paid by Aman, split equally (₹50 each)
            List<String> memberIds = new ArrayList<>();
            memberIds.add("m1");
            memberIds.add("m2");

            Map<String, Long> sharesInitial = SplitCalculator.calculateEqualSplit(10000L, memberIds);
            Expense expense = new Expense("e1", "g1", "Groceries", 10000L, LocalDate.now(),
                    aman.getId(), Category.GROCERIES, SplitType.EQUAL, sharesInitial);
            group.getExpenses().add(expense);

            List<BalanceCalculator.MemberBalance> b1 = BalanceCalculator.calculateGroupBalances(group);
            long amanNet1 = b1.get(0).getMember().getId().equals("m1") ? b1.get(0).getNetPaise() : b1.get(1).getNetPaise();
            if (amanNet1 != 5000L) {
                fail(name, "Initial Aman net should be +5000 but was " + amanNet1);
                return;
            }

            // Edit expense amount to ₹200 (20000 paise)
            Map<String, Long> sharesEdited = SplitCalculator.calculateEqualSplit(20000L, memberIds);
            expense.setAmountPaise(20000L);
            expense.setShares(sharesEdited);

            List<BalanceCalculator.MemberBalance> b2 = BalanceCalculator.calculateGroupBalances(group);
            long amanNet2 = b2.get(0).getMember().getId().equals("m1") ? b2.get(0).getNetPaise() : b2.get(1).getNetPaise();
            if (amanNet2 != 10000L) {
                fail(name, "After edit, Aman net should be +10000 but was " + amanNet2);
                return;
            }

            // Delete expense
            group.getExpenses().remove(expense);
            List<BalanceCalculator.MemberBalance> b3 = BalanceCalculator.calculateGroupBalances(group);
            long amanNet3 = b3.get(0).getMember().getId().equals("m1") ? b3.get(0).getNetPaise() : b3.get(1).getNetPaise();
            if (amanNet3 != 0L) {
                fail(name, "After delete, Aman net should be 0 but was " + amanNet3);
                return;
            }

            pass(name);
        } catch (Exception e) {
            fail(name, "Exception: " + e.getMessage());
        }
    }

    /**
     * Test 7: DataStore CSV round-trip serialization and loading.
     */
    private static void test7DataStoreRoundTrip() {
        String name = "Test 7: DataStore CSV Save and Load Round-Trip";
        try {
            String testDir = "build_test_data";
            splitsmart.storage.DataStore ds1 = new splitsmart.storage.DataStore(testDir);
            splitsmart.model.User user = ds1.registerUser("testuser", "Test User", "secret123");
            ds1.loadDemoData(user.getId());
            ds1.saveAll();

            // Load fresh into ds2
            splitsmart.storage.DataStore ds2 = new splitsmart.storage.DataStore(testDir);
            ds2.loadAll();

            splitsmart.model.User loadedUser = ds2.findUserByUsername("testuser");
            if (loadedUser == null || !loadedUser.getDisplayName().equals("Test User")) {
                fail(name, "User was not loaded correctly");
                return;
            }

            boolean auth = splitsmart.storage.PasswordUtil.verifyPassword("secret123", loadedUser.getSalt(), loadedUser.getPasswordHash());
            if (!auth) {
                fail(name, "Password verification failed after reload");
                return;
            }

            List<splitsmart.model.Group> groups = ds2.getGroupsForUser(loadedUser.getId());
            if (groups.size() != 1) {
                fail(name, "Expected 1 group but loaded " + groups.size());
                return;
            }

            splitsmart.model.Group group = groups.get(0);
            if (group.getMembers().size() != 4 || group.getExpenses().size() != 5) {
                fail(name, "Members count (" + group.getMembers().size() + ") or expenses count (" + group.getExpenses().size() + ") mismatch");
                return;
            }

            // Cleanup test files
            java.io.File dir = new java.io.File(testDir);
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
            }
            dir.delete();

            pass(name);
        } catch (Exception e) {
            fail(name, "Exception: " + e.getMessage());
        }
    }
}
