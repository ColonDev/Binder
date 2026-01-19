document.addEventListener('DOMContentLoaded', () => {
    const main = document.getElementById('main');
    const side = document.getElementById('manageSide');

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

        if (actionEl.dataset.action === 'toggle-manage') {
            toggleManage();
        }
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

    // Close manage side on Escape
    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        if (main && main.classList.contains('manage-open')) setManageOpen(false);
    });
});
