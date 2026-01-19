document.addEventListener('DOMContentLoaded', () => {
    const utils = window.PostUtils || {};
    const openModal = utils.openModal || (() => {});

    document.addEventListener('click', (event) => {
        const actionEl = event.target.closest('[data-action="open-submit-modal"]');
        if (!actionEl) return;

        const id = actionEl.dataset.assignmentId;
        const field = document.getElementById('smAssignmentId');
        if (field) field.value = id || '';

        const removeInput = document.getElementById('smRemoveAttachment');
        if (removeInput) removeInput.value = 'false';

        const postEl = document.querySelector(`[data-post-id="${id}-missing"]`);
        const titleEl = document.getElementById('smPostTitle');
        const descEl = document.getElementById('smPostDesc');
        const metaEl = document.getElementById('smPostMeta');
        const postTitle = postEl?.querySelector('.post-note-title')?.textContent?.trim() || '';
        const postDesc = postEl?.querySelector('.post-note-body')?.textContent?.trim() || '';
        const postMeta = postEl?.querySelector('.post-note-meta')?.textContent?.trim() || '';
        if (titleEl) titleEl.textContent = postTitle;
        if (descEl) descEl.textContent = postDesc;
        if (metaEl) metaEl.textContent = postMeta;

        const existingSection = document.getElementById('smExistingAttachmentSection');
        const existingPreview = document.getElementById('smExistingAttachment');
        if (existingSection && existingPreview) {
            existingPreview.innerHTML = '';
            existingSection.style.display = 'none';
            const dataContainer = document.getElementById('studentSubmissionData');
            const dataEl = dataContainer ? dataContainer.querySelector(`[data-assignment-id="${id}"]`) : null;
            const attachmentId = dataEl?.getAttribute('data-attachment-id');
            const attachmentName = dataEl?.getAttribute('data-attachment-name') || '';
            const attachmentImage = dataEl?.getAttribute('data-attachment-image') === 'true';
            const attachmentPdf = dataEl?.getAttribute('data-attachment-pdf') === 'true';
            if (attachmentId) {
                const item = document.createElement('div');
                item.className = 'file-preview-item';
                item.dataset.attachmentId = attachmentId;

                if (attachmentImage) {
                    const img = document.createElement('img');
                    img.src = `/attachments/${attachmentId}/inline`;
                    img.alt = attachmentName || 'Existing image';
                    img.className = 'file-preview-image';
                    item.appendChild(img);
                } else {
                    const fileBadge = document.createElement('div');
                    fileBadge.className = 'file-preview-file';
                    fileBadge.textContent = attachmentPdf ? 'PDF' : 'File';
                    item.appendChild(fileBadge);
                }

                const meta = document.createElement('div');
                meta.className = 'file-preview-meta';
                meta.textContent = attachmentName || 'Attachment';
                item.appendChild(meta);

                const removeBtn = document.createElement('button');
                removeBtn.type = 'button';
                removeBtn.className = 'file-preview-remove';
                removeBtn.dataset.action = 'remove-submission-attachment';
                removeBtn.setAttribute('aria-label', 'Remove attachment');
                removeBtn.textContent = '×';
                item.appendChild(removeBtn);

                existingPreview.appendChild(item);
                existingSection.style.display = 'block';
            }
        }

        openModal('submitModal');
    });

    document.addEventListener('click', (event) => {
        const actionEl = event.target.closest('[data-action="remove-submission-attachment"]');
        if (!actionEl) return;

        const existingSection = document.getElementById('smExistingAttachmentSection');
        const existingPreview = document.getElementById('smExistingAttachment');
        const removeInput = document.getElementById('smRemoveAttachment');
        if (existingPreview) existingPreview.innerHTML = '';
        if (existingSection) existingSection.style.display = 'none';
        if (removeInput) removeInput.value = 'true';
    });
});
