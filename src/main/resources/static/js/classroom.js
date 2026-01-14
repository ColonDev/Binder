document.addEventListener('DOMContentLoaded', () => {
    const main = document.getElementById('main');
    const side = document.getElementById('manageSide');
    const fileState = new Map();

    const formatBytes = (bytes) => {
        if (!Number.isFinite(bytes)) return '';
        const units = ['B', 'KB', 'MB', 'GB'];
        let size = bytes;
        let unit = 0;
        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit += 1;
        }
        const precision = unit === 0 ? 0 : size < 10 ? 1 : 0;
        return `${size.toFixed(precision)} ${units[unit]}`;
    };

    const clearPreview = (preview) => {
        preview.querySelectorAll('[data-object-url]').forEach((item) => {
            URL.revokeObjectURL(item.dataset.objectUrl);
            delete item.dataset.objectUrl;
        });
        preview.innerHTML = '';
        const empty = document.createElement('div');
        empty.className = 'file-preview-empty';
        empty.textContent = 'No file selected.';
        preview.appendChild(empty);
    };

    const renderPreview = (preview, files) => {
        preview.querySelectorAll('[data-object-url]').forEach((item) => {
            URL.revokeObjectURL(item.dataset.objectUrl);
            delete item.dataset.objectUrl;
        });
        preview.innerHTML = '';

        files.forEach((file, index) => {
            const item = document.createElement('div');
            item.className = 'file-preview-item';

            if (file.type && file.type.startsWith('image/')) {
                const img = document.createElement('img');
                const url = URL.createObjectURL(file);
                img.src = url;
                img.alt = file.name || 'Selected image';
                img.className = 'file-preview-image';
                item.appendChild(img);
                item.dataset.objectUrl = url;
            } else {
                const fileBadge = document.createElement('div');
                fileBadge.className = 'file-preview-file';
                fileBadge.textContent = file.name || 'Selected file';
                item.appendChild(fileBadge);
            }

            const meta = document.createElement('div');
            meta.className = 'file-preview-meta';
            const size = formatBytes(file.size);
            meta.textContent = size ? `${file.name} • ${size}` : file.name;
            item.appendChild(meta);

            const removeBtn = document.createElement('button');
            removeBtn.type = 'button';
            removeBtn.className = 'file-preview-remove';
            removeBtn.dataset.action = 'remove-file-preview';
            removeBtn.dataset.index = String(index);
            removeBtn.setAttribute('aria-label', 'Remove file');
            removeBtn.textContent = '×';
            item.appendChild(removeBtn);

            preview.appendChild(item);
        });
    };

    const setupFilePreview = (input) => {
        const targetId = input.dataset.previewTarget;
        if (!targetId) return;
        const preview = document.getElementById(targetId);
        if (!preview) return;
        const syncFiles = (files) => {
            const dt = new DataTransfer();
            files.forEach((file) => dt.items.add(file));
            input.files = dt.files;
        };
        const update = () => {
            const files = input.files ? Array.from(input.files) : [];
            fileState.set(input, files);
            if (!files.length) {
                clearPreview(preview);
                return;
            }
            renderPreview(preview, files);
        };
        preview.addEventListener('click', (event) => {
            const removeBtn = event.target.closest('[data-action="remove-file-preview"]');
            if (!removeBtn) return;
            const index = Number(removeBtn.dataset.index);
            const files = fileState.get(input) || [];
            if (!Number.isInteger(index) || index < 0 || index >= files.length) return;
            const next = files.slice();
            next.splice(index, 1);
            fileState.set(input, next);
            syncFiles(next);
            if (!next.length) {
                clearPreview(preview);
            } else {
                renderPreview(preview, next);
            }
        });
        input.addEventListener('change', update);
        clearPreview(preview);
    };

    const setModalVisibility = (id, isOpen) => {
        const el = document.getElementById(id);
        if (!el) return;
        el.setAttribute('aria-hidden', isOpen ? 'false' : 'true');
    };

    const openModal = (id) => {
        setModalVisibility(id, true);
        const el = document.getElementById(id);
        if (!el) return;
        el.querySelectorAll('input[type="file"][data-preview-target]').forEach((input) => {
            input.value = '';
            fileState.delete(input);
            const targetId = input.dataset.previewTarget;
            const preview = targetId ? document.getElementById(targetId) : null;
            if (preview) clearPreview(preview);
        });
    };
    const closeModal = (id) => setModalVisibility(id, false);

    const setManageOpen = (isOpen) => {
        if (!main || !side) return;
        main.classList.toggle('manage-open', isOpen);
        side.setAttribute('aria-hidden', isOpen ? 'false' : 'true');
        localStorage.setItem('classroomManageOpen', isOpen ? '1' : '0');
    };

    const toggleManage = () => {
        if (!main || !side) return;
        setManageOpen(!main.classList.contains('manage-open'));
    };

    if (main && side && localStorage.getItem('classroomManageOpen') === '1') {
        setManageOpen(true);
    }

    document.addEventListener('click', (event) => {
        const actionEl = event.target.closest('[data-action]');
        if (!actionEl) return;

        const action = actionEl.dataset.action;
        if (action === 'toggle-manage') {
            toggleManage();
            return;
        }

        if (action === 'open-modal') {
            openModal(actionEl.dataset.modalId);
            return;
        }

        if (action === 'close-modal') {
            closeModal(actionEl.dataset.modalId);
            return;
        }

        if (action === 'open-submit-modal') {
            const id = actionEl.dataset.assignmentId;
            const field = document.getElementById('smAssignmentId');
            if (field) field.value = id || '';

            const removeInput = document.getElementById('smRemoveAttachment');
            if (removeInput) removeInput.value = 'false';

            const postEl = document.querySelector(`[data-post-id="${id}"]`);
            const titleEl = document.getElementById('smPostTitle');
            const descEl = document.getElementById('smPostDesc');
            const metaEl = document.getElementById('smPostMeta');
            if (postEl) {
                const postTitle = postEl.querySelector('.post-note-title')?.textContent?.trim() || '';
                const postDesc = postEl.querySelector('.post-note-body')?.textContent?.trim() || '';
                const postMeta = postEl.querySelector('.post-note-meta')?.textContent?.trim() || '';
                if (titleEl) titleEl.textContent = postTitle;
                if (descEl) descEl.textContent = postDesc;
                if (metaEl) metaEl.textContent = postMeta;
            }

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
            return;
        }

        if (action === 'open-review-modal') {
            const assignmentId = actionEl.dataset.assignmentId || '';
            const filter = document.getElementById('submissionFilter');
            if (filter && assignmentId) {
                filter.value = assignmentId;
                filter.dispatchEvent(new Event('change'));
            }
            openModal('submissionReviewModal');
            return;
        }

        if (action === 'open-student-result') {
            const assignmentId = actionEl.dataset.assignmentId || '';
            const filter = document.getElementById('studentResultFilter');
            if (filter && assignmentId) {
                filter.value = assignmentId;
                filter.dispatchEvent(new Event('change'));
            }
            openModal('studentResultModal');
            return;
        }

        if (action === 'remove-submission-attachment') {
            const existingSection = document.getElementById('smExistingAttachmentSection');
            const existingPreview = document.getElementById('smExistingAttachment');
            const removeInput = document.getElementById('smRemoveAttachment');
            if (existingPreview) existingPreview.innerHTML = '';
            if (existingSection) existingSection.style.display = 'none';
            if (removeInput) removeInput.value = 'true';
            return;
        }

        if (action === 'open-edit-post') {
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
                    if (due && due !== 'null') {
                        const d = new Date(due);
                        const pad = (n) => String(n).padStart(2, '0');
                        dueLocal.value =
                            d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' +
                            pad(d.getHours()) + ':' + pad(d.getMinutes());
                        dueHidden.value = d.toISOString();
                    } else {
                        dueLocal.value = '';
                        dueHidden.value = '';
                    }
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
        }

        if (action === 'remove-existing-attachment') {
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
        }

    });

    // Create post type switch
    const typeSel = document.getElementById('pcType');
    const assignmentForm = document.getElementById('assignmentForm');
    const resourceForm = document.getElementById('resourceForm');
    typeSel?.addEventListener('change', () => {
        const isAssignment = typeSel.value === 'ASSIGNMENT';
        if (assignmentForm) assignmentForm.style.display = isAssignment ? 'block' : 'none';
        if (resourceForm) resourceForm.style.display = isAssignment ? 'none' : 'block';
    });

    // Convert datetime-local to ISO string on create assignment
    assignmentForm?.addEventListener('submit', () => {
        const local = document.getElementById('dueLocal')?.value || '';
        const dueHidden = document.getElementById('dueHidden');
        if (dueHidden) dueHidden.value = local ? new Date(local).toISOString() : '';
    });

    // Keep edit due hidden in sync
    document.getElementById('peDueLocal')?.addEventListener('change', () => {
        const v = document.getElementById('peDueLocal')?.value || '';
        const dueHidden = document.getElementById('peDueHidden');
        if (dueHidden) dueHidden.value = v ? new Date(v).toISOString() : '';
    });

    // If teacher selects a replacement file for a resource, auto-check replaceAttachments
    document.getElementById('peResourceFile')?.addEventListener('change', (event) => {
        const file = event.target.files && event.target.files.length > 0;
        const replaceCheck = document.getElementById('peReplaceAtts');
        if (file && replaceCheck) replaceCheck.checked = true;
    });

    document.querySelectorAll('input[type="file"][data-preview-target]').forEach(setupFilePreview);

    // Filter posts
    const filter = document.getElementById('postFilter');
    filter?.addEventListener('change', () => {
        const v = filter.value;
        document.querySelectorAll('[data-type]').forEach((el) => {
            const t = el.getAttribute('data-type');
            el.style.display = (v === 'ALL' || v === t) ? '' : 'none';
        });
    });

    const setupCarousel = (options) => {
        const {
            filterId,
            prevId,
            nextId,
            statusId,
            cardSelector,
            emptyId,
            dueData
        } = options;
        const filter = document.getElementById(filterId);
        const prev = document.getElementById(prevId);
        const next = document.getElementById(nextId);
        const status = document.getElementById(statusId);
        const cards = Array.from(document.querySelectorAll(cardSelector));
        const cardMap = new Map();
        cards.forEach((card) => {
            const assignmentId = card.getAttribute('data-assignment-id') || '';
            if (!cardMap.has(assignmentId)) cardMap.set(assignmentId, []);
            cardMap.get(assignmentId).push(card);
        });
        const assignmentDueMap = new Map();
        if (filter && dueData) {
            Array.from(filter.options).forEach((opt) => {
                const due = opt.dataset.due || '';
                if (opt.value) assignmentDueMap.set(opt.value, due);
            });
        }
        const state = {
            assignmentId: filter?.value || '',
            index: 0
        };
        const update = () => {
            const visible = cardMap.get(state.assignmentId) || [];
            cards.forEach((card) => {
                card.classList.remove('is-active', 'is-overdue');
            });
            if (visible.length > 0) {
                const safeIndex = Math.max(0, Math.min(state.index, visible.length - 1));
                state.index = safeIndex;
                visible[safeIndex].classList.add('is-active');
                if (dueData) {
                    const dueRaw = assignmentDueMap.get(state.assignmentId) || '';
                    const submittedRaw = visible[safeIndex].getAttribute('data-submitted-at') || '';
                    if (dueRaw && submittedRaw) {
                        const dueDate = new Date(dueRaw);
                        const submittedDate = new Date(submittedRaw);
                        if (!Number.isNaN(dueDate.getTime()) && !Number.isNaN(submittedDate.getTime())) {
                            if (submittedDate.getTime() > dueDate.getTime()) {
                                visible[safeIndex].classList.add('is-overdue');
                            }
                        }
                    }
                }
            }
            if (status) {
                status.textContent = visible.length ? `${state.index + 1} / ${visible.length}` : '0 / 0';
            }
            if (prev) prev.disabled = state.index <= 0;
            if (next) next.disabled = visible.length === 0 || state.index >= visible.length - 1;
            const empty = document.getElementById(emptyId);
            if (empty) empty.style.display = visible.length === 0 ? '' : 'none';
        };
        if (filter) {
            if (!filter.value && filter.options.length) {
                filter.value = filter.options[0].value;
            }
            state.assignmentId = filter.value;
            filter.addEventListener('change', () => {
                state.assignmentId = filter.value;
                state.index = 0;
                update();
            });
        }
        prev?.addEventListener('click', () => {
            state.index -= 1;
            update();
        });
        next?.addEventListener('click', () => {
            state.index += 1;
            update();
        });
        if (cards.length) {
            update();
        }
    };

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

    // Switch enrollment list
    const enrollmentFilter = document.getElementById('enrollmentFilter');
    const setEnrollmentView = (value) => {
        document.querySelectorAll('[data-list]').forEach((section) => {
            section.style.display = section.dataset.list === value ? '' : 'none';
        });
    };
    if (enrollmentFilter) {
        setEnrollmentView(enrollmentFilter.value);
        enrollmentFilter.addEventListener('change', () => {
            setEnrollmentView(enrollmentFilter.value);
        });
    }

    // Close on Escape
    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        ['postCreateModal', 'postEditModal', 'submitModal', 'submissionReviewModal', 'studentResultModal', 'classroomEditModal']
            .forEach((id) => closeModal(id));
        if (main && main.classList.contains('manage-open')) setManageOpen(false);
    });
});
