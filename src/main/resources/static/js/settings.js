class Settings {
  constructor() {
    this.settings = {};
    this.init();
  }

  async init() {
    console.log("Settings initialized");
    this.setupTabs();
    await this.fetchSettings();
    this.setupSave();
  }

  setupTabs() {
    const tabs = document.querySelectorAll(".settings-tab");
    const panes = document.querySelectorAll(".tab-pane");
    tabs.forEach(tab => {
      tab.addEventListener("click", (e) => {
        tabs.forEach(t => t.classList.remove("active"));
        panes.forEach(p => p.classList.add("hidden"));
        const targetId = tab.getAttribute("data-tab");
        e.target.classList.add("active");
        document.getElementById("tab-" + targetId).classList.remove("hidden");
      });
    });
  }

  async fetchSettings() {
    try {
      const res = await api.get("/vendors/me");
      if (res.success && res.data) {
        this.vendorId = res.data.id;
        this.settings = res.data.settings || {};
        this.populateForm();
        this.loadCashfreeStatus();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast("Failed to load settings", "error");
    }
  }

  populateForm() {
    document.getElementById("set-auto-accept").checked = this.settings.autoAcceptOrders || false;
    document.getElementById("set-allow-custom").checked = this.settings.allowCustomOrder || false;
    document.getElementById("set-gst-number").value = this.settings.gstNumber || "";
    document.getElementById("set-tax-name").value = this.settings.taxName || "Tax";
    document.getElementById("set-tax-pct").value = this.settings.taxPercentage || 0;
    document.getElementById("set-charge-name").value = this.settings.additionalChargeName || "";
    document.getElementById("set-charge-amt").value = this.settings.additionalCharges || 0;
    document.getElementById("set-enable-online-payment").checked = this.settings.enableOnlinePayment || false;
    document.getElementById("set-upi-id").value = this.settings.upiId || "";
    document.getElementById("set-bank-name").value = this.settings.bankAccountName || "";
    document.getElementById("set-bank-acc").value = this.settings.bankAccountNumber || "";
    document.getElementById("set-bank-ifsc").value = this.settings.bankIfscCode || "";
    this.renderCoupons();
    this.renderCustomFields();
  }

  /* ──────────── CUSTOM FIELDS ──────────── */

  _getParentCandidates(excludeId) {
    return (this.settings.customFields || []).filter(f =>
      f.id !== excludeId &&
      ["dropdown", "radio", "checkbox"].includes(f.type) &&
      f.options && f.options.length > 0
    );
  }

  renderCustomFields() {
    const list = document.getElementById("custom-fields-list");
    const fields = this.settings.customFields || [];

    if (fields.length === 0) {
      list.innerHTML = "<div class=\"text-sm text-muted\">No custom fields created.</div>";
    } else {
      fields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
      list.innerHTML = fields.map(f => {
        let condBadge = "";
        if (f.conditions && f.conditions.length > 0) {
          const cond = f.conditions[0];
          const parentField = (this.settings.customFields || []).find(pf => pf.id === cond.fieldId);
          const parentLabel = parentField ? parentField.label : cond.fieldId;
          let optionLabel = cond.value;
          if (parentField && parentField.options) {
            const opt = parentField.options.find(o => o.value === cond.value);
            if (opt) optionLabel = opt.label;
          }
          const opText = cond.operator === "not_equals" ? "≠" : "=";
          condBadge = `<div class="cf-cond-row"><span class="cf-cond-label">Show when:</span><span class="cf-cond-badge"><i data-lucide="git-branch" style="width:12px;height:12px;display:inline;vertical-align:middle;margin-right:3px;"></i><strong>${this._esc(parentLabel)}</strong>&nbsp;${opText}&nbsp;<strong>${this._esc(optionLabel)}</strong></span></div>`;
        }
        const optsList = (["dropdown","radio","checkbox"].includes(f.type)) && f.options
          ? `<div class="cf-options-preview">Options: ${f.options.map(o => `<span class="cf-opt-chip">${this._esc(o.label)}</span>`).join("")}</div>`
          : "";
        return `<div class="cf-field-row">
          <div class="cf-field-meta">
            <div class="cf-field-title">
              <span class="font-bold">${this._esc(f.label)}</span>
              <code class="cf-field-id">${this._esc(f.id)}</code>
              <span class="badge badge-info">${f.type}</span>
              ${f.required ? "<span class=\"badge badge-danger\">Required</span>" : ""}
              ${!f.enabled ? "<span class=\"badge badge-warning\">Disabled</span>" : ""}
            </div>
            ${condBadge}
            ${optsList}
          </div>
          <button class="btn btn-icon text-danger" onclick="settingsApp.deleteCustomField('${this._esc(f.id)}')" title="Delete field"><i data-lucide="trash-2"></i></button>
        </div>`;
      }).join("");
    }

    if (window.lucide) lucide.createIcons();
    this._refreshParentFieldDropdown();
  }

  _refreshParentFieldDropdown() {
    const select = document.getElementById("new-cf-depends-id");
    if (!select) return;
    const currentId = (document.getElementById("new-cf-id")?.value || "").trim() || null;
    const candidates = this._getParentCandidates(currentId);
    const prevVal = select.value;
    select.innerHTML = "<option value=\"\">— None (always show) —</option>";
    candidates.forEach(f => {
      const opt = document.createElement("option");
      opt.value = f.id;
      opt.textContent = `${f.label} (${f.id})`;
      select.appendChild(opt);
    });
    if (prevVal && candidates.find(f => f.id === prevVal)) select.value = prevVal;
    this._onParentFieldChange();
  }

  _onParentFieldChange() {
    const parentId = document.getElementById("new-cf-depends-id")?.value || "";
    const valueSelect = document.getElementById("new-cf-depends-val");
    const condBlock = document.getElementById("cf-condition-value-row");
    if (!valueSelect) return;
    if (!parentId) {
      valueSelect.innerHTML = "<option value=\"\">—</option>";
      if (condBlock) condBlock.classList.add("hidden");
      return;
    }
    const parentField = (this.settings.customFields || []).find(f => f.id === parentId);
    if (!parentField || !parentField.options) {
      valueSelect.innerHTML = "<option value=\"\">—</option>";
      if (condBlock) condBlock.classList.add("hidden");
      return;
    }
    valueSelect.innerHTML = "<option value=\"\">Select value...</option>";
    parentField.options.forEach(opt => {
      const el = document.createElement("option");
      el.value = opt.value;
      el.textContent = opt.label;
      valueSelect.appendChild(el);
    });
    if (condBlock) condBlock.classList.remove("hidden");
  }

  onCfTypeChange() {
    const type = document.getElementById("new-cf-type").value;
    const optContainer = document.getElementById("cf-options-container");
    if (["dropdown","radio","checkbox"].includes(type)) {
      optContainer.classList.remove("hidden");
    } else {
      optContainer.classList.add("hidden");
    }
  }

  addCustomField() {
    const id = document.getElementById("new-cf-id").value.trim();
    const label = document.getElementById("new-cf-label").value.trim();
    const type = document.getElementById("new-cf-type").value;
    const orderStr = document.getElementById("new-cf-order").value;
    const required = document.getElementById("new-cf-required").checked;
    const enabled = document.getElementById("new-cf-enabled").checked;
    const optionsStr = document.getElementById("new-cf-options").value;
    const dependsId = document.getElementById("new-cf-depends-id").value.trim();
    const dependsOperator = document.getElementById("new-cf-operator").value;
    const dependsVal = document.getElementById("new-cf-depends-val").value.trim();

    if (!id || !label) {
      if (window.showToast) showToast("Please enter both ID and Label", "error");
      return;
    }
    if (dependsId && dependsId === id) {
      if (window.showToast) showToast("A field cannot depend on itself", "error");
      return;
    }

    let options = [];
    if (["dropdown","radio","checkbox"].includes(type)) {
      const parts = optionsStr.split(",").map(o => o.trim()).filter(o => o);
      if (parts.length === 0) {
        if (window.showToast) showToast("Please enter at least one option", "error");
        return;
      }
      options = parts.map(o => ({
        value: o.toLowerCase().replace(/[^a-z0-9_]/g, "_").replace(/__+/g, "_"),
        label: o
      }));
    }

    let conditions = [];
    if (dependsId && dependsVal) {
      const parentField = (this.settings.customFields || []).find(f => f.id === dependsId);
      if (!parentField) {
        if (window.showToast) showToast("Selected parent field not found", "error");
        return;
      }
      conditions.push({ fieldId: dependsId, operator: dependsOperator, value: dependsVal });
    } else if (dependsId && !dependsVal) {
      if (window.showToast) showToast("Please select a condition value", "error");
      return;
    }

    const fieldData = {
      id, label, type,
      displayOrder: parseInt(orderStr) || 0,
      required, enabled,
      options: options.length > 0 ? options : null,
      conditions: conditions.length > 0 ? conditions : null
    };

    if (!this.settings.customFields) this.settings.customFields = [];
    const existingIdx = this.settings.customFields.findIndex(f => f.id === id);
    if (existingIdx >= 0) {
      this.settings.customFields[existingIdx] = fieldData;
    } else {
      this.settings.customFields.push(fieldData);
    }

    // Reset form
    document.getElementById("new-cf-id").value = "";
    document.getElementById("new-cf-label").value = "";
    document.getElementById("new-cf-options").value = "";
    document.getElementById("new-cf-order").value = "0";
    document.getElementById("new-cf-required").checked = true;
    document.getElementById("new-cf-enabled").checked = true;
    document.getElementById("new-cf-type").value = "text";
    this.onCfTypeChange();

    this.renderCustomFields();
    if (window.showToast) showToast(`Field "${label}" added. Click Save to persist!`, "success");
  }

  deleteCustomField(id) {
    if (!this.settings.customFields) return;
    const field = this.settings.customFields.find(f => f.id === id);
    const idx = this.settings.customFields.findIndex(f => f.id === id);
    if (idx >= 0) {
      this.settings.customFields.splice(idx, 1);
      // Remove conditions referencing the deleted field
      (this.settings.customFields || []).forEach(f => {
        if (f.conditions) {
          f.conditions = f.conditions.filter(c => c.fieldId !== id);
          if (f.conditions.length === 0) f.conditions = null;
        }
      });
      this.renderCustomFields();
      if (window.showToast && field) showToast(`Field "${field.label}" deleted. Click Save to persist!`, "info");
    }
  }

  /* ──────────── COUPONS ──────────── */

  renderCoupons() {
    const list = document.getElementById("coupons-list");
    const coupons = this.settings.coupons || [];
    if (coupons.length === 0) {
      list.innerHTML = "<div class=\"text-sm text-muted\">No coupons created yet.</div>";
      return;
    }
    list.innerHTML = coupons.map((c, idx) => `
      <div class="flex justify-between items-center border p-3 rounded">
        <div>
          <span class="font-bold uppercase">${this._esc(c.code)}</span>
          <span class="badge ${c.type === "PERCENTAGE" ? "badge-ready" : "badge-preparing"} ml-2">
            ${c.type === "PERCENTAGE" ? c.value + "% OFF" : "₹" + c.value + " OFF"}
          </span>
        </div>
        <button class="btn btn-icon text-danger" onclick="settingsApp.deleteCoupon(${idx})"><i data-lucide="trash-2"></i></button>
      </div>
    `).join("");
    if (window.lucide) lucide.createIcons();
  }

  addCoupon() {
    const code = document.getElementById("new-coupon-code").value.toUpperCase().trim();
    const type = document.getElementById("new-coupon-type").value;
    const value = parseFloat(document.getElementById("new-coupon-value").value);
    if (!code || isNaN(value)) {
      if (window.showToast) showToast("Please enter code and valid value", "error");
      return;
    }
    if (!this.settings.coupons) this.settings.coupons = [];
    this.settings.coupons.push({ code, type, value, active: true });
    document.getElementById("new-coupon-code").value = "";
    document.getElementById("new-coupon-value").value = "";
    this.renderCoupons();
  }

  deleteCoupon(idx) {
    if (!this.settings.coupons) return;
    this.settings.coupons.splice(idx, 1);
    this.renderCoupons();
  }

  /* ──────────── CASHFREE STATUS ──────────── */
  async loadCashfreeStatus() {
    const box = document.getElementById('cashfree-status-box');
    if (!box) return;
    try {
        const res = await api.get(`/platform/vendors/${this.vendorId}/cashfree/status`);
        if (res.success) {
            const data = res.data;
            if (data.status === 'NOT_ONBOARDED') {
                box.innerHTML = 'Not onboarded to Cashfree Easy Split.';
            } else {
                box.innerHTML = `
                    <div style="margin-bottom: 0.25rem"><strong>Vendor ID:</strong> ${data.cashfreeVendorId}</div>
                    <div style="margin-bottom: 0.25rem"><strong>Status:</strong> ${data.status}</div>
                    <div style="margin-bottom: 0.25rem"><strong>KYC Status:</strong> ${data.onboardingStatus}</div>
                    <div style="margin-bottom: 0.25rem"><strong>Easy Split:</strong> ${data.easySplitEnabled ? 'Active' : 'Inactive'}</div>
                    <div style="margin-bottom: 0.25rem"><strong>Bank Account:</strong> ${data.maskedBankAccount || 'N/A'}</div>
                    <div><strong>Last Synced:</strong> ${data.lastSyncedAt ? new Date(data.lastSyncedAt).toLocaleString() : 'Never'}</div>
                `;
            }
        }
    } catch(e) {
        box.innerHTML = 'Error loading Cashfree status.';
    }
  }

  /* ──────────── SAVE ──────────── */

  setupSave() {
    const saveBtn = document.querySelector(".header .btn-primary");
    if (saveBtn) saveBtn.onclick = () => this.saveSettings();
  }

  async saveSettings() {
    this.settings.autoAcceptOrders = document.getElementById("set-auto-accept").checked;
    this.settings.allowCustomOrder = document.getElementById("set-allow-custom").checked;
    this.settings.gstNumber = document.getElementById("set-gst-number").value.trim();
    this.settings.taxName = document.getElementById("set-tax-name").value.trim() || "Tax";
    this.settings.taxPercentage = parseFloat(document.getElementById("set-tax-pct").value) || 0;
    this.settings.additionalChargeName = document.getElementById("set-charge-name").value || "";
    this.settings.additionalCharges = parseFloat(document.getElementById("set-charge-amt").value) || 0;
    this.settings.enableOnlinePayment = document.getElementById("set-enable-online-payment").checked;
    this.settings.upiId = document.getElementById("set-upi-id").value.trim();
    this.settings.bankAccountName = document.getElementById("set-bank-name").value.trim();
    this.settings.bankAccountNumber = document.getElementById("set-bank-acc").value.trim();
    this.settings.bankIfscCode = document.getElementById("set-bank-ifsc").value.trim();

    try {
      const res = await api.patch("/vendors/me/settings", this.settings);
      if (res.success) {
        if (window.showToast) showToast("Settings saved successfully", "success");
        const userStr = localStorage.getItem("user");
        if (userStr) {
          const u = JSON.parse(userStr);
          u.settings = this.settings;
          localStorage.setItem("user", JSON.stringify(u));
        }
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast("Failed to save settings", "error");
    }
  }

  _esc(str) {
    if (!str) return "";
    return String(str).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
  }
}

document.addEventListener("DOMContentLoaded", () => {
  window.settingsApp = new Settings();
});
