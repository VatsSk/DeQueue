class Menu {
  constructor() {
    this.categories = [];
    this.items = [];
    // AI Extraction state
    this._aiFile = null;
    this._aiPreviewData = null;
    this.init();
  }

  async init() {
    this.setupSearch();
    this.setupAddModal();
    this.setupAiExtraction();
    await this.loadCategories();
    await this.loadItems();
  }

  // ─── Data Loading ──────────────────────────────────────────────

  async loadCategories() {
    try {
      const res = await api.get('/categories');
      if (res.success) {
        this.categories = res.data;
        this.renderCategorySidebar();
        this.renderCategorySelect();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to load categories', 'error');
    }
  }

  async loadItems(categoryId = null) {
    try {
      const url = categoryId ? `/menu/items?categoryId=${categoryId}` : `/menu/items`;
      const res = await api.get(url);
      if (res.success) {
        this.items = res.data.content ? res.data.content : res.data;
        this.renderItems();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to load menu items', 'error');
    }
  }

  // ─── Rendering ────────────────────────────────────────────────

  renderCategorySidebar() {
    const container = document.getElementById('categoryListContainer');
    if (!container) return;
    let html = `<div class="cat-item active" data-id="">All Items</div>`;
    this.categories.forEach(cat => {
      html += `<div class="cat-item" data-id="${cat.id}">${cat.name}</div>`;
    });
    container.innerHTML = html;
    const catItems = document.querySelectorAll('.cat-item');
    catItems.forEach(item => {
      item.addEventListener('click', (e) => {
        catItems.forEach(c => c.classList.remove('active'));
        e.currentTarget.classList.add('active');
        const catId = e.currentTarget.dataset.id;
        this.loadItems(catId || null);
      });
    });
  }

  renderCategorySelect() {
    const select = document.getElementById('addItemCategory');
    if (!select) return;
    let html = '';
    this.categories.forEach(cat => {
      html += `<option value="${cat.id}">${cat.name}</option>`;
    });
    select.innerHTML = html;
  }

  renderItems() {
    const grid = document.getElementById('menuItemsGrid');
    if (!grid) return;
    if (!this.items || this.items.length === 0) {
      grid.innerHTML = '<div class="text-muted p-4">No menu items found. Add some items to get started!</div>';
      return;
    }
    let html = '';
    this.items.forEach(item => {
      const img = item.image
        ? `<img src="${item.image}" alt="${item.name}" loading="lazy">`
        : `<div class="menu-img-placeholder"><i data-lucide="utensils"></i></div>`;
      const isChecked = item.available ? 'checked' : '';
      html += `
        <div class="menu-item-admin">
          <div class="menu-img-wrapper">
              ${img}
          </div>
          <div class="menu-details">
              <div class="menu-title-row">
                  <div class="menu-title">${item.name}
                      ${item.tags && item.tags.includes('Popular') ? '<span class="badge" style="background:#fef08a;color:#854d0e;font-size:0.65rem;margin-left:4px;"><i data-lucide="star" style="width:10px;height:10px;display:inline;margin-right:2px"></i>Popular</span>' : ''}
                      ${item.tags && item.tags.includes('Best Seller') ? '<span class="badge" style="background:#fed7aa;color:#9a3412;font-size:0.65rem;margin-left:4px;"><i data-lucide="award" style="width:10px;height:10px;display:inline;margin-right:2px"></i>Best Seller</span>' : ''}
                  </div>
                  <div class="menu-price">₹${item.price}</div>
              </div>
              <div class="menu-desc">${item.description || '<span class="text-muted" style="font-style:italic">No description</span>'}</div>
          </div>
          <div class="menu-footer">
              <label class="toggle-switch" title="${item.available ? 'Available' : 'Unavailable'}">
                  <input type="checkbox" ${isChecked} onchange="menuApp.toggleAvailability('${item.id}')">
                  <span class="slider"></span>
              </label>
              <div class="flex gap-2">
                  <button class="btn-icon" title="Edit item" onclick="menuApp.editItem('${item.id}')"><i data-lucide="pencil"></i></button>
                  <button class="btn-icon text-danger" title="Delete item" onclick="menuApp.deleteItem('${item.id}')"><i data-lucide="trash-2"></i></button>
              </div>
          </div>
        </div>
      `;
    });
    grid.innerHTML = html;
    if (window.lucide) lucide.createIcons();
  }


  // ─── Search ───────────────────────────────────────────────────

  setupSearch() {
    const searchInput = document.querySelector('input[placeholder="Search menu..."]');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase();
        const items = document.querySelectorAll('.menu-item-admin');
        items.forEach(item => {
          const title = item.querySelector('.menu-title').innerText.toLowerCase();
          item.style.display = title.includes(query) ? 'block' : 'none';
        });
      });
    }
  }

  // ─── Add / Edit Modal ─────────────────────────────────────────

  setupAddModal() {
    const btn = document.getElementById('saveItemBtn');
    if (btn) btn.addEventListener('click', () => this.saveItem());

    const fileInput = document.getElementById('itemImageFile');
    if (fileInput) {
      fileInput.addEventListener('change', async (e) => {
        if (e.target.files && e.target.files[0]) {
          const file = e.target.files[0];

          // Show local preview immediately
          const reader = new FileReader();
          reader.onload = (ev) => {
            const preview = document.getElementById('imagePreview');
            const wrap = document.getElementById('imagePreviewWrap');
            const container = document.getElementById('imagePreviewContainer');
            preview.src = ev.target.result;
            if (wrap) wrap.style.display = 'block';
            if (container) container.style.display = 'none';
          };
          reader.readAsDataURL(file);

          // Upload to Cloudinary via backend
          this._uploadImageFile(file);
        }
      });
    }

    // Patch openModal only ONCE. Only reset form when user opens "Add Item" explicitly
    // (NOT when editItem() calls openModal, which is flagged by _isEditing).
    const origOpen = window.openModal;
    if (!window.customModalPatched) {
      window.openModal = (id) => {
        if (id === 'add-item-modal' && !this._isEditing) {
          document.getElementById('modalTitle').innerText = 'Add Menu Item';
          document.getElementById('editItemId').value = '';
          this.resetForm();
          this.loadCustomizationGroups();
        }
        origOpen(id);
      };
      window.customModalPatched = true;
    }
  }

  async _uploadImageFile(file) {
    const btn = document.getElementById('saveItemBtn');
    const uploadHint = document.getElementById('imageUploadHint');
    if (uploadHint) uploadHint.textContent = 'Uploading…';
    if (btn) btn.disabled = true;

    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', 'menu');
    try {
      const uploadRes = await fetch('/api/v1/images/upload', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + api.token },
        body: formData
      });
      const resJson = await uploadRes.json();
      if (resJson.success) {
        document.getElementById('uploadedImageUrl').value = resJson.data.url;
        if (uploadHint) uploadHint.textContent = '✓ Image uploaded';
        if (window.showToast) showToast('Image uploaded successfully', 'success');
      } else {
        if (uploadHint) uploadHint.textContent = 'Upload failed — image won\'t be saved';
        if (window.showToast) showToast('Image upload failed: ' + (resJson.message || ''), 'error');
      }
    } catch (e) {
      console.error('Image upload failed', e);
      if (uploadHint) uploadHint.textContent = 'Upload error';
      if (window.showToast) showToast('Failed to upload image', 'error');
    } finally {
      if (btn) btn.disabled = false;
    }
  }

  resetForm() {
    document.getElementById('addItemName').value = '';
    document.getElementById('addItemPrice').value = '';
    document.getElementById('addItemDesc').value = '';
    document.getElementById('uploadedImageUrl').value = '';
    this._clearImage();
    document.getElementById('customizationsList').innerHTML = '';
    const checkboxes = document.querySelectorAll('input[name="saved_customizations"]');
    checkboxes.forEach(c => c.checked = false);
    const pop = document.getElementById('addItemPopular');
    const bs = document.getElementById('addItemBestSeller');
    if(pop) pop.checked = false;
    if(bs) bs.checked = false;
  }

  _clearImage() {
    const preview = document.getElementById('imagePreview');
    const wrap = document.getElementById('imagePreviewWrap');
    const container = document.getElementById('imagePreviewContainer');
    const hint = document.getElementById('imageUploadHint');
    const fileInput = document.getElementById('itemImageFile');
    if (preview) { preview.src = ''; }
    if (wrap) wrap.style.display = 'none';
    if (container) container.style.display = 'block';
    if (hint) hint.textContent = '';
    if (fileInput) fileInput.value = '';
    document.getElementById('uploadedImageUrl').value = '';
  }


  editItem(id) {
    const item = this.items.find(i => i.id === id);
    if (!item) return;

    // Set flag so the openModal patch does NOT reset the form we're about to fill
    this._isEditing = true;

    document.getElementById('modalTitle').innerText = 'Edit Menu Item';
    document.getElementById('editItemId').value = item.id;
    document.getElementById('addItemName').value = item.name || '';
    document.getElementById('addItemPrice').value = item.price || '';
    document.getElementById('addItemDesc').value = item.description || '';
    document.getElementById('customizationsList').innerHTML = '';

    if (item.image) {
      document.getElementById('uploadedImageUrl').value = item.image;
      document.getElementById('imagePreview').src = item.image;
      const wrap = document.getElementById('imagePreviewWrap');
      const container = document.getElementById('imagePreviewContainer');
      if (wrap) wrap.style.display = 'block';
      if (container) container.style.display = 'none';
    } else {
      this._clearImage();
    }
    const hint = document.getElementById('imageUploadHint');
    if (hint) hint.textContent = '';
    
    const tags = item.tags || [];
    const pop = document.getElementById('addItemPopular');
    const bs = document.getElementById('addItemBestSeller');
    if(pop) pop.checked = tags.includes('Popular');
    if(bs) bs.checked = tags.includes('Best Seller');

    // Load customizations then tick the right ones
    this.loadCustomizationGroups().then(() => {
      const checkboxes = document.querySelectorAll('input[name="saved_customizations"]');
      const attachedIds = item.customizationGroupIds || item.customizationGroups || [];
      checkboxes.forEach(c => { c.checked = attachedIds.includes(c.value); });
    });

    window.openModal('add-item-modal');

    // Set category AFTER modal opens (select DOM is ready)
    requestAnimationFrame(() => {
      const catSelect = document.getElementById('addItemCategory');
      if (catSelect) catSelect.value = item.categoryId || '';
      this._isEditing = false; // reset flag
    });
  }


  async loadCustomizationGroups() {
    try {
      const res = await api.get('/customizations');
      const list = document.getElementById('savedCustomizationsList');
      if (list) {
        if (res.success && res.data.length > 0) {
          let html = '';
          res.data.forEach(g => {
            html += `<label class="flex items-center gap-2 cursor-pointer p-2 border border-border rounded-md hover:bg-surface">
              <input type="checkbox" name="saved_customizations" value="${g.id}">
              <span>${g.name} <small class="text-muted">(${g.options ? g.options.length : 0} options)</small></span>
            </label>`;
          });
          list.innerHTML = html;
        } else {
          list.innerHTML = '<span class="text-muted text-sm italic">No saved customizations found.</span>';
        }
      }
    } catch (e) {
      console.error('Failed to load customizations', e);
    }
  }

  async saveItem() {
    const editId = document.getElementById('editItemId').value;
    const name = document.getElementById('addItemName').value;
    const price = document.getElementById('addItemPrice').value;
    const categoryId = document.getElementById('addItemCategory').value;
    const desc = document.getElementById('addItemDesc').value;
    const imageUrl = document.getElementById('uploadedImageUrl').value;

    if (!name || !price || !categoryId) {
      if (window.showToast) showToast('Please fill all required fields', 'error');
      return;
    }

    const rows = document.querySelectorAll('.custom-option-row');
    const options = [];
    rows.forEach(row => {
      const optName = row.querySelector('.custom-opt-name').value;
      const optPrice = row.querySelector('.custom-opt-price').value;
      if (optName) options.push({ name: optName, additionalPrice: parseFloat(optPrice || 0) });
    });

    const btn = document.getElementById('saveItemBtn');
    btn.disabled = true;
    btn.innerText = 'Saving...';

    try {
      let customizationGroupIds = [];
      const savedChecks = document.querySelectorAll('input[name="saved_customizations"]:checked');
      savedChecks.forEach(c => customizationGroupIds.push(c.value));

      if (options.length > 0) {
        const groupRes = await api.post('/customizations', {
          name: 'Options for ' + name,
          selectionType: 'MULTIPLE',
          required: false,
          minSelection: 0,
          maxSelection: options.length,
          options: options
        });
        if (groupRes.success) customizationGroupIds.push(groupRes.data.id);
      }
      
      const tags = [];
      const pop = document.getElementById('addItemPopular');
      const bs = document.getElementById('addItemBestSeller');
      if(pop && pop.checked) tags.push('Popular');
      if(bs && bs.checked) tags.push('Best Seller');

      const payload = { name, price: parseFloat(price), categoryId, description: desc, customizationGroupIds, tags };
      if (imageUrl) payload.image = imageUrl;

      let res;
      if (editId) {
        res = await api.put(`/menu/items/${editId}`, payload);
      } else {
        res = await api.post('/menu/items', payload);
      }

      if (res.success) {
        if (window.showToast) showToast(editId ? 'Item Updated Successfully' : 'Item Added Successfully', 'success');
        if (window.closeModal) closeModal('add-item-modal');
        this.resetForm();
        await this.loadItems();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to save item', 'error');
    } finally {
      btn.disabled = false;
      btn.innerText = 'Save Item';
    }
  }

  async toggleAvailability(id) {
    try {
      await api.patch(`/menu/items/${id}/availability`, {});
      if (window.showToast) showToast('Availability updated', 'success');
    } catch (e) {
      if (window.showToast) showToast('Failed to update availability', 'error');
      await this.loadItems();
    }
  }

  addCustomizationField() {
    const list = document.getElementById('customizationsList');
    if (!list) return;
    const div = document.createElement('div');
    div.className = 'flex items-center gap-2 custom-option-row';
    div.innerHTML = `
      <input type="text" class="form-control flex-1 custom-opt-name" placeholder="Option name (e.g. Extra Cheese)">
      <input type="number" class="form-control custom-opt-price" placeholder="+ Price" style="width: 100px;">
      <button type="button" class="btn-icon text-danger" onclick="this.parentElement.remove()"><i data-lucide="trash-2"></i></button>
    `;
    list.appendChild(div);
    if (window.lucide) lucide.createIcons();
  }

  async deleteItem(id) {
    if (!confirm('Are you sure you want to delete this item?')) return;
    try {
      await api.delete(`/menu/items/${id}`);
      if (window.showToast) showToast('Item deleted', 'success');
      await this.loadItems();
    } catch (e) {
      if (window.showToast) showToast('Failed to delete item', 'error');
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // AI MENU EXTRACTION — Two-step flow
  // ═══════════════════════════════════════════════════════════════

  setupAiExtraction() {
    const fileInput = document.getElementById('aiMenuImageFile');
    const dropZone  = document.getElementById('aiDropZone');
    if (!fileInput || !dropZone) return;

    // File input change
    fileInput.addEventListener('change', (e) => {
      if (e.target.files && e.target.files[0]) this._setAiFile(e.target.files[0]);
    });

    // Click on drop zone to trigger file picker (only when no file yet)
    dropZone.addEventListener('click', (e) => {
      if (dropZone.classList.contains('has-file')) return;
      if (e.target.closest('button')) return;
      fileInput.click();
    });

    // Drag-and-drop
    dropZone.addEventListener('dragover', (e) => {
      e.preventDefault();
      dropZone.classList.add('dragover');
    });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
    dropZone.addEventListener('drop', (e) => {
      e.preventDefault();
      dropZone.classList.remove('dragover');
      const file = e.dataTransfer.files[0];
      if (file && file.type.startsWith('image/')) {
        this._setAiFile(file);
      } else {
        if (window.showToast) showToast('Please drop an image file', 'error');
      }
    });
  }

  _setAiFile(file) {
    if (file.size > 10 * 1024 * 1024) {
      if (window.showToast) showToast('Image must be under 10 MB', 'error');
      return;
    }
    this._aiFile = file;

    const reader = new FileReader();
    reader.onload = (e) => { document.getElementById('aiImagePreview').src = e.target.result; };
    reader.readAsDataURL(file);

    document.getElementById('aiFileName').textContent = `${file.name} (${(file.size / 1024).toFixed(0)} KB)`;
    document.getElementById('aiUploadPrompt').style.display = 'none';
    document.getElementById('aiImagePreviewWrap').style.display = 'block';
    document.getElementById('aiDropZone').classList.add('has-file');
    document.getElementById('aiExtractBtn').disabled = false;
  }

  resetAiUpload() {
    this._aiFile = null;
    this._aiPreviewData = null;
    document.getElementById('aiMenuImageFile').value = '';
    document.getElementById('aiUploadPrompt').style.display = 'block';
    document.getElementById('aiImagePreviewWrap').style.display = 'none';
    document.getElementById('aiDropZone').classList.remove('has-file');
    document.getElementById('aiExtractBtn').disabled = true;
  }

  _showAiLoading(text) {
    document.getElementById('aiLoadingText').textContent = text || 'Analyzing your menu with AI...';
    document.getElementById('aiLoadingOverlay').style.display = 'flex';
  }

  _hideAiLoading() {
    document.getElementById('aiLoadingOverlay').style.display = 'none';
  }

  _goToStep(stepNum) {
    const s1       = document.getElementById('ai-step-1');
    const s1footer = document.getElementById('ai-step-1-footer');
    const s2       = document.getElementById('ai-step-2');
    const ind1     = document.getElementById('step-indicator-1');
    const ind2     = document.getElementById('step-indicator-2');
    const line     = document.querySelector('.ai-step-line');

    if (stepNum === 1) {
      s1.style.display = 'block';
      s1footer.style.display = 'flex';
      s2.style.display = 'none';
      ind1.className = 'ai-step active';
      ind2.className = 'ai-step';
      if (line) line.className = 'ai-step-line';
    } else {
      s1.style.display = 'none';
      s1footer.style.display = 'none';
      s2.style.display = 'block';
      ind1.className = 'ai-step done';
      ind2.className = 'ai-step active';
      if (line) line.className = 'ai-step-line done';
    }
    if (window.lucide) lucide.createIcons();
  }

  /** Step 1: send image to backend → Gemini → get preview */
  async extractMenuFromImage() {
    if (!this._aiFile) return;

    this._showAiLoading('Analyzing your menu with AI...');

    const formData = new FormData();
    formData.append('image', this._aiFile);

    try {
      const response = await fetch('/api/v1/menu/extract-from-image', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + api.token },
        body: formData
      });
      const result = await response.json();
      this._hideAiLoading();

      if (!result.success) {
        if (window.showToast) showToast(result.message || 'Extraction failed', 'error');
        return;
      }

      this._aiPreviewData = result.data;
      this._renderPreviewStep(result.data);
      this._goToStep(2);

    } catch (e) {
      this._hideAiLoading();
      console.error('AI extraction error', e);
      if (window.showToast) showToast('Network error during AI extraction', 'error');
    }
  }

  /** Populates Step 2: summary banner, category filter, preview table */
  _renderPreviewStep(data) {
    document.getElementById('aiSummaryText').textContent =
      `${data.totalItems} item${data.totalItems !== 1 ? 's' : ''} detected`;
    document.getElementById('aiSummarySubtext').textContent =
      data.summary || `Across ${(data.detectedCategories || []).length} categories`;

    const catFilter = document.getElementById('aiCategoryFilter');
    catFilter.innerHTML = '<option value="">All categories</option>';
    (data.detectedCategories || []).forEach(cat => {
      const opt = document.createElement('option');
      opt.value = cat;
      opt.textContent = cat;
      catFilter.appendChild(opt);
    });

    this._renderPreviewTable(data.items, '');
  }

  _renderPreviewTable(items, filterCat) {
    const wrap = document.getElementById('aiPreviewTable');
    if (!items || items.length === 0) {
      wrap.innerHTML = '<p class="text-muted p-4 text-center">No items to show.</p>';
      return;
    }
    const filtered = filterCat ? items.filter(i => i.categoryName === filterCat) : items;
    let rows = '';
    filtered.forEach(item => {
      // Find original index to update data
      const origIndex = this._aiPreviewData.items.indexOf(item);
      const imgPreview = item.imageUrl 
          ? `<img src="${item.imageUrl}" style="width:40px;height:40px;object-fit:cover;border-radius:4px;">` 
          : `<i data-lucide="image" style="width:20px;height:20px;color:var(--text-muted);"></i>`;

      rows += `
        <tr data-index="${origIndex}">
          <td style="width:50px; text-align:center;">
              <div class="ai-item-img-upload" onclick="menuApp._triggerAiItemImageUpload(${origIndex})" style="cursor:pointer; width:40px; height:40px; background:var(--surface-hover); border:1px dashed var(--border); border-radius:4px; display:flex; align-items:center; justify-content:center; overflow:hidden;" title="Upload Image">
                  ${imgPreview}
              </div>
          </td>
          <td>
            <input type="text" class="form-control" style="padding:0.25rem 0.5rem; min-height:30px; font-weight:600; font-size:0.9rem;" value="${this._esc(item.name || '')}" onchange="menuApp._updateAiItem(${origIndex}, 'name', this.value)" placeholder="Item name">
            <textarea class="form-control" style="padding:0.25rem 0.5rem; min-height:40px; font-size:0.8rem; margin-top:0.25rem; color:var(--text-muted);" onchange="menuApp._updateAiItem(${origIndex}, 'description', this.value)" placeholder="Description">${this._esc(item.description || '')}</textarea>
          </td>
          <td style="width:100px;">
             <input type="number" class="form-control" style="padding:0.25rem 0.5rem; min-height:30px; font-size:0.9rem;" value="${item.price || 0}" onchange="menuApp._updateAiItem(${origIndex}, 'price', this.value)">
          </td>
          <td style="width:120px;">
             <input type="text" class="form-control" style="padding:0.25rem 0.5rem; min-height:30px; font-size:0.85rem;" value="${this._esc(item.categoryName || 'General')}" onchange="menuApp._updateAiItem(${origIndex}, 'categoryName', this.value)">
          </td>
        </tr>`;
    });
    wrap.innerHTML = `
      <table class="ai-preview-table">
        <thead><tr><th>Img</th><th>Item & Description</th><th>Price</th><th>Category</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <input type="file" id="aiItemImageUploadInput" accept="image/*" style="display:none;" onchange="menuApp._handleAiItemImageUpload(event)">
    `;
    if (window.lucide) lucide.createIcons();
  }

  _updateAiItem(index, field, value) {
      if (!this._aiPreviewData || !this._aiPreviewData.items[index]) return;
      this._aiPreviewData.items[index][field] = value;
  }

  _triggerAiItemImageUpload(index) {
      this._currentAiItemIndex = index;
      document.getElementById('aiItemImageUploadInput').click();
  }

  async _handleAiItemImageUpload(event) {
      const file = event.target.files[0];
      if (!file) return;
      const index = this._currentAiItemIndex;
      if (index === undefined || index === null) return;

      const formData = new FormData();
      formData.append('image', file);
      
      try {
          const response = await fetch('/api/v1/images/upload', {
              method: 'POST',
              headers: { 'Authorization': 'Bearer ' + api.token },
              body: formData
          });
          const result = await response.json();
          if (result.success) {
              this._aiPreviewData.items[index].imageUrl = result.data;
              // Re-render table while preserving focus might be tricky, so let's just re-render fully 
              // (focus will be lost, but since they just uploaded an image via file picker, focus was already lost)
              this.filterPreviewItems(); 
          } else {
              if (window.showToast) showToast(result.message || 'Image upload failed', 'error');
          }
      } catch (e) {
          console.error(e);
          if (window.showToast) showToast('Failed to upload image', 'error');
      } finally {
          event.target.value = '';
      }
  }

  filterPreviewItems() {
    if (!this._aiPreviewData) return;
    const cat = document.getElementById('aiCategoryFilter').value;
    this._renderPreviewTable(this._aiPreviewData.items, cat);
  }

  goBackToUpload() {
    this._goToStep(1);
  }

  /** Step 2: confirm session → persist to DB */
  async confirmExtraction() {
    if (!this._aiPreviewData || !this._aiPreviewData.extractionSessionId) return;

    const btn = document.getElementById('aiConfirmBtn');
    btn.disabled = true;
    btn.innerHTML = '<i data-lucide="loader-2"></i> Saving...';
    if (window.lucide) lucide.createIcons();

    this._showAiLoading('Saving menu items to database...');

    try {
      const res = await api.post('/menu/extract-from-image/confirm', {
        extractionSessionId: this._aiPreviewData.extractionSessionId,
        items: this._aiPreviewData.items
      });
      this._hideAiLoading();

      if (res.success) {
        const d = res.data;
        if (window.showToast) showToast(
          `✓ ${d.itemsCreated} items saved · ${d.categoriesCreated} new categories · ${d.categoriesReused} reused`,
          'success'
        );
        closeModal('ai-extract-modal');
        this.resetAiUpload();
        this._aiPreviewData = null;
        this._goToStep(1);
        await this.loadCategories();
        await this.loadItems();
      } else {
        if (window.showToast) showToast(res.message || 'Failed to save', 'error');
      }
    } catch (e) {
      this._hideAiLoading();
      console.error('Confirm extraction error', e);
      if (window.showToast) showToast('Network error while saving', 'error');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i data-lucide="save"></i> Save to Menu';
      if (window.lucide) lucide.createIcons();
    }
  }

  /** Escape HTML to prevent XSS in dynamically rendered strings */
  _esc(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.menuApp = new Menu();
});
