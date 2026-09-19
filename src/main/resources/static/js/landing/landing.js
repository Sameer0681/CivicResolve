/**
 * CivicResolve - Landing Page Interactive Scripts
 * Architected by Prince Gupt (Frontend Lead)
 * Handles:
 * - 14 Sectors showcase live search & category filtering
 * - Interactive Accordion FAQ view with search filter
 * - Hero animated counters & quick tracker redirection
 */

document.addEventListener('DOMContentLoaded', () => {
    initHeroCounters();
    initSectorFilter();
    initFaqAccordion();
    initHeroTracker();
});

/**
 * 1. Animated Numbers Counter for Hero Statistics
 */
function initHeroCounters() {
    const statElements = document.querySelectorAll('[data-counter-target]');
    if (!statElements || statElements.length === 0) return;

    const observer = new IntersectionObserver((entries, obs) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const el = entry.target;
                const target = parseFloat(el.getAttribute('data-counter-target'));
                const suffix = el.getAttribute('data-counter-suffix') || '';
                const prefix = el.getAttribute('data-counter-prefix') || '';
                const duration = 1200; // ms
                const start = 0;
                const startTime = performance.now();

                function update(now) {
                    const elapsed = now - startTime;
                    const progress = Math.min(elapsed / duration, 1);
                    // Ease out expo
                    const ease = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
                    const current = Math.floor(start + (target - start) * ease);
                    el.textContent = `${prefix}${current.toLocaleString()}${suffix}`;
                    if (progress < 1) {
                        requestAnimationFrame(update);
                    } else {
                        el.textContent = `${prefix}${target.toLocaleString()}${suffix}`;
                    }
                }

                requestAnimationFrame(update);
                obs.unobserve(el);
            }
        });
    }, { threshold: 0.2 });

    statElements.forEach(el => observer.observe(el));
}

/**
 * 2. 14 Sectors Live Search & Category Filtering
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
 * 3. Interactive Accordion FAQ View with Search
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

/**
 * 4. Hero Tracker Form Redirect
 */
function initHeroTracker() {
    const form = document.getElementById('heroTrackerForm');
    const input = document.getElementById('heroTrackerInput');

    if (form && input) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            const id = input.value.trim();
            if (!id) {
                input.focus();
                return;
            }
            window.location.href = `/citizen/complaint-details?id=${encodeURIComponent(id)}`;
        });
    }
}
