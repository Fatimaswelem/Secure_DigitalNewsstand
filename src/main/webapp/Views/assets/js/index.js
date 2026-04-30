document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');

    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            
            // Java-Friendly Fetch Request
            fetch('/Secure_DigitalNewsstand/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userEmail: email,
                    userPassword: password
                })
            })
            .then(response => {
                if (response.ok) return response.json();
                throw new Error('Invalid email or password.');
            })
            .then(data => {
                // Store user info if needed and redirect
                localStorage.setItem('user', JSON.stringify(data));
                window.location.href = '../Client/sections.html';
            })
            .catch(error => {
                alert(error.message);
            });
        });
    }

    // Visual Effects
    const buttons = document.querySelectorAll('button');
    buttons.forEach(button => {
        button.addEventListener('mousedown', () => button.style.transform = 'scale(0.98)');
        button.addEventListener('mouseup', () => button.style.transform = 'scale(1)');
        button.addEventListener('mouseleave', () => button.style.transform = 'scale(1)');
    });
});