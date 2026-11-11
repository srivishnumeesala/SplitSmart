package splitsmart.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import splitsmart.logic.BalanceCalculator;
import splitsmart.logic.Money;
import splitsmart.logic.SettlementPlanner;
import splitsmart.logic.SplitCalculator;
import splitsmart.logic.Validator;
import splitsmart.model.Category;
import splitsmart.model.Expense;
import splitsmart.model.Group;
import splitsmart.model.Member;
import splitsmart.model.SplitType;
import splitsmart.model.Transfer;
import splitsmart.model.User;
import splitsmart.storage.DataStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Built-in HTTP web server providing REST API endpoints and serving the modern
 * web frontend for SplitSmart. Requires zero external dependencies.
 */
public class WebServer {

    private final int port;
    private final DataStore dataStore;
    private final Map<String, User> sessions = new ConcurrentHashMap<>();
    private HttpServer server;

    public WebServer(int port) {
        this.port = port;
        this.dataStore = DataStore.getInstance();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        // API Handler
        server.createContext("/api/", new ApiHandler());

        // Static Files Handler
        server.createContext("/", new StaticHandler());

        server.start();

        System.out.println("==========================================================");
        System.out.println("  SplitSmart Web Application is LIVE!");
        System.out.println("  Localhost URL: http://localhost:" + port);
        System.out.println("==========================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private User getAuthenticatedUser(HttpExchange exchange) {
        // 1. Check Cookie
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String c : cookies) {
                String[] pair = c.trim().split("=");
                if (pair.length == 2 && "splitsmart_session".equals(pair[0])) {
                    User u = sessions.get(pair[1].trim());
                    if (u != null) {
                        return u;
                    }
                }
            }
        }
        // 2. Check Authorization header
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            User u = sessions.get(token);
            if (u != null) {
                return u;
            }
        }
        // Fallback: if only one user exists in system, use that user for convenience
        if (dataStore.getCurrentUser() != null) {
            return dataStore.getCurrentUser();
        }
        if (dataStore.getUsers().size() == 1) {
            return dataStore.getUsers().get(0);
        }
        return null;
    }

    private class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable CORS
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            String body = readBody(exchange);

            try {
                if (path.equals("/api/auth/register") && method.equals("POST")) {
                    handleRegister(exchange, body);
                } else if (path.equals("/api/auth/login") && method.equals("POST")) {
                    handleLogin(exchange, body);
                } else if (path.equals("/api/auth/logout") && method.equals("POST")) {
                    handleLogout(exchange);
                } else if (path.equals("/api/auth/me") && method.equals("GET")) {
                    handleMe(exchange);
                } else if (path.equals("/api/dashboard") && method.equals("GET")) {
                    handleDashboard(exchange);
                } else if (path.equals("/api/groups") && method.equals("GET")) {
                    handleGetGroups(exchange);
                } else if (path.equals("/api/groups") && method.equals("POST")) {
                    handleCreateGroup(exchange, body);
                } else if (path.startsWith("/api/groups/") && method.equals("GET") && !path.contains("/expenses") && !path.contains("/members")) {
                    String groupId = path.substring("/api/groups/".length());
                    handleGetGroupDetail(exchange, groupId);
                } else if (path.startsWith("/api/groups/") && method.equals("PUT") && !path.contains("/expenses")) {
                    String groupId = path.substring("/api/groups/".length());
                    handleUpdateGroup(exchange, groupId, body);
                } else if (path.startsWith("/api/groups/") && method.equals("DELETE") && !path.contains("/expenses") && !path.contains("/members")) {
                    String groupId = path.substring("/api/groups/".length());
                    handleDeleteGroup(exchange, groupId);
                } else if (path.matches("/api/groups/[^/]+/members") && method.equals("POST")) {
                    String groupId = path.split("/")[3];
                    handleAddMember(exchange, groupId, body);
                } else if (path.matches("/api/groups/[^/]+/members/[^/]+") && method.equals("DELETE")) {
                    String[] parts = path.split("/");
                    handleRemoveMember(exchange, parts[3], parts[5]);
                } else if (path.matches("/api/groups/[^/]+/expenses") && method.equals("POST")) {
                    String groupId = path.split("/")[3];
                    handleAddExpense(exchange, groupId, body);
                } else if (path.matches("/api/groups/[^/]+/expenses/[^/]+") && method.equals("PUT")) {
                    String[] parts = path.split("/");
                    handleUpdateExpense(exchange, parts[3], parts[5], body);
                } else if (path.matches("/api/groups/[^/]+/expenses/[^/]+") && method.equals("DELETE")) {
                    String[] parts = path.split("/");
                    handleDeleteExpense(exchange, parts[3], parts[5]);
                } else if (path.matches("/api/groups/[^/]+/settle") && method.equals("POST")) {
                    String groupId = path.split("/")[3];
                    handleSettle(exchange, groupId, body);
                } else if (path.equals("/api/transactions") && method.equals("GET")) {
                    handleTransactions(exchange);
                } else if (path.equals("/api/analytics") && method.equals("GET")) {
                    handleAnalytics(exchange);
                } else if (path.equals("/api/demo") && method.equals("POST")) {
                    handleLoadDemo(exchange);
                } else if (path.equals("/api/reset") && method.equals("POST")) {
                    handleResetData(exchange);
                } else if (path.equals("/api/settings") && method.equals("GET")) {
                    handleGetSettings(exchange);
                } else if (path.equals("/api/settings") && method.equals("POST")) {
                    handleSaveSettings(exchange, body);
                } else {
                    sendJson(exchange, 404, errorMap("Endpoint not found: " + path));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                sendJson(exchange, 400, errorMap(ex.getMessage()));
            }
        }
    }

    private void handleRegister(HttpExchange exchange, String body) throws IOException {
        Map<String, Object> req = SimpleJson.parseObject(body);
        String username = (String) req.get("username");
        String displayName = (String) req.get("displayName");
        String password = (String) req.get("password");

        String uErr = Validator.validateUsername(username);
        if (uErr != null) { sendJson(exchange, 400, errorMap(uErr)); return; }
        String dErr = Validator.validateDisplayName(displayName);
        if (dErr != null) { sendJson(exchange, 400, errorMap(dErr)); return; }
        String pErr = Validator.validatePassword(password);
        if (pErr != null) { sendJson(exchange, 400, errorMap(pErr)); return; }

        User user = dataStore.registerUser(username, displayName, password);
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        dataStore.setCurrentUser(user);

        exchange.getResponseHeaders().set("Set-Cookie", "splitsmart_session=" + token + "; Path=/; SameSite=Lax");
        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("user", userToMap(user));
        sendJson(exchange, 200, res);
    }

    private void handleLogin(HttpExchange exchange, String body) throws IOException {
        Map<String, Object> req = SimpleJson.parseObject(body);
        String username = (String) req.get("username");
        String password = (String) req.get("password");

        if (username == null || password == null) {
            sendJson(exchange, 400, errorMap("Username and password are required."));
            return;
        }

        User user = dataStore.authenticate(username, password);
        if (user == null) {
            sendJson(exchange, 401, errorMap("Invalid username or password."));
            return;
        }

        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        dataStore.setCurrentUser(user);

        exchange.getResponseHeaders().set("Set-Cookie", "splitsmart_session=" + token + "; Path=/; SameSite=Lax");
        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("user", userToMap(user));
        sendJson(exchange, 200, res);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        dataStore.setCurrentUser(null);
        exchange.getResponseHeaders().set("Set-Cookie", "splitsmart_session=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        sendJson(exchange, 200, res);
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) {
            sendJson(exchange, 401, errorMap("Not authenticated."));
            return;
        }
        sendJson(exchange, 200, userToMap(user));
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJson(exchange, 401, errorMap("Not authenticated.")); return; }

        List<Group> userGroups = dataStore.getGroupsForUser(user.getId());
        long totalReceivable = 0;
        long totalPayable = 0;

        List<Map<String, Object>> groupCards = new ArrayList<>();
        for (int i = 0; i < userGroups.size(); i++) {
            Group g = userGroups.get(i);
            List<BalanceCalculator.MemberBalance> bList = BalanceCalculator.calculateGroupBalances(g);
            long totalExp = 0;
            for (int k = 0; k < g.getExpenses().size(); k++) {
                totalExp += g.getExpenses().get(k).getAmountPaise();
            }

            for (int j = 0; j < bList.size(); j++) {
                long net = bList.get(j).getNetPaise();
                if (net > 0) totalReceivable += net;
                else if (net < 0) totalPayable += (-net);
            }

            Map<String, Object> gm = new HashMap<>();
            gm.put("id", g.getId());
            gm.put("name", g.getName());
            gm.put("memberCount", g.getMembers().size());
            gm.put("expenseCount", g.getExpenses().size());
            gm.put("totalExpensePaise", totalExp);
            gm.put("totalExpenseFormatted", Money.format(totalExp));
            groupCards.add(gm);
        }

        long netOverall = totalReceivable - totalPayable;

        // Recent 5 expenses
        List<Expense> allExp = dataStore.getAllExpensesForUser(user.getId());
        List<Map<String, Object>> recent = new ArrayList<>();
        int maxRecent = Math.min(5, allExp.size());
        for (int i = 0; i < maxRecent; i++) {
            recent.add(expenseToMap(allExp.get(i)));
        }

        // Category spending
        Map<String, Long> catMap = new HashMap<>();
        for (int i = 0; i < allExp.size(); i++) {
            Expense e = allExp.get(i);
            String cat = e.getCategory().getDisplayName();
            long curr = catMap.containsKey(cat) ? catMap.get(cat) : 0L;
            catMap.put(cat, curr + e.getAmountPaise());
        }

        List<Map<String, Object>> catList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : catMap.entrySet()) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("category", entry.getKey());
            cm.put("amountPaise", entry.getValue());
            cm.put("amountFormatted", Money.format(entry.getValue()));
            catList.add(cm);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("user", userToMap(user));
        res.put("totalReceivablePaise", totalReceivable);
        res.put("totalReceivableFormatted", Money.format(totalReceivable));
        res.put("totalPayablePaise", totalPayable);
        res.put("totalPayableFormatted", Money.format(totalPayable));
        res.put("netOverallPaise", netOverall);
        res.put("netOverallFormatted", Money.format(netOverall));
        res.put("groups", groupCards);
        res.put("recentExpenses", recent);
        res.put("categorySpending", catList);

        sendJson(exchange, 200, res);
    }

    private void handleGetGroups(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJson(exchange, 401, errorMap("Not authenticated.")); return; }

        List<Group> userGroups = dataStore.getGroupsForUser(user.getId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < userGroups.size(); i++) {
            Group g = userGroups.get(i);
            list.add(groupToSummaryMap(g));
        }
        sendJson(exchange, 200, list);
    }

    private void handleCreateGroup(HttpExchange exchange, String body) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJson(exchange, 401, errorMap("Not authenticated.")); return; }

        Map<String, Object> req = SimpleJson.parseObject(body);
        String name = (String) req.get("name");
        if (name == null || name.trim().isEmpty()) {
            sendJson(exchange, 400, errorMap("Group name cannot be empty."));
            return;
        }

        List<?> membersRaw = (List<?>) req.get("members");
        if (membersRaw == null || membersRaw.size() < 2) {
            sendJson(exchange, 400, errorMap("A group must have at least 2 members."));
            return;
        }

        String groupId = "grp_" + UUID.randomUUID().toString().substring(0, 8);
        Group group = new Group(groupId, user.getId(), name.trim());
        for (int i = 0; i < membersRaw.size(); i++) {
            String mName = String.valueOf(membersRaw.get(i)).trim();
            if (!mName.isEmpty()) {
                String mId = "mem_" + UUID.randomUUID().toString().substring(0, 8);
                group.getMembers().add(new Member(mId, mName));
            }
        }

        dataStore.addGroup(group);
        sendJson(exchange, 201, groupToDetailMap(group));
    }

    private void handleGetGroupDetail(HttpExchange exchange, String groupId) throws IOException {
        Group g = dataStore.findGroupById(groupId);
        if (g == null) { sendJson(exchange, 404, errorMap("Group not found.")); return; }
        sendJson(exchange, 200, groupToDetailMap(g));
    }

    private void handleUpdateGroup(HttpExchange exchange, String groupId, String body) throws IOException {
        Group g = dataStore.findGroupById(groupId);
        if (g == null) { sendJson(exchange, 404, errorMap("Group not found.")); return; }

        Map<String, Object> req = SimpleJson.parseObject(body);
        String name = (String) req.get("name");
        if (name != null && !name.trim().isEmpty()) {
            g.setName(name.trim());
            dataStore.updateGroup(g);
        }
        sendJson(exchange, 200, groupToDetailMap(g));
    }

    private void handleDeleteGroup(HttpExchange exchange, String groupId) throws IOException {
        dataStore.deleteGroup(groupId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        sendJson(exchange, 200, res);
    }

    private void handleAddMember(HttpExchange exchange, String groupId, String body) throws IOException {
        Map<String, Object> req = SimpleJson.parseObject(body);
        String name = (String) req.get("name");
        if (name == null || name.trim().isEmpty()) {
            sendJson(exchange, 400, errorMap("Member name cannot be empty."));
            return;
        }
        Member m = dataStore.addMember(groupId, name.trim());
        Map<String, Object> res = new HashMap<>();
        res.put("id", m.getId());
        res.put("name", m.getName());
        sendJson(exchange, 201, res);
    }

    private void handleRemoveMember(HttpExchange exchange, String groupId, String memberId) throws IOException {
        try {
            dataStore.removeMember(groupId, memberId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            sendJson(exchange, 200, res);
        } catch (Exception ex) {
            sendJson(exchange, 400, errorMap(ex.getMessage()));
        }
    }

    private void handleAddExpense(HttpExchange exchange, String groupId, String body) throws IOException {
        Group group = dataStore.findGroupById(groupId);
        if (group == null) { sendJson(exchange, 404, errorMap("Group not found.")); return; }

        Map<String, Object> req = SimpleJson.parseObject(body);
        Expense expense = parseExpenseFromMap(groupId, null, req);

        dataStore.addExpense(groupId, expense);
        sendJson(exchange, 201, expenseToMap(expense));
    }

    private void handleUpdateExpense(HttpExchange exchange, String groupId, String expenseId, String body) throws IOException {
        Group group = dataStore.findGroupById(groupId);
        if (group == null) { sendJson(exchange, 404, errorMap("Group not found.")); return; }

        Map<String, Object> req = SimpleJson.parseObject(body);
        Expense expense = parseExpenseFromMap(groupId, expenseId, req);

        dataStore.updateExpense(groupId, expense);
        sendJson(exchange, 200, expenseToMap(expense));
    }

    private void handleDeleteExpense(HttpExchange exchange, String groupId, String expenseId) throws IOException {
        dataStore.deleteExpense(groupId, expenseId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        sendJson(exchange, 200, res);
    }

    private void handleSettle(HttpExchange exchange, String groupId, String body) throws IOException {
        Group group = dataStore.findGroupById(groupId);
        if (group == null) { sendJson(exchange, 404, errorMap("Group not found.")); return; }

        Map<String, Object> req = SimpleJson.parseObject(body);
        String fromMemberId = (String) req.get("fromMemberId");
        String toMemberId = (String) req.get("toMemberId");
        long amountPaise = ((Number) req.get("amountPaise")).longValue();

        Member from = group.findMemberById(fromMemberId);
        Member to = group.findMemberById(toMemberId);
        if (from == null || to == null) {
            sendJson(exchange, 400, errorMap("Invalid members for settlement."));
            return;
        }

        String desc = "Settlement: " + from.getName() + " paid " + to.getName();
        Map<String, Long> shares = new HashMap<>();
        shares.put(toMemberId, amountPaise);

        String expId = "exp_settle_" + UUID.randomUUID().toString().substring(0, 8);
        Expense settleExp = new Expense(expId, groupId, desc, amountPaise, LocalDate.now(),
                fromMemberId, Category.SETTLEMENT, SplitType.EXACT, shares);

        dataStore.addExpense(groupId, settleExp);
        sendJson(exchange, 200, groupToDetailMap(group));
    }

    private void handleTransactions(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJson(exchange, 401, errorMap("Not authenticated.")); return; }

        List<Expense> allExp = dataStore.getAllExpensesForUser(user.getId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < allExp.size(); i++) {
            list.add(expenseToMap(allExp.get(i)));
        }
        sendJson(exchange, 200, list);
    }

    private void handleAnalytics(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJson(exchange, 401, errorMap("Not authenticated.")); return; }

        List<Expense> allExp = dataStore.getAllExpensesForUser(user.getId());
        long totalSpend = 0;
        Map<String, Long> catMap = new HashMap<>();
        TreeMap<LocalDate, Long> weekMap = new TreeMap<>();
        DateTimeFormatter weekFmt = DateTimeFormatter.ofPattern("d MMM");

        for (int i = 0; i < allExp.size(); i++) {
            Expense e = allExp.get(i);
            totalSpend += e.getAmountPaise();

            String cat = e.getCategory().getDisplayName();
            long cSum = catMap.containsKey(cat) ? catMap.get(cat) : 0L;
            catMap.put(cat, cSum + e.getAmountPaise());

            LocalDate monday = e.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long wSum = weekMap.containsKey(monday) ? weekMap.get(monday) : 0L;
            weekMap.put(monday, wSum + e.getAmountPaise());
        }

        List<Map<String, Object>> catList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : catMap.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("label", entry.getKey());
            m.put("amountPaise", entry.getValue());
            m.put("amountFormatted", Money.format(entry.getValue()));
            catList.add(m);
        }

        List<Map<String, Object>> weekList = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : weekMap.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("label", entry.getKey().format(weekFmt));
            m.put("amountPaise", entry.getValue());
            m.put("amountFormatted", Money.format(entry.getValue()));
            weekList.add(m);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("totalSpendPaise", totalSpend);
        res.put("totalSpendFormatted", Money.format(totalSpend));
        res.put("byCategory", catList);
        res.put("byWeek", weekList);

        sendJson(exchange, 200, res);
    }

    private void handleLoadDemo(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJson(exchange, 401, errorMap("Not authenticated.")); return; }

        dataStore.loadDemoData(user.getId());
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Demo data loaded for Aman, Bina, Charan, Diya.");
        sendJson(exchange, 200, res);
    }

    private void handleResetData(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJson(exchange, 401, errorMap("Not authenticated.")); return; }

        dataStore.deleteAllUserData(user.getId());
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        sendJson(exchange, 200, res);
    }

    private void handleGetSettings(HttpExchange exchange) throws IOException {
        Map<String, Object> res = new HashMap<>();
        res.put("theme", dataStore.getSetting("theme", "LIGHT"));
        sendJson(exchange, 200, res);
    }

    private void handleSaveSettings(HttpExchange exchange, String body) throws IOException {
        Map<String, Object> req = SimpleJson.parseObject(body);
        String theme = (String) req.get("theme");
        if (theme != null) {
            dataStore.setSetting("theme", theme);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("theme", dataStore.getSetting("theme", "LIGHT"));
        sendJson(exchange, 200, res);
    }

    // ==================== MAPPING HELPERS ====================

    private Expense parseExpenseFromMap(String groupId, String expenseId, Map<String, Object> req) {
        String desc = (String) req.get("description");
        if (desc == null || desc.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty.");
        }

        long amountPaise;
        if (req.get("amountPaise") != null) {
            amountPaise = ((Number) req.get("amountPaise")).longValue();
        } else if (req.get("amount") != null) {
            amountPaise = Money.parse(String.valueOf(req.get("amount")));
        } else {
            throw new IllegalArgumentException("Amount is required.");
        }

        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        LocalDate date = LocalDate.now();
        if (req.get("date") != null) {
            date = Validator.parseAndValidateDate(String.valueOf(req.get("date")));
        }

        String payerId = (String) req.get("payerMemberId");
        if (payerId == null || payerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payer must be selected.");
        }

        Category cat = Category.OTHER;
        if (req.get("category") != null) {
            cat = Category.valueOf(String.valueOf(req.get("category")));
        }

        SplitType splitType = SplitType.EQUAL;
        if (req.get("splitType") != null) {
            splitType = SplitType.valueOf(String.valueOf(req.get("splitType")));
        }

        Map<String, Long> shares = new HashMap<>();
        if (splitType == SplitType.EQUAL) {
            List<?> memberIdsRaw = (List<?>) req.get("memberIds");
            List<String> mIds = new ArrayList<>();
            if (memberIdsRaw != null) {
                for (Object o : memberIdsRaw) mIds.add(String.valueOf(o));
            }
            shares = SplitCalculator.calculateEqualSplit(amountPaise, mIds);
        } else if (splitType == SplitType.EXACT) {
            Map<?, ?> rawShares = (Map<?, ?>) req.get("shares");
            if (rawShares != null) {
                for (Map.Entry<?, ?> entry : rawShares.entrySet()) {
                    shares.put(String.valueOf(entry.getKey()), ((Number) entry.getValue()).longValue());
                }
            }
            SplitCalculator.validateExactSplit(amountPaise, shares);
        } else if (splitType == SplitType.PERCENTAGE) {
            List<?> memberIdsRaw = (List<?>) req.get("memberIds");
            List<String> mIds = new ArrayList<>();
            if (memberIdsRaw != null) {
                for (Object o : memberIdsRaw) mIds.add(String.valueOf(o));
            }
            Map<?, ?> rawPct = (Map<?, ?>) req.get("percentages");
            Map<String, Integer> bpMap = new HashMap<>();
            if (rawPct != null) {
                for (Map.Entry<?, ?> entry : rawPct.entrySet()) {
                    double val = Double.parseDouble(String.valueOf(entry.getValue()));
                    bpMap.put(String.valueOf(entry.getKey()), (int) Math.round(val * 100.0));
                }
            }
            shares = SplitCalculator.calculatePercentageSplit(amountPaise, mIds, bpMap);
        }

        String id = expenseId != null ? expenseId : "exp_" + UUID.randomUUID().toString().substring(0, 8);
        return new Expense(id, groupId, desc.trim(), amountPaise, date, payerId, cat, splitType, shares);
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("displayName", user.getDisplayName());
        return m;
    }

    private Map<String, Object> groupToSummaryMap(Group g) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", g.getId());
        m.put("name", g.getName());
        m.put("memberCount", g.getMembers().size());
        m.put("expenseCount", g.getExpenses().size());

        long totalExp = 0;
        for (int i = 0; i < g.getExpenses().size(); i++) {
            totalExp += g.getExpenses().get(i).getAmountPaise();
        }
        m.put("totalExpensePaise", totalExp);
        m.put("totalExpenseFormatted", Money.format(totalExp));

        List<String> memberNames = new ArrayList<>();
        for (int i = 0; i < g.getMembers().size(); i++) {
            memberNames.add(g.getMembers().get(i).getName());
        }
        m.put("memberNames", memberNames);
        return m;
    }

    private Map<String, Object> groupToDetailMap(Group g) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", g.getId());
        m.put("name", g.getName());

        List<Map<String, Object>> memList = new ArrayList<>();
        for (int i = 0; i < g.getMembers().size(); i++) {
            Member mem = g.getMembers().get(i);
            Map<String, Object> mm = new HashMap<>();
            mm.put("id", mem.getId());
            mm.put("name", mem.getName());
            mm.put("inExpenses", g.isMemberInExpenses(mem.getId()));
            memList.add(mm);
        }
        m.put("members", memList);

        List<Map<String, Object>> expList = new ArrayList<>();
        for (int i = 0; i < g.getExpenses().size(); i++) {
            expList.add(expenseToMap(g.getExpenses().get(i)));
        }
        m.put("expenses", expList);

        // Balances
        List<BalanceCalculator.MemberBalance> bList = BalanceCalculator.calculateGroupBalances(g);
        List<Map<String, Object>> balMaps = new ArrayList<>();
        for (int i = 0; i < bList.size(); i++) {
            BalanceCalculator.MemberBalance b = bList.get(i);
            Map<String, Object> bm = new HashMap<>();
            bm.put("memberId", b.getMember().getId());
            bm.put("memberName", b.getMember().getName());
            bm.put("paidPaise", b.getPaidPaise());
            bm.put("paidFormatted", Money.format(b.getPaidPaise()));
            bm.put("sharePaise", b.getSharePaise());
            bm.put("shareFormatted", Money.format(b.getSharePaise()));
            bm.put("netPaise", b.getNetPaise());
            bm.put("netFormatted", Money.format(b.getNetPaise()));
            balMaps.add(bm);
        }
        m.put("balances", balMaps);

        // Settle-up transfers
        List<Transfer> transfers = SettlementPlanner.planSettlements(bList);
        List<Map<String, Object>> transferMaps = new ArrayList<>();
        for (int i = 0; i < transfers.size(); i++) {
            Transfer t = transfers.get(i);
            Map<String, Object> tm = new HashMap<>();
            tm.put("fromMemberId", t.getFromMember().getId());
            tm.put("fromMemberName", t.getFromMember().getName());
            tm.put("toMemberId", t.getToMember().getId());
            tm.put("toMemberName", t.getToMember().getName());
            tm.put("amountPaise", t.getAmountPaise());
            tm.put("amountFormatted", Money.format(t.getAmountPaise()));
            transferMaps.add(tm);
        }
        m.put("transfers", transferMaps);

        return m;
    }

    private Map<String, Object> expenseToMap(Expense e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("groupId", e.getGroupId());
        Group g = dataStore.findGroupById(e.getGroupId());
        m.put("groupName", g != null ? g.getName() : "Unknown");
        m.put("description", e.getDescription());
        m.put("amountPaise", e.getAmountPaise());
        m.put("amountFormatted", Money.format(e.getAmountPaise()));
        m.put("amountPlain", Money.formatPlain(e.getAmountPaise()));
        m.put("date", e.getDate().toString());
        m.put("payerMemberId", e.getPayerMemberId());
        Member payer = g != null ? g.findMemberById(e.getPayerMemberId()) : null;
        m.put("payerName", payer != null ? payer.getName() : "Unknown");
        m.put("category", e.getCategory().name());
        m.put("categoryDisplayName", e.getCategory().getDisplayName());
        m.put("splitType", e.getSplitType().name());
        m.put("splitTypeDisplayName", e.getSplitType().getDisplayName());
        m.put("shares", e.getShares());
        return m;
    }

    private Map<String, Object> errorMap(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("error", msg != null ? msg : "An error occurred.");
        return m;
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = SimpleJson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    // ==================== STATIC FILE HANDLER ====================

    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                file = new File("web/index.html");
            }

            if (!file.exists()) {
                String notFound = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String contentType = "text/plain";
            String fn = file.getName().toLowerCase();
            if (fn.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (fn.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (fn.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (fn.endsWith(".svg")) contentType = "image/svg+xml";
            else if (fn.endsWith(".json")) contentType = "application/json; charset=UTF-8";

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (InputStream in = new FileInputStream(file);
                 OutputStream out = exchange.getResponseBody()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
        }
    }
}
