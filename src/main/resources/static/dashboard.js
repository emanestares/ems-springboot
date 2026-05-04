/* ── API base URLs (from urls.properties via static config) ─────────────── */
const BASE_URL = 'http://localhost:8080';
const AUTH_API = `${BASE_URL}/api/auth`;
const EMP_API  = `${BASE_URL}/api/employees`;
const DEPT_API = `${BASE_URL}/api/departments`;

/* ── State ─────────────────────────────────────────────────────────────── */
let employees      = [];
let departments    = [];      // lookup table cache
let editingId      = null;
let deleteTarget   = null;
let toggleTarget   = null;
let searchTimer    = null;
let sortOrder      = 'asc';

/* ── Pagination state ──────────────────────────────────────────────────── */
let currentPage    = 0;
const PAGE_SIZE    = 5;
let totalPages     = 0;
let totalElements  = 0;
let lastSearch     = '';
let lastDeptFilter = '';

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
const statusFilter = document.getElementById('status-filter');
const addEmpBtn    = document.getElementById('add-emp-btn');
const toast        = document.getElementById('toast');
let toastTimer;

/* ── Init ──────────────────────────────────────────────────────────────── */
window.addEventListener('DOMContentLoaded', async () => {
    loadAdminInfo();
    initNav();
    await loadDepartments();   // load departments first (used in dropdown)
    loadStats();
    loadEmployees();
    initModal();
    initProfileModal();
    initConfirmModal();
    initToggleModal();
    initSearch();
    initReportFilters();
    initSortToggle();
    initDeptAdmin();

    refreshBtn.addEventListener('click', () => {
        const active = document.querySelector('.section.active');
        if (active?.id === 'section-overview')    { loadStats(); loadEmployees(); }
        if (active?.id === 'section-employees')   loadEmployees();
        if (active?.id === 'section-reports')     triggerActiveReport();
        if (active?.id === 'section-departments') loadDeptTable();
    });

    menuToggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    logoutBtn.addEventListener('click', doLogout);
});

/* ── Departments (lookup) ──────────────────────────────────────────────── */
async function loadDepartments() {
    try {
        const res = await fetch(DEPT_API);
        departments = await res.json();
        populateDeptDropdown_modal();
    } catch (e) {
        console.error('Dept load error', e);
    }
}

function populateDeptDropdown_modal() {
    const sel = document.getElementById('emp-department');
    if (!sel) return;
    const current = sel.value;
    sel.innerHTML = '<option value="">Select department…</option>' +
        departments.map(d =>
            `<option value="${d.id}"${String(d.id) === String(current) ? ' selected' : ''}>${esc(d.name)}</option>`
        ).join('');
}

function getDeptName(deptObj) {
    if (!deptObj) return '—';
    return deptObj.name || '—';
}

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
            showSection(item.dataset.section);
            sidebar.classList.remove('open');
        });
    });
}

function showSection(name) {
    document.querySelectorAll('.nav-item').forEach(i => i.classList.toggle('active', i.dataset.section === name));
    document.querySelectorAll('.section').forEach(s => s.classList.toggle('active', s.id === `section-${name}`));
    const titles = { overview: 'Overview', employees: 'Employee Management', reports: 'Reports', departments: 'Department Admin' };
    topbarTitle.textContent = titles[name] || name;

    if (name === 'employees')   loadEmployees();
    if (name === 'overview')    { loadStats(); loadDeptBreakdown(); }
    if (name === 'reports')     triggerActiveReport();
    if (name === 'departments') loadDeptTable();
}

/* ── Stats ─────────────────────────────────────────────────────────────── */
let _overviewFilter = 'active';

function setOverviewFilter(filter) {
    _overviewFilter = filter;
    document.querySelectorAll('#overview-filter-toggle .view-toggle-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.filter === filter);
    });
    const labelMap = { active: 'Active', inactive: 'Inactive', all: 'All' };
    const label = labelMap[filter] || 'Active';
    const salaryLabel = document.getElementById('label-avg-salary');
    const ageLabel    = document.getElementById('label-avg-age');
    if (salaryLabel) salaryLabel.textContent = `Average Salary (${label})`;
    if (ageLabel)    ageLabel.textContent    = `Average Age (${label})`;
    loadStats();
}

async function loadStats() {
    try {
        const res  = await fetch(`${EMP_API}/stats?filter=${_overviewFilter}`);
        const data = await res.json();
        document.getElementById('stat-total').textContent      = data.totalEmployees  ?? 0;
        document.getElementById('stat-active').textContent     = data.activeEmployees ?? 0;
        document.getElementById('stat-avg-salary').textContent = '₱' + (data.averageSalary ?? 0).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        document.getElementById('stat-avg-age').textContent    = (data.averageAge ?? 0).toFixed(1) + ' yrs';
        const deptCount = data.byDepartment ? Object.keys(data.byDepartment).length : 0;
        document.getElementById('stat-depts').textContent = deptCount;
        const headcountForBreakdown = _overviewFilter === 'all'
            ? (data.totalEmployees ?? 0)
            : Object.values(data.byDepartment || {}).reduce((s, arr) => s + arr.length, 0);
        renderDeptBreakdown(data.byDepartment, headcountForBreakdown);
    } catch (e) {
        console.error('Stats load error', e);
    }
}

function loadDeptBreakdown() { loadStats(); }

let _lastDeptMap  = null;
let _lastTotal    = 0;
let _deptViewMode = 'table';

function renderDeptBreakdown(deptMap, total) {
    _lastDeptMap = deptMap;
    _lastTotal   = total;
    if (_deptViewMode === 'pie') {
        renderDeptPie(deptMap, total);
    } else {
        renderDeptTable(deptMap, total);
    }
}

function renderDeptTable(deptMap, total) {
    const wrap = document.getElementById('dept-breakdown-wrap');
    if (!wrap) return;
    if (!deptMap || Object.keys(deptMap).length === 0) {
        wrap.innerHTML = '<table class="data-table" id="dept-breakdown-table"><thead><tr><th>Department</th><th>Headcount</th><th>% of Total</th></tr></thead><tbody><tr><td colspan="3" class="loading-row">No data yet.</td></tr></tbody></table>';
        return;
    }
    const sorted = Object.entries(deptMap).sort((a, b) => b[1].length - a[1].length);
    const rows = sorted.map(([dept, emps]) => {
        const pct = total > 0 ? ((emps.length / total) * 100).toFixed(1) : 0;
        return `<tr>
            <td><span class="dept-badge">${esc(dept)}</span></td>
            <td>${emps.length}</td>
            <td>${pct}%</td>
        </tr>`;
    }).join('');
    wrap.innerHTML = `<table class="data-table" id="dept-breakdown-table">
        <thead><tr><th>Department</th><th>Headcount</th><th>% of Total</th></tr></thead>
        <tbody>${rows}</tbody>
    </table>`;
}

const PIE_COLORS = [
    '#1e40af','#0369a1','#0d9488','#16a34a','#ca8a04',
    '#c2410c','#7c3aed','#be185d','#0891b2','#4f46e5'
];

function renderDeptPie(deptMap, total) {
    const wrap = document.getElementById('dept-breakdown-wrap');
    if (!wrap) return;
    if (!deptMap || Object.keys(deptMap).length === 0) {
        wrap.innerHTML = '<p style="text-align:center;color:var(--text-muted);padding:2rem">No data yet.</p>';
        return;
    }
    const sorted = Object.entries(deptMap).sort((a, b) => b[1].length - a[1].length);
    const size = 220;
    const cx = size / 2, cy = size / 2, r = 88, ir = 44;
    let slices = '';
    let legend = '';
    let angle  = -Math.PI / 2;
    sorted.forEach(([dept, emps], i) => {
        const pct   = total > 0 ? emps.length / total : 0;
        const sweep = pct * 2 * Math.PI;
        const x1 = cx + r * Math.cos(angle);
        const y1 = cy + r * Math.sin(angle);
        angle += sweep;
        const x2 = cx + r * Math.cos(angle);
        const y2 = cy + r * Math.sin(angle);
        const ix1 = cx + ir * Math.cos(angle);
        const iy1 = cy + ir * Math.sin(angle);
        const ix2 = cx + ir * Math.cos(angle - sweep);
        const iy2 = cy + ir * Math.sin(angle - sweep);
        const large = sweep > Math.PI ? 1 : 0;
        const color = PIE_COLORS[i % PIE_COLORS.length];
        const midA  = angle - sweep / 2;
        const lx    = cx + (r + 14) * Math.cos(midA);
        const ly    = cy + (r + 14) * Math.sin(midA);
        const pctStr = (pct * 100).toFixed(1);
        slices += `<path d="M${x1},${y1} A${r},${r} 0 ${large},1 ${x2},${y2} L${ix1},${iy1} A${ir},${ir} 0 ${large},0 ${ix2},${iy2} Z"
            fill="${color}" stroke="white" stroke-width="1.5">
            <title>${esc(dept)}: ${emps.length} (${pctStr}%)</title></path>`;
        if (pct > 0.07) {
            slices += `<text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle"
                font-size="9" fill="white" font-weight="600" pointer-events="none">${pctStr}%</text>`;
        }
        legend += `<div class="pie-legend-item">
            <span class="pie-legend-dot" style="background:${color}"></span>
            <span class="pie-legend-label">${esc(dept)}</span>
            <span class="pie-legend-val">${emps.length} <span style="color:var(--text-muted)">(${pctStr}%)</span></span>
        </div>`;
    });
    wrap.innerHTML = `<div class="pie-chart-wrap">
        <svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}" style="flex-shrink:0">${slices}</svg>
        <div class="pie-legend">${legend}</div>
    </div>`;
}

function switchDeptView(mode) {
    _deptViewMode = mode;
    document.getElementById('view-table-btn')?.classList.toggle('active', mode === 'table');
    document.getElementById('view-pie-btn')?.classList.toggle('active', mode === 'pie');
    if (_lastDeptMap) renderDeptBreakdown(_lastDeptMap, _lastTotal);
}

/* ── Employees list (paginated) ────────────────────────────────────────── */
async function loadEmployees(page = currentPage) {
    currentPage = page;
    const tbody = document.getElementById('employees-body');
    tbody.innerHTML = '<tr><td colspan="7" class="loading-row">Loading…</td></tr>';
    try {
        lastSearch     = searchInput.value.trim();
        lastDeptFilter = '';

        const params = new URLSearchParams({ page, size: PAGE_SIZE });
        if (lastSearch) params.set('search', lastSearch);

        const res  = await fetch(`${EMP_API}?${params}`);
        const data = await res.json();

        employees     = data.content;
        totalPages    = data.totalPages;
        totalElements = data.totalElements;

        renderEmployees(applyStatusFilter(employees));
        renderPagination();
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading-row">Failed to load. Is the backend running?</td></tr>';
    }
}

function applyStatusFilter(list) {
    const status = statusFilter ? statusFilter.value : '';
    if (!status) return list;
    return list.filter(e => {
        const active = e.isActive !== false;
        return status === 'active' ? active : !active;
    });
}

function renderEmployees(list) {
    const tbody = document.getElementById('employees-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading-row">No employees found.</td></tr>';
        renderPagination();
        return;
    }
    tbody.innerHTML = list.map(e => {
        const active    = e.isActive !== false;
        const deptName  = getDeptName(e.department);
        const pill      = active
            ? '<span class="status-pill active">Active</span>'
            : '<span class="status-pill inactive">Inactive</span>';
        const toggleBtn = active
            ? `<button class="btn-deactivate" onclick="openToggleModal(${e.id}, '${esc(e.firstname || '')} ${esc(e.lastname || '')}', true)" title="Deactivate">
                <svg viewBox="0 0 20 20" fill="none"><path d="M10 3v7l4 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/><circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.4"/><path d="M7 7l6 6M13 7l-6 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
               </button>`
            : `<button class="btn-activate" onclick="openToggleModal(${e.id}, '${esc(e.firstname || '')} ${esc(e.lastname || '')}', false)" title="Activate">
                <svg viewBox="0 0 20 20" fill="none"><path d="M7 10l2 2 4-4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/><circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.4"/></svg>
               </button>`;
        return `
        <tr class="emp-row" onclick="openProfileModal(${e.id})" style="cursor:pointer;">
            <td>#${e.id}</td>
            <td><span class="emp-name">${esc(e.firstname || '')} ${esc(e.lastname || '')}</span></td>
            <td>${e.birthday ? formatDate(e.birthday) : '—'}</td>
            <td><span class="dept-badge ${deptClass(deptName)}">${esc(deptName)}</span></td>
            <td>₱${(e.salary ?? 0).toLocaleString('en-PH', { minimumFractionDigits: 2 })}</td>
            <td>${pill}</td>
            <td onclick="event.stopPropagation()">
                <div class="action-btns">
                    ${toggleBtn}
                    <button class="btn-edit" onclick="openEditModal(${e.id})" title="Edit">
                        <svg viewBox="0 0 20 20" fill="none"><path d="M4 13.5V16h2.5l7.372-7.372-2.5-2.5L4 13.5z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M13.872 4.128a1.768 1.768 0 012.5 2.5l-1.128 1.128-2.5-2.5 1.128-1.128z" stroke="currentColor" stroke-width="1.3"/></svg>
                    </button>
                    <button class="btn-delete" onclick="openDeleteModal(${e.id}, '${esc(e.firstname || '')} ${esc(e.lastname || '')}')" title="Delete">
                        <svg viewBox="0 0 20 20" fill="none"><path d="M5 6h10l-1 10H6L5 6z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M3 6h14M8 6V4h4v2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
                    </button>
                </div>
            </td>
        </tr>`;
    }).join('');
}

/* ── Pagination controls ───────────────────────────────────────────────── */
/* ── Shared smart-pagination renderer ─────────────────────────────────── */
function buildPageButtons(current, total, onClickFn) {
    if (total <= 1) return '';
    // Build window: always show first, last, current ±2, with ellipsis gaps
    const pages = new Set([0, total - 1]);
    for (let i = Math.max(0, current - 2); i <= Math.min(total - 1, current + 2); i++) pages.add(i);
    const sorted = [...pages].sort((a, b) => a - b);

    let btns = '';
    let prev = -1;
    for (const p of sorted) {
        if (p - prev > 1) btns += `<span class="page-ellipsis">…</span>`;
        btns += `<button class="page-btn${p === current ? ' active' : ''}" onclick="${onClickFn}(${p})">${p + 1}</button>`;
        prev = p;
    }
    return btns;
}

function renderPagination() {
    const container = document.getElementById('pagination-controls');
    if (!container) return;

    if (totalPages <= 1) { container.style.display = 'none'; container.innerHTML = ''; return; }
    container.style.display = 'flex';

    const prevDis = currentPage === 0 ? 'disabled' : '';
    const nextDis = currentPage >= totalPages - 1 ? 'disabled' : '';

    container.innerHTML = `
        <span class="page-info">Page ${currentPage + 1} of ${totalPages} &nbsp;·&nbsp; ${totalElements} records</span>
        <div class="page-btns">
            <button class="page-btn" onclick="loadEmployees(${currentPage - 1})" ${prevDis}>‹ Prev</button>
            ${buildPageButtons(currentPage, totalPages, 'loadEmployees')}
            <button class="page-btn" onclick="loadEmployees(${currentPage + 1})" ${nextDis}>Next ›</button>
        </div>`;
}

/* ── Search & Status filter ────────────────────────────────────────────── */
function initSearch() {
    searchInput.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => { currentPage = 0; loadEmployees(0); }, 350);
    });
    if (statusFilter) {
        statusFilter.addEventListener('change', () => renderEmployees(applyStatusFilter(employees)));
    }
}

/* ── Toggle Active Modal ───────────────────────────────────────────────── */
function initToggleModal() {
    document.getElementById('toggle-close').addEventListener('click',  closeToggleModal);
    document.getElementById('toggle-cancel').addEventListener('click', closeToggleModal);
    document.getElementById('toggle-confirm').addEventListener('click', doToggleActive);
    document.getElementById('toggle-modal').addEventListener('click', e => { if (e.target === e.currentTarget) closeToggleModal(); });
}

window.openToggleModal = function(id, name, isCurrentlyActive) {
    toggleTarget = id;
    if (isCurrentlyActive) {
        document.getElementById('toggle-modal-title').textContent = 'Deactivate Employee';
        document.getElementById('toggle-modal-body').innerHTML    = `Are you sure you want to deactivate <strong>${esc(name)}</strong>? They will be marked inactive but not deleted.`;
        document.getElementById('toggle-confirm').textContent     = 'Deactivate';
        document.getElementById('toggle-confirm').className       = 'btn-warning';
    } else {
        document.getElementById('toggle-modal-title').textContent = 'Activate Employee';
        document.getElementById('toggle-modal-body').innerHTML    = `Are you sure you want to re-activate <strong>${esc(name)}</strong>?`;
        document.getElementById('toggle-confirm').textContent     = 'Activate';
        document.getElementById('toggle-confirm').className       = 'btn-primary';
    }
    document.getElementById('toggle-modal').classList.remove('hidden');
};

function closeToggleModal() {
    document.getElementById('toggle-modal').classList.add('hidden');
    toggleTarget = null;
}

async function doToggleActive() {
    if (!toggleTarget) return;
    try {
        const res = await fetch(`${EMP_API}/${toggleTarget}/toggle-active`, { method: 'PATCH' });
        if (!res.ok) { showToast('Toggle failed.', 'error'); return; }
        closeToggleModal();
        await loadEmployees(currentPage);
        loadStats();
        showToast('Employee status updated.', 'success');
    } catch (e) {
        showToast('Cannot connect to server.', 'error');
    }
}

/* ── Profile Modal ─────────────────────────────────────────────────────── */
window.openProfileModal = function(id) {
    const emp = employees.find(e => e.id === id);
    if (!emp) return;

    const active    = emp.isActive !== false;
    const deptName  = getDeptName(emp.department);
    const fullName  = `${emp.firstname || ''} ${emp.lastname || ''}`.trim();
    const initials  = [(emp.firstname || '')[0], (emp.lastname || '')[0]].filter(Boolean).join('').toUpperCase() || '?';
    const age       = calcAge(emp.birthday);
    const range     = ageRange(age);

    const avatarColors = ['#2563eb','#0891b2','#059669','#d97706','#7c3aed','#db2777','#dc2626','#0284c7'];
    const avatarBg = avatarColors[emp.id % avatarColors.length];

    document.getElementById('profile-avatar-initials').textContent = initials;
    document.getElementById('profile-avatar-circle').style.background = avatarBg;
    document.getElementById('profile-fullname').textContent   = fullName || '—';
    document.getElementById('profile-dept-badge').textContent = deptName;
    document.getElementById('profile-dept-badge').className   = `dept-badge ${deptClass(deptName)}`;

    const statusEl = document.getElementById('profile-status-pill');
    statusEl.textContent = active ? 'Active' : 'Inactive';
    statusEl.className   = `status-pill ${active ? 'active' : 'inactive'}`;

    document.getElementById('profile-id').textContent        = `#${emp.id}`;
    document.getElementById('profile-firstname').textContent = emp.firstname  || '—';
    document.getElementById('profile-lastname').textContent  = emp.lastname   || '—';
    document.getElementById('profile-birthday').textContent  = emp.birthday   ? formatDate(emp.birthday) : '—';
    document.getElementById('profile-age').textContent       = age !== null   ? `${age} yrs${range ? ' · ' + range.label : ''}` : '—';
    document.getElementById('profile-department').textContent = deptName;
    document.getElementById('profile-salary').textContent    = '₱' + (emp.salary ?? 0).toLocaleString('en-PH', { minimumFractionDigits: 2 });

    document.getElementById('profile-edit-btn').onclick = () => {
        closeProfileModal();
        openEditModal(id);
    };

    document.getElementById('profile-modal').classList.remove('hidden');
};

function closeProfileModal() {
    document.getElementById('profile-modal').classList.add('hidden');
}

function initProfileModal() {
    document.getElementById('profile-modal-close').addEventListener('click', closeProfileModal);
    document.getElementById('profile-modal').addEventListener('click', e => { if (e.target === e.currentTarget) closeProfileModal(); });
}

/* ── Add/Edit Employee Modal ───────────────────────────────────────────── */
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
    populateDeptDropdown_modal();
    document.getElementById('emp-modal').classList.remove('hidden');
}

window.openEditModal = function(id) {
    const emp = employees.find(e => e.id === id);
    if (!emp) return;
    editingId = id;
    document.getElementById('modal-title').textContent = 'Edit Employee';
    document.getElementById('save-btn').querySelector('.btn-text').textContent = 'Update Employee';
    document.getElementById('emp-id').value        = emp.id;
    document.getElementById('emp-firstname').value = emp.firstname  || '';
    document.getElementById('emp-lastname').value  = emp.lastname   || '';
    document.getElementById('emp-birthday').value  = emp.birthday   ? formatDateInput(emp.birthday) : '';
    document.getElementById('emp-salary').value    = emp.salary     ?? '';
    populateDeptDropdown_modal();
    // Pre-select existing department
    if (emp.department?.id) {
        document.getElementById('emp-department').value = emp.department.id;
    }
    clearModalErrors();
    document.getElementById('emp-modal').classList.remove('hidden');
};

function closeModal() {
    document.getElementById('emp-modal').classList.add('hidden');
    clearModalForm();
}

function clearModalForm() {
    ['emp-firstname','emp-lastname','emp-birthday','emp-salary'].forEach(id => {
        document.getElementById(id).value = '';
        document.getElementById(id).classList.remove('invalid');
    });
    const deptSel = document.getElementById('emp-department');
    if (deptSel) { deptSel.value = ''; deptSel.classList.remove('invalid'); }
    clearModalErrors();
}

function clearModalErrors() {
    ['efn-err','eln-err','ebd-err','edept-err','esal-err'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = '';
    });
    const alert = document.getElementById('modal-alert');
    if (alert) alert.className = 'alert hidden';
}

function validateEmpForm() {
    let ok = true;
    const ln   = document.getElementById('emp-lastname');
    const dept = document.getElementById('emp-department');
    const sal  = document.getElementById('emp-salary');
    if (!ln.value.trim())   { setFErr(ln,   'eln-err',   'Last name is required.');             ok = false; }
    if (!dept.value)        { setFErr(dept, 'edept-err', 'Department is required.');            ok = false; }
    if (!sal.value || isNaN(sal.value) || Number(sal.value) < 0) { setFErr(sal, 'esal-err', 'Enter a valid salary (≥ 0).'); ok = false; }
    return ok;
}

function setFErr(inp, errId, msg) {
    inp.classList.add('invalid');
    const el = document.getElementById(errId);
    if (el) el.textContent = msg;
}

async function saveEmployee() {
    clearModalErrors();
    if (!validateEmpForm()) return;
    const btn = document.getElementById('save-btn');
    btn.disabled = true;
    btn.querySelector('.btn-spinner').classList.remove('hidden');

    const payload = {
        firstname:    document.getElementById('emp-firstname').value.trim(),
        lastname:     document.getElementById('emp-lastname').value.trim(),
        birthday:     document.getElementById('emp-birthday').value || null,
        departmentId: parseInt(document.getElementById('emp-department').value),
        salary:       parseFloat(document.getElementById('emp-salary').value)
    };

    try {
        const url    = editingId ? `${EMP_API}/${editingId}` : EMP_API;
        const method = editingId ? 'PUT' : 'POST';
        const res    = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        const data   = await res.json();

        if (res.ok) {
            closeModal();
            await loadEmployees(currentPage);
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

function closeConfirmModal() {
    document.getElementById('confirm-modal').classList.add('hidden');
    deleteTarget = null;
}

async function doDelete() {
    if (!deleteTarget) return;
    try {
        const res = await fetch(`${EMP_API}/${deleteTarget}`, { method: 'DELETE' });
        if (res.ok || res.status === 204) {
            closeConfirmModal();
            // Stay on same page, but back one if it was the only record
            const newPage = employees.length === 1 && currentPage > 0 ? currentPage - 1 : currentPage;
            await loadEmployees(newPage);
            loadStats();
            showToast('Employee deleted.', 'success');
        } else {
            showToast('Delete failed.', 'error');
        }
    } catch (e) { showToast('Cannot connect to server.', 'error'); }
}

/* ── Department Admin CRUD ─────────────────────────────────────────────── */
let deptEditingId   = null;
let deptDeleteTarget = null;

function initDeptAdmin() {
    // Add button
    const addDeptBtn = document.getElementById('add-dept-btn');
    if (addDeptBtn) addDeptBtn.addEventListener('click', openAddDeptModal);

    // Modal controls
    document.getElementById('dept-modal-close')?.addEventListener('click', closeDeptModal);
    document.getElementById('dept-cancel-btn')?.addEventListener('click', closeDeptModal);
    document.getElementById('dept-save-btn')?.addEventListener('click', saveDepartment);
    document.getElementById('dept-modal')?.addEventListener('click', e => { if (e.target === e.currentTarget) closeDeptModal(); });

    // Delete modal
    document.getElementById('dept-confirm-close')?.addEventListener('click', closeDeptConfirm);
    document.getElementById('dept-confirm-cancel')?.addEventListener('click', closeDeptConfirm);
    document.getElementById('dept-confirm-delete')?.addEventListener('click', doDeleteDept);
    document.getElementById('dept-confirm-modal')?.addEventListener('click', e => { if (e.target === e.currentTarget) closeDeptConfirm(); });
}

/* ── Department pagination state ───────────────────────────────────────── */
let deptCurrentPage = 0;
const DEPT_PAGE_SIZE = 5;

async function loadDeptTable(page) {
    if (page === undefined) page = deptCurrentPage;
    deptCurrentPage = page;
    await loadDepartments();
    const tbody = document.getElementById('dept-table-body');
    if (!tbody) return;
    if (!departments.length) {
        tbody.innerHTML = '<tr><td colspan="3" class="loading-row">No departments yet.</td></tr>';
        renderDeptPagination(0, 0, 0);
        return;
    }
    const totalDept = departments.length;
    const totalDeptPages = Math.ceil(totalDept / DEPT_PAGE_SIZE);
    deptCurrentPage = Math.min(deptCurrentPage, Math.max(0, totalDeptPages - 1));
    const start = deptCurrentPage * DEPT_PAGE_SIZE;
    const pageItems = departments.slice(start, start + DEPT_PAGE_SIZE);

    tbody.innerHTML = pageItems.map(d => `
        <tr>
            <td>#${d.id}</td>
            <td>${esc(d.name)}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-edit" onclick="openEditDeptModal(${d.id}, '${esc(d.name)}')" title="Edit">
                        <svg viewBox="0 0 20 20" fill="none"><path d="M4 13.5V16h2.5l7.372-7.372-2.5-2.5L4 13.5z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M13.872 4.128a1.768 1.768 0 012.5 2.5l-1.128 1.128-2.5-2.5 1.128-1.128z" stroke="currentColor" stroke-width="1.3"/></svg>
                    </button>
                    <button class="btn-delete" onclick="openDeleteDeptModal(${d.id}, '${esc(d.name)}')" title="Delete">
                        <svg viewBox="0 0 20 20" fill="none"><path d="M5 6h10l-1 10H6L5 6z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M3 6h14M8 6V4h4v2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
                    </button>
                </div>
            </td>
        </tr>`).join('');

    renderDeptPagination(deptCurrentPage, totalDeptPages, totalDept);
}

function renderDeptPagination(current, total, totalItems) {
    const container = document.getElementById('dept-pagination');
    if (!container) return;
    if (total <= 1) { container.style.display = 'none'; container.innerHTML = ''; return; }
    container.style.display = 'flex';
    const prevDis = current === 0 ? 'disabled' : '';
    const nextDis = current >= total - 1 ? 'disabled' : '';
    container.innerHTML = `
        <span class="page-info">Page ${current + 1} of ${total} &nbsp;·&nbsp; ${totalItems} departments</span>
        <div class="page-btns">
            <button class="page-btn" onclick="loadDeptTable(${current - 1})" ${prevDis}>‹ Prev</button>
            ${buildPageButtons(current, total, 'loadDeptTable')}
            <button class="page-btn" onclick="loadDeptTable(${current + 1})" ${nextDis}>Next ›</button>
        </div>`;
}

function openAddDeptModal() {
    deptEditingId = null;
    document.getElementById('dept-modal-title').textContent = 'Add Department';
    document.getElementById('dept-name-input').value = '';
    document.getElementById('dept-name-err').textContent = '';
    document.getElementById('dept-modal-alert').className = 'alert hidden';
    document.getElementById('dept-modal').classList.remove('hidden');
}

window.openEditDeptModal = function(id, name) {
    deptEditingId = id;
    document.getElementById('dept-modal-title').textContent = 'Edit Department';
    document.getElementById('dept-name-input').value = name;
    document.getElementById('dept-name-err').textContent = '';
    document.getElementById('dept-modal-alert').className = 'alert hidden';
    document.getElementById('dept-modal').classList.remove('hidden');
};

function closeDeptModal() {
    document.getElementById('dept-modal').classList.add('hidden');
    deptEditingId = null;
}

async function saveDepartment() {
    const nameInput = document.getElementById('dept-name-input');
    const name = nameInput.value.trim();
    document.getElementById('dept-name-err').textContent = '';

    if (!name) {
        document.getElementById('dept-name-err').textContent = 'Department name is required.';
        nameInput.classList.add('invalid');
        return;
    }
    nameInput.classList.remove('invalid');

    const payload = { name };
    const url    = deptEditingId ? `${DEPT_API}/${deptEditingId}` : DEPT_API;
    const method = deptEditingId ? 'PUT' : 'POST';

    try {
        const res  = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        const data = await res.json();
        if (res.ok) {
            closeDeptModal();
            deptCurrentPage = 0;
            await loadDeptTable(0);
            await loadDepartments();  // refresh dropdown cache
            showToast(deptEditingId ? 'Department updated!' : 'Department added!', 'success');
        } else {
            document.getElementById('dept-modal-alert').textContent = data.error || 'Save failed.';
            document.getElementById('dept-modal-alert').className = 'alert error';
        }
    } catch (e) {
        document.getElementById('dept-modal-alert').textContent = 'Cannot connect to server.';
        document.getElementById('dept-modal-alert').className = 'alert error';
    }
}

window.openDeleteDeptModal = function(id, name) {
    deptDeleteTarget = id;
    document.getElementById('dept-confirm-name').textContent = name;
    document.getElementById('dept-confirm-modal').classList.remove('hidden');
};

function closeDeptConfirm() {
    document.getElementById('dept-confirm-modal').classList.add('hidden');
    deptDeleteTarget = null;
}

async function doDeleteDept() {
    if (!deptDeleteTarget) return;
    try {
        const res = await fetch(`${DEPT_API}/${deptDeleteTarget}`, { method: 'DELETE' });
        if (res.ok || res.status === 204) {
            closeDeptConfirm();
            await loadDeptTable(deptCurrentPage);
            await loadDepartments();
            showToast('Department deleted.', 'success');
        } else {
            const data = await res.json();
            closeDeptConfirm();
            showToast(data.error || 'Delete failed.', 'error');
        }
    } catch (e) { showToast('Cannot connect to server.', 'error'); }
}

/* ── Report pagination state ───────────────────────────────────────────── */
let reportCurrentPage = 0;
const REPORT_PAGE_SIZE = 5;
let reportFilteredAll = [];

/* ── Reports ───────────────────────────────────────────────────────────── */
const DEPT_COLORS = {
    engineering: 'dept-engineering', hr: 'dept-hr', 'human resources': 'dept-hr',
    finance: 'dept-finance', marketing: 'dept-marketing', operations: 'dept-operations',
    sales: 'dept-sales', it: 'dept-it', legal: 'dept-legal',
    design: 'dept-design', product: 'dept-product',
};
function deptClass(dept) { return DEPT_COLORS[(dept || '').toLowerCase()] || 'dept-default'; }

function calcAge(birthday) {
    if (!birthday) return null;
    let dob;
    if (Array.isArray(birthday)) { const [y,m,d] = birthday; dob = new Date(y, m-1, d); }
    else dob = new Date(birthday);
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
    const map = { 'under25':'age-under25','25-34':'age-25-34','35-44':'age-35-44','45-54':'age-45-54','55plus':'age-55plus' };
    return map[key] || '';
}

let reportAllEmployees = [];

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
    const btn      = document.getElementById('sort-toggle-btn');
    const label    = document.getElementById('sort-label');
    const iconAsc  = document.getElementById('sort-icon-asc');
    const iconDesc = document.getElementById('sort-icon-desc');
    if (!btn) return;
    const isDesc = sortOrder === 'desc';
    label.textContent      = isDesc ? 'Desc' : 'Asc';
    iconAsc.style.display  = isDesc ? 'none'  : 'block';
    iconDesc.style.display = isDesc ? 'block' : 'none';
    btn.classList.toggle('active', isDesc);
}

function initReportFilters() {
    const deptSel   = document.getElementById('dept-filter-select');
    const ageSel    = document.getElementById('age-filter-select');
    const statusSel = document.getElementById('report-status-filter');
    if (deptSel)   deptSel.addEventListener('change',   () => { reportCurrentPage = 0; applyReportFilters(0); });
    if (ageSel)    ageSel.addEventListener('change',    () => { reportCurrentPage = 0; applyReportFilters(0); });
    if (statusSel) statusSel.addEventListener('change', () => { updatePdfLinks(); reportCurrentPage = 0; applyReportFilters(0); });
    updatePdfLinks();
}

function updatePdfLinks() {
    const statusVal   = document.getElementById('report-status-filter')?.value || '';
    const activeParam = statusVal === 'active' ? '?active=true' : statusVal === 'inactive' ? '?active=false' : '';
    const deptBtn = document.getElementById('pdf-dept-btn');
    const ageBtn  = document.getElementById('pdf-age-btn');
    if (deptBtn) deptBtn.href = `${BASE_URL}/api/report/pdf/by-department${activeParam}`;
    if (ageBtn)  ageBtn.href  = `${BASE_URL}/api/report/pdf/by-age${activeParam}`;
}

function triggerActiveReport() { loadReportData(); }

async function loadReportData() {
    const tbody = document.getElementById('report-body');
    tbody.innerHTML = '<tr><td colspan="7" class="loading-row">Loading…</td></tr>';
    try {
        // Reports use unpaginated endpoint (all records)
        const res = await fetch(`${EMP_API}?size=10000`);
        const data = await res.json();
        reportAllEmployees = data.content || [];
        populateDeptDropdown_report(reportAllEmployees);
        applyReportFilters();
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading-row">Failed to load report.</td></tr>';
    }
}

function populateDeptDropdown_report(list) {
    const sel = document.getElementById('dept-filter-select');
    if (!sel) return;
    const current = sel.value;
    const depts = [...new Set((list || [])
        .map(e => getDeptName(e.department))
        .filter(n => n && n !== '—'))].sort();
    sel.innerHTML = '<option value="">All Departments</option>' +
        depts.map(d => `<option value="${esc(d)}"${d === current ? ' selected' : ''}>${esc(d)}</option>`).join('');
}

function applyReportFilters(page) {
    if (page === undefined) { reportCurrentPage = 0; page = 0; }
    reportCurrentPage = page;

    const deptVal   = (document.getElementById('dept-filter-select')?.value   || '').toLowerCase();
    const ageVal    =  document.getElementById('age-filter-select')?.value    || '';
    const statusVal =  document.getElementById('report-status-filter')?.value || '';

    let filtered = reportAllEmployees;
    if (deptVal) filtered = filtered.filter(e => getDeptName(e.department).toLowerCase() === deptVal);
    if (ageVal)  filtered = filtered.filter(e => { const r = ageRange(calcAge(e.birthday)); return r && r.key === ageVal; });
    if (statusVal === 'active')   filtered = filtered.filter(e => e.isActive !== false);
    if (statusVal === 'inactive') filtered = filtered.filter(e => e.isActive === false);

    const dir = sortOrder === 'asc' ? 1 : -1;
    filtered = [...filtered].sort((a, b) => {
        const dA = getDeptName(a.department).localeCompare(getDeptName(b.department)) * dir;
        if (dA !== 0) return dA;
        return ((calcAge(a.birthday) ?? 99) - (calcAge(b.birthday) ?? 99)) * dir;
    });

    reportFilteredAll = filtered;
    const totalR = filtered.length;
    const totalRPages = Math.ceil(totalR / REPORT_PAGE_SIZE);
    reportCurrentPage = Math.min(reportCurrentPage, Math.max(0, totalRPages - 1));
    const start = reportCurrentPage * REPORT_PAGE_SIZE;
    const pageSlice = filtered.slice(start, start + REPORT_PAGE_SIZE);

    renderReport(pageSlice, reportCurrentPage, totalRPages, totalR);
}

function renderReport(list, currentR, totalRPages, totalR) {
    const tbody = document.getElementById('report-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading-row">No employees match the selected filters.</td></tr>';
        renderReportPagination(0, 0, 0);
        return;
    }
    tbody.innerHTML = list.map(e => {
        const age      = calcAge(e.birthday);
        const range    = ageRange(age);
        const deptName = getDeptName(e.department);
        const ageBadge = range
            ? `<span class="age-badge ${ageClass(range.key)}">${esc(range.label)}</span>`
            : '<span style="color:#94a3b8">—</span>';
        const deptBadge = `<span class="dept-badge ${deptClass(deptName)}">${esc(deptName)}</span>`;
        const active    = e.isActive !== false;
        const pill      = active
            ? '<span class="status-pill active">Active</span>'
            : '<span class="status-pill inactive">Inactive</span>';
        return `
        <tr>
            <td>#${e.id}</td>
            <td><span class="emp-name">${esc(e.firstname || '')} ${esc(e.lastname || '')}</span></td>
            <td>${e.birthday ? formatDate(e.birthday) : '—'}</td>
            <td>${ageBadge}</td>
            <td>${deptBadge}</td>
            <td>₱${(e.salary ?? 0).toLocaleString('en-PH', { minimumFractionDigits: 2 })}</td>
            <td>${pill}</td>
        </tr>`;
    }).join('');
    renderReportPagination(currentR, totalRPages, totalR);
}

function renderReportPagination(current, total, totalItems) {
    const container = document.getElementById('report-pagination');
    if (!container) return;
    if (total <= 1) { container.style.display = 'none'; container.innerHTML = ''; return; }
    container.style.display = 'flex';
    const prevDis = current === 0 ? 'disabled' : '';
    const nextDis = current >= total - 1 ? 'disabled' : '';
    container.innerHTML = `
        <span class="page-info">Page ${current + 1} of ${total} &nbsp;·&nbsp; ${totalItems} records</span>
        <div class="page-btns">
            <button class="page-btn" onclick="applyReportFilters(${current - 1})" ${prevDis}>‹ Prev</button>
            ${buildPageButtons(current, total, 'applyReportFilters')}
            <button class="page-btn" onclick="applyReportFilters(${current + 1})" ${nextDis}>Next ›</button>
        </div>`;
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
        const [y,m,day] = d;
        return `${String(day).padStart(2,'0')}/${String(m).padStart(2,'0')}/${y}`;
    }
    const dt = new Date(d);
    if (isNaN(dt)) return d;
    return dt.toLocaleDateString('en-GB');
}

function formatDateInput(d) {
    if (!d) return '';
    if (Array.isArray(d)) {
        const [y,m,day] = d;
        return `${y}-${String(m).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
    }
    if (typeof d === 'string' && /^\d{4}-\d{2}-\d{2}/.test(d)) return d.slice(0,10);
    const dt = new Date(d);
    if (isNaN(dt)) return '';
    return dt.toISOString().slice(0,10);
}
