document.addEventListener('DOMContentLoaded', async function () {
    const roleEl     = document.getElementById('roleMessage');
    const detailsDiv = document.getElementById('planDetails');
    const featuresUl = document.getElementById('featuresList');
    const upgradeBtn = document.getElementById('upgradeBtn');

    try {
        const res  = await fetch('/Secure_DigitalNewsstand/auth/me', { credentials: 'include' });
        if (!res.ok) { window.location.href = '../Auth/index.html'; return; }
        const user = await res.json();

        const roleName = user.roleName || 'Unknown';
        const article  = (roleName.toLowerCase() === 'admin') ? 'an' : 'a';
        roleEl.textContent = `You are currently signed in as ${article} ${roleName} user.`;

        // Show plan details for subscribed roles
        const featureMap = {
            Basic:    ['✔ Access to basic content', '✔ 5GB Storage', '✔ Email Support'],
            Standard: ['✔ Everything in Basic', '✔ 50GB Storage', '✔ Priority Support'],
            Premium:  ['✔ Everything in Standard', '✔ 200GB Storage', '✔ 1-on-1 Coaching']
        };

        if (featureMap[roleName]) {
            detailsDiv.style.display = 'block';
            featuresUl.innerHTML = featureMap[roleName].map(f => `<li>${f}</li>`).join('');
        }

        // Hide "Change Plan" button for admin
        if (roleName === 'Admin') {
            upgradeBtn.style.display = 'none';
        }
    } catch (err) {
        window.location.href = '../Auth/index.html';
    }
});