document.addEventListener('DOMContentLoaded', function() {
    console.log("Secure Personalized News Feed Loaded.");

    // 1. Fetch Personalized Content from Java Backend
    // This proves your system identifies the logged-in user via their session cookie
    fetch('/Secure_DigitalNewsstand/api/news/personalized')
        .then(response => {
            if (response.status === 403) {
                throw new Error("Access Denied: You don't have permission to view this content.");
            }
            return response.json();
        })
        .then(data => {
            displayPersonalizedArticles(data);
        })
        .catch(error => {
            console.error('Error fetching personalized news:', error);
            const container = document.querySelector('.news-grid-foryou');
            if (container) {
                container.innerHTML = `<p class="error-msg">${error.message}</p>`;
            }
        });

    // 2. Handle Game Sidebar Navigation
    const gameLinks = document.querySelectorAll('.games-sidebar .game-link');
    gameLinks.forEach(link => {
        // Visual feedback
        link.addEventListener('mouseover', () => {
            link.style.backgroundColor = '#f0f0f0'; 
        });
        link.addEventListener('mouseout', () => {
            link.style.backgroundColor = 'transparent';
        });

        // Actual Navigation Logic
        link.addEventListener('click', (event) => {
            event.preventDefault(); 
            const gameType = link.getAttribute('data-game') || 'default';
            // Redirect to a secure Java-mapped view
            window.location.href = `/Secure_DigitalNewsstand/Views/Client/games.html?type=${gameType}`;
        });
    });
});

/**
 * Dynamically populates the news grid with data from the Java API
 */
function displayPersonalizedArticles(articles) {
    const container = document.querySelector('.news-grid-foryou');
    if (!container) return;

    if (articles.length === 0) {
        container.innerHTML = '<p class="no-articles-msg">No articles found for your interests yet.</p>';
        return;
    }

    container.innerHTML = articles.map(article => `
        <div class="news-card">
            <a href="article.html?id=${article.id}" class="news-card-link">
                <img src="${article.image}" alt="${article.title}">
                <div class="news-card-content">
                    <span class="category">${article.categoryName}</span>
                    <h3>${article.title}</h3>
                    <p>${article.summary}</p>
                    <div class="news-meta">
                        <span>${article.authorName}</span>
                        <span>${new Date(article.publishedDate).toLocaleDateString()}</span>
                    </div>
                </div>
            </a>
        </div>
    `).join('');
}