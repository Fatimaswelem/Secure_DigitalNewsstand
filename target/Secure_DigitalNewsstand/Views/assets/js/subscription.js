document.addEventListener('DOMContentLoaded', function () {
  const selectButtons = document.querySelectorAll('.select-button');

  selectButtons.forEach(button => {
    button.addEventListener('click', () => {
      const plan = button.getAttribute('data-plan');
      const price = button.getAttribute('data-price');
      const planName = button.parentElement.querySelector('h3').textContent;

      // Store in localStorage for the checkout/pay page
      localStorage.setItem('selectedPlan', plan);
      localStorage.setItem('selectedPrice', price);
      localStorage.setItem('selectedPlanName', planName);

      // Redirect to Java-hosted HTML page
      window.location.href = 'pay.html'; 
    });

    // Feedback effects
    button.addEventListener('mousedown', () => button.style.transform = 'scale(0.98)');
    button.addEventListener('mouseup', () => button.style.transform = 'scale(1)');
  });
});