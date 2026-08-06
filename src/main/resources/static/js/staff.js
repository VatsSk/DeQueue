class Staff {
  constructor() {
    this.staff = [];
    this.departments = [];
    this.init();
  }

  async init() {
    console.log('Staff initialized');
    this.setupModal();
    await this.loadDepartments();
    await this.loadStaff();
  }

  async loadDepartments() {
    try {
      const res = await api.get('/departments');
      if (res.success) {
        this.departments = res.data;
        const select = document.getElementById('addStaffDept');
        if (select) {
          let html = '<option value="">None</option>';
          this.departments.forEach(dept => {
            html += `<option value="${dept.id}">${dept.name}</option>`;
          });
          select.innerHTML = html;
        }
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to load departments', 'error');
    }
  }

  async loadStaff() {
    try {
      const res = await api.get('/staff?size=100');
      if (res.success) {
        this.staff = res.data.content ? res.data.content : res.data;
        this.renderStaff();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to load staff', 'error');
    }
  }

  renderStaff() {
    const tbody = document.getElementById('staffTableBody');
    if (!tbody) return;
    
    if (!this.staff || this.staff.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" class="text-center p-4 text-muted">No staff found.</td></tr>';
      return;
    }

    let html = '';
    this.staff.forEach(s => {
      const deptName = this.departments.find(d => d.id === s.departmentId)?.name || 'None';
      const statusBadge = s.status === 'ACTIVE' ? '<span class="badge badge-ready">Active</span>' : '<span class="badge badge-pending">Inactive</span>';
      
      html += `
        <tr>
            <td class="font-medium">${s.name} <br><small class="text-muted" style="font-weight:normal">${s.email}</small></td>
            <td class="text-muted">${s.role}</td>
            <td>${deptName}</td>
            <td>${statusBadge}</td>
            <td>
                <div class="flex gap-2">
                    <button class="btn-icon text-danger" title="Delete" onclick="staffApp.deleteStaff('${s.id}')"><i data-lucide="trash-2"></i></button>
                </div>
            </td>
        </tr>
      `;
    });
    tbody.innerHTML = html;
    if (window.lucide) lucide.createIcons();
  }

  setupModal() {
    const btn = document.getElementById('saveStaffBtn');
    if (btn) {
      btn.addEventListener('click', () => this.saveStaff());
    }
  }

  async saveStaff() {
    const name = document.getElementById('addStaffName').value;
    const email = document.getElementById('addStaffEmail').value;
    const password = document.getElementById('addStaffPassword').value;
    const role = document.getElementById('addStaffRole').value;
    const deptId = document.getElementById('addStaffDept').value;

    if (!name || !email || !password || !role) {
      if (window.showToast) showToast('Please fill all required fields', 'error');
      return;
    }

    const btn = document.getElementById('saveStaffBtn');
    btn.disabled = true;
    btn.innerText = 'Saving...';

    try {
      const res = await api.post('/staff', {
        name: name,
        email: email,
        password: password,
        role: role,
        departmentId: deptId || null
      });
      
      if (res.success) {
        if (window.showToast) showToast('Staff Added Successfully', 'success');
        if (window.closeModal) closeModal('add-staff-modal');
        document.getElementById('addStaffName').value = '';
        document.getElementById('addStaffEmail').value = '';
        document.getElementById('addStaffPassword').value = '';
        
        await this.loadStaff();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to add staff', 'error');
    } finally {
      btn.disabled = false;
      btn.innerText = 'Save';
    }
  }

  async deleteStaff(id) {
    if (!confirm('Are you sure you want to delete this staff member?')) return;
    try {
      await api.delete(`/staff/${id}`);
      if (window.showToast) showToast('Staff deleted', 'success');
      await this.loadStaff();
    } catch (e) {
      if (window.showToast) showToast('Failed to delete staff', 'error');
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.staffApp = new Staff();
});
