document.addEventListener('DOMContentLoaded', () => {
    const utils = window.PostUtils || {};
    const openModal = utils.openModal || (() => {});

    document.addEventListener('click', (event) => {
        const actionEl = event.target.closest('[data-action="open-review-modal"]');
        if (!actionEl) return;

        const assignmentId = actionEl.dataset.assignmentId || '';
        const filter = document.getElementById('submissionFilter');
        if (filter && assignmentId) {
            filter.value = assignmentId;
            filter.dispatchEvent(new Event('change'));
        }
        openModal('submissionReviewModal');
    });

    document.addEventListener('click', (event) => {
        const actionEl = event.target.closest('[data-action="open-student-result"]');
        if (!actionEl) return;

        const assignmentId = actionEl.dataset.assignmentId || '';
        const filter = document.getElementById('studentResultFilter');
        if (filter && assignmentId) {
            filter.value = assignmentId;
            filter.dispatchEvent(new Event('change'));
        }
        openModal('studentResultModal');
    });
});
