document.addEventListener('DOMContentLoaded', () => {
    // Filter posts
    const filter = document.getElementById('postFilter');
    filter?.addEventListener('change', () => {
        const v = filter.value;
        document.querySelectorAll('[data-type]').forEach((el) => {
            const t = el.getAttribute('data-type');
            el.style.display = (v === 'ALL' || v === t) ? '' : 'none';
        });
    });
});
