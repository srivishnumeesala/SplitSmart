/**
 * SplitSmart — Front-End Application Logic (ES6 Vanilla)
 */

let currentUser = null;
let currentGroup = null;
let editingExpenseId = null;
let editingGroupId = null;
let modalMembers = [];
let allTransactions = [];

// Initialize application on DOM load
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    checkAuth();
});

// ==================== THEME MANAGEMENT ====================

function initTheme() {
    fetch('/api/settings')
        .then(res => res.json())
        .then(data => {
            if (data.theme === 'DARK') {
                document.body.className = 'theme-dark';
                updateThemeButton(true);
            } else {
                document.body.className = 'theme-light';
                updateThemeButton(false);
            }
        })
        .catch(() => {
            document.body.className = 'theme-light';
        });
}

function toggleTheme() {
    const isDark = document.body.classList.contains('theme-dark');
    const newTheme = isDark ? 'LIGHT' : 'DARK';
    document.body.className = isDark ? 'theme-light' : 'theme-dark';
    updateThemeButton(!isDark);

    fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ theme: newTheme })
    });
}

function updateThemeButton(isDark) {
    const btn = document.getElementById('themeToggleBtn');
    if (btn) {
        btn.textContent = isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode';
    }
}

// ==================== AUTHENTICATION ====================

function checkAuth() {
    fetch('/api/auth/me')
        .then(res => {
            if (!res.ok) throw new Error();
            return res.json();
        })
        .then(user => {
            onLoginSuccess(user);
        })
        .catch(() => {
            showAuthView();
        });
}

function toggleAuthMode(isRegister) {
    document.getElementById('loginForm').classList.toggle('hidden', isRegister);
    document.getElementById('registerForm').classList.toggle('hidden', !isRegister);
    document.getElementById('loginError').textContent = '';
    document.getElementById('registerError').textContent = '';
}

async function handleLoginSubmit(e) {
    e.preventDefault();
    const errorEl = document.getElementById('loginError');
    errorEl.textContent = '';

    const username = document.getElementById('loginUser').value.trim();
    const password = document.getElementById('loginPass').value;

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (!res.ok) {
            errorEl.textContent = data.error || 'Login failed.';
            return;
        }
        onLoginSuccess(data.user);
    } catch (err) {
        errorEl.textContent = 'Network error while logging in.';
    }
}

async function handleRegisterSubmit(e) {
    e.preventDefault();
    const errorEl = document.getElementById('registerError');
    errorEl.textContent = '';

    const displayName = document.getElementById('regName').value.trim();
    const username = document.getElementById('regUser').value.trim();
    const password = document.getElementById('regPass').value;
    const confirm = document.getElementById('regPassConfirm').value;

    if (password !== confirm) {
        errorEl.textContent = 'Passwords do not match.';
        return;
    }

    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ displayName, username, password })
        });
        const data = await res.json();
        if (!res.ok) {
            errorEl.textContent = data.error || 'Registration failed.';
            return;
        }
        onLoginSuccess(data.user);
    } catch (err) {
        errorEl.textContent = 'Network error while registering.';
    }
}

async function handleLogout() {
    await fetch('/api/auth/logout', { method: 'POST' });
    currentUser = null;
    showAuthView();
}

function onLoginSuccess(user) {
    currentUser = user;
    document.getElementById('sidebarUserName').textContent = user.displayName;
    document.getElementById('sidebarUserHandle').textContent = '@' + user.username;

    document.getElementById('authContainer').classList.add('hidden');
    document.getElementById('appContainer').classList.remove('hidden');

    navigateTo('dashboard');
}

function showAuthView() {
    document.getElementById('authContainer').classList.remove('hidden');
    document.getElementById('appContainer').classList.add('hidden');
}

// ==================== NAVIGATION ====================

function navigateTo(viewName) {
    document.querySelectorAll('.view-page').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));

    const navMap = {
        dashboard: 0,
        groups: 1,
        transactions: 2,
        analytics: 3,
        settings: 4
    };

    const targetSection = document.getElementById('view' + capitalize(viewName));
    if (targetSection) {
        targetSection.classList.add('active');
    }

    const navItems = document.querySelectorAll('.nav-item');
    if (navMap[viewName] !== undefined && navItems[navMap[viewName]]) {
        navItems[navMap[viewName]].classList.add('active');
    }

    if (viewName === 'dashboard') loadDashboard();
    else if (viewName === 'groups') loadGroups();
    else if (viewName === 'transactions') loadTransactions();
    else if (viewName === 'analytics') loadAnalytics();
}

function capitalize(s) {
    return s.charAt(0).toUpperCase() + s.slice(1);
}

// ==================== DASHBOARD ====================

async function loadDashboard() {
    const greetingEl = document.getElementById('dashboardGreeting');
    const dateEl = document.getElementById('dashboardDate');

    if (currentUser) {
        greetingEl.textContent = `Welcome back, ${currentUser.displayName}`;
    }

    const now = new Date();
    dateEl.textContent = now.toLocaleDateString('en-IN', {
        weekday: 'long',
        day: 'numeric',
        month: 'short',
        year: 'numeric'
    });

    try {
        const res = await fetch('/api/dashboard');
        const data = await res.json();

        document.getElementById('kpiOwed').textContent = data.totalReceivableFormatted;
        document.getElementById('kpiOwe').textContent = data.totalPayableFormatted;
        const netEl = document.getElementById('kpiNet');
        netEl.textContent = data.netOverallFormatted;
        if (data.netOverallPaise > 0) netEl.className = 'summary-val text-accent';
        else if (data.netOverallPaise < 0) netEl.className = 'summary-val text-rose';
        else netEl.className = 'summary-val text-muted';

        // Groups summary list
        const groupsContainer = document.getElementById('dashGroupsList');
        groupsContainer.innerHTML = '';
        if (data.groups && data.groups.length > 0) {
            data.groups.slice(0, 4).forEach(g => {
                const item = document.createElement('div');
                item.className = 'dash-group-item';
                item.innerHTML = `
                    <div>
                        <div style="font-weight: 600;">${escapeHtml(g.name)}</div>
                        <div style="font-size: 11px; color: var(--muted);">${g.memberCount} members • ${g.expenseCount} expenses</div>
                    </div>
                    <button class="btn btn-secondary btn-sm" onclick="openGroupDetail('${g.id}')">Open</button>
                `;
                groupsContainer.appendChild(item);
            });
        } else {
            groupsContainer.innerHTML = '<div style="color: var(--muted); padding: 12px; font-size: 13px;">No groups yet. Click "My Groups" to create one.</div>';
        }

        // Category chart
        renderCanvasBarChart('dashCategoryChart', data.categorySpending || []);

        // Recent expenses table
        const tbody = document.getElementById('recentTableBody');
        tbody.innerHTML = '';
        if (data.recentExpenses && data.recentExpenses.length > 0) {
            data.recentExpenses.forEach(e => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${e.date}</td>
                    <td style="font-weight: 500;">${escapeHtml(e.description)}</td>
                    <td>${escapeHtml(e.groupName)}</td>
                    <td>${escapeHtml(e.payerName)}</td>
                    <td><span class="chip">${escapeHtml(e.categoryDisplayName)}</span></td>
                    <td class="text-right" style="font-weight: 600;">${e.amountFormatted}</td>
                `;
                tbody.appendChild(tr);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center" style="color: var(--muted); padding: 24px;">No recent expenses recorded yet.</td></tr>';
        }

    } catch (err) {
        console.error('Failed to load dashboard:', err);
    }
}

// ==================== GROUPS ====================

async function loadGroups() {
    const grid = document.getElementById('groupsGrid');
    grid.innerHTML = '<div style="color: var(--muted);">Loading groups...</div>';

    try {
        const res = await fetch('/api/groups');
        const groups = await res.json();
        grid.innerHTML = '';

        if (groups.length === 0) {
            grid.innerHTML = `
                <div class="glossy-panel" style="grid-column: 1 / -1; padding: 40px; text-align: center;">
                    <div style="font-size: 16px; font-weight: 600; margin-bottom: 8px;">No groups yet</div>
                    <p style="color: var(--muted); font-size: 13px; margin-bottom: 16px;">Create a group to start sharing expenses with friends.</p>
                    <button class="btn btn-primary" onclick="openCreateGroupModal()">+ Create Your First Group</button>
                </div>
            `;
            return;
        }

        groups.forEach(g => {
            const card = document.createElement('div');
            card.className = 'glossy-panel group-card';
            const membersText = g.memberNames.join(', ');

            card.innerHTML = `
                <div class="group-card-top">
                    <div>
                        <div style="font-size: 18px; font-weight: 700;">${escapeHtml(g.name)}</div>
                        <div style="font-size: 12px; color: var(--muted); margin-top: 4px;">${g.memberCount} Members: ${escapeHtml(membersText)}</div>
                    </div>
                    <div style="font-size: 16px; font-weight: 700; color: var(--accent);">${g.totalExpenseFormatted}</div>
                </div>
                <div style="font-size: 12px; color: var(--muted);">${g.expenseCount} recorded expenses</div>
                <div class="group-card-actions">
                    <button class="btn btn-secondary btn-sm" onclick="handleDeleteGroup('${g.id}', '${escapeHtml(g.name)}')">Delete</button>
                    <button class="btn btn-secondary btn-sm" onclick="openEditGroupModal('${g.id}', '${escapeHtml(g.name)}')">Edit</button>
                    <button class="btn btn-primary btn-sm" onclick="openGroupDetail('${g.id}')">Open</button>
                </div>
            `;
            grid.appendChild(card);
        });
    } catch (err) {
        grid.innerHTML = '<div style="color: var(--owe-rose);">Failed to load groups.</div>';
    }
}

// ==================== GROUP DETAIL ====================

async function openGroupDetail(groupId) {
    navigateTo('groupDetail');
    const res = await fetch(`/api/groups/${groupId}`);
    currentGroup = await res.json();

    document.getElementById('groupDetailName').textContent = currentGroup.name;

    // Render Member chips
    const chipsContainer = document.getElementById('groupDetailMembers');
    chipsContainer.innerHTML = '';
    currentGroup.members.forEach(m => {
        const chip = document.createElement('span');
        chip.className = 'chip';
        chip.textContent = m.name;
        chipsContainer.appendChild(chip);
    });

    // Render Balances Table
    const balTbody = document.getElementById('groupBalancesBody');
    balTbody.innerHTML = '';
    currentGroup.balances.forEach(b => {
        const tr = document.createElement('tr');
        let netColorClass = 'text-muted';
        if (b.netPaise > 0) netColorClass = 'text-accent';
        else if (b.netPaise < 0) netColorClass = 'text-rose';

        tr.innerHTML = `
            <td style="font-weight: 600;">${escapeHtml(b.memberName)}</td>
            <td class="text-right">${b.paidFormatted}</td>
            <td class="text-right">${b.shareFormatted}</td>
            <td class="text-right ${netColorClass}" style="font-weight: 700;">${b.netFormatted}</td>
        `;
        balTbody.appendChild(tr);
    });

    // Render Settle-Up Transfers
    const transfersContainer = document.getElementById('settleTransfersList');
    transfersContainer.innerHTML = '';
    const titleEl = document.getElementById('settlePlanTitle');
    titleEl.textContent = `Settle-Up Plan (${currentGroup.transfers.length} transfers)`;

    if (currentGroup.transfers.length === 0) {
        transfersContainer.innerHTML = '<div style="color: var(--muted); font-size: 13px; padding: 12px 0;">All balances are settled! No transfers needed.</div>';
    } else {
        currentGroup.transfers.forEach(t => {
            const row = document.createElement('div');
            row.className = 'transfer-row';
            row.innerHTML = `
                <div>
                    <strong>${escapeHtml(t.fromMemberName)}</strong> pays <strong>${escapeHtml(t.toMemberName)}</strong>
                    <span style="margin-left: 8px; color: var(--accent); font-weight: 700;">${t.amountFormatted}</span>
                </div>
                <button class="btn btn-primary btn-sm" onclick="handleMarkPaid('${t.fromMemberId}', '${t.toMemberId}', ${t.amountPaise})">Mark as Paid</button>
            `;
            transfersContainer.appendChild(row);
        });
    }

    // Render Expenses Ledger
    const expTbody = document.getElementById('groupExpensesBody');
    expTbody.innerHTML = '';
    if (currentGroup.expenses.length === 0) {
        expTbody.innerHTML = '<tr><td colspan="7" class="text-center" style="color: var(--muted); padding: 24px;">No expenses recorded yet. Click "+ Add Expense" above.</td></tr>';
    } else {
        currentGroup.expenses.forEach(e => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${e.date}</td>
                <td style="font-weight: 500;">${escapeHtml(e.description)}</td>
                <td>${escapeHtml(e.payerName)}</td>
                <td><span class="chip">${escapeHtml(e.categoryDisplayName)}</span></td>
                <td style="color: var(--muted); font-size: 12px;">${escapeHtml(e.splitTypeDisplayName)}</td>
                <td class="text-right" style="font-weight: 600;">${e.amountFormatted}</td>
                <td class="text-center">
                    <button class="btn btn-secondary btn-sm" onclick="openEditExpenseModal('${e.id}')">Edit</button>
                    <button class="btn btn-secondary btn-sm" style="color: var(--owe-rose);" onclick="handleDeleteExpense('${e.id}', '${escapeHtml(e.description)}')">Delete</button>
                </td>
            `;
            expTbody.appendChild(tr);
        });
    }
}

async function handleMarkPaid(fromMemberId, toMemberId, amountPaise) {
    if (!currentGroup) return;
    try {
        const res = await fetch(`/api/groups/${currentGroup.id}/settle`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fromMemberId, toMemberId, amountPaise })
        });
        if (res.ok) {
            showToast('Settlement recorded! Balances updated.');
            openGroupDetail(currentGroup.id);
        }
    } catch (err) {
        showToast('Failed to record settlement.');
    }
}

async function handleDeleteExpense(expenseId, desc) {
    if (!currentGroup) return;
    if (!confirm(`Are you sure you want to delete expense "${desc}"?`)) return;

    try {
        const res = await fetch(`/api/groups/${currentGroup.id}/expenses/${expenseId}`, {
            method: 'DELETE'
        });
        if (res.ok) {
            showToast('Expense deleted.');
            openGroupDetail(currentGroup.id);
        }
    } catch (err) {
        showToast('Failed to delete expense.');
    }
}

async function handleDeleteGroup(groupId, name) {
    if (!confirm(`Are you sure you want to delete group "${name}" and all its expenses?`)) return;

    try {
        const res = await fetch(`/api/groups/${groupId}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('Group deleted.');
            loadGroups();
        }
    } catch (err) {
        showToast('Failed to delete group.');
    }
}

// ==================== TRANSACTIONS ====================

async function loadTransactions() {
    try {
        const res = await fetch('/api/transactions');
        allTransactions = await res.json();

        // Populate groups filter
        const groupSelect = document.getElementById('filterGroup');
        groupSelect.innerHTML = '<option value="">All Groups</option>';
        const groupsRes = await fetch('/api/groups');
        const groups = await groupsRes.json();
        groups.forEach(g => {
            const opt = document.createElement('option');
            opt.value = g.name;
            opt.textContent = g.name;
            groupSelect.appendChild(opt);
        });

        // Populate category filter
        const catSelect = document.getElementById('filterCategory');
        catSelect.innerHTML = '<option value="">All Categories</option>';
        const cats = [
            { val: 'FOOD', label: 'Food & Dining' },
            { val: 'GROCERIES', label: 'Groceries' },
            { val: 'TRAVEL', label: 'Travel & Transport' },
            { val: 'ENTERTAINMENT', label: 'Entertainment' },
            { val: 'UTILITIES', label: 'Utilities & Bills' },
            { val: 'RENT', label: 'Rent & Stay' },
            { val: 'SETTLEMENT', label: 'Settlement' },
            { val: 'OTHER', label: 'Other' }
        ];
        cats.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.val;
            opt.textContent = c.label;
            catSelect.appendChild(opt);
        });

        applyTransactionFilters();
    } catch (err) {
        console.error('Failed to load transactions:', err);
    }
}

function applyTransactionFilters() {
    const search = document.getElementById('filterSearch').value.toLowerCase().trim();
    const group = document.getElementById('filterGroup').value;
    const cat = document.getElementById('filterCategory').value;

    const filtered = allTransactions.filter(e => {
        if (group && e.groupName !== group) return false;
        if (cat && e.category !== cat) return false;
        if (search) {
            const mDesc = e.description.toLowerCase().includes(search);
            const mPayer = e.payerName.toLowerCase().includes(search);
            const mGroup = e.groupName.toLowerCase().includes(search);
            if (!mDesc && !mPayer && !mGroup) return false;
        }
        return true;
    });

    const tbody = document.getElementById('transactionsTableBody');
    tbody.innerHTML = '';
    let totalPaise = 0;

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center" style="color: var(--muted); padding: 24px;">No transactions match your filters.</td></tr>';
    } else {
        filtered.forEach(e => {
            totalPaise += e.amountPaise;
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${e.date}</td>
                <td style="font-weight: 500;">${escapeHtml(e.description)}</td>
                <td>${escapeHtml(e.groupName)}</td>
                <td>${escapeHtml(e.payerName)}</td>
                <td><span class="chip">${escapeHtml(e.categoryDisplayName)}</span></td>
                <td style="color: var(--muted); font-size: 12px;">${escapeHtml(e.splitTypeDisplayName)}</td>
                <td class="text-right" style="font-weight: 600;">${e.amountFormatted}</td>
            `;
            tbody.appendChild(tr);
        });
    }

    const rupees = (totalPaise / 100).toFixed(2);
    document.getElementById('transactionsSummary').textContent =
        `Showing ${filtered.length} of ${allTransactions.length} transactions  •  Filtered Total: ₹${formatIndianNumber(rupees)}`;
}

function resetTransactionFilters() {
    document.getElementById('filterSearch').value = '';
    document.getElementById('filterGroup').value = '';
    document.getElementById('filterCategory').value = '';
    applyTransactionFilters();
}

// ==================== ANALYTICS ====================

async function loadAnalytics() {
    try {
        const res = await fetch('/api/analytics');
        const data = await res.json();

        document.getElementById('analyticsTotalSpend').textContent = data.totalSpendFormatted;

        let topCat = 'None';
        let maxVal = 0;
        (data.byCategory || []).forEach(c => {
            if (c.amountPaise > maxVal) {
                maxVal = c.amountPaise;
                topCat = c.label;
            }
        });
        document.getElementById('analyticsTopCat').textContent = topCat;

        const groupsRes = await fetch('/api/groups');
        const groups = await groupsRes.json();
        document.getElementById('analyticsGroupCount').textContent = groups.length;

        // Render charts
        renderCanvasBarChart('analyticsCategoryChart', (data.byCategory || []).map(c => ({
            category: c.label,
            amountPaise: c.amountPaise,
            amountFormatted: c.amountFormatted
        })));

        renderCanvasBarChart('analyticsWeeklyChart', (data.byWeek || []).map(w => ({
            category: w.label,
            amountPaise: w.amountPaise,
            amountFormatted: w.amountFormatted
        })));
    } catch (err) {
        console.error('Failed to load analytics:', err);
    }
}

// ==================== MODALS ====================

function openCreateGroupModal() {
    editingGroupId = null;
    document.getElementById('groupModalTitle').textContent = 'Create Group';
    document.getElementById('groupNameInput').value = '';
    document.getElementById('newMemberNameInput').value = '';
    modalMembers = ['Aman', 'Bina'];
    renderModalMemberChips();
    document.getElementById('groupModalError').textContent = '';
    document.getElementById('groupModal').classList.remove('hidden');
}

async function openEditGroupModal(groupId, name) {
    editingGroupId = groupId;
    document.getElementById('groupModalTitle').textContent = 'Edit Group';
    document.getElementById('groupNameInput').value = name;
    document.getElementById('newMemberNameInput').value = '';

    const res = await fetch(`/api/groups/${groupId}`);
    const g = await res.json();
    modalMembers = g.members.map(m => m.name);
    renderModalMemberChips();
    document.getElementById('groupModalError').textContent = '';
    document.getElementById('groupModal').classList.remove('hidden');
}

function handleAddMemberChip() {
    const input = document.getElementById('newMemberNameInput');
    const name = input.value.trim();
    if (!name) return;
    if (modalMembers.some(m => m.toLowerCase() === name.toLowerCase())) {
        document.getElementById('groupModalError').textContent = 'Member already in list.';
        return;
    }
    modalMembers.push(name);
    input.value = '';
    document.getElementById('groupModalError').textContent = '';
    renderModalMemberChips();
}

function handleRemoveMemberChip(index) {
    if (modalMembers.length <= 2) {
        document.getElementById('groupModalError').textContent = 'A group must have at least 2 members.';
        return;
    }
    modalMembers.splice(index, 1);
    renderModalMemberChips();
}

function renderModalMemberChips() {
    const container = document.getElementById('modalMembersList');
    container.innerHTML = '';
    modalMembers.forEach((m, idx) => {
        const chip = document.createElement('span');
        chip.className = 'chip-editable';
        chip.innerHTML = `${escapeHtml(m)} <button type="button" onclick="handleRemoveMemberChip(${idx})">×</button>`;
        container.appendChild(chip);
    });
}

async function handleSaveGroup(e) {
    e.preventDefault();
    const name = document.getElementById('groupNameInput').value.trim();
    if (!name) return;

    if (modalMembers.length < 2) {
        document.getElementById('groupModalError').textContent = 'A group must have at least 2 members.';
        return;
    }

    try {
        let res;
        if (editingGroupId) {
            res = await fetch(`/api/groups/${editingGroupId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name })
            });
        } else {
            res = await fetch('/api/groups', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, members: modalMembers })
            });
        }
        if (res.ok) {
            closeModal('groupModal');
            showToast(editingGroupId ? 'Group updated.' : 'Group created.');
            loadGroups();
        } else {
            const err = await res.json();
            document.getElementById('groupModalError').textContent = err.error || 'Failed to save group.';
        }
    } catch (err) {
        document.getElementById('groupModalError').textContent = 'Network error saving group.';
    }
}

// ==================== EXPENSE MODAL ====================

function openAddExpenseModal() {
    if (!currentGroup) return;
    editingExpenseId = null;
    document.getElementById('expenseModalTitle').textContent = 'Record Expense';
    document.getElementById('expDescInput').value = '';
    document.getElementById('expAmountInput').value = '';
    document.getElementById('expDateInput').value = new Date().toISOString().split('T')[0];
    document.getElementById('expCategorySelect').value = 'FOOD';
    document.getElementById('expSplitTypeSelect').value = 'EQUAL';

    populatePayerSelect();
    updateModalSharesUI();

    document.getElementById('expenseModalError').textContent = '';
    document.getElementById('expenseModal').classList.remove('hidden');
}

function openEditExpenseModal(expenseId) {
    if (!currentGroup) return;
    const exp = currentGroup.expenses.find(e => e.id === expenseId);
    if (!exp) return;

    editingExpenseId = expenseId;
    document.getElementById('expenseModalTitle').textContent = 'Edit Expense';
    document.getElementById('expDescInput').value = exp.description;
    document.getElementById('expAmountInput').value = exp.amountPlain;
    document.getElementById('expDateInput').value = exp.date;
    document.getElementById('expCategorySelect').value = exp.category;
    document.getElementById('expSplitTypeSelect').value = exp.splitType;

    populatePayerSelect(exp.payerMemberId);
    updateModalSharesUI(exp.shares);

    document.getElementById('expenseModalError').textContent = '';
    document.getElementById('expenseModal').classList.remove('hidden');
}

function populatePayerSelect(selectedId) {
    const sel = document.getElementById('expPayerSelect');
    sel.innerHTML = '';
    currentGroup.members.forEach(m => {
        const opt = document.createElement('option');
        opt.value = m.id;
        opt.textContent = m.name;
        if (selectedId && m.id === selectedId) opt.selected = true;
        sel.appendChild(opt);
    });
}

function updateModalSharesUI(prefillShares) {
    const splitType = document.getElementById('expSplitTypeSelect').value;
    const container = document.getElementById('expSharesContainer');
    container.innerHTML = '';

    const amountStr = document.getElementById('expAmountInput').value.trim();
    const amountFloat = parseFloat(amountStr) || 0;

    currentGroup.members.forEach(m => {
        const row = document.createElement('div');
        row.className = 'share-row';

        const check = document.createElement('input');
        check.type = 'checkbox';
        check.id = 'share_cb_' + m.id;
        check.checked = prefillShares ? (prefillShares[m.id] !== undefined) : true;

        const label = document.createElement('label');
        label.htmlFor = 'share_cb_' + m.id;
        label.textContent = m.name;
        label.style.marginLeft = '8px';
        label.style.cursor = 'pointer';

        const left = document.createElement('div');
        left.appendChild(check);
        left.appendChild(label);
        row.appendChild(left);

        if (splitType === 'EXACT') {
            const input = document.createElement('input');
            input.type = 'text';
            input.id = 'share_val_' + m.id;
            input.placeholder = '₹0.00';
            if (prefillShares && prefillShares[m.id]) {
                input.value = (prefillShares[m.id] / 100).toFixed(2);
            }
            row.appendChild(input);
        } else if (splitType === 'PERCENTAGE') {
            const wrap = document.createElement('div');
            const input = document.createElement('input');
            input.type = 'text';
            input.id = 'share_val_' + m.id;
            input.placeholder = '0';
            if (prefillShares && prefillShares[m.id] && amountFloat > 0) {
                const pct = ((prefillShares[m.id] / 100) / amountFloat) * 100;
                input.value = pct.toFixed(1).replace('.0', '');
            }
            wrap.appendChild(input);
            wrap.appendChild(document.createTextNode(' %'));
            row.appendChild(wrap);
        }

        container.appendChild(row);
    });
}

async function handleSaveExpense(e) {
    e.preventDefault();
    const errEl = document.getElementById('expenseModalError');
    errEl.textContent = '';

    const desc = document.getElementById('expDescInput').value.trim();
    const amountStr = document.getElementById('expAmountInput').value.trim();
    const date = document.getElementById('expDateInput').value;
    const payerMemberId = document.getElementById('expPayerSelect').value;
    const category = document.getElementById('expCategorySelect').value;
    const splitType = document.getElementById('expSplitTypeSelect').value;

    if (!desc || !amountStr || !date || !payerMemberId) {
        errEl.textContent = 'Please fill in all required fields.';
        return;
    }

    const selectedMemberIds = [];
    currentGroup.members.forEach(m => {
        const cb = document.getElementById('share_cb_' + m.id);
        if (cb && cb.checked) selectedMemberIds.push(m.id);
    });

    if (selectedMemberIds.length === 0) {
        errEl.textContent = 'Select at least one member to share this expense.';
        return;
    }

    const payload = {
        description: desc,
        amount: amountStr,
        date: date,
        payerMemberId: payerMemberId,
        category: category,
        splitType: splitType,
        memberIds: selectedMemberIds
    };

    if (splitType === 'EXACT') {
        const shares = {};
        for (const mid of selectedMemberIds) {
            const valInput = document.getElementById('share_val_' + mid);
            const val = parseFloat(valInput ? valInput.value : '0') || 0;
            shares[mid] = Math.round(val * 100);
        }
        payload.shares = shares;
    } else if (splitType === 'PERCENTAGE') {
        const percentages = {};
        for (const mid of selectedMemberIds) {
            const valInput = document.getElementById('share_val_' + mid);
            percentages[mid] = parseFloat(valInput ? valInput.value : '0') || 0;
        }
        payload.percentages = percentages;
    }

    try {
        let res;
        if (editingExpenseId) {
            res = await fetch(`/api/groups/${currentGroup.id}/expenses/${editingExpenseId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            res = await fetch(`/api/groups/${currentGroup.id}/expenses`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }

        if (res.ok) {
            closeModal('expenseModal');
            showToast(editingExpenseId ? 'Expense updated.' : 'Expense recorded.');
            openGroupDetail(currentGroup.id);
        } else {
            const err = await res.json();
            errEl.textContent = err.error || 'Failed to save expense.';
        }
    } catch (err) {
        errEl.textContent = 'Network error saving expense.';
    }
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.add('hidden');
}

// ==================== SETTINGS ACTIONS ====================

async function loadDemoData() {
    try {
        const res = await fetch('/api/demo', { method: 'POST' });
        if (res.ok) {
            showToast('Demo data loaded for Aman, Bina, Charan, Diya!');
            loadDashboard();
        }
    } catch (err) {
        showToast('Failed to load demo data.');
    }
}

async function resetAllData() {
    if (!confirm('Are you sure you want to permanently delete all your groups and expenses?')) return;
    try {
        const res = await fetch('/api/reset', { method: 'POST' });
        if (res.ok) {
            showToast('All user data deleted.');
            loadDashboard();
        }
    } catch (err) {
        showToast('Failed to reset data.');
    }
}

// ==================== CANVAS CHART RENDERING ====================

function renderCanvasBarChart(canvasId, items) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();

    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    const w = rect.width;
    const h = rect.height;

    ctx.clearRect(0, 0, w, h);

    if (!items || items.length === 0) {
        ctx.fillStyle = getComputedStyle(document.body).getPropertyValue('--muted').trim();
        ctx.font = '13px ' + getComputedStyle(document.body).getPropertyValue('--font-family').trim();
        ctx.textAlign = 'center';
        ctx.fillText('No data to display', w / 2, h / 2);
        return;
    }

    let maxVal = 0;
    items.forEach(it => {
        const amt = it.amountPaise !== undefined ? it.amountPaise : 0;
        if (amt > maxVal) maxVal = amt;
    });

    if (maxVal === 0) maxVal = 1;

    const padLeft = 20;
    const padRight = 20;
    const padBottom = 28;
    const padTop = 24;
    const plotW = w - padLeft - padRight;
    const plotH = h - padTop - padBottom;
    const baselineY = h - padBottom;

    // Baseline
    ctx.strokeStyle = getComputedStyle(document.body).getPropertyValue('--border').trim();
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padLeft, baselineY);
    ctx.lineTo(w - padRight, baselineY);
    ctx.stroke();

    const n = items.length;
    const slotW = plotW / n;
    const barW = Math.max(16, Math.min(44, slotW - 14));

    const accent = getComputedStyle(document.body).getPropertyValue('--accent').trim();
    const muted = getComputedStyle(document.body).getPropertyValue('--muted').trim();
    const text = getComputedStyle(document.body).getPropertyValue('--text').trim();
    const font = getComputedStyle(document.body).getPropertyValue('--font-family').trim();

    items.forEach((it, i) => {
        const centerX = padLeft + (i * slotW) + (slotW / 2);
        const barX = centerX - (barW / 2);

        const amt = it.amountPaise !== undefined ? it.amountPaise : 0;
        let barHeight = (amt / maxVal) * (plotH - 10);
        barHeight = Math.max(3, barHeight);
        const barY = baselineY - barHeight;

        // Draw bar
        const grad = ctx.createLinearGradient(0, barY, 0, baselineY);
        grad.addColorStop(0, accent);
        grad.addColorStop(1, '#C76212');
        ctx.fillStyle = grad;

        drawRoundedRect(ctx, barX, barY, barW, barHeight, 5);
        ctx.fill();

        // Amount on top
        ctx.fillStyle = text;
        ctx.font = '600 11px ' + font;
        ctx.textAlign = 'center';
        const formatted = it.amountFormatted || ('₹' + (amt / 100).toFixed(0));
        ctx.fillText(formatted, centerX, barY - 5);

        // Label below
        ctx.fillStyle = muted;
        ctx.font = '11px ' + font;
        let label = it.category || it.label || '';
        if (label.length > 9) label = label.substring(0, 8) + '…';
        ctx.fillText(label, centerX, baselineY + 16);
    });
}

function drawRoundedRect(ctx, x, y, width, height, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height);
    ctx.lineTo(x, y + height);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
}

// ==================== UTILITIES ====================

function formatIndianNumber(numStr) {
    const parts = numStr.split('.');
    let x = parts[0];
    const dec = parts.length > 1 ? '.' + parts[1] : '';
    const lastThree = x.substring(x.length - 3);
    const otherNumbers = x.substring(0, x.length - 3);
    if (otherNumbers !== '') {
        return otherNumbers.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + ',' + lastThree + dec;
    }
    return lastThree + dec;
}

function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/&/g, '&amp;')
               .replace(/</g, '&lt;')
               .replace(/>/g, '&gt;')
               .replace(/"/g, '&quot;')
               .replace(/'/g, '&#039;');
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.remove('hidden');
    setTimeout(() => {
        toast.classList.add('hidden');
    }, 3200);
}
