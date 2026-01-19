document.addEventListener('DOMContentLoaded', () => {
    const utils = window.PostUtils || {};
    const setupCarousel = utils.setupCarousel || (() => {});

    setupCarousel({
        filterId: 'submissionFilter',
        prevId: 'submissionPrev',
        nextId: 'submissionNext',
        statusId: 'submissionNavStatus',
        cardSelector: '[data-submission-card]',
        emptyId: 'submissionReviewEmpty',
        dueData: true
    });

    setupCarousel({
        filterId: 'studentResultFilter',
        prevId: 'studentResultPrev',
        nextId: 'studentResultNext',
        statusId: 'studentResultStatus',
        cardSelector: '[data-student-result-card]',
        emptyId: 'studentResultEmpty',
        dueData: false
    });
});
