/* ═══════════════════════════════════════════════════════════════
   EMS — dashboard.js
   Matches dashboard.html element IDs exactly.
═══════════════════════════════════════════════════════════════ */

const EMP_API  = 'http://localhost:8080/api/employees';
const AUTH_API = 'http://localhost:8080/api/auth';

/* ── State ──────────────────────────────────────────────────── */
let allEmployees    = [];
let currentDetailId = null;
let editingId       = null;
let deleteTarget    = null;

/* ── DOM ready ──────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
    loadStats();
    loadEmployees();
    setNavUser();
});

function setNavUser() {
    const el = document.getElementById('navUser');
    if (el) el.textContent = sessionStorage.getItem('adminName') || '';
}

function signOut() {
    sessionStorage.clear();
    window.location.href = 'signin.html';
}

/* ════════════════════════════════════════════════════════════════
   STATS
════════════════════════════════════════════════════════════════ */
async function loadStats() {
    try {
        const res  = await fetch(`${EMP_API}/stats`);
        const data = await res.json();
        setText('statTotal',     data.totalEmployees  ?? 0);
        setText('statActive',    data.activeEmployees ?? 0);
        setText('statDepts',     data.byDepartment ? Object.keys(data.byDepartment).length : 0);
        setText('statAvgSalary', '₱' + (data.averageSalary ?? 0).toLocaleString('en-PH', { minimumFractionDigits: 2 }));
    } catch (e) {
        console.error('Stats error', e);
    }
}

/* ════════════════════════════════════════════════════════════════
   EMPLOYEE LIST
════════════════════════════════════════════════════════════════ */
async function loadEmployees() {
    const tbody = document.getElementById('empTableBody');
    tbody.innerHTML = '<tr><td colspan="7" class="table-empty">Loading employees...</td></tr>';
    try {
        const res    = await fetch(EMP_API);
        allEmployees = await res.json();
        populateDeptFilter();
        filterTable();
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="7" class="table-empty">Failed to load. Is the backend running?</td></tr>';
    }
}

function populateDeptFilter() {
    const sel = document.getElementById('deptFilter');
    const cur = sel.value;
    const depts = [...new Set(allEmployees.map(e => e.department).filter(Boolean))].sort();
    sel.innerHTML = '<option value="">All Departments</option>' +
        depts.map(d => '<option value="' + esc(d) + '"' + (d === cur ? ' selected' : '') + '>' + esc(d) + '</option>').join('');
}

function filterTable() {
    const search  = (document.getElementById('searchInput').value  || '').toLowerCase().trim();
    const dept    = (document.getElementById('deptFilter').value   || '').toLowerCase();
    const status  = (document.getElementById('statusFilter').value || '');

    let list = allEmployees.filter(function(e) {
        const name = ((e.firstname || '') + ' ' + (e.lastname || '')).toLowerCase();
        if (search && !name.includes(search)) return false;
        if (dept   && (e.department || '').toLowerCase() !== dept) return false;
        if (status === 'active'   && !e.isActive) return false;
        if (status === 'inactive' && e.isActive)  return false;
        return true;
    });

    renderTable(list);

    const countEl = document.getElementById('tableCount');
    if (countEl) countEl.textContent = 'Showing ' + list.length + ' of ' + allEmployees.length + ' employee' + (allEmployees.length !== 1 ? 's' : '');
}

function renderTable(list) {
    const tbody = document.getElementById('empTableBody');
    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="table-empty">No employees found.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(function(e) {
        const active      = e.isActive !== false;
        const pill        = active
            ? '<span class="status-pill active">Active</span>'
            : '<span class="status-pill inactive">Inactive</span>';
        const toggleTitle = active ? 'Set Inactive' : 'Set Active';
        const toggleIcon  = active ? '✅' : '⬜';
        const toggleClass = active ? 'btn-toggle is-active' : 'btn-toggle is-inactive';

        return '<tr class="emp-row-clickable" onclick="openDetailModal(' + e.id + ')">' +
            '<td>#' + e.id + '</td>' +
            '<td><span class="emp-name">' + esc(e.firstname || '') + ' ' + esc(e.lastname || '') + '</span></td>' +
            '<td><span class="dept-badge">' + esc(e.department || '—') + '</span></td>' +
            '<td>' + (e.birthday ? formatDate(e.birthday) : '—') + '</td>' +
            '<td>₱' + (e.salary != null ? e.salary : 0).toLocaleString('en-PH', { minimumFractionDigits: 2 }) + '</td>' +
            '<td>' + pill + '</td>' +
            '<td onclick="event.stopPropagation()">' +
                '<div class="action-btns">' +
                    '<button class="' + toggleClass + '" onclick="toggleActive(' + e.id + ')" title="' + toggleTitle + '">' + toggleIcon + '</button>' +
                    '<button class="btn-edit" onclick="openEditModal(' + e.id + ')" title="Edit">' +
                        '<svg viewBox="0 0 20 20" fill="none"><path d="M4 13.5V16h2.5l7.372-7.372-2.5-2.5L4 13.5z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M13.872 4.128a1.768 1.768 0 012.5 2.5l-1.128 1.128-2.5-2.5 1.128-1.128z" stroke="currentColor" stroke-width="1.3"/></svg>' +
                    '</button>' +
                    '<button class="btn-delete" onclick="deleteEmployee(' + e.id + ')" title="Delete">' +
                        '<svg viewBox="0 0 20 20" fill="none"><path d="M5 6h10l-1 10H6L5 6z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M3 6h14M8 6V4h4v2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>' +
                    '</button>' +
                '</div>' +
            '</td>' +
        '</tr>';
    }).join('');
}

/* ════════════════════════════════════════════════════════════════
   TOGGLE ACTIVE
════════════════════════════════════════════════════════════════ */
async function toggleActive(id) {
    try {
        const res = await fetch(EMP_API + '/' + id + '/toggle-active', { method: 'PATCH' });
        if (!res.ok) { showToast('Toggle failed.', 'error'); return; }
        const updated = await res.json();
        const idx = allEmployees.findIndex(function(e) { return e.id === id; });
        if (idx !== -1) allEmployees[idx] = updated;
        filterTable();
        loadStats();
        showToast(updated.isActive ? 'Employee set to Active.' : 'Employee set to Inactive.', 'success');
    } catch (e) {
        showToast('Cannot connect to server.', 'error');
    }
}

/* ════════════════════════════════════════════════════════════════
   DETAIL MODAL
════════════════════════════════════════════════════════════════ */
function openDetailModal(id) {
    const emp = allEmployees.find(function(e) { return e.id === id; });
    if (!emp) return;
    currentDetailId = id;

    const active = emp.isActive !== false;

    setText('detailName',     (emp.firstname || '') + ' ' + (emp.lastname || ''));
    setText('detailDept',     emp.department || '—');
    setText('detailId',       '#' + emp.id);
    setText('detailFirst',    emp.firstname  || '—');
    setText('detailLast',     emp.lastname   || '—');
    setText('detailDeptVal',  emp.department || '—');
    setText('detailBirthday', emp.birthday   ? formatDate(emp.birthday) : '—');
    var age = calcAge(emp.birthday);
    setText('detailAge',      age !== null ? age + ' yrs' : '—');
    setText('detailSalary',   '₱' + (emp.salary != null ? emp.salary : 0).toLocaleString('en-PH', { minimumFractionDigits: 2 }));
    setText('detailStatus',   active ? 'Active' : 'Inactive');

    var badge = document.getElementById('detailBadge');
    if (badge) {
        badge.textContent = active ? 'Active' : 'Inactive';
        badge.className   = 'detail-badge ' + (active ? 'active' : 'inactive');
    }

    document.getElementById('detailBackdrop').classList.add('open');
}

function closeDetailModal(event) {
    if (event && event.target !== event.currentTarget) return;
    document.getElementById('detailBackdrop').classList.remove('open');
    currentDetailId = null;
}

/* ════════════════════════════════════════════════════════════════
   ADD / EDIT MODAL
════════════════════════════════════════════════════════════════ */
function openAddModal() {
    editingId = null;
    setText('formTitle', 'Add Employee');
    var btn = document.getElementById('formSubmitBtn');
    if (btn) btn.textContent = 'Save Employee';
    clearFormFields();
    document.getElementById('editId').value = '';
    document.getElementById('formError').textContent = '';
    document.getElementById('formBackdrop').classList.add('open');
}

function openEditModal(id) {
    const emp = allEmployees.find(function(e) { return e.id === id; });
    if (!emp) return;
    editingId = id;

    setText('formTitle', 'Edit Employee');
    var btn = document.getElementById('formSubmitBtn');
    if (btn) btn.textContent = 'Update Employee';

    document.getElementById('editId').value       = emp.id;
    document.getElementById('fFirstname').value   = emp.firstname  || '';
    document.getElementById('fLastname').value    = emp.lastname   || '';
    document.getElementById('fDepartment').value  = emp.department || '';
    document.getElementById('fBirthday').value    = emp.birthday   ? formatDateInput(emp.birthday) : '';
    document.getElementById('fSalary').value      = emp.salary     != null ? emp.salary : '';
    document.getElementById('formError').textContent = '';

    document.getElementById('formBackdrop').classList.add('open');
}

function closeFormModal(event) {
    if (event && event.target !== event.currentTarget) return;
    document.getElementById('formBackdrop').classList.remove('open');
    clearFormFields();
    editingId = null;
}

function clearFormFields() {
    ['fFirstname','fLastname','fDepartment','fBirthday','fSalary'].forEach(function(id) {
        document.getElementById(id).value = '';
    });
}

async function submitForm() {
    var error = document.getElementById('formError');
    error.textContent = '';

    var lastname   = document.getElementById('fLastname').value.trim();
    var department = document.getElementById('fDepartment').value.trim();
    var salaryRaw  = document.getElementById('fSalary').value;

    if (!lastname)                                        { error.textContent = 'Last name is required.';        return; }
    if (!department)                                      { error.textContent = 'Department is required.';       return; }
    if (!salaryRaw || isNaN(salaryRaw) || Number(salaryRaw) < 0) { error.textContent = 'Enter a valid salary (>= 0).'; return; }

    var payload = {
        firstname:  document.getElementById('fFirstname').value.trim(),
        lastname:   lastname,
        birthday:   document.getElementById('fBirthday').value || null,
        department: department,
        salary:     parseFloat(salaryRaw),
    };

    var btn = document.getElementById('formSubmitBtn');
    btn.disabled = true;

    try {
        var url    = editingId ? (EMP_API + '/' + editingId) : EMP_API;
        var method = editingId ? 'PUT' : 'POST';
        var res    = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        var data = await res.json();

        if (res.ok) {
            closeFormModal();
            await loadEmployees();
            loadStats();
            showToast(editingId ? 'Employee updated!' : 'Employee added!', 'success');
        } else {
            error.textContent = data.error || 'Save failed.';
        }
    } catch (e) {
        error.textContent = 'Cannot connect to server.';
    } finally {
        btn.disabled = false;
    }
}

/* ════════════════════════════════════════════════════════════════
   DELETE
════════════════════════════════════════════════════════════════ */
function deleteEmployee(id) {
    var emp  = allEmployees.find(function(e) { return e.id === id; });
    deleteTarget = id;
    var name = emp ? ((emp.firstname || '') + ' ' + (emp.lastname || '')).trim() : '#' + id;
    setText('deleteConfirmText', 'Delete "' + name + '"? This action cannot be undone.');
    document.getElementById('deleteBackdrop').classList.add('open');
    document.getElementById('confirmDeleteBtn').onclick = doDelete;
}

function closeDeleteModal(event) {
    if (event && event.target !== event.currentTarget) return;
    document.getElementById('deleteBackdrop').classList.remove('open');
    deleteTarget = null;
}

async function doDelete() {
    if (!deleteTarget) return;
    try {
        var res = await fetch(EMP_API + '/' + deleteTarget, { method: 'DELETE' });
        if (res.ok || res.status === 204) {
            document.getElementById('deleteBackdrop').classList.remove('open');
            deleteTarget = null;
            await loadEmployees();
            loadStats();
            showToast('Employee deleted.', 'success');
        } else {
            showToast('Delete failed.', 'error');
        }
    } catch (e) {
        showToast('Cannot connect to server.', 'error');
    }
}

/* ════════════════════════════════════════════════════════════════
   REPORTS
════════════════════════════════════════════════════════════════ */
async function downloadReport(type) {
    try {
        var res  = await fetch(EMP_API + '/report/' + type);
        var list = await res.json();

        var csv = 'ID,First Name,Last Name,Department,Birthday,Salary,Status\n';
        csv += list.map(function(e) {
            return e.id + ',"' + (e.firstname || '') + '","' + (e.lastname || '') + '","' + (e.department || '') + '",' +
                (e.birthday ? formatDate(e.birthday) : '') + ',' + (e.salary != null ? e.salary : 0) + ',' +
                (e.isActive !== false ? 'Active' : 'Inactive');
        }).join('\n');

        var blob = new Blob([csv], { type: 'text/csv' });
        var url  = URL.createObjectURL(blob);
        var a    = document.createElement('a');
        a.href     = url;
        a.download = 'employees-' + type + '-' + new Date().toISOString().slice(0,10) + '.csv';
        a.click();
        URL.revokeObjectURL(url);
        showToast('Report downloaded!', 'success');
    } catch (e) {
        showToast('Could not generate report.', 'error');
    }
}

/* ════════════════════════════════════════════════════════════════
   TOAST
════════════════════════════════════════════════════════════════ */
var toastTimer;
function showToast(msg, type) {
    type = type || 'success';
    clearTimeout(toastTimer);
    var el = document.getElementById('toast');
    el.textContent = msg;
    el.className   = 'toast ' + type;
    toastTimer = setTimeout(function() { el.className = 'toast hidden'; }, 3000);
}

/* ════════════════════════════════════════════════════════════════
   HELPERS
════════════════════════════════════════════════════════════════ */
function esc(str) {
    return String(str)
        .replace(/&/g,'&amp;')
        .replace(/</g,'&lt;')
        .replace(/>/g,'&gt;')
        .replace(/"/g,'&quot;');
}

function setText(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
}

function formatDate(d) {
    if (!d) return '—';
    if (Array.isArray(d)) {
        var y = d[0], m = d[1], day = d[2];
        return String(day).padStart(2,'0') + '/' + String(m).padStart(2,'0') + '/' + y;
    }
    var dt = new Date(d);
    if (isNaN(dt)) return d;
    return dt.toLocaleDateString('en-GB');
}

function formatDateInput(d) {
    if (!d) return '';
    if (Array.isArray(d)) {
        var y = d[0], m = d[1], day = d[2];
        return y + '-' + String(m).padStart(2,'0') + '-' + String(day).padStart(2,'0');
    }
    if (typeof d === 'string' && /^\d{4}-\d{2}-\d{2}/.test(d)) return d.slice(0,10);
    var dt = new Date(d);
    if (isNaN(dt)) return '';
    return dt.toISOString().slice(0,10);
}

function calcAge(birthday) {
    if (!birthday) return null;
    var dob;
    if (Array.isArray(birthday)) {
        dob = new Date(birthday[0], birthday[1] - 1, birthday[2]);
    } else {
        dob = new Date(birthday);
    }
    if (isNaN(dob)) return null;
    var today = new Date();
    var age   = today.getFullYear() - dob.getFullYear();
    var mo    = today.getMonth() - dob.getMonth();
    if (mo < 0 || (mo === 0 && today.getDate() < dob.getDate())) age--;
    return age;
}
