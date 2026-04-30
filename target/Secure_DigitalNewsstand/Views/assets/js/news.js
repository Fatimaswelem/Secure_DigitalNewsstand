document.addEventListener('DOMContentLoaded', function() {
    const gameLinks = document.querySelectorAll('.games-sidebar .game-link');
    gameLinks.forEach(link => {
        link.addEventListener('click', (event) => {
            event.preventDefault();
            const gameId = link.dataset.gameId;
            // Redirect to the secure Java Game Controller
            window.location.href = `/Secure_DigitalNewsstand/client/games?id=${gameId}`;
        });
    });

    // Load personalized news feed
    fetch('/Secure_DigitalNewsstand/api/news/personalized')
        .then(res => res.json())
        .then(data => {
            // Logic to populate .news-grid-foryou
        });
});