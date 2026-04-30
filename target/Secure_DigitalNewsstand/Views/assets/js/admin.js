// Add Article
addArticleForm.addEventListener('submit', function(e) {
    e.preventDefault();
    const formData = {
        title: document.getElementById('articleTitle').value,
        summary: document.getElementById('articleSummary').value,
        category: document.getElementById('articleCategory').value,
        language: document.getElementById('articleLanguage').value
    };

    fetch('/Secure_DigitalNewsstand/api/admin/articles', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
    })
    .then(res => res.ok ? alert('Article added successfully!') : alert('Failed to add article.'));
});

// Remove Article
removeArticleForm.addEventListener('submit', function(e) {
    e.preventDefault();
    const id = document.getElementById('articleIdToRemove').value;

    fetch(`/Secure_DigitalNewsstand/api/admin/articles?id=${id}`, {
        method: 'DELETE'
    })
    .then(res => res.ok ? alert('Deleted!') : alert('Error deleting article.'));
});