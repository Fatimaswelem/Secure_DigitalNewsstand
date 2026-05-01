// sections.js – loads follow state from backend + toggles buttons
console.log("sections.js loaded");

document.addEventListener('DOMContentLoaded', function () {
    renderSections();
    loadTrendingTopics();
    animateSectionCards();
});

async function renderSections() {
    const grid = document.getElementById('sectionsGrid');
    if (!grid) {
        console.error("Grid element #sectionsGrid not found");
        return;
    }

    try {
        // 1. Fetch categories and followed sections in parallel
        const [catRes, followRes] = await Promise.all([
            fetch('/Secure_DigitalNewsstand/api/categories', { credentials: 'include' }),
            fetch('/Secure_DigitalNewsstand/api/favorites/sections', { credentials: 'include' })
        ]);

        if (!catRes.ok) throw new Error('Failed to fetch categories');
        const categories = await catRes.json();
        console.log("Categories received:", categories);

        // Build a Set of followed category IDs (if the endpoint succeeded)
        let followedIds = new Set();
        if (followRes.ok) {
            const followed = await followRes.json();
            followedIds = new Set(followed.map(f => f.id));
        }

        if (categories.length === 0) {
            grid.innerHTML = '<p style="grid-column:1/-1; text-align:center; color:#888;">No categories available right now.</p>';
            return;
        }

        // 2. Render cards, marking followed ones
        grid.innerHTML = categories.map(cat => {
            const isFollowing = followedIds.has(cat.id);
            return `
                <div class="section-card" data-category-id="${cat.id}" style="cursor:pointer;">
                    <span class="section-icon">${cat.icon || '📂'}</span>
                    <h3>${cat.name}</h3>
                    <button class="follow-btn ${isFollowing ? 'following' : ''}" data-id="${cat.id}">
                        ${isFollowing ? 'Following ✓' : 'Follow'}
                    </button>
                </div>
            `;
        }).join('');

        // 3. Single delegated click handler
        grid.addEventListener('click', async function (e) {
            const btn = e.target.closest('.follow-btn');
            if (btn) {
                e.stopPropagation();   // prevent card navigation
                const categoryId = parseInt(btn.dataset.id, 10);
                const isFollowing = btn.classList.contains('following');

                // Optimistically toggle the button
                btn.classList.toggle('following');
                btn.textContent = btn.classList.contains('following') ? 'Following ✓' : 'Follow';

                try {
                    const res = await fetch('/Secure_DigitalNewsstand/api/favorites/sections', {
                        method: 'POST',
                        credentials: 'include',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            sectionId: categoryId,
                            action: isFollowing ? 'unfollow' : 'follow'
                        })
                    });

                    if (!res.ok) {
                        // Revert on failure
                        btn.classList.toggle('following');
                        btn.textContent = btn.classList.contains('following') ? 'Following ✓' : 'Follow';
                        console.error('Failed to update follow status');
                    }
                } catch (err) {
                    // Revert on network error
                    btn.classList.toggle('following');
                    btn.textContent = btn.classList.contains('following') ? 'Following ✓' : 'Follow';
                }
                return;
            }

            // Otherwise, navigate to articles if card clicked
            const card = e.target.closest('.section-card');
            if (card) {
                const catId = card.dataset.categoryId;
                if (catId) {
                    window.location.href = `articles.html?categoryId=${catId}`;
                }
            }
        });

    } catch (err) {
        console.error("Error loading categories:", err);
        grid.innerHTML = '<p style="grid-column:1/-1; text-align:center; color:#888;">No categories available right now.</p>';
    }
}

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

function animateSectionCards() {
    document.querySelectorAll('.section-card').forEach(card => {
        card.addEventListener('mouseenter', () => card.style.transform = 'translateY(-5px)');
        card.addEventListener('mouseleave', () => card.style.transform = 'translateY(0)');
    });
}