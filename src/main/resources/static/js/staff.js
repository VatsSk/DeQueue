class Staff {
  constructor() {
    this.staff = [];
    this.departments = [];
    this.roles = [];
    this.init();
  }

  async init() {
    console.log('Staff initialized');
    this.setupModal();
    await this.loadDepartments();
    await this.loadRoles();
    await this.loadStaff();
  }

  async loadDepartments() {
    try {
      const res = await api.get('/departments');
      if (res.success) {
        this.departments = res.data;
        this.renderDepartmentDropdown();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to load departments', 'error');
    }
  }

  async loadRoles() {
    try {
      const res = await api.get('/roles');
      if (res.success) {
        this.roles = res.data;
        this.renderRoleDropdown();
      }
    } catch (e) {
      console.error('Failed to load roles', e);
      if (window.showToast) showToast('Failed to load roles', 'error');
    }
  }

  renderDepartmentDropdown() {
    const select = document.getElementById('addStaffDept');
    if (select) {
      let html = '<option value="">None</option>';
      this.departments.forEach(dept => {
        html += `<option value="${dept.id}">${dept.name}</option>`;
      });
      select.innerHTML = html;
    }
  }

  renderRoleDropdown() {
    const select = document.getElementById('addStaffRole');
    if (select) {
      if (this.roles.length === 0) {
        select.innerHTML = '<option value="">No roles available - Create a role first</option>';
        select.disabled = true;
      } else {
        let html = '<option value="">Select a role</option>';
        this.roles.forEach(role => {
          html += `<option value="${role.id}">${role.name}</option>`;
        });
        select.innerHTML = html;
        select.disabled = false;
      }
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
      // Get department names
      let deptNames = 'None';
      if (s.departmentIds && s.departmentIds.length > 0) {
        const depts = s.departmentIds.map(id => {
          const dept = this.departments.find(d => d.id === id);
          return dept ? dept.name : '';
        }).filter(n => n);
        deptNames = depts.length > 0 ? depts.join(', ') : 'None';
      }

      // Get role names
      let roleNames = 'No role';
      if (s.roleIds && s.roleIds.length > 0) {
        const roles = s.roleIds.map(id => {
          const role = this.roles.find(r => r.id === id);
          return role ? role.name : '';
        }).filter(n => n);
        roleNames = roles.length > 0 ? roles.join(', ') : 'No role';
      }

      const statusBadge = s.status === 'ACTIVE' 
        ? '<span class="badge badge-ready">Active</span>' 
        : '<span class="badge badge-pending">Inactive</span>';
      
      html += `
        <tr>
            <td class="font-medium">${s.name} <br><small class="text-muted" style="font-weight:normal">${s.email}</small></td>
            <td class="text-muted">${roleNames}</td>
            <td>${deptNames}</td>
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
    const roleId = document.getElementById('addStaffRole').value;
    const deptId = document.getElementById('addStaffDept').value;

    if (!name || !email || !password || !roleId) {
      if (window.showToast) showToast('Please fill all required fields', 'error');
      return;
    }

    const btn = document.getElementById('saveStaffBtn');
    btn.disabled = true;
    btn.innerText = 'Saving...';

    try {
      const payload = {
        name: name,
        email: email,
        password: password,
        roleIds: [roleId], // Array of role IDs
        departmentIds: deptId ? [deptId] : [] // Array of department IDs
      };

      const res = await api.post('/staff', payload);
      
      if (res.success) {
        if (window.showToast) showToast('Staff Added Successfully', 'success');
        if (window.closeModal) closeModal('add-staff-modal');
        document.getElementById('addStaffName').value = '';
        document.getElementById('addStaffEmail').value = '';
        document.getElementById('addStaffPassword').value = '';
        document.getElementById('addStaffRole').value = '';
        document.getElementById('addStaffDept').value = '';
        
        await this.loadStaff();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast(e.message || 'Failed to add staff', 'error');
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
