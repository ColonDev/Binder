document.addEventListener('DOMContentLoaded', () => {
    const utils = window.PostUtils || {};
    const openModal = utils.openModal || (() => {});
    const closeModal = utils.closeModal || (() => {});

    document.addEventListener('click', (event) => {
        const actionEl = event.target.closest('[data-action]');
        if (!actionEl) return;

        const action = actionEl.dataset.action;
        if (action === 'open-modal') {
            openModal(actionEl.dataset.modalId);
            return;
        }

        if (action === 'close-modal') {
            closeModal(actionEl.dataset.modalId);
            return;
        }
    });

    // Close on Escape
    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        ['postCreateModal', 'postEditModal', 'submitModal', 'submissionReviewModal', 'studentResultModal', 'classroomEditModal']
            .forEach((id) => closeModal(id));
    });
});
