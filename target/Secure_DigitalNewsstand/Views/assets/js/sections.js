// sections.js — no hardcoded categories; everything comes from backend

console.log("sections.js loaded");   // ← keep this for confirmation

document.addEventListener('DOMContentLoaded', function () {
    renderSections();
    loadTrendingTopics();
    animateSectionCards();
});

// ── Fetch sections from backend ──────────────────────────────────────────
async function renderSections() {
    const grid = document.getElementById('sectionsGrid');
    if (!grid) return;

    try {
        const res = await fetch('/Secure_DigitalNewsstand/api/categories', {
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Not available');

        const categories = await res.json();

        grid.innerHTML = categories.map(cat => `
            <div class="section-card">
                <span class="section-icon">${cat.icon || '📂'}</span>
                <h3>${cat.name}</h3>
                <button class="follow-btn" data-id="${cat.id}">Follow</button>
            </div>
        `).join('');   // ← was .join() before, now .join('')

        // ── Event delegation: any click on a .follow-btn will trigger ──
        grid.addEventListener('click', async function (e) {
            const btn = e.target.closest('.follow-btn');
            if (!btn) return;                           // not a follow button

            const categoryId = parseInt(btn.dataset.id, 10);
            const isFollowing = btn.classList.contains('following');

            try {
                const res = await fetch('/Secure_DigitalNewsstand/api/favorites/sections', {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        sectionId: categoryId,          // integer now
                        action: isFollowing ? 'unfollow' : 'follow'
                    })
                });

                if (res.ok) {
                    btn.classList.toggle('following');
                    btn.textContent = btn.classList.contains('following') ? 'Following ✓' : 'Follow';
                }
            } catch {
                // toggle visually even if server is unreachable
                btn.classList.toggle('following');
                btn.textContent = btn.classList.contains('following') ? 'Following ✓' : 'Follow';
            }
        });

    } catch (err) {
        grid.innerHTML = '<p style="grid-column:1/-1; text-align:center; color:#888;">No categories available right now.</p>';
    }
}

// ── Trending topics (unchanged) ──────────────────────────────────────────
function loadTrendingTopics() {
    fetch('/Secure_DigitalNewsstand/api/trending', { credentials: 'include' })
        .then(res => {
            if (!res.ok) throw new Error('not available');
            return res.json();
        })
        .then(topics => {
            const grid = document.getElementById('topicsGrid');
            if (!grid || !topics.length) return;
            grid.innerHTML = topics.map(t => `
                <a href="news.html?topic=${t.id}" class="topic-tag">${t.name} (${t.count})</a>
            `).join('');
        })
        .catch(() => {
            const section = document.querySelector('.trending-topics');
            if (section) section.style.display = 'none';
        });
}

// ── Hover animation ──────────────────────────────────────────────────────
function animateSectionCards() {
    document.querySelectorAll('.section-card').forEach(card => {
        card.addEventListener('mouseenter', () => card.style.transform = 'translateY(-5px)');
        card.addEventListener('mouseleave', () => card.style.transform = 'translateY(0)');
    });
}
