document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const articleId = urlParams.get('id');

    // Fetch related articles based on current ID
    fetch(`/Secure_DigitalNewsstand/api/articles/related?id=${articleId}`)
        .then(res => res.json())
        .then(data => {
            const relatedGrid = document.getElementById('relatedArticles');
            relatedGrid.innerHTML = data.map(article => `
                <div class="related-article" onclick="location.href='article.html?id=${article.id}'">
                    <img src="${article.image}" alt="${article.title}">
                    <div class="related-article-content">
                        <h4>${article.title}</h4>
                        <span>${new Date(article.date).toLocaleDateString()}</span>
                    </div>
                </div>
            `).join('');
        });

    // Handle Persistent Save Logic
    const saveBtn = document.querySelector('.save-btn');
    saveBtn.addEventListener('click', function() {
        fetch('/Secure_DigitalNewsstand/api/favorites/articles', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ articleId: articleId })
        })
        .then(res => {
            if (res.ok) this.classList.toggle('saved');
        });
    });
});