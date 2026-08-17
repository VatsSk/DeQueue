class Departments {
  constructor() {
    this.departments = [];
    this.init();
  }

  async init() {
    console.log('Departments initialized');
    await this.loadDepartments();
    this.setupModal();
  }

  async loadDepartments() {
    try {
      const res = await api.get('/departments');
      if (res.success) {
        this.departments = res.data;
        this.renderDepartments();
      }
    } catch (e) {
      console.error('Failed to load departments', e);
      this.showError('Failed to load departments');
    }
  }

  renderDepartments() {
    const grid = document.getElementById('departments-grid');
    if (!grid) return;

    if (!this.departments || this.departments.length === 0) {
      grid.innerHTML = `
        <div class="text-center text-muted p-8 col-span-3">
          <i data-lucide="briefcase" style="width: 64px; height: 64px; opacity: 0.2; margin-bottom: 1rem;"></i>
          <h3 style="margin-bottom: 0.5rem;">No departments yet</h3>
          <p>Create your first department to organize your staff</p>
        </div>
      `;
      if (window.lucide) lucide.createIcons();
      return;
    }

    let html = '';
    this.departments.forEach(dept => {
      const statusBadge = dept.active 
        ? '<span class="badge badge-ready">Active</span>'
        : '<span class="badge badge-secondary">Inactive</span>';

      html += `
        <div class="card p-4">
          <div class="flex justify-between items-start mb-3">
            <div>
              <h3 class="mb-1">${dept.name}</h3>
              ${statusBadge}
            </div>
            <div class="flex gap-2">
              <button class="btn-icon" onclick="departmentsApp.editDepartment('${dept.id}')" title="Edit">
                <i data-lucide="edit-2"></i>
              </button>
              <button class="btn-icon text-danger" onclick="departmentsApp.deleteDepartment('${dept.id}')" title="Delete">
                <i data-lucide="trash-2"></i>
              </button>
            </div>
          </div>
          <p class="text-muted text-sm">${dept.description || 'No description'}</p>
        </div>
      `;
    });

    grid.innerHTML = html;
    if (window.lucide) lucide.createIcons();
  }

  setupModal() {
    const saveBtn = document.querySelector('#add-dept-modal-overlay .btn-primary');
    if (saveBtn) {
      saveBtn.onclick = () => this.saveDepartment();
    }
  }

  showAddModal() {
    const modal = document.getElementById('add-dept-modal-overlay');
    const nameInput = modal.querySelector('input[type="text"]');
    const descInput = modal.querySelector('textarea');
    
    modal.querySelector('.modal-header h3').textContent = 'Add Department';
    modal.dataset.editId = '';
    nameInput.value = '';
    descInput.value = '';
    
    if (window.openModal) openModal('add-dept-modal');
    setTimeout(() => nameInput.focus(), 100);
  }

  editDepartment(id) {
    const dept = this.departments.find(d => d.id === id);
    if (!dept) return;

    const modal = document.getElementById('add-dept-modal-overlay');
    const nameInput = modal.querySelector('input[type="text"]');
    const descInput = modal.querySelector('textarea');
    
    modal.querySelector('.modal-header h3').textContent = 'Edit Department';
    modal.dataset.editId = id;
    nameInput.value = dept.name;
    descInput.value = dept.description || '';
    
    if (window.openModal) openModal('add-dept-modal');
    setTimeout(() => nameInput.focus(), 100);
  }

  async saveDepartment() {
    const modal = document.getElementById('add-dept-modal-overlay');
    const nameInput = modal.querySelector('input[type="text"]');
    const descInput = modal.querySelector('textarea');
    const editId = modal.dataset.editId;

    const name = nameInput.value.trim();
    if (!name) {
      if (window.showToast) showToast('Please enter a department name', 'error');
      return;
    }

    const payload = {
      name: name,
      description: descInput.value.trim()
    };

    try {
      let res;
      if (editId) {
        res = await api.put(`/departments/${editId}`, payload);
      } else {
        res = await api.post('/departments', payload);
      }

      if (res.success) {
        if (window.showToast) showToast(`Department ${editId ? 'updated' : 'created'} successfully`, 'success');
        if (window.closeModal) closeModal('add-dept-modal');
        await this.loadDepartments();
      }
    } catch (e) {
      console.error('Failed to save department', e);
      if (window.showToast) showToast(e.message || 'Failed to save department', 'error');
    }
  }

  async deleteDepartment(id) {
    const dept = this.departments.find(d => d.id === id);
    if (!dept) return;

    if (!confirm(`Are you sure you want to delete the department "${dept.name}"?`)) {
      return;
    }

    try {
      await api.delete(`/departments/${id}`);
      if (window.showToast) showToast('Department deleted successfully', 'success');
      await this.loadDepartments();
    } catch (e) {
      console.error('Failed to delete department', e);
      if (window.showToast) showToast(e.message || 'Failed to delete department', 'error');
    }
  }

  showError(message) {
    const grid = document.getElementById('departments-grid');
    if (grid) {
      grid.innerHTML = `
        <div class="text-center text-error p-8 col-span-3">
          <i data-lucide="alert-circle" style="width: 48px; height: 48px; margin-bottom: 1rem;"></i>
          <p>${message}</p>
        </div>
      `;
      if (window.lucide) lucide.createIcons();
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.departmentsApp = new Departments();
  
  // Override the button onclick to use our method
  const addBtn = document.querySelector('button[onclick*="add-dept-modal"]');
  if (addBtn) {
    addBtn.onclick = () => departmentsApp.showAddModal();
  }
});
