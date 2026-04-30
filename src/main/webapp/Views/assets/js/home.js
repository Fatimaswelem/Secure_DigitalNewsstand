document.addEventListener('DOMContentLoaded', function() {
    // Fetch articles from the Java Backend
    fetch('/Secure_DigitalNewsstand/api/articles')
        .then(response => response.json())
        .then(data => {
            displayArticles(data);
            displayForYouArticles(data);
        })
        .catch(err => console.error('Failed to load articles:', err));
});

function createArticleCard(article) {
    // Updated links to point to the secure Java-hosted view
    const articleLink = `/Secure_DigitalNewsstand/Views/Client/article.html?id=${article.id}`;
    return `
        <a href="${articleLink}"><article class="article-card">
            <img src="${article.image}" alt="${article.title}">
            <div class="article-content">
                <h3>${article.title}</h3>
                <p>${article.summary}</p>
                <div class="article-meta">
                    <span>${article.author}</span>
                    <span>${new Date(article.date).toLocaleDateString()}</span>
                </div>
            </div>
        </article></a>
    `;
}