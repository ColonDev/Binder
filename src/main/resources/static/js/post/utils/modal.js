window.PostModal = (() => {
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
            if (!preview) return;
            preview.querySelectorAll('[data-object-url]').forEach((item) => {
                URL.revokeObjectURL(item.dataset.objectUrl);
                delete item.dataset.objectUrl;
            });
            preview.innerHTML = '';
            const empty = document.createElement('div');
            empty.className = 'file-preview-empty';
            empty.textContent = 'No file selected.';
            preview.appendChild(empty);
        });
    };

    const closeModal = (id) => setModalVisibility(id, false);

    return { openModal, closeModal };
})();
