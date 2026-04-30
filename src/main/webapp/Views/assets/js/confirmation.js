document.addEventListener('DOMContentLoaded', function () {
    // ── Load plan info from sessionStorage ──
    const planName  = sessionStorage.getItem('selectedPlan')  || 'Selected Plan';
    const planPrice = sessionStorage.getItem('selectedPrice') || '-';
    document.getElementById('planName').textContent  = planName;
    document.getElementById('planPrice').textContent = planPrice;

    // ── Promo code validation ──────────────────────────
    const promoInput = document.getElementById('promo');
    promoInput.addEventListener('blur', async function () {
        const code = promoInput.value.trim();
        if (!code) return;

        const planId = parseInt(sessionStorage.getItem('selectedPlanId'));
        if (!planId) return;

        try {
            const res = await fetch('/Secure_DigitalNewsstand/api/promo/validate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ promoCode: code, planId: planId })
            });
            const data = await res.json();

            if (data.valid) {
                document.getElementById('planPrice').textContent = `$${data.discountedPrice}/mo`;
                // Save discount details for later
                sessionStorage.setItem('appliedPromo', JSON.stringify(data));
                alert('Promo code applied!');
            } else {
                alert(data.message || 'Invalid promo code.');
                promoInput.value = '';
                // Reset price to original
                document.getElementById('planPrice').textContent = sessionStorage.getItem('selectedPrice') || '-';
                sessionStorage.removeItem('appliedPromo');
            }
        } catch (err) {
            alert('Could not validate promo code. Please try again.');
        }
    });

    // ── Form validation & proceed ──────────────────────
    document.querySelector('.next-btn').addEventListener('click', function (e) {
        e.preventDefault();

        const firstName = document.getElementById('firs-name').value.trim();
        const email = document.getElementById('email').value.trim();
        const phone = document.getElementById('phone').value.trim();

        let isValid = true;

        [document.getElementById('firs-name'),
         document.getElementById('email'),
         document.getElementById('phone')].forEach(function (el) {
            el.classList.remove('warning');
            if (!el.value.trim()) { el.classList.add('warning'); isValid = false; }
        });

        if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            document.getElementById('email').classList.add('warning');
            isValid = false;
        }
        if (phone && !/^\+?[\d\s\-]{7,15}$/.test(phone)) {
            document.getElementById('phone').classList.add('warning');
            isValid = false;
        }

        if (!isValid) {
            alert('Please fill all fields correctly.');
            return;
        }

        // Build subscription data (can include promo code)
        const promoData = JSON.parse(sessionStorage.getItem('appliedPromo') || '{}');
        const subscriptionData = {
            firstName: firstName,
            phone: phone,
            planId: parseInt(sessionStorage.getItem('selectedPlanId')) || 1,
            promoCode: promoData.valid ? document.getElementById('promo').value.trim() : null
        };

        // Send to backend (this endpoint may not exist yet – graceful fallback)
        fetch('/Secure_DigitalNewsstand/api/subscribe', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(subscriptionData)
        })
        .then(function (res) {
            if (res.ok || res.status === 404) {
                // Proceed to payment even if subscribe endpoint is pending
                window.location.href = 'pay.html';
            } else {
                alert('Could not process subscription. Please try again.');
            }
        })
        .catch(function () {
            window.location.href = 'pay.html';   // offline fallback
        });
    });
});