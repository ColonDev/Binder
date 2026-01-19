window.PostFilePreview = (() => {
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

    return { setupFilePreview };
})();
