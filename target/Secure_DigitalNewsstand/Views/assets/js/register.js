document.addEventListener('DOMContentLoaded', function () {
    const registerForm = document.getElementById('registerForm');

    if (registerForm) {
        registerForm.addEventListener('submit', function (e) {
            e.preventDefault();

            const name = document.getElementById('name').value;
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const language = document.getElementById('language').value;
            
            fetch('/Secure_DigitalNewsstand/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userName: name,
                    userEmail: email,
                    userPassword: password,
                    languageId: parseInt(language)
                })
            })
            .then(response => {
                if (response.status === 201) return response.json();
                return response.json().then(err => { throw new Error(err.message || 'Registration failed'); });
            })
            .then(data => {
                alert('Registration successful! Please login.');
                window.location.href = 'index.html';
            })
            .catch(error => {
                alert(error.message);
            });
        });
    }
});