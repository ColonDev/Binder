document.addEventListener('DOMContentLoaded', () => {
    const utils = window.PostUtils || {};
    const openModal = utils.openModal || (() => {});

    document.addEventListener('click', (event) => {
        const actionEl = event.target.closest('[data-action="open-edit-post"]');
        if (!actionEl) return;

        const postId = actionEl.dataset.postId;
        const postType = actionEl.dataset.postType;
        const title = actionEl.dataset.postTitle || '';
        const desc = actionEl.dataset.postDesc || '';
        const ttc = actionEl.dataset.postTtc || '';
        const due = actionEl.dataset.postDue || '';
        const max = actionEl.dataset.postMax || '';

        const postIdField = document.getElementById('pePostId');
        const titleField = document.getElementById('peTitle');
        const descField = document.getElementById('peDesc');
        if (postIdField) postIdField.value = postId || '';
        if (titleField) titleField.value = title;
        if (descField) descField.value = desc;

        const isResource = postType === 'RESOURCE';
        const assignmentFields = document.getElementById('peAssignmentFields');
        const resourceFields = document.getElementById('peResourceFields');
        if (assignmentFields) assignmentFields.style.display = isResource ? 'none' : 'block';
        if (resourceFields) resourceFields.style.display = isResource ? 'block' : 'none';

        if (isResource) {
            const fileInput = document.getElementById('peResourceFile');
            const replaceCheck = document.getElementById('peReplaceAtts');
            if (fileInput) fileInput.value = '';
            if (replaceCheck) replaceCheck.checked = false;
        }

        if (!isResource) {
            const ttcField = document.getElementById('peTtc');
            if (ttcField) ttcField.value = ttc;

            const dueLocal = document.getElementById('peDueLocal');
            const dueHidden = document.getElementById('peDueHidden');
            if (dueLocal && dueHidden) {
                const d = new Date(due);
                const pad = (n) => String(n).padStart(2, '0');
                dueLocal.value =
                    d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' +
                    pad(d.getHours()) + ':' + pad(d.getMinutes());
                dueHidden.value = d.toISOString();
            }

            const maxField = document.getElementById('peMax');
            if (maxField) maxField.value = (max && max !== 'null') ? max : '';
        }

        const form = document.getElementById('postEditForm');
        if (form) {
            form.action = isResource ? '/classroom/post/resource/edit' : '/classroom/post/assignment/edit';
        }

        const existingSection = document.getElementById('peExistingAttachmentsSection');
        const existingPreview = document.getElementById('peExistingAttachments');
        if (existingSection && existingPreview) {
            existingPreview.innerHTML = '';
            existingSection.style.display = 'none';
            form?.querySelectorAll('input[name="removeAttachmentIds"]').forEach((input) => input.remove());

            const postEl = actionEl.closest('.post-note');
            const attachmentEls = postEl ? Array.from(postEl.querySelectorAll('[data-attachment-id]')) : [];
            const attachments = attachmentEls.map((el) => ({
                id: el.dataset.attachmentId,
                name: el.dataset.attachmentName,
                isImage: el.dataset.attachmentImage === 'true',
                inlineUrl: el.dataset.attachmentInline,
                downloadUrl: el.dataset.attachmentDownload
            })).filter((att) => att.id);

            if (attachments.length) {
                existingSection.style.display = 'block';
                attachments.forEach((att) => {
                    const item = document.createElement('div');
                    item.className = 'file-preview-item';
                    item.dataset.attachmentId = att.id;

                    if (att.isImage && att.inlineUrl) {
                        const img = document.createElement('img');
                        img.src = att.inlineUrl;
                        img.alt = att.name || 'Existing image';
                        img.className = 'file-preview-image';
                        item.appendChild(img);
                    } else {
                        const fileBadge = document.createElement('div');
                        fileBadge.className = 'file-preview-file';
                        fileBadge.textContent = 'File';
                        item.appendChild(fileBadge);
                    }

                    const meta = document.createElement('div');
                    meta.className = 'file-preview-meta';
                    meta.textContent = att.name || 'Attachment';
                    item.appendChild(meta);

                    const removeBtn = document.createElement('button');
                    removeBtn.type = 'button';
                    removeBtn.className = 'file-preview-remove';
                    removeBtn.dataset.action = 'remove-existing-attachment';
                    removeBtn.dataset.attachmentId = att.id;
                    removeBtn.setAttribute('aria-label', 'Remove attachment');
                    removeBtn.textContent = '×';
                    item.appendChild(removeBtn);

                    existingPreview.appendChild(item);
                });
            }
        }

        openModal('postEditModal');
    });

    document.addEventListener('click', (event) => {
        const actionEl = event.target.closest('[data-action="remove-existing-attachment"]');
        if (!actionEl) return;

        const attachmentId = actionEl.dataset.attachmentId;
        const form = document.getElementById('postEditForm');
        if (!attachmentId || !form) return;
        const existing = form.querySelector(`input[name="removeAttachmentIds"][value="${attachmentId}"]`);
        if (!existing) {
            const hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.name = 'removeAttachmentIds';
            hidden.value = attachmentId;
            form.appendChild(hidden);
        }
        const item = actionEl.closest('.file-preview-item');
        const list = document.getElementById('peExistingAttachments');
        if (item) item.remove();
        if (list && list.children.length === 0) {
            const section = document.getElementById('peExistingAttachmentsSection');
            if (section) section.style.display = 'none';
        }
    });
});
