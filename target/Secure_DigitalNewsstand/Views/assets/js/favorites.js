document.addEventListener('DOMContentLoaded', function () {
    // Tab switching
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            this.classList.add('active');
            document.getElementById(this.dataset.tab).classList.add('active');
        });
    });

    // Load content from backend
    loadSavedArticles();
    loadFollowedSections();
});

// ── Saved Articles ───────────────────────────────────────────────────────
async function loadSavedArticles() {
    const grid = document.querySelector('#articles .saved-articles-grid');
    if (!grid) return;

    try {
        const res = await fetch('/Secure_DigitalNewsstand/api/favorites/articles', {
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Not available');
        const articles = await res.json();

        if (articles.length === 0) {
            grid.innerHTML = '<p style="text-align:center; color:#888;">No saved articles yet.</p>';
            return;
        }

        grid.innerHTML = articles.map(article => `
            <div class="article-card">
                <img src="${article.image || 'https://via.placeholder.com/300x200'}" alt="${article.title}">
                <div class="article-content">
                    <span class="category">${article.category || ''}</span>
                    <h3>${article.title}</h3>
                    <p>${article.summary || ''}</p>
                    <div class="article-meta">
                        <span class="date">${new Date(article.date).toLocaleDateString()}</span>
                        <button class="remove-btn" data-id="${article.id}">Remove</button>
                    </div>
                </div>
            </div>
        `).join('');

        // Attach remove handlers
        document.querySelectorAll('#articles .remove-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const articleId = this.dataset.id;
                const card = this.closest('.article-card');
                // Send DELETE to backend
                fetch(`/Secure_DigitalNewsstand/api/favorites/articles?articleId=${articleId}`, {
                    method: 'DELETE',
                    credentials: 'include'
                });
                if (card) card.remove();
            });
        });

    } catch (err) {
        grid.innerHTML = '<p style="text-align:center; color:#888;">Could not load saved articles.</p>';
    }
}

// ── Followed Sections ─────────────────────────────────────────────────────
async function loadFollowedSections() {
    const grid = document.querySelector('#sections .favorite-sections-grid');
    if (!grid) return;

    try {
        const res = await fetch('/Secure_DigitalNewsstand/api/favorites/sections', {
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Not available');
        const sections = await res.json();

        if (sections.length === 0) {
            grid.innerHTML = '<p style="text-align:center; color:#888;">No followed sections yet.</p>';
            return;
        }

        grid.innerHTML = sections.map(sec => `
            <div class="section-card">
                <div class="section-icon">${sec.icon || '📌'}</div>
                <h3>${sec.name}</h3>
                <p>${sec.description || ''}</p>
                <button class="unfollow-btn" data-id="${sec.id}">Unfollow</button>
            </div>
        `).join('');

        // Attach unfollow handlers
        document.querySelectorAll('#sections .unfollow-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const sectionId = this.dataset.id;
                const card = this.closest('.section-card');
                fetch('/Secure_DigitalNewsstand/api/favorites/sections', {
                    method: 'POST',   // or DELETE, adjust to your backend
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sectionId, action: 'unfollow' })
                });
                if (card) card.remove();
            });
        });

    } catch (err) {
        grid.innerHTML = '<p style="text-align:center; color:#888;">Could not load followed sections.</p>';
    }
}