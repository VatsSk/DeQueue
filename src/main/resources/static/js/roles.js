// Roles Management
let allRoles = [];
let allPermissions = [];
let selectedPermissionIds = [];

// Load roles and permissions on page load
document.addEventListener('DOMContentLoaded', async () => {
    await Promise.all([
        loadRoles(),
        loadPermissions()
    ]);
});

async function loadRoles() {
    try {
        const response = await api.get('/roles');
        if (response && response.success && response.data) {
            allRoles = response.data;
            renderRoles();
        } else {
            showToast('Failed to load roles', 'error');
        }
    } catch (error) {
        console.error('Failed to load roles', error);
        showToast(error.message || 'Failed to load roles', 'error');
        document.getElementById('roles-table-body').innerHTML = 
            '<tr><td colspan="5" class="text-center text-error">Failed to load roles</td></tr>';
    }
}

async function loadPermissions() {
    try {
        const response = await api.get('/platform/permissions');
        if (response && response.success && response.data) {
            allPermissions = response.data;
        }
    } catch (error) {
        console.error('Failed to load permissions', error);
    }
}

function renderRoles() {
    const tbody = document.getElementById('roles-table-body');
    if (!tbody) return;

    if (allRoles.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center text-muted" style="padding: 2rem;">
                    <i data-lucide="shield" style="width: 48px; height: 48px; opacity: 0.3; margin-bottom: 1rem;"></i>
                    <p>No roles yet. Create your first role to get started.</p>
                </td>
            </tr>
        `;
        if (window.lucide) lucide.createIcons();
        return;
    }

    let html = '';
    allRoles.forEach(role => {
        const permissionCount = role.permissionIds ? role.permissionIds.length : 0;
        const statusBadge = role.active 
            ? '<span class="badge badge-ready">Active</span>'
            : '<span class="badge badge-secondary">Inactive</span>';

        html += `
            <tr>
                <td><strong>${role.name}</strong></td>
                <td>${role.description || 'No description'}</td>
                <td>
                    <span class="role-badge">
                        <i data-lucide="shield" style="width: 14px; height: 14px;"></i>
                        ${permissionCount} permission${permissionCount !== 1 ? 's' : ''}
                    </span>
                </td>
                <td>${statusBadge}</td>
                <td>
                    <div class="flex gap-2">
                        <button class="btn btn-sm btn-secondary" onclick="editRole('${role.id}')" title="Edit role">
                            <i data-lucide="edit-2"></i>
                        </button>
                        <button class="btn btn-sm btn-error" onclick="deleteRole('${role.id}')" title="Delete role">
                            <i data-lucide="trash-2"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
    if (window.lucide) lucide.createIcons();
}

function showCreateRoleModal() {
    document.getElementById('modal-title').textContent = 'Create Role';
    document.getElementById('role-form').reset();
    document.getElementById('role-id').value = '';
    selectedPermissionIds = [];
    
    renderPermissionsCheckboxes();
    
    const modal = document.getElementById('role-modal');
    modal.classList.add('active');
    
    setTimeout(() => {
        document.getElementById('role-name').focus();
        if (window.lucide) lucide.createIcons();
    }, 100);
}

function renderPermissionsCheckboxes() {
    const container = document.getElementById('permissions-container');
    if (!container) return;

    if (allPermissions.length === 0) {
        container.innerHTML = `
            <div class="empty-permissions">
                <i data-lucide="alert-circle"></i>
                <p>No permissions available</p>
            </div>
        `;
        if (window.lucide) lucide.createIcons();
        return;
    }

    // Group permissions by resource
    const grouped = {};
    allPermissions.forEach(perm => {
        if (!grouped[perm.resource]) {
            grouped[perm.resource] = [];
        }
        grouped[perm.resource].push(perm);
    });

    let html = '';
    Object.keys(grouped).sort().forEach(resource => {
        grouped[resource].forEach(perm => {
            const isChecked = selectedPermissionIds.includes(perm.id);
            const selectedClass = isChecked ? 'selected' : '';
            
            html += `
                <div class="permission-card ${selectedClass}" onclick="togglePermission('${perm.id}')">
                    <label>
                        <input 
                            type="checkbox" 
                            value="${perm.id}" 
                            ${isChecked ? 'checked' : ''}
                            onchange="togglePermission('${perm.id}')"
                        >
                        <div class="permission-info">
                            <div class="permission-key">${perm.permissionKey}</div>
                            <div class="permission-desc">${perm.description || 'No description'}</div>
                        </div>
                    </label>
                </div>
            `;
        });
    });

    container.innerHTML = html;
}

function togglePermission(permissionId) {
    const index = selectedPermissionIds.indexOf(permissionId);
    if (index > -1) {
        selectedPermissionIds.splice(index, 1);
    } else {
        selectedPermissionIds.push(permissionId);
    }
    renderPermissionsCheckboxes();
}

async function editRole(roleId) {
    const role = allRoles.find(r => r.id === roleId);
    if (!role) return;

    document.getElementById('modal-title').textContent = 'Edit Role';
    document.getElementById('role-id').value = role.id;
    document.getElementById('role-name').value = role.name;
    document.getElementById('role-description').value = role.description || '';
    
    selectedPermissionIds = role.permissionIds || [];
    renderPermissionsCheckboxes();
    
    const modal = document.getElementById('role-modal');
    modal.classList.add('active');
    
    setTimeout(() => {
        document.getElementById('role-name').focus();
        if (window.lucide) lucide.createIcons();
    }, 100);
}

async function deleteRole(roleId) {
    const role = allRoles.find(r => r.id === roleId);
    if (!role) return;

    if (!confirm(`Are you sure you want to delete the role "${role.name}"?\n\nThis action cannot be undone.`)) {
        return;
    }

    try {
        const response = await api.delete(`/roles/${roleId}`);
        if (response.success) {
            showToast('Role deleted successfully', 'success');
            await loadRoles();
        } else {
            showToast(response.message || 'Failed to delete role', 'error');
        }
    } catch (error) {
        console.error('Failed to delete role', error);
        showToast(error.message || 'Failed to delete role', 'error');
    }
}

function closeRoleModal() {
    const modal = document.getElementById('role-modal');
    modal.classList.remove('active');
}

// Form submission handler
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('role-form');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const roleId = document.getElementById('role-id').value;
            const isEdit = !!roleId;
            
            // Validate at least one permission is selected
            if (selectedPermissionIds.length === 0) {
                showToast('Please select at least one permission', 'error');
                return;
            }
            
            const saveBtn = document.getElementById('save-role-btn');
            const originalHTML = saveBtn.innerHTML;
            saveBtn.disabled = true;
            saveBtn.innerHTML = '<i data-lucide="loader" class="spin"></i> Saving...';
            if (window.lucide) lucide.createIcons();

            const payload = {
                name: document.getElementById('role-name').value.trim(),
                description: document.getElementById('role-description').value.trim(),
                permissionIds: selectedPermissionIds,
                orderVisibility: {
                    statuses: ['PENDING', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED']
                }
            };

            try {
                let response;
                if (isEdit) {
                    response = await api.put(`/roles/${roleId}`, payload);
                } else {
                    response = await api.post('/roles', payload);
                }

                if (response.success) {
                    showToast(`Role ${isEdit ? 'updated' : 'created'} successfully`, 'success');
                    closeRoleModal();
                    await loadRoles();
                } else {
                    showToast(response.message || 'Operation failed', 'error');
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = originalHTML;
                    if (window.lucide) lucide.createIcons();
                }
            } catch (error) {
                console.error('Failed to save role', error);
                showToast(error.message || 'Failed to save role', 'error');
                saveBtn.disabled = false;
                saveBtn.innerHTML = originalHTML;
                if (window.lucide) lucide.createIcons();
            }
        });
    }

    // Close modal on outside click
    const modal = document.getElementById('role-modal');
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeRoleModal();
            }
        });
    }
    
    // Close modal on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal && modal.classList.contains('active')) {
            closeRoleModal();
        }
    });
});
