// Customer App Logic - DeQueue Customer Ordering System
class CustomerApp {
  constructor() {
    this.cart = [];
    this.vendorCode = this.getVendorCodeFromUrl();
    this.vendor = null;
    this.menu = null;
    this.activeOrder = null;
    this.pollingInterval = null;
    this.init();
  }

  getVendorCodeFromUrl() {
    const path = window.location.pathname;
    const match = path.match(/\/v\/([^\/]+)/);
    if (match) return match[1];
    
    const params = new URLSearchParams(window.location.search);
    return params.get('vendor') || params.get('v');
  }

  async init() {
    if (!this.vendorCode) {
      this.showError('Invalid vendor link. Please scan the QR code again.');
      return;
    }

    // Check for active order in localStorage
    const stored = localStorage.getItem(`dequeue_order_${this.vendorCode}`);
    if (stored) {
      try {
        this.activeOrder = JSON.parse(stored);
        if (this.activeOrder && this.activeOrder.queueNumber) {
          await this.checkExistingOrder();
          return;
        }
      } catch (e) { /* ignore parse errors */ }
    }

    await this.loadVendor();
  }

  async loadVendor() {
    try {
      const res = await fetch(`/api/v1/public/vendors/${this.vendorCode}`);
      const data = await res.json();
      
      if (!data.success) {
        this.showError(data.message || 'Vendor not found');
        return;
      }

      this.vendor = data.data;
      document.title = `${this.vendor.shopName} - Order | DeQueue`;

      // Update header
      const shopName = document.querySelector('.shop-info h1');
      if (shopName) shopName.textContent = this.vendor.shopName;

      if (this.vendor.logo) {
        const logoEl = document.querySelector('.shop-logo');
        if (logoEl) logoEl.innerHTML = `<img src="${this.vendor.logo}" alt="Logo" style="width:40px;height:40px;border-radius:50%;object-fit:cover;">`;
      }

      if (this.vendor.shopStatus !== 'OPEN') {
        this.showClosed();
        return;
      }

      const statusEl = document.querySelector('.shop-status');
      if (statusEl) {
        statusEl.className = 'shop-status open';
        statusEl.textContent = 'Accepting Orders';
      }

      await this.loadMenu();
    } catch (err) {
      this.showError('Unable to load shop information. Please try again.');
      console.error(err);
    }
  }

  async loadMenu() {
    try {
      const res = await fetch(`/api/v1/public/menu/${this.vendorCode}/categories`);
      const data = await res.json();

      if (!data.success) {
        this.showError('Unable to load menu');
        return;
      }

      this.menu = data.data;
      this.renderCategories();
      this.renderMenuItems();
    } catch (err) {
      this.showError('Unable to load menu. Please try again.');
      console.error(err);
    }
  }

  renderCategories() {
    const container = document.querySelector('.category-pills');
    if (!container || !this.menu || !this.menu.categories) return;

    container.innerHTML = `<button class="category-pill active" data-category="all">All</button>`;
    this.menu.categories.forEach(cat => {
      container.innerHTML += `<button class="category-pill" data-category="${cat.id}">${cat.name}</button>`;
    });

    container.querySelectorAll('.category-pill').forEach(pill => {
      pill.addEventListener('click', (e) => {
        container.querySelectorAll('.category-pill').forEach(p => p.classList.remove('active'));
        e.target.classList.add('active');
        this.filterByCategory(e.target.dataset.category);
      });
    });
  }

  renderMenuItems(categoryId = 'all') {
    const grid = document.querySelector('.menu-grid');
    if (!grid || !this.menu) return;

    let items = [];
    if (this.menu.categories) {
      this.menu.categories.forEach(cat => {
        if (cat.items) {
          cat.items.forEach(item => {
            // Treat missing flags as true (robust against legacy DB entries)
            const isAvailable = item.available !== false;
            const isVisible = item.visible !== false;
            if (isAvailable && isVisible) {
              items.push({ ...item, categoryId: cat.id, categoryName: cat.name });
            }
          });
        }
      });
    }

    if (categoryId !== 'all') {
      items = items.filter(i => i.categoryId === categoryId);
    }

    if (items.length === 0) {
      grid.innerHTML = `<div class="text-center text-muted py-8" style="grid-column: 1/-1;">No items available</div>`;
      return;
    }

    grid.innerHTML = items.map(item => `
      <div class="menu-item-card" data-item-id="${item.id}">
        <div class="item-info">
          <div class="item-title">${item.name}</div>
          <div class="item-desc">${item.description || ''}</div>
          ${item.preparationTime ? `<div class="text-xs text-muted mt-1"><i data-lucide="clock" style="width:12px;height:12px;display:inline"></i> ~${item.preparationTime} min</div>` : ''}
          <div class="flex items-center justify-between mt-auto pt-2">
            <div class="item-price">${this.formatPrice(item.price)}</div>
            <button class="btn btn-primary add-to-cart-btn" style="padding: 0.25rem 0.75rem; min-height: 32px; font-size: 0.85rem;"
              onclick="customerApp.handleAddToCart('${item.id}')">Add</button>
          </div>
        </div>
        ${item.image ? `<img src="${item.image}" alt="${item.name}" class="item-image" loading="lazy">` : 
          `<div class="item-image" style="background:var(--surface);display:flex;align-items:center;justify-content:center;"><i data-lucide="utensils" style="opacity:0.3"></i></div>`}
      </div>
    `).join('');

    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  filterByCategory(categoryId) {
    this.renderMenuItems(categoryId);
  }

  getItemById(itemId) {
    if (!this.menu || !this.menu.categories) return null;
    for (const cat of this.menu.categories) {
      if (cat.items) {
        const item = cat.items.find(i => i.id === itemId);
        if (item) return item;
      }
    }
    return null;
  }

  handleAddToCart(itemId) {
    const item = this.getItemById(itemId);
    if (!item) return;

    // Check if item has customization groups
    if (item.customizationGroups && item.customizationGroups.length > 0) {
      this.showCustomizationModal(item);
    } else {
      this.addToCart(item, []);
    }
  }

  showCustomizationModal(item) {
    // Simple customization selection - can be enhanced later
    this.addToCart(item, []);
  }

  addToCart(item, customizations = []) {
    const cartItem = {
      menuItemId: item.id,
      menuItemName: item.name,
      quantity: 1,
      unitPrice: item.price,
      totalPrice: item.price,
      customizations: customizations,
      cartId: Date.now()
    };

    // Check if same item (without customizations) already in cart
    const existing = this.cart.find(c => c.menuItemId === item.id && customizations.length === 0);
    if (existing) {
      existing.quantity++;
      existing.totalPrice = existing.unitPrice * existing.quantity;
    } else {
      this.cart.push(cartItem);
    }

    this.updateCartUI();
    if (typeof showToast === 'function') showToast(`Added ${item.name} to cart`, 'success');
  }

  removeFromCart(cartId) {
    this.cart = this.cart.filter(c => c.cartId !== cartId);
    this.updateCartUI();
    this.renderCartModal();
  }

  updateCartUI() {
    const cartBtn = document.getElementById('floating-cart');
    if (!cartBtn) return;

    const totalItems = this.cart.reduce((sum, item) => sum + item.quantity, 0);
    const totalPrice = this.cart.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0);

    if (totalItems > 0) {
      document.getElementById('cart-count').innerText = `${totalItems} Item${totalItems > 1 ? 's' : ''}`;
      document.getElementById('cart-total').innerText = this.formatPrice(totalPrice);
      cartBtn.classList.add('visible');
    } else {
      cartBtn.classList.remove('visible');
    }
  }

  renderCartModal() {
    const body = document.querySelector('#cart-modal-overlay .modal-body');
    if (!body) return;

    if (this.cart.length === 0) {
      body.innerHTML = `<p class="text-muted text-center py-4">Your cart is empty</p>`;
      return;
    }

    body.innerHTML = `
      <div class="cart-items">
        ${this.cart.map(item => `
          <div class="flex items-center justify-between py-3 border-b border-border">
            <div class="flex-1">
              <div class="font-medium">${item.menuItemName}</div>
              <div class="text-sm text-muted">${this.formatPrice(item.unitPrice)} × ${item.quantity}</div>
            </div>
            <div class="flex items-center gap-3">
              <span class="font-bold">${this.formatPrice(item.unitPrice * item.quantity)}</span>
              <button class="btn-icon text-danger" onclick="customerApp.removeFromCart(${item.cartId})" style="padding:4px;">
                <i data-lucide="trash-2" style="width:16px;height:16px;"></i>
              </button>
            </div>
          </div>
        `).join('')}
      </div>
      <div class="flex items-center justify-between py-4 font-bold text-lg">
        <span>Total</span>
        <span>${this.formatPrice(this.cart.reduce((s, i) => s + i.unitPrice * i.quantity, 0))}</span>
      </div>
      <textarea id="customer-note" class="form-control mb-3" placeholder="Any special instructions..." rows="2"></textarea>
      <button class="btn btn-primary w-full" onclick="customerApp.placeOrder()" style="padding: 1rem;">
        Place Order
      </button>
    `;
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  async placeOrder() {
    if (this.cart.length === 0) return;

    const note = document.getElementById('customer-note')?.value || '';
    
    const orderData = {
      items: this.cart.map(item => ({
        menuItemId: item.menuItemId,
        quantity: item.quantity,
        customizations: item.customizations || []
      })),
      customerNote: note
    };

    try {
      const res = await fetch(`/api/v1/public/orders/${this.vendorCode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(orderData)
      });
      const data = await res.json();

      if (!data.success) {
        if (typeof showToast === 'function') showToast(data.message || 'Failed to place order', 'error');
        return;
      }

      this.activeOrder = data.data;
      localStorage.setItem(`dequeue_order_${this.vendorCode}`, JSON.stringify(this.activeOrder));
      this.cart = [];

      // Close cart modal and show tracking
      const floatingCart = document.getElementById('floating-cart');
      if (floatingCart) floatingCart.classList.remove('visible');
      if (typeof closeModal === 'function') closeModal('cart-modal');
      this.showOrderTracking();

    } catch (err) {
      if (typeof showToast === 'function') showToast('Failed to place order. Please try again.', 'error');
      console.error(err);
    }
  }

  async checkExistingOrder() {
    try {
      const res = await fetch(`/api/v1/public/orders/${this.vendorCode}/track/${this.activeOrder.queueNumber}`);
      const data = await res.json();

      if (data.success) {
        this.activeOrder = data.data;
        if (data.data.status === 'COLLECTED' || data.data.status === 'CANCELLED') {
          localStorage.removeItem(`dequeue_order_${this.vendorCode}`);
          this.activeOrder = null;
          await this.loadVendor();
        } else {
          await this.loadVendor();
          this.showOrderTracking();
        }
      } else {
        localStorage.removeItem(`dequeue_order_${this.vendorCode}`);
        this.activeOrder = null;
        await this.loadVendor();
      }
    } catch (err) {
      await this.loadVendor();
    }
  }

  showOrderTracking() {
    const menuView = document.getElementById('menu-view');
    const orderView = document.getElementById('order-view');
    if (menuView) menuView.classList.add('hidden');
    if (orderView) orderView.classList.remove('hidden');

    if (this.activeOrder) {
      const qnEl = document.getElementById('queue-number');
      if (qnEl) qnEl.textContent = this.activeOrder.queueNumber || 'N/A';

      this.updateStatusDisplay(this.activeOrder.status);
    }

    // Start polling for status updates
    this.startPolling();
  }

  updateStatusDisplay(status) {
    const steps = ['PENDING', 'PREPARING', 'READY'];
    const labels = { PENDING: 'Order Received', ACCEPTED: 'Order Accepted', PREPARING: 'Preparing', READY: 'Ready for Collection', COLLECTED: 'Collected' };
    
    const statusBadge = document.querySelector('#order-view .badge');
    if (statusBadge) {
      statusBadge.textContent = labels[status] || status;
      statusBadge.className = `badge badge-${status.toLowerCase()} text-lg px-4 py-2 mt-4`;
    }

    // Update step indicators
    const stepEls = document.querySelectorAll('#order-view .card .flex.items-center');
    const currentStep = steps.indexOf(status === 'ACCEPTED' ? 'PENDING' : status);
    
    stepEls.forEach((el, idx) => {
      const icon = el.querySelector('[data-lucide]');
      if (idx <= currentStep) {
        el.className = 'flex items-center gap-4 mb-4 text-primary';
        if (icon) icon.setAttribute('data-lucide', 'check-circle-2');
      } else {
        el.className = 'flex items-center gap-4 mb-4 text-muted';
        if (icon) icon.setAttribute('data-lucide', 'circle');
      }
    });
    
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  startPolling() {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
    
    this.pollingInterval = setInterval(async () => {
      if (!this.activeOrder || !this.activeOrder.queueNumber) return;
      
      try {
        const res = await fetch(`/api/v1/public/orders/${this.vendorCode}/track/${this.activeOrder.queueNumber}`);
        const data = await res.json();
        
        if (data.success) {
          this.activeOrder = data.data;
          localStorage.setItem(`dequeue_order_${this.vendorCode}`, JSON.stringify(this.activeOrder));
          this.updateStatusDisplay(data.data.status);
          
          if (data.data.status === 'READY') {
            if (typeof showToast === 'function') showToast('Your order is READY! Please collect it.', 'success');
          }
          
          if (data.data.status === 'COLLECTED' || data.data.status === 'CANCELLED') {
            clearInterval(this.pollingInterval);
            localStorage.removeItem(`dequeue_order_${this.vendorCode}`);
            setTimeout(() => {
              this.activeOrder = null;
              const menuView = document.getElementById('menu-view');
              const orderView = document.getElementById('order-view');
              if (menuView) menuView.classList.remove('hidden');
              if (orderView) orderView.classList.add('hidden');
            }, 5000);
          }
        }
      } catch (err) {
        console.error('Polling error:', err);
      }
    }, 5000);
  }

  showClosed() {
    const menuView = document.getElementById('menu-view');
    if (menuView) {
      menuView.innerHTML = `
        <div class="flex flex-col items-center justify-center text-center py-16">
          <div class="inline-block p-4 rounded-full bg-surface mb-4">
            <i data-lucide="store" class="text-muted" style="width:48px;height:48px;"></i>
          </div>
          <h2 class="text-xl font-bold mb-2">Shop is Currently Closed</h2>
          <p class="text-muted">Please check back during business hours.</p>
        </div>
      `;
      if (typeof lucide !== 'undefined') lucide.createIcons();
    }
    const statusEl = document.querySelector('.shop-status');
    if (statusEl) {
      statusEl.className = 'shop-status closed';
      statusEl.textContent = 'Closed';
    }
  }

  showError(message) {
    const app = document.querySelector('.customer-app');
    if (app) {
      app.innerHTML = `
        <div class="flex flex-col items-center justify-center text-center min-h-screen p-8">
          <div class="inline-block p-4 rounded-full bg-surface mb-4">
            <i data-lucide="alert-circle" class="text-danger" style="width:48px;height:48px;"></i>
          </div>
          <h2 class="text-xl font-bold mb-2">Oops!</h2>
          <p class="text-muted">${message}</p>
        </div>
      `;
      if (typeof lucide !== 'undefined') lucide.createIcons();
    }
  }

  formatPrice(amount) {
    return `₹${Number(amount).toFixed(0)}`;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.customerApp = new CustomerApp();
});
