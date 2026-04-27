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
            showAlert('Please fill in both email and password.', 'error');
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
                showAlert('Sign in successful! Redirecting…', 'success');
                setTimeout(() => { window.location.href = 'dashboard.html'; }, 800);
            } else {
                const err = await response.json();
                showAlert(err.message || 'Incorrect email or password. Please try again.', 'error');
                // Shake the inputs to draw attention
                document.getElementById('email').classList.add('invalid');
                document.getElementById('password').classList.add('invalid');
                setTimeout(() => {
                    document.getElementById('email').classList.remove('invalid');
                    document.getElementById('password').classList.remove('invalid');
                }, 2000);
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

    // Toggle password visibility
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
            // Remove all state classes, then apply the correct one
            alertBox.classList.remove('hidden', 'error', 'success');
            alertBox.classList.add(type);

            // Auto-hide after 4 seconds
            setTimeout(() => {
                alertBox.classList.add('hidden');
                alertBox.classList.remove('error', 'success');
            }, 4000);
        } else {
            console.error(message);
        }
    }
});