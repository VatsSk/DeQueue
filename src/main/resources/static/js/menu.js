class Menu {
  constructor() {
    this.categories = [];
    this.items = [];
    this.init();
  }

  async init() {
    this.setupSearch();
    this.setupAddModal();
    await this.loadCategories();
    await this.loadItems();
  }

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
        // Handle both paginated response and list response
        this.items = res.data.content ? res.data.content : res.data;
        this.renderItems();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to load menu items', 'error');
    }
  }

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
      const img = item.image || 'https://images.unsplash.com/photo-1544148103-0773bf10d330?w=500&q=80';
      const isChecked = item.available ? 'checked' : '';
      html += `
        <div class="menu-item-admin">
          <div class="menu-img-wrapper">
              <img src="${img}" alt="${item.name}">
          </div>
          <div class="menu-details">
              <div class="menu-title-row">
                  <div class="menu-title">${item.name}</div>
                  <div class="menu-price">₹${item.price}</div>
              </div>
              <div class="menu-desc">${item.description || ''}</div>
          </div>
          <div class="menu-footer">
              <label class="toggle-switch">
                  <input type="checkbox" ${isChecked} onchange="menuApp.toggleAvailability('${item.id}')">
                  <span class="slider"></span>
              </label>
              <div class="flex gap-2">
                  <button class="btn-icon text-danger" onclick="menuApp.deleteItem('${item.id}')"><i data-lucide="trash-2"></i></button>
              </div>
          </div>
        </div>
      `;
    });
    grid.innerHTML = html;
    if (window.lucide) lucide.createIcons();
  }

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

  setupAddModal() {
    const btn = document.getElementById('saveItemBtn');
    if (btn) {
      btn.addEventListener('click', () => this.saveItem());
    }
  }

  async saveItem() {
    const name = document.getElementById('addItemName').value;
    const price = document.getElementById('addItemPrice').value;
    const categoryId = document.getElementById('addItemCategory').value;
    const desc = document.getElementById('addItemDesc').value;

    if (!name || !price || !categoryId) {
        if (window.showToast) showToast('Please fill all required fields', 'error');
        return;
    }

    const rows = document.querySelectorAll('.custom-option-row');
    const options = [];
    rows.forEach(row => {
        const optName = row.querySelector('.custom-opt-name').value;
        const optPrice = row.querySelector('.custom-opt-price').value;
        if (optName) {
            options.push({
                name: optName,
                additionalPrice: parseFloat(optPrice || 0)
            });
        }
    });

    const btn = document.getElementById('saveItemBtn');
    btn.disabled = true;
    btn.innerText = 'Saving...';

    try {
      let customizationGroupIds = [];
      if (options.length > 0) {
          const groupRes = await api.post('/menu/customizations', {
              name: 'Options for ' + name,
              selectionType: 'MULTIPLE',
              required: false,
              minSelection: 0,
              maxSelection: options.length,
              options: options
          });
          if (groupRes.success) {
              customizationGroupIds.push(groupRes.data.id);
          }
      }

      const res = await api.post('/menu/items', {
        name: name,
        price: parseFloat(price),
        categoryId: categoryId,
        description: desc,
        customizationGroupIds: customizationGroupIds
      });
      
      if (res.success) {
        if (window.showToast) showToast('Item Added Successfully', 'success');
        if (window.closeModal) closeModal('add-item-modal');
        // Clear form
        document.getElementById('addItemName').value = '';
        document.getElementById('addItemPrice').value = '';
        document.getElementById('addItemDesc').value = '';
        document.getElementById('customizationsList').innerHTML = '';
        
        await this.loadItems();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to add item', 'error');
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
      await this.loadItems(); // rollback visually
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
}

document.addEventListener('DOMContentLoaded', () => {
  window.menuApp = new Menu();
});
