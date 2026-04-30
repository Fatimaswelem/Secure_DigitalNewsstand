document.addEventListener('DOMContentLoaded', function () {
    // Payment method toggle highlight
    document.querySelectorAll('.method').forEach(function (method) {
        method.addEventListener('click', function () {
            document.querySelectorAll('.method').forEach(m => m.classList.remove('blue-border'));
            this.classList.add('blue-border');
        });
    });

    document.querySelector('.next-btn').addEventListener('click', function (e) {
        e.preventDefault();

        const cardInputs = document.querySelectorAll('.input-fields input');
        cardInputs.forEach(i => i.classList.remove('warning'));

        let isValid = true;
        cardInputs.forEach(function (input) {
            if (!input.value.trim()) {
                input.classList.add('warning');
                isValid = false;
            }
        });

        const cardNumber = document.getElementById('cardnumber').value.trim();
        const date       = document.getElementById('date').value.trim();
        const cvv        = document.getElementById('verification').value.trim();

        if (!/^\d{16}$/.test(cardNumber)) {
            document.getElementById('cardnumber').classList.add('warning');
            isValid = false;
        }
        if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(date)) {
            document.getElementById('date').classList.add('warning');
            isValid = false;
        }
        if (!/^\d{3,4}$/.test(cvv)) {
            document.getElementById('verification').classList.add('warning');
            isValid = false;
        }

        if (!isValid) {
            alert('Please fill all fields correctly.');
        } else {
            window.location.href = 'finish.html';
        }
    });
});