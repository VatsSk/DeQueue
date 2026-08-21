// Platform Vendor Management
let currentPage = 0;
let pageSize = 10;
let totalVendors = 0;
let allVendors = [];

async function loadVendors() {
    try {
        const response = await api.get('/platform/vendors');
        if (response && response.success && response.data) {
            allVendors = response.data;
            totalVendors = allVendors.length;
            updateStatistics();
            filterVendors();
        } else {
            showToast('Failed to load vendors', 'error');
            showEmptyState('error', 'Failed to load vendors');
        }
    } catch (error) {
        console.error('Failed to load vendors', error);
        showToast(error.message || 'Failed to load vendors', 'error');
        showEmptyState('error', 'Failed to load vendors. Please try again.');
    }
}

function updateStatistics() {
    const total = allVendors.length;
    const open = allVendors.filter(v => v.status === 'OPEN').length;
    const paused = allVendors.filter(v => v.status === 'PAUSED').length;
    const closed = allVendors.filter(v => v.status === 'CLOSED').length;
    
    document.getElementById('stat-total').textContent = total;
    document.getElementById('stat-open').textContent = open;
    document.getElementById('stat-paused').textContent = paused;
    document.getElementById('stat-closed').textContent = closed;
}

function filterVendors() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const statusFilter = document.getElementById('statusFilter').value;

    let filtered = allVendors.filter(vendor => {
        const matchesSearch = !searchTerm || 
            (vendor.shopName && vendor.shopName.toLowerCase().includes(searchTerm)) ||
            (vendor.vendorCode && vendor.vendorCode.toLowerCase().includes(searchTerm)) ||
            (vendor.email && vendor.email.toLowerCase().includes(searchTerm)) ||
            (vendor.phone && vendor.phone.toLowerCase().includes(searchTerm));
        
        const matchesStatus = !statusFilter || vendor.status === statusFilter;
        
        return matchesSearch && matchesStatus;
    });

    renderVendors(filtered);
    updatePagination(filtered.length);
}

function getStatusBadge(status) {
    const statusLower = (status || 'unknown').toLowerCase();
    const statusUpper = (status || 'UNKNOWN').toUpperCase();
    
    const classes = {
        'open': 'status-badge open',
        'paused': 'status-badge paused',
        'closed': 'status-badge closed'
    };
    
    const badgeClass = classes[statusLower] || 'status-badge unknown';
    return `<span class="${badgeClass}">${statusUpper}</span>`;
}

function renderVendors(vendors) {
    const tbody = document.getElementById('vendors-table-body');
    if (!tbody) return;

    if (vendors.length === 0) {
        const searchTerm = document.getElementById('searchInput').value;
        const statusFilter = document.getElementById('statusFilter').value;
        
        if (searchTerm || statusFilter) {
            showEmptyState('search', 'No vendors found matching your filters');
        } else {
            showEmptyState('empty', 'No vendors yet', 'Get started by adding your first vendor');
        }
        return;
    }

    const start = currentPage * pageSize;
    const end = Math.min(start + pageSize, vendors.length);
    const pageVendors = vendors.slice(start, end);

    let html = '';
    pageVendors.forEach(vendor => {
        const createdDate = vendor.createdAt ? new Date(vendor.createdAt).toLocaleDateString('en-IN', {
            day: 'numeric',
            month: 'short',
            year: 'numeric'
        }) : 'N/A';

        html += `
            <tr>
                <td>
                    <span class="vendor-code">${vendor.vendorCode || 'N/A'}</span>
                </td>
                <td>
                    <div class="vendor-name">${vendor.shopName || 'Unnamed Vendor'}</div>
                    <div class="vendor-subtext">${vendor.ownerName || 'No owner specified'}</div>
                </td>
                <td>
                    <div class="contact-info">
                        <div><i data-lucide="mail"></i> <span>${vendor.email || 'N/A'}</span></div>
                        <div><i data-lucide="phone"></i> <span>${vendor.phone || 'N/A'}</span></div>
                    </div>
                </td>
                <td>
                    ${getStatusBadge(vendor.status)}
                </td>
                <td>
                    <div style="font-size: 0.8125rem; color: var(--text-muted);">${createdDate}</div>
                </td>
                <td>
                    <div class="action-btns">
                        <button class="btn btn-sm btn-secondary" onclick="viewVendor('${vendor.id}')" title="View details" aria-label="View vendor details">
                            <i data-lucide="eye"></i>
                        </button>
                        <button class="btn btn-sm btn-primary" onclick="editVendor('${vendor.id}')" title="Edit vendor" aria-label="Edit vendor">
                            <i data-lucide="edit-2"></i>
                        </button>
                        <button class="btn btn-sm btn-error" onclick="deleteVendor('${vendor.id}')" title="Delete vendor" aria-label="Delete vendor">
                            <i data-lucide="trash-2"></i>
                        </button>
                        <button class="btn btn-sm" style="background: #10b981; color: white;" onclick="manageCashfree('${vendor.id}')" title="Manage Cashfree" aria-label="Manage Cashfree">
                            CF
                        </button>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
    if (window.lucide) lucide.createIcons();
}

function showEmptyState(type, title, message) {
    const tbody = document.getElementById('vendors-table-body');
    if (!tbody) return;
    
    let icon = 'search';
    
    if (type === 'error') {
        icon = 'alert-circle';
    } else if (type === 'empty') {
        icon = 'building';
    }
    
    const messageHTML = message ? `<p>${message}</p>` : '';
    
    tbody.innerHTML = `
        <tr>
            <td colspan="6">
                <div class="empty-state">
                    <i data-lucide="${icon}"></i>
                    <h3>${title}</h3>
                    ${messageHTML}
                </div>
            </td>
        </tr>
    `;
    
    if (window.lucide) lucide.createIcons();
}

function updatePagination(totalFiltered) {
    const start = currentPage * pageSize + 1;
    const end = Math.min((currentPage + 1) * pageSize, totalFiltered);
    
    document.getElementById('showing-start').textContent = totalFiltered > 0 ? start : 0;
    document.getElementById('showing-end').textContent = end;
    document.getElementById('showing-total').textContent = totalFiltered;

    document.getElementById('prev-page-btn').disabled = currentPage === 0;
    document.getElementById('next-page-btn').disabled = end >= totalFiltered;
}

function prevPage() {
    if (currentPage > 0) {
        currentPage--;
        filterVendors();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function nextPage() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const statusFilter = document.getElementById('statusFilter').value;
    
    let filtered = allVendors.filter(vendor => {
        const matchesSearch = !searchTerm || 
            (vendor.shopName && vendor.shopName.toLowerCase().includes(searchTerm)) ||
            (vendor.vendorCode && vendor.vendorCode.toLowerCase().includes(searchTerm)) ||
            (vendor.email && vendor.email.toLowerCase().includes(searchTerm));
        const matchesStatus = !statusFilter || vendor.status === statusFilter;
        return matchesSearch && matchesStatus;
    });
    
    const maxPage = Math.ceil(filtered.length / pageSize) - 1;
    if (currentPage < maxPage) {
        currentPage++;
        filterVendors();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function showAddVendorModal() {
    document.getElementById('modal-title').textContent = 'Add New Vendor';
    document.getElementById('vendor-form').reset();
    document.getElementById('vendor-id').value = '';
    document.getElementById('vendor-code').disabled = false;
    document.getElementById('password').required = true;
    document.getElementById('password').placeholder = 'Minimum 8 characters';
    document.getElementById('save-vendor-btn').innerHTML = '<i data-lucide="save"></i> Create Vendor';
    
    const modal = document.getElementById('vendor-modal');
    modal.classList.add('active');
    
    // Focus first input and scroll modal body to top
    setTimeout(() => {
        const modalBody = modal.querySelector('.modal-body');
        if (modalBody) modalBody.scrollTop = 0;
        document.getElementById('vendor-code').focus();
        if (window.lucide) lucide.createIcons();
    }, 100);
}

function closeVendorModal() {
    const modal = document.getElementById('vendor-modal');
    modal.classList.remove('active');
}

async function viewVendor(vendorId) {
    const vendor = allVendors.find(v => v.id === vendorId);
    if (!vendor) return;

    const details = `
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
       VENDOR DETAILS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Shop Name: ${vendor.shopName}
Vendor Code: ${vendor.vendorCode}

Owner: ${vendor.ownerName || 'N/A'}
Email: ${vendor.email}
Phone: ${vendor.phone}

Status: ${vendor.status || 'UNKNOWN'}
Created: ${vendor.createdAt ? new Date(vendor.createdAt).toLocaleString('en-IN') : 'N/A'}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    `.trim();
    
    alert(details);
}

async function editVendor(vendorId) {
    const vendor = allVendors.find(v => v.id === vendorId);
    if (!vendor) return;

    document.getElementById('modal-title').textContent = 'Edit Vendor';
    document.getElementById('vendor-id').value = vendor.id;
    document.getElementById('vendor-code').value = vendor.vendorCode;
    document.getElementById('vendor-code').disabled = true;
    document.getElementById('shop-name').value = vendor.shopName;
    document.getElementById('owner-name').value = vendor.ownerName || '';
    document.getElementById('email').value = vendor.email;
    document.getElementById('phone').value = vendor.phone;
    document.getElementById('address').value = vendor.address || '';
    document.getElementById('password').value = '';
    document.getElementById('password').required = false;
    document.getElementById('password').placeholder = 'Leave blank to keep current password';
    document.getElementById('save-vendor-btn').innerHTML = '<i data-lucide="save"></i> Update Vendor';
    
    const modal = document.getElementById('vendor-modal');
    modal.classList.add('active');
    
    setTimeout(() => {
        const modalBody = modal.querySelector('.modal-body');
        if (modalBody) modalBody.scrollTop = 0;
        document.getElementById('shop-name').focus();
        if (window.lucide) lucide.createIcons();
    }, 100);
}

async function deleteVendor(vendorId) {
    const vendor = allVendors.find(v => v.id === vendorId);
    if (!vendor) return;

    const confirmed = confirm(
        `⚠️ Delete Vendor?\n\n` +
        `Are you sure you want to delete "${vendor.shopName}"?\n\n` +
        `This action cannot be undone and will remove:\n` +
        `• All vendor data\n` +
        `• All menu items\n` +
        `• All orders\n` +
        `• All staff accounts`
    );
    
    if (!confirmed) return;

    try {
        const response = await api.delete(`/platform/vendors/${vendorId}`);
        if (response.success) {
            showToast('Vendor deleted successfully', 'success');
            await loadVendors();
        } else {
            showToast(response.message || 'Failed to delete vendor', 'error');
        }
    } catch (error) {
        console.error('Failed to delete vendor', error);
        showToast(error.message || 'Failed to delete vendor', 'error');
    }
}

async function manageCashfree(vendorId) {
    const vendor = allVendors.find(v => v.id === vendorId);
    if (!vendor) return;
    try {
        const res = await api.get(`/platform/vendors/${vendorId}/cashfree/status`);
        if (res.success) {
            const data = res.data;
            if (data.status === 'NOT_ONBOARDED') {
                if (confirm('Vendor is not onboarded to Cashfree Easy Split. Open onboarding wizard?')) {
                    window.location.href = `vendor-onboarding.html?vendorId=${vendorId}`;
                }
            } else {
                alert(`Cashfree Status:\\nVendor ID: ${data.cashfreeVendorId}\\nStatus: ${data.status}\\nOnboarding: ${data.onboardingStatus}\\nEasy Split Enabled: ${data.easySplitEnabled}`);
                if (confirm('Do you want to sync status from Cashfree?')) {
                    const syncRes = await api.post(`/platform/vendors/${vendorId}/cashfree/sync`);
                    if (syncRes.success) alert('Synced successfully. Status: ' + syncRes.data.status);
                }
            }
        }
    } catch(e) {
        alert('Error managing Cashfree: ' + e.message);
    }
}

// Form submission handler
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('vendor-form');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const vendorId = document.getElementById('vendor-id').value;
            const isEdit = !!vendorId;
            
            const saveBtn = document.getElementById('save-vendor-btn');
            const originalHTML = saveBtn.innerHTML;
            saveBtn.disabled = true;
            saveBtn.innerHTML = '<i data-lucide="loader" class="spin"></i> Saving...';
            if (window.lucide) lucide.createIcons();

            const payload = {
                vendorCode: document.getElementById('vendor-code').value.trim(),
                shopName: document.getElementById('shop-name').value.trim(),
                ownerName: document.getElementById('owner-name').value.trim(),
                email: document.getElementById('email').value.trim(),
                phone: document.getElementById('phone').value.trim(),
                address: document.getElementById('address').value.trim()
            };

            const password = document.getElementById('password').value;
            if (password) {
                payload.password = password;
            }

            try {
                let response;
                if (isEdit) {
                    response = await api.put(`/platform/vendors/${vendorId}`, payload);
                } else {
                    response = await api.post('/platform/vendors', payload);
                }

                if (response.success) {
                    showToast(`Vendor ${isEdit ? 'updated' : 'created'} successfully`, 'success');
                    closeVendorModal();
                    await loadVendors();
                } else {
                    showToast(response.message || 'Operation failed', 'error');
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = originalHTML;
                    if (window.lucide) lucide.createIcons();
                }
            } catch (error) {
                console.error('Failed to save vendor', error);
                showToast(error.message || 'Failed to save vendor', 'error');
                saveBtn.disabled = false;
                saveBtn.innerHTML = originalHTML;
                if (window.lucide) lucide.createIcons();
            }
        });
    }

    // Close modal on outside click
    const modal = document.getElementById('vendor-modal');
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeVendorModal();
            }
        });
    }
    
    // Close modal on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.classList.contains('active')) {
            closeVendorModal();
        }
    });
});
