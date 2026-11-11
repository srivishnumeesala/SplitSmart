package splitsmart.storage;

import splitsmart.logic.Money;
import splitsmart.logic.SplitCalculator;
import splitsmart.model.Category;
import splitsmart.model.Expense;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.model.SplitType;
import splitsmart.model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles persistent storage of users, groups, members, expenses, and settings using CSV files.
 * Provides automatic saving upon modifications and resilient error handling for corrupt data.
 */
public class DataStore {

    private static DataStore instance;

    private final String dataDir;
    private final List<User> users;
    private final List<Group> groups;
    private final Map<String, String> settings;
    private User currentUser;

    public DataStore(String dataDir) {
        this.dataDir = dataDir;
        this.users = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.settings = new HashMap<>();
        this.currentUser = null;

        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore("data");
            instance.loadAll();
        }
        return instance;
    }

    public static synchronized void setInstance(DataStore ds) {
        instance = ds;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    /**
     * Retrieves all groups belonging to the specified user.
     */
    public List<Group> getGroupsForUser(String userId) {
        List<Group> list = new ArrayList<>();
        if (userId == null) {
            return list;
        }
        for (int i = 0; i < groups.size(); i++) {
            Group g = groups.get(i);
            if (userId.equals(g.getOwnerUserId())) {
                list.add(g);
            }
        }
        return list;
    }

    /**
     * Finds a group by ID.
     */
    public Group findGroupById(String groupId) {
        if (groupId == null) {
            return null;
        }
        for (int i = 0; i < groups.size(); i++) {
            Group g = groups.get(i);
            if (groupId.equals(g.getId())) {
                return g;
            }
        }
        return null;
    }

    /**
     * Finds a user by ID.
     */
    public User findUserById(String userId) {
        if (userId == null) {
            return null;
        }
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (userId.equals(u.getId())) {
                return u;
            }
        }
        return null;
    }

    /**
     * Finds a user by username.
     */
    public User findUserByUsername(String username) {
        if (username == null) {
            return null;
        }
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (username.equalsIgnoreCase(u.getUsername())) {
                return u;
            }
        }
        return null;
    }

    /**
     * Registers a new user. Throws IllegalArgumentException if username is already taken.
     */
    public User registerUser(String username, String displayName, String password) {
        if (findUserByUsername(username) != null) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        String id = "usr_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User(id, username.trim(), displayName.trim(), hash, salt);
        users.add(user);
        saveAll();
        return user;
    }

    /**
     * Authenticates a user with username and password. Returns the User if successful, or null.
     */
    public User authenticate(String username, String password) {
        User user = findUserByUsername(username);
        if (user == null) {
            return null;
        }
        boolean ok = PasswordUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (ok) {
            return user;
        }
        return null;
    }

    /**
     * Adds a new group and saves data.
     */
    public void addGroup(Group group) {
        if (group != null) {
            groups.add(group);
            saveAll();
        }
    }

    /**
     * Updates an existing group and saves data.
     */
    public void updateGroup(Group group) {
        saveAll();
    }

    /**
     * Deletes a group and its associated data, then saves changes.
     */
    public void deleteGroup(String groupId) {
        Group g = findGroupById(groupId);
        if (g != null) {
            groups.remove(g);
            saveAll();
        }
    }

    /**
     * Adds a member to a group and auto-saves.
     */
    public Member addMember(String groupId, String memberName) {
        Group group = findGroupById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found.");
        }
        String memberId = "mem_" + UUID.randomUUID().toString().substring(0, 8);
        Member m = new Member(memberId, memberName.trim());
        group.getMembers().add(m);
        saveAll();
        return m;
    }

    /**
     * Removes a member from a group. Fails if member appears in expenses.
     */
    public void removeMember(String groupId, String memberId) {
        Group group = findGroupById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found.");
        }
        if (group.isMemberInExpenses(memberId)) {
            throw new IllegalStateException("Cannot remove member who has recorded expenses or shares.");
        }
        Member toRemove = group.findMemberById(memberId);
        if (toRemove != null) {
            group.getMembers().remove(toRemove);
            saveAll();
        }
    }

    /**
     * Adds an expense to a group and saves data.
     */
    public void addExpense(String groupId, Expense expense) {
        Group group = findGroupById(groupId);
        if (group != null && expense != null) {
            expense.setGroupId(groupId);
            group.getExpenses().add(expense);
            saveAll();
        }
    }

    /**
     * Updates an existing expense in a group and saves data.
     */
    public void updateExpense(String groupId, Expense expense) {
        Group group = findGroupById(groupId);
        if (group != null && expense != null) {
            for (int i = 0; i < group.getExpenses().size(); i++) {
                if (group.getExpenses().get(i).getId().equals(expense.getId())) {
                    group.getExpenses().set(i, expense);
                    break;
                }
            }
            saveAll();
        }
    }

    /**
     * Deletes an expense from a group and saves data.
     */
    public void deleteExpense(String groupId, String expenseId) {
        Group group = findGroupById(groupId);
        if (group != null) {
            for (int i = 0; i < group.getExpenses().size(); i++) {
                if (group.getExpenses().get(i).getId().equals(expenseId)) {
                    group.getExpenses().remove(i);
                    break;
                }
            }
            saveAll();
        }
    }

    /**
     * Retrieves all expenses across all groups owned by the specified user.
     */
    public List<Expense> getAllExpensesForUser(String userId) {
        List<Expense> all = new ArrayList<>();
        List<Group> userGroups = getGroupsForUser(userId);
        for (int i = 0; i < userGroups.size(); i++) {
            all.addAll(userGroups.get(i).getExpenses());
        }
        // Sort descending by date
        for (int i = 0; i < all.size() - 1; i++) {
            for (int j = i + 1; j < all.size(); j++) {
                if (all.get(i).getDate().isBefore(all.get(j).getDate())) {
                    Expense temp = all.get(i);
                    all.set(i, all.get(j));
                    all.set(j, temp);
                }
            }
        }
        return all;
    }

    /**
     * Populates demo data using strictly: Aman, Bina, Charan, Diya.
     */
    public void loadDemoData(String userId) {
        String groupId = "grp_demo_" + UUID.randomUUID().toString().substring(0, 6);
        Group group = new Group(groupId, userId, "Goa Trip");

        Member aman = new Member("mem_aman", "Aman");
        Member bina = new Member("mem_bina", "Bina");
        Member charan = new Member("mem_charan", "Charan");
        Member diya = new Member("mem_diya", "Diya");

        group.getMembers().add(aman);
        group.getMembers().add(bina);
        group.getMembers().add(charan);
        group.getMembers().add(diya);

        List<String> all4Ids = new ArrayList<>();
        all4Ids.add(aman.getId());
        all4Ids.add(bina.getId());
        all4Ids.add(charan.getId());
        all4Ids.add(diya.getId());

        List<String> first3Ids = new ArrayList<>();
        first3Ids.add(aman.getId());
        first3Ids.add(bina.getId());
        first3Ids.add(charan.getId());

        LocalDate today = LocalDate.now();

        // 1. Dinner at Bistro - ₹1,200 (Aman pays, all 4 split equal ₹300 each)
        Expense e1 = new Expense("exp_demo_1", groupId, "Dinner at Bistro", 120000L,
                today.minusDays(4), aman.getId(), Category.FOOD, SplitType.EQUAL,
                SplitCalculator.calculateEqualSplit(120000L, all4Ids));

        // 2. Groceries & Snacks - ₹800 (Bina pays, all 4 split equal ₹200 each)
        Expense e2 = new Expense("exp_demo_2", groupId, "Groceries & Snacks", 80000L,
                today.minusDays(3), bina.getId(), Category.GROCERIES, SplitType.EQUAL,
                SplitCalculator.calculateEqualSplit(80000L, all4Ids));

        // 3. Cab to Beach - ₹600 (Charan pays, Aman, Bina, Charan split equal ₹200 each)
        Expense e3 = new Expense("exp_demo_3", groupId, "Cab to Beach", 60000L,
                today.minusDays(2), charan.getId(), Category.TRAVEL, SplitType.EQUAL,
                SplitCalculator.calculateEqualSplit(60000L, first3Ids));

        // 4. Water Sports - ₹1,000 (Diya pays, all 4 split equal ₹250 each)
        Expense e4 = new Expense("exp_demo_4", groupId, "Water Sports", 100000L,
                today.minusDays(1), diya.getId(), Category.ENTERTAINMENT, SplitType.EQUAL,
                SplitCalculator.calculateEqualSplit(100000L, all4Ids));

        // 5. Villa Electricity & Wifi - ₹1,500 (Aman pays, all 4 split equal ₹375 each)
        Expense e5 = new Expense("exp_demo_5", groupId, "Villa Electricity & Wifi", 150000L,
                today, aman.getId(), Category.UTILITIES, SplitType.EQUAL,
                SplitCalculator.calculateEqualSplit(150000L, all4Ids));

        group.getExpenses().add(e1);
        group.getExpenses().add(e2);
        group.getExpenses().add(e3);
        group.getExpenses().add(e4);
        group.getExpenses().add(e5);

        groups.add(group);
        saveAll();
    }

    /**
     * Deletes all groups and expenses for a user.
     */
    public void deleteAllUserData(String userId) {
        if (userId == null) {
            return;
        }
        for (int i = groups.size() - 1; i >= 0; i--) {
            if (userId.equals(groups.get(i).getOwnerUserId())) {
                groups.remove(i);
            }
        }
        saveAll();
    }

    public String getSetting(String key, String defaultValue) {
        String val = settings.get(key);
        return val != null ? val : defaultValue;
    }

    public void setSetting(String key, String value) {
        settings.put(key, value);
        saveSettings();
    }

    // ==================== CSV LOAD & SAVE ====================

    /**
     * Loads all data files from disk into memory. Missing files are created automatically.
     */
    public synchronized void loadAll() {
        users.clear();
        groups.clear();
        settings.clear();

        loadUsers();
        loadGroups();
        loadMembers();
        loadExpenses();
        loadSettings();
    }

    /**
     * Persists all in-memory data to CSV files.
     */
    public synchronized void saveAll() {
        saveUsers();
        saveGroups();
        saveMembers();
        saveExpenses();
        saveSettings();
    }

    private void loadUsers() {
        File file = new File(dataDir, "users.csv");
        if (!file.exists()) {
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 5) {
                    System.err.println("Warning: Skipped corrupted line in users.csv: " + line);
                    continue;
                }
                users.add(new User(cols.get(0), cols.get(1), cols.get(2), cols.get(3), cols.get(4)));
            }
        } catch (Exception e) {
            System.err.println("Error reading users.csv: " + e.getMessage());
        } finally {
            closeQuietly(reader);
        }
    }

    private void saveUsers() {
        File file = new File(dataDir, "users.csv");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write("id,username,displayName,passwordHash,salt\n");
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                writer.write(escapeCsv(u.getId()) + ","
                        + escapeCsv(u.getUsername()) + ","
                        + escapeCsv(u.getDisplayName()) + ","
                        + escapeCsv(u.getPasswordHash()) + ","
                        + escapeCsv(u.getSalt()) + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing users.csv: " + e.getMessage());
        } finally {
            closeQuietly(writer);
        }
    }

    private void loadGroups() {
        File file = new File(dataDir, "groups.csv");
        if (!file.exists()) {
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 3) {
                    System.err.println("Warning: Skipped corrupted line in groups.csv: " + line);
                    continue;
                }
                groups.add(new Group(cols.get(0), cols.get(1), cols.get(2)));
            }
        } catch (Exception e) {
            System.err.println("Error reading groups.csv: " + e.getMessage());
        } finally {
            closeQuietly(reader);
        }
    }

    private void saveGroups() {
        File file = new File(dataDir, "groups.csv");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write("id,ownerUserId,name\n");
            for (int i = 0; i < groups.size(); i++) {
                Group g = groups.get(i);
                writer.write(escapeCsv(g.getId()) + ","
                        + escapeCsv(g.getOwnerUserId()) + ","
                        + escapeCsv(g.getName()) + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing groups.csv: " + e.getMessage());
        } finally {
            closeQuietly(writer);
        }
    }

    private void loadMembers() {
        File file = new File(dataDir, "members.csv");
        if (!file.exists()) {
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 3) {
                    System.err.println("Warning: Skipped corrupted line in members.csv: " + line);
                    continue;
                }
                String id = cols.get(0);
                String groupId = cols.get(1);
                String name = cols.get(2);
                Group group = findGroupById(groupId);
                if (group != null) {
                    group.getMembers().add(new Member(id, name));
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading members.csv: " + e.getMessage());
        } finally {
            closeQuietly(reader);
        }
    }

    private void saveMembers() {
        File file = new File(dataDir, "members.csv");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write("id,groupId,name\n");
            for (int i = 0; i < groups.size(); i++) {
                Group g = groups.get(i);
                for (int j = 0; j < g.getMembers().size(); j++) {
                    Member m = g.getMembers().get(j);
                    writer.write(escapeCsv(m.getId()) + ","
                            + escapeCsv(g.getId()) + ","
                            + escapeCsv(m.getName()) + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing members.csv: " + e.getMessage());
        } finally {
            closeQuietly(writer);
        }
    }

    private void loadExpenses() {
        File file = new File(dataDir, "expenses.csv");
        if (!file.exists()) {
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 9) {
                    System.err.println("Warning: Skipped corrupted line in expenses.csv: " + line);
                    continue;
                }
                try {
                    String id = cols.get(0);
                    String groupId = cols.get(1);
                    String description = cols.get(2);
                    long amountPaise = Long.parseLong(cols.get(3));
                    LocalDate date = LocalDate.parse(cols.get(4));
                    String payerMemberId = cols.get(5);
                    Category category = Category.valueOf(cols.get(6));
                    SplitType splitType = SplitType.valueOf(cols.get(7));

                    Map<String, Long> shares = new HashMap<>();
                    String sharesRaw = cols.get(8);
                    if (sharesRaw != null && !sharesRaw.trim().isEmpty()) {
                        String[] pairs = sharesRaw.split(";");
                        for (int p = 0; p < pairs.length; p++) {
                            String pair = pairs[p].trim();
                            if (!pair.isEmpty()) {
                                String[] kv = pair.split(":");
                                if (kv.length == 2) {
                                    shares.put(kv[0].trim(), Long.parseLong(kv[1].trim()));
                                }
                            }
                        }
                    }

                    Expense exp = new Expense(id, groupId, description, amountPaise, date,
                            payerMemberId, category, splitType, shares);

                    Group group = findGroupById(groupId);
                    if (group != null) {
                        group.getExpenses().add(exp);
                    }
                } catch (Exception ex) {
                    System.err.println("Warning: Skipped corrupted line in expenses.csv: " + line + " -> " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading expenses.csv: " + e.getMessage());
        } finally {
            closeQuietly(reader);
        }
    }

    private void saveExpenses() {
        File file = new File(dataDir, "expenses.csv");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write("id,groupId,description,amountPaise,date,payerMemberId,category,splitType,shares\n");
            for (int i = 0; i < groups.size(); i++) {
                Group g = groups.get(i);
                for (int j = 0; j < g.getExpenses().size(); j++) {
                    Expense e = g.getExpenses().get(j);
                    StringBuilder sharesSb = new StringBuilder();
                    if (e.getShares() != null) {
                        boolean first = true;
                        for (Map.Entry<String, Long> entry : e.getShares().entrySet()) {
                            if (!first) {
                                sharesSb.append(";");
                            }
                            sharesSb.append(entry.getKey()).append(":").append(entry.getValue());
                            first = false;
                        }
                    }

                    writer.write(escapeCsv(e.getId()) + ","
                            + escapeCsv(g.getId()) + ","
                            + escapeCsv(e.getDescription()) + ","
                            + e.getAmountPaise() + ","
                            + e.getDate().toString() + ","
                            + escapeCsv(e.getPayerMemberId()) + ","
                            + e.getCategory().name() + ","
                            + e.getSplitType().name() + ","
                            + escapeCsv(sharesSb.toString()) + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing expenses.csv: " + e.getMessage());
        } finally {
            closeQuietly(writer);
        }
    }

    private void loadSettings() {
        File file = new File(dataDir, "settings.csv");
        if (!file.exists()) {
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() >= 2) {
                    settings.put(cols.get(0), cols.get(1));
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading settings.csv: " + e.getMessage());
        } finally {
            closeQuietly(reader);
        }
    }

    private void saveSettings() {
        File file = new File(dataDir, "settings.csv");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write("key,value\n");
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                writer.write(escapeCsv(entry.getKey()) + "," + escapeCsv(entry.getValue()) + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing settings.csv: " + e.getMessage());
        } finally {
            closeQuietly(writer);
        }
    }

    // ==================== CSV HELPERS ====================

    /**
     * Escapes a string for CSV output, wrapping in quotes if containing commas, quotes, or newlines.
     */
    public static String escapeCsv(String text) {
        if (text == null) {
            return "";
        }
        boolean mustQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        String sanitized = text.replace("\"", "\"\"");
        if (mustQuote) {
            return "\"" + sanitized + "\"";
        }
        return sanitized;
    }

    /**
     * Parses a CSV line handling double-quoted fields with escaped quotes.
     */
    public static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '\"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                        sb.append('\"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '\"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        result.add(sb.toString());
        return result;
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
