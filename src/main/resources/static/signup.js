const API = 'http://localhost:8080/api/auth';

const form       = document.getElementById('signup-form');
const fnInput    = document.getElementById('firstname');
const lnInput    = document.getElementById('lastname');
const emailInput = document.getElementById('email');
const pwInput    = document.getElementById('password');
const cfInput    = document.getElementById('confirm');
const alertBox   = document.getElementById('alert-box');
const submitBtn  = document.getElementById('submit-btn');
const btnText    = submitBtn.querySelector('.btn-text');
const btnSpinner = submitBtn.querySelector('.btn-spinner');
const togglePw   = document.getElementById('toggle-pw');
const strengthFill  = document.getElementById('strength-fill');
const strengthLabel = document.getElementById('strength-label');

const ERRORS = {
    fn:      document.getElementById('fn-err'),
    ln:      document.getElementById('ln-err'),
    email:   document.getElementById('email-err'),
    pw:      document.getElementById('pw-err'),
    confirm: document.getElementById('confirm-err')
};

// ── Toggle password visibility ──────────────────────────────────────────
togglePw.addEventListener('click', () => {
    pwInput.type = pwInput.type === 'text' ? 'password' : 'text';
});

// ── Password strength ───────────────────────────────────────────────────
pwInput.addEventListener('input', () => {
    const val = pwInput.value;
    let score = 0;
    if (val.length >= 6)                score++;
    if (val.length >= 10)               score++;
    if (/[A-Z]/.test(val))              score++;
    if (/[0-9]/.test(val))              score++;
    if (/[^A-Za-z0-9]/.test(val))       score++;

    const colors  = ['#ef4444','#f97316','#eab308','#22c55e','#16a34a'];
    const labels  = ['Very weak','Weak','Fair','Strong','Very strong'];
    const widths  = ['20%','40%','60%','80%','100%'];

    strengthFill.style.width      = score > 0 ? widths[score - 1] : '0';
    strengthFill.style.background = score > 0 ? colors[score - 1] : '';
    strengthLabel.textContent     = score > 0 ? labels[score - 1] : '';
});

// ── Clear errors on input ───────────────────────────────────────────────
Object.entries({ fn: fnInput, ln: lnInput, email: emailInput, pw: pwInput, confirm: cfInput })
    .forEach(([key, el]) => {
        el.addEventListener('input', () => {
            el.classList.remove('invalid');
            ERRORS[key].textContent = '';
        });
    });

function setError(input, errEl, msg) {
    input.classList.add('invalid');
    errEl.textContent = msg;
    return false;
}

function validateForm() {
    let ok = true;
    if (!fnInput.value.trim())
        ok = setError(fnInput, ERRORS.fn, 'First name is required.') && ok;
    if (!lnInput.value.trim())
        ok = setError(lnInput, ERRORS.ln, 'Last name is required.') && ok;
    if (!emailInput.value.trim() || !/\S+@\S+\.\S+/.test(emailInput.value))
        ok = setError(emailInput, ERRORS.email, 'Enter a valid email address.') && ok;
    if (!pwInput.value || pwInput.value.length < 6)
        ok = setError(pwInput, ERRORS.pw, 'Password must be at least 6 characters.') && ok;
    if (pwInput.value !== cfInput.value)
        ok = setError(cfInput, ERRORS.confirm, 'Passwords do not match.') && ok;
    return ok;
}

function showAlert(msg, type = 'error') {
    alertBox.textContent = msg;
    alertBox.className = `alert ${type}`;
}
function hideAlert() { alertBox.className = 'alert hidden'; }

function setLoading(loading) {
    submitBtn.disabled = loading;
    btnText.textContent = loading ? 'Creating account…' : 'Create Account';
    btnSpinner.classList.toggle('hidden', !loading);
}

// ── Submit ──────────────────────────────────────────────────────────────
form.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert();
    if (!validateForm()) return;

    setLoading(true);
    try {
        const res = await fetch(`${API}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                firstname: fnInput.value.trim(),
                lastname:  lnInput.value.trim(),
                email:     emailInput.value.trim(),
                password:  pwInput.value
            })
        });

        const data = await res.json();

        if (res.ok) {
            showAlert('Account created! Redirecting to sign in…', 'success');
            setTimeout(() => { window.location.href = 'signin.html'; }, 1200);
        } else if (res.status === 409) {
            showAlert(data.error || 'Email is already registered.');
            setLoading(false);
        } else {
            showAlert(data.error || 'Registration failed. Please try again.');
            setLoading(false);
        }
    } catch (err) {
        showAlert('Cannot connect to the server. Make sure the backend is running.');
        setLoading(false);
    }
});