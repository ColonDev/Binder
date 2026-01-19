window.PostCarousel = (() => {
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

    return { setupCarousel };
})();
