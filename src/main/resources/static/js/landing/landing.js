/**
 * CivicResolve - Landing Page Interactive Scripts
 * Architected by Prince Gupt (Frontend Lead)
 * Handles:
 * - 14 Sectors showcase live search & category filtering
 * - Interactive Accordion FAQ view with search filter
 */

document.addEventListener('DOMContentLoaded', () => {
    initSectorFilter();
    initFaqAccordion();
});

/**
 * 1. Sector Search & Category Filtering
 */
function initSectorFilter() {
    const searchInput = document.getElementById('sectorSearchInput');
    const filterButtons = document.querySelectorAll('.cr-sector-filter-btn');
    const sectorCards = document.querySelectorAll('.sector-showcase-item');
    const noResultsMsg = document.getElementById('sectorNoResults');

    let currentCategory = 'ALL';
    let currentSearchQuery = '';

    function applyFilter() {
        let visibleCount = 0;

        sectorCards.forEach(card => {
            const cardCategory = card.getAttribute('data-category') || 'ALL';
            const cardKeywords = (card.getAttribute('data-keywords') || '').toLowerCase();
            const cardTitle = (card.querySelector('.sector-title')?.textContent || '').toLowerCase();
            const cardDesc = (card.querySelector('.sector-desc')?.textContent || '').toLowerCase();
            const combinedText = `${cardTitle} ${cardDesc} ${cardKeywords}`;

            const matchesCategory = (currentCategory === 'ALL' || cardCategory === currentCategory);
            const matchesSearch = (!currentSearchQuery || combinedText.includes(currentSearchQuery));

            if (matchesCategory && matchesSearch) {
                card.style.display = 'block';
                visibleCount++;
            } else {
                card.style.display = 'none';
            }
        });

        if (noResultsMsg) {
            noResultsMsg.style.display = visibleCount === 0 ? 'block' : 'none';
        }
    }

    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            currentSearchQuery = e.target.value.trim().toLowerCase();
            applyFilter();
        });
    }

    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            filterButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentCategory = btn.getAttribute('data-filter') || 'ALL';
            applyFilter();
        });
    });
}

/**
 * 2. Interactive FAQ Accordion with Search
 */
function initFaqAccordion() {
    const faqItems = document.querySelectorAll('.cr-faq-item');
    const faqSearch = document.getElementById('faqSearchInput');

    faqItems.forEach(item => {
        const question = item.querySelector('.cr-faq-question');
        if (!question) return;

        question.addEventListener('click', () => {
            const isOpen = item.classList.contains('active');
            // Close other items
            faqItems.forEach(other => {
                if (other !== item) other.classList.remove('active');
            });
            // Toggle current
            item.classList.toggle('active', !isOpen);
        });
    });

    if (faqSearch) {
        faqSearch.addEventListener('input', (e) => {
            const query = e.target.value.trim().toLowerCase();
            let visibleCount = 0;

            faqItems.forEach(item => {
                const questionText = (item.querySelector('.cr-faq-question')?.textContent || '').toLowerCase();
                const answerText = (item.querySelector('.cr-faq-answer')?.textContent || '').toLowerCase();
                const matches = questionText.includes(query) || answerText.includes(query);

                if (matches) {
                    item.style.display = 'block';
                    visibleCount++;
                    if (query.length > 2) {
                        item.classList.add('active'); // auto-expand on specific match
                    }
                } else {
                    item.style.display = 'none';
                    item.classList.remove('active');
                }
            });

            const noFaq = document.getElementById('faqNoResults');
            if (noFaq) noFaq.style.display = (visibleCount === 0) ? 'block' : 'none';
        });
    }
}
