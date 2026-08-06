class Customizations {
  constructor() {
    this.groups = [];
    this.init();
  }

  async init() {
    this.setupModal();
    this.setupSearch();
    await this.loadGroups();
  }

  async loadGroups() {
    try {
      const res = await api.get('/customizations');
      if (res.success) {
        this.groups = res.data;
        this.renderGroups();
      }
    } catch (e) {
      console.error('Failed to load customizations', e);
      if (window.showToast) showToast('Failed to load customizations', 'error');
    }
  }

  renderGroups(searchQuery = '') {
    const grid = document.querySelector('.grid-cols-2');
    if (!grid) return;

    if (this.groups.length === 0) {
      grid.innerHTML = '<div class="text-muted col-span-2 text-center p-4">No customization groups found.</div>';
      return;
    }

    let filtered = this.groups;
    if (searchQuery) {
      filtered = filtered.filter(g => g.name.toLowerCase().includes(searchQuery.toLowerCase()));
    }

    let html = '';
    filtered.forEach(group => {
      let optionsHtml = '';
      if (group.options) {
        group.options.forEach(opt => {
          optionsHtml += `<li class="flex justify-between"><span>${opt.name}</span> <span>+₹${opt.additionalPrice || 0}</span></li>`;
        });
      }

      html += `
        <div class="card">
            <div class="flex justify-between items-center border-b border-border pb-4 mb-4">
                <div>
                    <h3 class="font-bold text-lg">${group.name}</h3>
                    <span class="badge ${group.required ? 'badge-preparing' : 'badge-pending'} mt-1">
                        ${group.selectionType === 'SINGLE' ? 'Single Selection' : 'Multiple Selection'} • ${group.required ? 'Required' : 'Optional'}
                    </span>
                </div>
                <div class="flex gap-2">
                    <button class="btn-icon text-danger" onclick="customizationsApp.deleteGroup('${group.id}')"><i data-lucide="trash-2"></i></button>
                </div>
            </div>
            <ul class="flex flex-col gap-2 text-muted">
                ${optionsHtml}
            </ul>
        </div>
      `;
    });

    grid.innerHTML = html;
    if (window.lucide) lucide.createIcons();
  }

  setupSearch() {
    const searchInput = document.querySelector('input[placeholder="Search groups..."]');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.renderGroups(e.target.value);
      });
    }
  }

  setupModal() {
    const saveBtn = document.querySelector('#add-group-modal-overlay .modal-footer .btn-primary');
    if (saveBtn) {
        // Unbind inline onclick to prevent duplicated execution if any, but replacing HTML is safer
        const modalFooter = document.querySelector('#add-group-modal-overlay .modal-footer');
        modalFooter.innerHTML = `
            <button class="btn btn-secondary" onclick="closeModal('add-group-modal')">Cancel</button>
            <button class="btn btn-primary" id="saveCustomizationBtn">Save</button>
        `;
        document.getElementById('saveCustomizationBtn').addEventListener('click', () => this.saveGroup());
    }
  }

  addOptionRow() {
    const container = document.getElementById('options-container');
    const row = document.createElement('div');
    row.className = 'flex gap-2 items-center option-row mt-2';
    row.innerHTML = `
        <input type="text" class="form-control opt-name" placeholder="Option name (e.g., Oat Milk)">
        <input type="number" class="form-control opt-price" placeholder="Price (+₹)" style="width: 120px;" value="0">
        <button class="btn-icon text-danger" onclick="this.parentElement.remove()"><i data-lucide="x"></i></button>
    `;
    container.appendChild(row);
    if (window.lucide) lucide.createIcons({root: row});
  }

  async saveGroup() {
    const nameInput = document.querySelector('#add-group-modal-overlay input[placeholder="e.g., Milk Choice"]');
    const selectType = document.querySelector('#add-group-modal-overlay select');
    const requiredCheck = document.querySelector('#add-group-modal-overlay input[type="checkbox"]');
    
    const name = nameInput.value.trim();
    const selectionType = selectType.value.includes('Single') ? 'SINGLE' : 'MULTIPLE';
    const isRequired = requiredCheck.checked;

    if (!name) {
        if (window.showToast) showToast('Group name is required', 'error');
        return;
    }

    const options = [];
    const rows = document.querySelectorAll('#options-container .flex.gap-2.items-center');
    rows.forEach(row => {
        const optName = row.querySelector('input[type="text"]').value.trim();
        const optPrice = row.querySelector('input[type="number"]').value || 0;
        if (optName) {
            options.push({ name: optName, additionalPrice: parseFloat(optPrice) });
        }
    });

    if (options.length === 0) {
        if (window.showToast) showToast('At least one option is required', 'error');
        return;
    }

    const btn = document.getElementById('saveCustomizationBtn');
    btn.disabled = true;
    btn.innerText = 'Saving...';

    try {
      const res = await api.post('/customizations', {
          name: name,
          selectionType: selectionType,
          required: isRequired,
          options: options
      });
      
      if (res.success) {
        if (window.showToast) showToast('Customization Group Saved', 'success');
        if (window.closeModal) closeModal('add-group-modal');
        
        // Reset form
        nameInput.value = '';
        requiredCheck.checked = false;
        selectType.selectedIndex = 0;
        document.getElementById('options-container').innerHTML = `
            <div class="flex gap-2 items-center option-row mt-2">
                <input type="text" class="form-control opt-name" placeholder="Option name (e.g., Oat Milk)">
                <input type="number" class="form-control opt-price" placeholder="Price (+₹)" style="width: 120px;" value="0">
                <button class="btn-icon text-danger" onclick="this.parentElement.remove()"><i data-lucide="x"></i></button>
            </div>
        `;

        await this.loadGroups();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to save group', 'error');
    } finally {
      btn.disabled = false;
      btn.innerText = 'Save';
    }
  }

  async deleteGroup(id) {
    if (!confirm('Are you sure you want to delete this customization group?')) return;
    try {
      const res = await api.delete('/customizations/' + id);
      if (res.success || res.status === 200 || res.status === 204) {
        if (window.showToast) showToast('Group deleted', 'success');
        await this.loadGroups();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to delete group', 'error');
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.customizationsApp = new Customizations();
});
