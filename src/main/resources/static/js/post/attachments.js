document.addEventListener('DOMContentLoaded', () => {
    const utils = window.PostUtils || {};
    const setupFilePreview = utils.setupFilePreview || (() => {});

    document.querySelectorAll('input[type="file"][data-preview-target]').forEach(setupFilePreview);

    // If teacher selects a replacement file for a resource, auto-check replaceAttachments
    document.getElementById('peResourceFile')?.addEventListener('change', (event) => {
        const file = event.target.files && event.target.files.length > 0;
        const replaceCheck = document.getElementById('peReplaceAtts');
        if (file && replaceCheck) replaceCheck.checked = true;
    });
});
