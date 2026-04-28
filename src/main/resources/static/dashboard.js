/* ── API base URLs ─────────────────────────────────────────────────────── */
const AUTH_API = 'http://localhost:8080/api/auth';
const EMP_API  = 'http://localhost:8080/api/employees';

/* ── State ─────────────────────────────────────────────────────────────── */
let employees    = [];
let editingId    = null;
let deleteTarget = null;
let searchTimer  = null;
let sortOrder    = 'asc';

/* ── DOM refs ──────────────────────────────────────────────────────────── */
const adminNameEl  = document.getElementById('admin-name');
const adminEmailEl = document.getElementById('admin-email');
const adminAvatar  = document.getElementById('admin-avatar');
const logoutBtn    = document.getElementById('logout-btn');
const refreshBtn   = document.getElementById('refresh-btn');
const menuToggle   = document.getElementById('menu-toggle');
const sidebar      = document.getElementById('sidebar');
const topbarTitle  = document.getElementById('topbar-title');
const searchInput  = document.getElementById('search-input');
const addEmpBtn    = document.getElementById('add-emp-btn');
const toast        = document.getElementById('toast');
let toastTimer;

/* ── Init ──────────────────────────────────────────────────────────────── */
window.addEventListener('DOMContentLoaded', () => {
    loadAdminInfo();
    initNav();
    loadStats();
    loadEmployees();
    initModal();
    initConfirmModal();
    initSearch();
    initReportFilters();
    initSortToggle();

    refreshBtn.addEventListener('click', () => {
        const active = document.querySelector('.section.active');
        if (active?.id === 'section-overview')  { loadStats(); loadEmployees(); }
        if (active?.id === 'section-employees') loadEmployees();
        if (active?.id === 'section-reports')   triggerActiveReport();
    });

    menuToggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    logoutBtn.addEventListener('click', doLogout);
});

/* ── Admin info ────────────────────────────────────────────────────────── */
function loadAdminInfo() {
    const name  = sessionStorage.getItem('adminName')  || 'Admin';
    const email = sessionStorage.getItem('adminEmail') || '';
    adminNameEl.textContent  = name;
    adminEmailEl.textContent = email;
    adminAvatar.textContent  = name.charAt(0).toUpperCase();
}

/* ── Logout ────────────────────────────────────────────────────────────── */
async function doLogout() {
    try {
        await fetch(`${AUTH_API}/logout`, { method: 'POST', credentials: 'include' });
    } catch (_) {}
    sessionStorage.clear();
    window.location.href = 'signin.html';
}

/* ── Navigation ────────────────────────────────────────────────────────── */
function initNav() {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', e => {
            e.preventDefault();
            const section = item.dataset.section;
            showSection(section);
            sidebar.classList.remove('open');
        });
    });
}

function showSection(name) {
    document.querySelectorAll('.nav-item').forEach(i => i.classList.toggle('active', i.dataset.section === name));
    document.querySelectorAll('.section').forEach(s => s.classList.toggle('active', s.id === `section-${name}`));
    const titles = { overview: 'Overview', employees: 'Employee Management', reports: 'Reports' };
    topbarTitle.textContent = titles[name] || name;

    if (name === 'employees') loadEmployees();
    if (name === 'overview')  { loadStats(); loadDeptBreakdown(); }
    if (name === 'reports')   triggerActiveReport();
}

/* ── Stats ─────────────────────────────────────────────────────────────── */
async function loadStats() {
    try {
        const res  = await fetch(`${EMP_API}/stats`);
        const data = await res.json();
        document.getElementById('stat-total').textContent      = data.totalEmployees ?? 0;
        document.getElementById('stat-avg-salary').textContent = '₱' + (data.averageSalary ?? 0).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        document.getElementById('stat-avg-age').textContent    = (data.averageAge ?? 0).toFixed(1) + ' yrs';
        const deptCount = data.byDepartment ? Object.keys(data.byDepartment).length : 0;
        document.getElementById('stat-depts').textContent = deptCount;
        renderDeptBreakdown(data.byDepartment, data.totalEmployees);
    } catch (e) {
        console.error('Stats load error', e);
    }
}

function loadDeptBreakdown() { loadStats(); }

function renderDeptBreakdown(deptMap, total) {
    const tbody = document.getElementById('dept-breakdown-body');
    if (!deptMap || Object.keys(deptMap).length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="loading-row">No data yet.</td></tr>';
        return;
    }
    const sorted = Object.entries(deptMap).sort((a, b) => b[1].length - a[1].length);
    tbody.innerHTML = sorted.map(([dept, emps]) => {
        const pct = total > 0 ? ((emps.length / total) * 100).toFixed(1) : 0;
        return `<tr>
            <td><span class="dept-badge">${esc(dept)}</span></td>
            <td>${emps.length}</td>
            <td>${pct}%</td>
        </tr>`;
    }).join('');
}

/* ── Employees list ────────────────────────────────────────────────────── */
async function loadEmployees(search = '') {
    const tbody = document.getElementById('employees-body');
    tbody.innerHTML = '<tr><td colspan="6" class="loading-row">Loading…</td></tr>';
    try {
        const url = search ? `${EMP_API}?search=${encodeURIComponent(search)}` : EMP_API;
        const res  = await fetch(url);
        employees  = await res.json();
        renderEmployees(employees);
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="6" class="loading-row">Failed to load. Is the backend running?</td></tr>';
    }
}

function renderEmployees(list) {
    const tbody = document.getElementById('employees-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="loading-row">No employees found.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(e => `
        <tr>
            <td>#${e.id}</td>
            <td><span class="emp-name">${esc(e.firstname || '')} ${esc(e.lastname || '')}</span></td>
            <td>${e.birthday ? formatDate(e.birthday) : '—'}</td>
            <td><span class="dept-badge">${esc(e.department || '—')}</span></td>
            <td>₱${(e.salary ?? 0).toLocaleString('en-PH', { minimumFractionDigits: 2 })}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-edit" onclick="openEditModal(${e.id})" title="Edit">
                        <svg viewBox="0 0 20 20" fill="none"><path d="M4 13.5V16h2.5l7.372-7.372-2.5-2.5L4 13.5z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M13.872 4.128a1.768 1.768 0 012.5 2.5l-1.128 1.128-2.5-2.5 1.128-1.128z" stroke="currentColor" stroke-width="1.3"/></svg>
                    </button>
                    <button class="btn-delete" onclick="openDeleteModal(${e.id}, '${esc(e.firstname || '')} ${esc(e.lastname || '')}')" title="Delete">
                        <svg viewBox="0 0 20 20" fill="none"><path d="M5 6h10l-1 10H6L5 6z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M3 6h14M8 6V4h4v2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

/* ── Search ────────────────────────────────────────────────────────────── */
function initSearch() {
    searchInput.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => loadEmployees(searchInput.value.trim()), 350);
    });
}

/* ── Add/Edit Modal ────────────────────────────────────────────────────── */
function initModal() {
    addEmpBtn.addEventListener('click', openAddModal);
    document.getElementById('modal-close').addEventListener('click', closeModal);
    document.getElementById('cancel-btn').addEventListener('click', closeModal);
    document.getElementById('emp-modal').addEventListener('click', e => { if (e.target === e.currentTarget) closeModal(); });
    document.getElementById('save-btn').addEventListener('click', saveEmployee);
}

function openAddModal() {
    editingId = null;
    document.getElementById('modal-title').textContent = 'Add Employee';
    document.getElementById('save-btn').querySelector('.btn-text').textContent = 'Save Employee';
    clearModalForm();
    document.getElementById('emp-modal').classList.remove('hidden');
}

window.openEditModal = function(id) {
    const emp = employees.find(e => e.id === id);
    if (!emp) return;
    editingId = id;
    document.getElementById('modal-title').textContent = 'Edit Employee';
    document.getElementById('save-btn').querySelector('.btn-text').textContent = 'Update Employee';
    document.getElementById('emp-id').value         = emp.id;
    document.getElementById('emp-firstname').value  = emp.firstname  || '';
    document.getElementById('emp-lastname').value   = emp.lastname   || '';
    document.getElementById('emp-birthday').value   = emp.birthday   || '';
    document.getElementById('emp-department').value = emp.department || '';
    document.getElementById('emp-salary').value     = emp.salary     ?? '';
    clearModalErrors();
    document.getElementById('emp-modal').classList.remove('hidden');
};

function closeModal() {
    document.getElementById('emp-modal').classList.add('hidden');
    clearModalForm();
}

function clearModalForm() {
    ['emp-firstname','emp-lastname','emp-birthday','emp-department','emp-salary'].forEach(id => {
        document.getElementById(id).value = '';
        document.getElementById(id).classList.remove('invalid');
    });
    clearModalErrors();
}
function clearModalErrors() {
    ['efn-err','eln-err','ebd-err','edept-err','esal-err'].forEach(id => { document.getElementById(id).textContent = ''; });
    document.getElementById('modal-alert').className = 'alert hidden';
}

function validateEmpForm() {
    let ok = true;
    const ln   = document.getElementById('emp-lastname');
    const dept = document.getElementById('emp-department');
    const sal  = document.getElementById('emp-salary');
    if (!ln.value.trim())   { setFErr(ln,   'eln-err',   'Last name is required.');             ok = false; }
    if (!dept.value.trim()) { setFErr(dept, 'edept-err', 'Department is required.');            ok = false; }
    if (!sal.value || isNaN(sal.value) || Number(sal.value) < 0) { setFErr(sal, 'esal-err', 'Enter a valid salary (≥ 0).'); ok = false; }
    return ok;
}
function setFErr(inp, errId, msg) { inp.classList.add('invalid'); document.getElementById(errId).textContent = msg; }

async function saveEmployee() {
    clearModalErrors();
    if (!validateEmpForm()) return;
    const btn = document.getElementById('save-btn');
    btn.disabled = true;
    btn.querySelector('.btn-spinner').classList.remove('hidden');

    const payload = {
        firstname:  document.getElementById('emp-firstname').value.trim(),
        lastname:   document.getElementById('emp-lastname').value.trim(),
        birthday:   document.getElementById('emp-birthday').value || null,
        department: document.getElementById('emp-department').value.trim(),
        salary:     parseFloat(document.getElementById('emp-salary').value)
    };

    try {
        const url    = editingId ? `${EMP_API}/${editingId}` : EMP_API;
        const method = editingId ? 'PUT' : 'POST';
        const res    = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        const data   = await res.json();

        if (res.ok) {
            closeModal();
            loadEmployees(searchInput.value.trim());
            loadStats();
            showToast(editingId ? 'Employee updated!' : 'Employee added!', 'success');
        } else {
            document.getElementById('modal-alert').textContent = data.error || 'Save failed.';
            document.getElementById('modal-alert').className = 'alert error';
        }
    } catch (e) {
        document.getElementById('modal-alert').textContent = 'Cannot connect to server.';
        document.getElementById('modal-alert').className = 'alert error';
    } finally {
        btn.disabled = false;
        btn.querySelector('.btn-spinner').classList.add('hidden');
    }
}

/* ── Delete Modal ──────────────────────────────────────────────────────── */
function initConfirmModal() {
    document.getElementById('confirm-close').addEventListener('click',  closeConfirmModal);
    document.getElementById('confirm-cancel').addEventListener('click', closeConfirmModal);
    document.getElementById('confirm-delete').addEventListener('click', doDelete);
    document.getElementById('confirm-modal').addEventListener('click', e => { if (e.target === e.currentTarget) closeConfirmModal(); });
}

window.openDeleteModal = function(id, name) {
    deleteTarget = id;
    document.getElementById('confirm-name').textContent = name;
    document.getElementById('confirm-modal').classList.remove('hidden');
};

function closeConfirmModal() { document.getElementById('confirm-modal').classList.add('hidden'); deleteTarget = null; }

async function doDelete() {
    if (!deleteTarget) return;
    try {
        const res = await fetch(`${EMP_API}/${deleteTarget}`, { method: 'DELETE' });
        if (res.ok || res.status === 204) {
            closeConfirmModal();
            loadEmployees(searchInput.value.trim());
            loadStats();
            showToast('Employee deleted.', 'success');
        } else {
            showToast('Delete failed.', 'error');
        }
    } catch (e) { showToast('Cannot connect to server.', 'error'); }
}

/* ── Reports ───────────────────────────────────────────────────────────── */

/* Department colour map */
const DEPT_COLORS = {
    engineering:       'dept-engineering',
    hr:                'dept-hr',
    'human resources': 'dept-hr',
    finance:           'dept-finance',
    marketing:         'dept-marketing',
    operations:        'dept-operations',
    sales:             'dept-sales',
    it:                'dept-it',
    legal:             'dept-legal',
    design:            'dept-design',
    product:           'dept-product',
};

function deptClass(dept) {
    return DEPT_COLORS[(dept || '').toLowerCase()] || 'dept-default';
}

/* Age-range helpers */
function calcAge(birthday) {
    if (!birthday) return null;
    let dob;
    if (Array.isArray(birthday)) {
        const [y, m, d] = birthday;
        dob = new Date(y, m - 1, d);
    } else {
        dob = new Date(birthday);
    }
    if (isNaN(dob)) return null;
    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const mo = today.getMonth() - dob.getMonth();
    if (mo < 0 || (mo === 0 && today.getDate() < dob.getDate())) age--;
    return age;
}

function ageRange(age) {
    if (age === null) return null;
    if (age < 25) return { key: 'under25', label: 'Under 25' };
    if (age < 35) return { key: '25-34',   label: '25 – 34'  };
    if (age < 45) return { key: '35-44',   label: '35 – 44'  };
    if (age < 55) return { key: '45-54',   label: '45 – 54'  };
    return            { key: '55plus',  label: '55 & Above' };
}

function ageClass(key) {
    const map = { 'under25': 'age-under25', '25-34': 'age-25-34', '35-44': 'age-35-44', '45-54': 'age-45-54', '55plus': 'age-55plus' };
    return map[key] || '';
}

/* Full employee list cache for filtering */
let reportAllEmployees = [];

/* ── Sort toggle ───────────────────────────────────────────────────────── */
function initSortToggle() {
    const btn = document.getElementById('sort-toggle-btn');
    if (!btn) return;
    btn.addEventListener('click', () => {
        sortOrder = sortOrder === 'asc' ? 'desc' : 'asc';
        updateSortToggleUI();
        applyReportFilters();
    });
}

function updateSortToggleUI() {
    const btn       = document.getElementById('sort-toggle-btn');
    const label     = document.getElementById('sort-label');
    const iconAsc   = document.getElementById('sort-icon-asc');
    const iconDesc  = document.getElementById('sort-icon-desc');
    if (!btn) return;

    const isDesc = sortOrder === 'desc';
    label.textContent      = isDesc ? 'Desc' : 'Asc';
    iconAsc.style.display  = isDesc ? 'none'  : 'block';
    iconDesc.style.display = isDesc ? 'block' : 'none';
    btn.classList.toggle('active', isDesc);
}

/* ── Report filters ────────────────────────────────────────────────────── */
function initReportFilters() {
    const deptSel = document.getElementById('dept-filter-select');
    const ageSel  = document.getElementById('age-filter-select');
    if (!deptSel || !ageSel) return;
    deptSel.addEventListener('change', applyReportFilters);
    ageSel.addEventListener('change',  applyReportFilters);
}

function triggerActiveReport() {
    loadReportData();
}

async function loadReportData() {
    const tbody = document.getElementById('report-body');
    tbody.innerHTML = '<tr><td colspan="6" class="loading-row">Loading…</td></tr>';
    try {
        const res  = await fetch(EMP_API);
        reportAllEmployees = await res.json();
        populateDeptDropdown(reportAllEmployees);
        applyReportFilters();
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="6" class="loading-row">Failed to load report.</td></tr>';
    }
}

function populateDeptDropdown(list) {
    const sel = document.getElementById('dept-filter-select');
    if (!sel) return;
    const current = sel.value;
    const depts = [...new Set((list || []).map(e => e.department).filter(Boolean))].sort();
    sel.innerHTML = '<option value="">All Departments</option>' +
        depts.map(d => `<option value="${esc(d)}"${d === current ? ' selected' : ''}>${esc(d)}</option>`).join('');
}

function applyReportFilters() {
    const deptVal = (document.getElementById('dept-filter-select')?.value || '').toLowerCase();
    const ageVal  = document.getElementById('age-filter-select')?.value || '';

    let filtered = reportAllEmployees;

    if (deptVal) {
        filtered = filtered.filter(e => (e.department || '').toLowerCase() === deptVal);
    }
    if (ageVal) {
        filtered = filtered.filter(e => {
            const age   = calcAge(e.birthday);
            const range = ageRange(age);
            return range && range.key === ageVal;
        });
    }

    /* Sort: primary = department (alpha), secondary = age (numeric).
       sortOrder flips both keys so the whole table consistently goes
       A→Z / youngest→oldest (asc) or Z→A / oldest→youngest (desc). */
    const dir = sortOrder === 'asc' ? 1 : -1;
    filtered = [...filtered].sort((a, b) => {
        const dA = (a.department || '').localeCompare(b.department || '') * dir;
        if (dA !== 0) return dA;
        return ((calcAge(a.birthday) ?? 99) - (calcAge(b.birthday) ?? 99)) * dir;
    });

    renderReport(filtered);
}

function renderReport(list) {
    const tbody = document.getElementById('report-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="loading-row">No employees match the selected filters.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(e => {
        const age      = calcAge(e.birthday);
        const range    = ageRange(age);
        const ageBadge = range
            ? `<span class="age-badge ${ageClass(range.key)}">${esc(range.label)}</span>`
            : '<span style="color:#94a3b8">—</span>';
        const deptBadge = `<span class="dept-badge ${deptClass(e.department)}">${esc(e.department || '—')}</span>`;
        return `
        <tr>
            <td>#${e.id}</td>
            <td><span class="emp-name">${esc(e.firstname || '')} ${esc(e.lastname || '')}</span></td>
            <td>${e.birthday ? formatDate(e.birthday) : '—'}</td>
            <td>${ageBadge}</td>
            <td>${deptBadge}</td>
            <td>₱${(e.salary ?? 0).toLocaleString('en-PH', { minimumFractionDigits: 2 })}</td>
        </tr>`;
    }).join('');
}

/* ── Toast ─────────────────────────────────────────────────────────────── */
function showToast(msg, type = 'success') {
    clearTimeout(toastTimer);
    toast.textContent = msg;
    toast.className = `toast ${type}`;
    toastTimer = setTimeout(() => { toast.className = 'toast hidden'; }, 3000);
}

/* ── Helpers ───────────────────────────────────────────────────────────── */
function esc(str) {
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function formatDate(d) {
    if (!d) return '—';
    if (Array.isArray(d)) {
        const [y, m, day] = d;
        return `${String(day).padStart(2,'0')}/${String(m).padStart(2,'0')}/${y}`;
    }
    const dt = new Date(d);
    if (isNaN(dt)) return d;
    return dt.toLocaleDateString('en-GB');
}