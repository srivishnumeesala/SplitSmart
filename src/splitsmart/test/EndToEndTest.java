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
import splitsmart.model.User;
import splitsmart.storage.DataStore;
import splitsmart.ui.Theme;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Programmatic end-to-end integration test validating the entire user walkthrough:
 * Registration, Authentication, Demo Data loading, Group creation,
 * Equal/Exact/Percentage expenses, Expense editing/deletion, Balance calculations,
 * Settle-up transfers with "Mark as Paid", Theme toggling, and Persistence verification.
 */
public class EndToEndTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  SplitSmart End-to-End Walkthrough Test");
        System.out.println("==================================================");

        String testDataDir = "e2e_test_data";

        // Clean any existing test folder
        cleanDir(new File(testDataDir));

        try {
            DataStore ds = new DataStore(testDataDir);

            // Step 1: Register
            System.out.println("[Step 1] Registering new user...");
            User user = ds.registerUser("collegestudent", "College Student", "securePass123");
            check(user != null && user.getUsername().equals("collegestudent"), "User registered successfully");

            // Step 2: Login authentication
            System.out.println("[Step 2] Authenticating credentials...");
            User authUser = ds.authenticate("collegestudent", "securePass123");
            check(authUser != null, "Valid login succeeded");
            User wrongAuth = ds.authenticate("collegestudent", "wrongPassword");
            check(wrongAuth == null, "Invalid credentials correctly rejected");
            ds.setCurrentUser(authUser);

            // Step 3: Load Demo Data
            System.out.println("[Step 3] Loading demo data...");
            ds.loadDemoData(authUser.getId());
            List<Group> userGroups = ds.getGroupsForUser(authUser.getId());
            check(userGroups.size() == 1, "Demo group created");
            Group demoGroup = userGroups.get(0);
            check(demoGroup.getMembers().size() == 4, "Demo group has 4 members (Aman, Bina, Charan, Diya)");
            check(demoGroup.getExpenses().size() == 5, "Demo group has 5 initial expenses");

            // Step 4: Add New Group
            System.out.println("[Step 4] Creating custom group...");
            String gId = "grp_test_" + UUID.randomUUID().toString().substring(0, 6);
            Group customGroup = new Group(gId, authUser.getId(), "Room 204");
            Member aman = new Member("m_aman", "Aman");
            Member bina = new Member("m_bina", "Bina");
            customGroup.getMembers().add(aman);
            customGroup.getMembers().add(bina);
            ds.addGroup(customGroup);
            check(ds.getGroupsForUser(authUser.getId()).size() == 2, "Custom group added to user");

            List<String> twoMembers = new ArrayList<>();
            twoMembers.add("m_aman");
            twoMembers.add("m_bina");

            // Step 5: Add Equal Expense
            System.out.println("[Step 5] Adding Equal Split expense...");
            Map<String, Long> equalShares = SplitCalculator.calculateEqualSplit(100000L, twoMembers); // ₹1,000
            Expense eqExp = new Expense("exp_eq_1", gId, "Wifi Router", 100000L,
                    LocalDate.now(), "m_aman", Category.UTILITIES, SplitType.EQUAL, equalShares);
            ds.addExpense(gId, eqExp);
            check(customGroup.getExpenses().size() == 1, "Equal expense recorded");

            // Step 6: Add Exact Expense
            System.out.println("[Step 6] Adding Exact Amounts expense...");
            Map<String, Long> exactShares = new HashMap<>();
            exactShares.put("m_aman", 20000L); // ₹200
            exactShares.put("m_bina", 30000L); // ₹300
            SplitCalculator.validateExactSplit(50000L, exactShares);
            Expense exactExp = new Expense("exp_ex_1", gId, "Electricity", 50000L,
                    LocalDate.now(), "m_bina", Category.UTILITIES, SplitType.EXACT, exactShares);
            ds.addExpense(gId, exactExp);
            check(customGroup.getExpenses().size() == 2, "Exact expense recorded");

            // Step 7: Add Percentage Expense
            System.out.println("[Step 7] Adding Percentage Split expense...");
            Map<String, Integer> bpMap = new HashMap<>();
            bpMap.put("m_aman", 6000); // 60%
            bpMap.put("m_bina", 4000); // 40%
            Map<String, Long> pctShares = SplitCalculator.calculatePercentageSplit(200000L, twoMembers, bpMap); // ₹2,000
            Expense pctExp = new Expense("exp_pct_1", gId, "Groceries", 200000L,
                    LocalDate.now(), "m_aman", Category.GROCERIES, SplitType.PERCENTAGE, pctShares);
            ds.addExpense(gId, pctExp);
            check(customGroup.getExpenses().size() == 3, "Percentage expense recorded");

            // Step 8: Edit Expense
            System.out.println("[Step 8] Editing expense...");
            eqExp.setAmountPaise(120000L); // ₹1,200
            eqExp.setShares(SplitCalculator.calculateEqualSplit(120000L, twoMembers));
            ds.updateExpense(gId, eqExp);
            check(customGroup.findMemberById("m_aman") != null, "Group intact after edit");

            // Step 9: Delete Expense
            System.out.println("[Step 9] Deleting expense...");
            ds.deleteExpense(gId, exactExp.getId());
            check(customGroup.getExpenses().size() == 2, "Expense successfully deleted");

            // Step 10: View Balances
            System.out.println("[Step 10] Checking balance calculation...");
            List<BalanceCalculator.MemberBalance> bList = BalanceCalculator.calculateGroupBalances(customGroup);
            long netSum = 0;
            for (int i = 0; i < bList.size(); i++) {
                netSum += bList.get(i).getNetPaise();
            }
            check(netSum == 0, "Sum of all member net balances equals exactly zero");

            // Step 11: Settle-up Plan
            System.out.println("[Step 11] Computing settlement plan...");
            List<Transfer> transfers = SettlementPlanner.planSettlements(bList);
            check(!transfers.isEmpty(), "Settlement plan generated transfers: " + transfers.size());
            Transfer t = transfers.get(0);
            System.out.println("  Suggested: " + t.getFromMember().getName() + " pays " + t.getToMember().getName()
                    + " " + Money.format(t.getAmountPaise()));

            // Step 12: Mark as Paid
            System.out.println("[Step 12] Marking transfer as paid...");
            Map<String, Long> settleShares = new HashMap<>();
            settleShares.put(t.getToMember().getId(), t.getAmountPaise());
            Expense settleExp = new Expense("exp_settle_1", gId,
                    "Settlement: " + t.getFromMember().getName() + " paid " + t.getToMember().getName(),
                    t.getAmountPaise(), LocalDate.now(), t.getFromMember().getId(),
                    Category.SETTLEMENT, SplitType.EXACT, settleShares);
            ds.addExpense(gId, settleExp);

            List<BalanceCalculator.MemberBalance> bSettled = BalanceCalculator.calculateGroupBalances(customGroup);
            List<Transfer> remainingTransfers = SettlementPlanner.planSettlements(bSettled);
            check(remainingTransfers.isEmpty(), "All balances cleared to zero transfers after Mark as Paid");

            // Step 13: Switch Theme
            System.out.println("[Step 13] Toggling theme setting...");
            Theme.isDark = true;
            ds.setSetting("theme", "DARK");
            check("DARK".equals(ds.getSetting("theme", "LIGHT")), "Theme setting saved as DARK");

            // Step 14: Close & Reopen (Reload Persistence)
            System.out.println("[Step 14] Reopening DataStore to verify persistent storage...");
            DataStore reloadedDs = new DataStore(testDataDir);
            reloadedDs.loadAll();

            User reloadedUser = reloadedDs.findUserByUsername("collegestudent");
            check(reloadedUser != null, "User reloaded from CSV");
            check("DARK".equals(reloadedDs.getSetting("theme", "LIGHT")), "Theme setting restored");

            List<Group> reloadedGroups = reloadedDs.getGroupsForUser(reloadedUser.getId());
            check(reloadedGroups.size() == 2, "Both groups reloaded successfully");

            Group reloadedCustom = reloadedDs.findGroupById(gId);
            check(reloadedCustom != null && reloadedCustom.getExpenses().size() == 3, "Group expenses preserved across reload");

            System.out.println("==================================================");
            System.out.println("WALKTHROUGH SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
            System.out.println("==================================================");

        } finally {
            cleanDir(new File(testDataDir));
        }

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            passed++;
        } else {
            System.out.println("  [FAIL] " + description);
            failed++;
        }
    }

    private static void cleanDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    cleanDir(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        dir.delete();
    }
}
