document.addEventListener('DOMContentLoaded', () => {
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
});
