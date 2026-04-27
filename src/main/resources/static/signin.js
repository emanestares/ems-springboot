const API_BASE = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('signin-form');
    const alertBox = document.getElementById('alert-box');
    const submitBtn = document.getElementById('submit-btn');
    const btnText = submitBtn?.querySelector('.btn-text');
    const btnSpinner = submitBtn?.querySelector('.btn-spinner');

    if (!form) {
        console.error('Form with id "signin-form" not found');
        return;
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        // Basic client-side validation
        if (!email || !password) {
            showAlert('Please fill in both email and password', 'error');
            return;
        }

        // Show loading state
        if (submitBtn) {
            submitBtn.disabled = true;
            btnText?.classList.add('hidden');
            btnSpinner?.classList.remove('hidden');
        }

        try {
            const response = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (response.ok) {
                const data = await response.json();
                // Store token and user info
                localStorage.setItem('token', data.token);
                localStorage.setItem('user', JSON.stringify(data.user));
                window.location.href = 'dashboard.html';
            } else {
                const err = await response.json();
                showAlert(err.message || 'Invalid email or password', 'error');
            }
        } catch (err) {
            showAlert('Network error. Is the backend running?', 'error');
        } finally {
            // Reset button state
            if (submitBtn) {
                submitBtn.disabled = false;
                btnText?.classList.remove('hidden');
                btnSpinner?.classList.add('hidden');
            }
        }
    });

    // Optional: Toggle password visibility
    const togglePw = document.getElementById('toggle-pw');
    const passwordField = document.getElementById('password');
    if (togglePw && passwordField) {
        togglePw.addEventListener('click', () => {
            const type = passwordField.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordField.setAttribute('type', type);
        });
    }

    function showAlert(message, type = 'error') {
        if (alertBox) {
            alertBox.textContent = message;
            alertBox.classList.remove('hidden');
            // Auto-hide after 4 seconds
            setTimeout(() => {
                alertBox.classList.add('hidden');
            }, 4000);
        } else {
            console.error(message);
        }
    }
});