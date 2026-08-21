class Settings {
  constructor() {
    this.settings = {};
    this.init();
  }

  async init() {
    console.log('Settings initialized');
    this.setupTabs();
    await this.fetchSettings();
    this.setupSave();
  }

  setupTabs() {
    const tabs = document.querySelectorAll('.settings-tab');
    const panes = document.querySelectorAll('.tab-pane');
    
    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            tabs.forEach(t => t.classList.remove('active'));
            panes.forEach(p => p.classList.add('hidden'));
            
            const targetId = tab.getAttribute('data-tab');
            e.target.classList.add('active');
            document.getElementById('tab-' + targetId).classList.remove('hidden');
        });
    });
  }

  async fetchSettings() {
    try {
      const res = await api.get('/vendors/me');
      if (res.success && res.data) {
        this.settings = res.data.settings || {};
        this.populateForm();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to load settings', 'error');
    }
  }

  populateForm() {
    document.getElementById('set-auto-accept').checked = this.settings.autoAcceptOrders || false;
    document.getElementById('set-allow-custom').checked = this.settings.allowCustomOrder || false;
    document.getElementById('set-gst-number').value = this.settings.gstNumber || '';
    
    document.getElementById('set-tax-name').value = this.settings.taxName || 'Tax';
    document.getElementById('set-tax-pct').value = this.settings.taxPercentage || 0;
    document.getElementById('set-charge-name').value = this.settings.additionalChargeName || '';
    document.getElementById('set-charge-amt').value = this.settings.additionalCharges || 0;


    document.getElementById('set-enable-online-payment').checked = this.settings.enableOnlinePayment || false;
    document.getElementById('set-upi-id').value = this.settings.upiId || '';
    document.getElementById('set-bank-name').value = this.settings.bankAccountName || '';
    document.getElementById('set-bank-acc').value = this.settings.bankAccountNumber || '';
    document.getElementById('set-bank-ifsc').value = this.settings.bankIfscCode || '';

    this.renderCoupons();
    this.renderCustomFields();
  }

  renderCustomFields() {
    const list = document.getElementById('custom-fields-list');
    const fields = this.settings.customFields || [];

    if (fields.length === 0) {
      list.innerHTML = '<div class="text-sm text-muted">No custom fields created.</div>';
      return;
    }
    
    // Sort fields by displayOrder
    fields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));

    list.innerHTML = fields.map((f, idx) => `
      <div class="flex justify-between items-center border p-3 rounded mb-2">
        <div>
          <span class="font-bold">${this._esc(f.label)} <small class="text-muted">(${this._esc(f.id)})</small></span>
          <span class="badge badge-info ml-2">${f.type}</span>
          ${f.required ? '<span class="badge badge-danger ml-2">Required</span>' : ''}
          ${!f.enabled ? '<span class="badge badge-warning ml-2">Disabled</span>' : ''}
          ${(f.conditions && f.conditions.length > 0) ? `<span class="badge ml-2" style="background:var(--bg-color);border:1px solid var(--border)"><i data-lucide="git-commit" style="width:12px;height:12px;display:inline"></i> if ${this._esc(f.conditions[0].fieldId)} ${this._esc(f.conditions[0].operator)} ${this._esc(f.conditions[0].value)}</span>` : ''}
          ${(f.type === 'dropdown' || f.type === 'radio' || f.type === 'checkbox') && f.options ? `<div class="text-xs text-muted mt-1">Options: ${f.options.map(o => o.label).join(', ')}</div>` : ''}
        </div>
        <button class="btn btn-icon text-danger" onclick="settingsApp.deleteCustomField('${this._esc(f.id)}')">
          <i data-lucide="trash-2"></i>
        </button>
      </div>
    `).join('');
    
    if (window.lucide) lucide.createIcons();
  }

  onCfTypeChange() {
    const type = document.getElementById('new-cf-type').value;
    const optContainer = document.getElementById('cf-options-container');
    if (type === 'dropdown' || type === 'radio' || type === 'checkbox') {
        optContainer.classList.remove('hidden');
    } else {
        optContainer.classList.add('hidden');
    }
  }

  addCustomField() {
    const id = document.getElementById('new-cf-id').value.trim();
    const label = document.getElementById('new-cf-label').value.trim();
    const type = document.getElementById('new-cf-type').value;
    const orderStr = document.getElementById('new-cf-order').value;
    const required = document.getElementById('new-cf-required').checked;
    const enabled = document.getElementById('new-cf-enabled').checked;
    const optionsStr = document.getElementById('new-cf-options').value;
    
    const dependsId = document.getElementById('new-cf-depends-id').value.trim();
    const dependsOperator = document.getElementById('new-cf-operator').value;
    const dependsVal = document.getElementById('new-cf-depends-val').value.trim();

    if (!id || !label) {
      if (window.showToast) showToast('Please enter both ID and Label', 'error');
      return;
    }

    let options = [];
    if (type === 'dropdown' || type === 'radio' || type === 'checkbox') {
        const optionParts = optionsStr.split(',').map(o => o.trim()).filter(o => o);
        if (optionParts.length === 0) {
            if (window.showToast) showToast('Please enter at least one option', 'error');
            return;
        }
        options = optionParts.map(o => ({ value: o.toLowerCase().replace(/[^a-z0-9_ ]/g, '_').replace(/ /g, '_'), label: o }));
    }
    
    if (dependsId && dependsId === id) {
        if (window.showToast) showToast('A field cannot depend on itself', 'error');
        return;
    }

    let conditions = [];
    if (dependsId && dependsVal) {
        conditions.push({ fieldId: dependsId, operator: dependsOperator, value: dependsVal });
    }

    const fieldData = {
        id,
        label,
        type,
        displayOrder: parseInt(orderStr) || 0,
        required,
        enabled,
        options: options.length > 0 ? options : null,
        conditions: conditions.length > 0 ? conditions : null
    };

    if (!this.settings.customFields) this.settings.customFields = [];
    
    // Check if ID already exists
    const existingIdx = this.settings.customFields.findIndex(f => f.id === id);
    if (existingIdx >= 0) {
        this.settings.customFields[existingIdx] = fieldData; // update
    } else {
        this.settings.customFields.push(fieldData);
    }
    
    document.getElementById('new-cf-id').value = '';
    document.getElementById('new-cf-label').value = '';
    document.getElementById('new-cf-options').value = '';
    document.getElementById('new-cf-depends-id').value = '';
    document.getElementById('new-cf-depends-val').value = '';
    this.renderCustomFields();
  }

  deleteCustomField(id) {
    if (!this.settings.customFields) return;
    const idx = this.settings.customFields.findIndex(f => f.id === id);
    if (idx >= 0) {
        this.settings.customFields.splice(idx, 1);
        this.renderCustomFields();
    }
  }

  renderCoupons() {
    const list = document.getElementById('coupons-list');
    const coupons = this.settings.coupons || [];
    
    if (coupons.length === 0) {
      list.innerHTML = '<div class="text-sm text-muted">No coupons created yet.</div>';
      return;
    }

    list.innerHTML = coupons.map((c, idx) => `
      <div class="flex justify-between items-center border p-3 rounded">
        <div>
          <span class="font-bold uppercase">${this._esc(c.code)}</span>
          <span class="badge ${c.type === 'PERCENTAGE' ? 'badge-ready' : 'badge-preparing'} ml-2">
            ${c.type === 'PERCENTAGE' ? c.value + '% OFF' : '₹' + c.value + ' OFF'}
          </span>
        </div>
        <button class="btn btn-icon text-danger" onclick="settingsApp.deleteCoupon(${idx})">
          <i data-lucide="trash-2"></i>
        </button>
      </div>
    `).join('');
    
    if (window.lucide) lucide.createIcons();
  }

  addCoupon() {
    const code = document.getElementById('new-coupon-code').value.toUpperCase().trim();
    const type = document.getElementById('new-coupon-type').value;
    const value = parseFloat(document.getElementById('new-coupon-value').value);

    if (!code || isNaN(value)) {
      if (window.showToast) showToast('Please enter code and valid value', 'error');
      return;
    }

    if (!this.settings.coupons) this.settings.coupons = [];
    this.settings.coupons.push({ code, type, value, active: true });
    
    document.getElementById('new-coupon-code').value = '';
    document.getElementById('new-coupon-value').value = '';
    this.renderCoupons();
  }

  deleteCoupon(idx) {
    if (!this.settings.coupons) return;
    this.settings.coupons.splice(idx, 1);
    this.renderCoupons();
  }

  setupSave() {
    const saveBtn = document.querySelector('.header .btn-primary');
    if (saveBtn) {
      saveBtn.onclick = () => this.saveSettings();
    }
  }

  async saveSettings() {
    this.settings.autoAcceptOrders = document.getElementById('set-auto-accept').checked;
    this.settings.allowCustomOrder = document.getElementById('set-allow-custom').checked;
    this.settings.gstNumber = document.getElementById('set-gst-number').value.trim();
    this.settings.taxName = document.getElementById('set-tax-name').value.trim() || 'Tax';
    this.settings.taxPercentage = parseFloat(document.getElementById('set-tax-pct').value) || 0;
    this.settings.additionalChargeName = document.getElementById('set-charge-name').value || '';
    this.settings.additionalCharges = parseFloat(document.getElementById('set-charge-amt').value) || 0;

    this.settings.enableOnlinePayment = document.getElementById('set-enable-online-payment').checked;
    this.settings.upiId = document.getElementById('set-upi-id').value.trim();
    this.settings.bankAccountName = document.getElementById('set-bank-name').value.trim();
    this.settings.bankAccountNumber = document.getElementById('set-bank-acc').value.trim();
    this.settings.bankIfscCode = document.getElementById('set-bank-ifsc').value.trim();

    try {
      const res = await api.patch('/vendors/me/settings', this.settings);
      if (res.success) {
        if (window.showToast) showToast('Settings saved successfully', 'success');
        const userStr = localStorage.getItem('user');
        if (userStr) {
           const u = JSON.parse(userStr);
           u.settings = this.settings;
           localStorage.setItem('user', JSON.stringify(u));
        }
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to save settings', 'error');
    }
  }
  
  _esc(str) {
    if (!str) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.settingsApp = new Settings();
});
