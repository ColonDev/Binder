document.addEventListener('DOMContentLoaded', () => {
    const main = document.getElementById('main');
    const side = document.getElementById('manageSide');

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
        if (preview.dataset.objectUrl) {
            URL.revokeObjectURL(preview.dataset.objectUrl);
            delete preview.dataset.objectUrl;
        }
        preview.innerHTML = '';

        files.forEach((file) => {
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

            preview.appendChild(item);
        });
    };

    const setupFilePreview = (input) => {
        const targetId = input.dataset.previewTarget;
        if (!targetId) return;
        const preview = document.getElementById(targetId);
        if (!preview) return;
        const update = () => {
            const files = input.files ? Array.from(input.files) : [];
            if (!files.length) {
                clearPreview(preview);
                return;
            }
            renderPreview(preview, files);
        };
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
            openModal('submitModal');
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

            openModal('postEditModal');
        }

        if (action === 'preview-attachment') {
            return;
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
        ['postCreateModal', 'postEditModal', 'submitModal', 'classroomEditModal'].forEach((id) => closeModal(id));
        if (main && main.classList.contains('manage-open')) setManageOpen(false);
    });
});
