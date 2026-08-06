class Categories {
  constructor() {
    this.categories = [];
    this.init();
  }

  async init() {
    console.log('Categories initialized');
    this.setupModal();
    this.setupSearch();
    await this.loadCategories();
  }

  async loadCategories() {
    try {
      const res = await api.get('/categories');
      if (res.success) {
        this.categories = res.data;
        this.renderCategories();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to load categories', 'error');
    }
  }

  renderCategories(filter = '') {
    const list = document.querySelector('.category-list');
    if (!list) return;

    if (this.categories.length === 0) {
      list.innerHTML = '<div class="text-muted p-4 text-center">No categories found. Click "Add Category" to create one.</div>';
      return;
    }

    let html = '';
    const filtered = this.categories.filter(c => c.name.toLowerCase().includes(filter.toLowerCase()));
    
    filtered.forEach(cat => {
      html += `
        <div class="category-row">
            <div class="flex items-center">
                <i data-lucide="grip-vertical" class="drag-handle"></i>
                <div>
                    <h3 class="font-bold text-lg">${cat.name}</h3>
                    <span class="text-sm text-muted">ID: ${cat.id.substring(0, 8)}...</span>
                </div>
            </div>
            <div class="flex gap-2">
                <button class="btn-icon text-danger" onclick="categoriesApp.deleteCategory('${cat.id}')"><i data-lucide="trash-2"></i></button>
            </div>
        </div>
      `;
    });
    
    list.innerHTML = html;
    if (window.lucide) lucide.createIcons();
  }

  setupSearch() {
    const searchInput = document.querySelector('input[placeholder="Search categories..."]');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.renderCategories(e.target.value);
      });
    }
  }

  setupModal() {
    const modalFooter = document.querySelector('#add-category-modal-overlay .modal-footer');
    if (modalFooter) {
      // Replace the hardcoded save button with dynamic one to avoid duplicate listeners if called multiple times
      modalFooter.innerHTML = `
          <button class="btn btn-secondary" onclick="closeModal('add-category-modal')">Cancel</button>
          <button class="btn btn-primary" id="saveCategoryBtn">Save</button>
      `;
      document.getElementById('saveCategoryBtn').addEventListener('click', () => this.saveCategory());
    }
  }

  async saveCategory() {
    const input = document.querySelector('#add-category-modal-overlay input[type="text"]');
    const name = input.value.trim();

    if (!name) {
        if (window.showToast) showToast('Category name is required', 'error');
        return;
    }

    const btn = document.getElementById('saveCategoryBtn');
    btn.disabled = true;
    btn.innerText = 'Saving...';

    try {
      const res = await api.post('/categories', { name: name });
      if (res.success) {
        if (window.showToast) showToast('Category Added Successfully', 'success');
        if (window.closeModal) closeModal('add-category-modal');
        input.value = '';
        await this.loadCategories();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to add category', 'error');
    } finally {
      btn.disabled = false;
      btn.innerText = 'Save';
    }
  }

  async deleteCategory(id) {
    if (!confirm('Are you sure you want to delete this category?')) return;
    try {
      const res = await api.delete('/categories/' + id);
      if (res.success || res.status === 200 || res.status === 204) {
        if (window.showToast) showToast('Category deleted', 'success');
        await this.loadCategories();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to delete category', 'error');
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.categoriesApp = new Categories();
});
