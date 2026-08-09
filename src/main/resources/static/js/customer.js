// Customer App Logic - DeQueue Customer Ordering System
class CustomerApp {
  constructor() {
    this.cart = [];
    this.vendorCode = this.getVendorCodeFromUrl();
    this.vendor = null;
    this.menu = null;
    this.activeOrder = null;
    this.pollingInterval = null;
    this.notificationManager = null;
    this.sessionId = this.getOrCreateSessionId();
    this.init();
  }

  getOrCreateSessionId() {
    let sid = localStorage.getItem('dequeue_customer_session');
    if (!sid) {
      sid = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2, 15);
      localStorage.setItem('dequeue_customer_session', sid);
    }
    return sid;
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
      
      // Fetch currently serving initially and start polling
      await this.updateCurrentlyServing();
      if (!this.pollingInterval) {
          this.pollingInterval = setInterval(() => this.updateCurrentlyServing(), 10000);
      }

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

  async updateCurrentlyServing() {
      try {
          const currRes = await fetch(`/api/v1/public/orders/${this.vendorCode}/currently-serving`);
          const currData = await currRes.json();
          const b = document.getElementById('running-queue-banner');
          const d = document.getElementById('running-queue-display');
          
          if (b && d) {
              if (currData.success && currData.data) {
                  d.textContent = currData.data;
                  b.classList.remove('hidden');
              } else {
                  b.classList.add('hidden');
              }
          }
      } catch (e) {
          // Silent fail for polling
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
    const title = document.getElementById('cust-modal-title');
    const body = document.getElementById('cust-modal-body');
    const addBtn = document.getElementById('cust-modal-add-btn');
    if (!title || !body || !addBtn) return;

    title.innerText = `Customize ${item.name}`;
    
    let html = '';
    
    // Add image if requested
    if (item.image) {
        html += `<div style="margin-bottom: 1rem;"><img src="${item.image}" alt="${item.name}" style="width: 100%; height: 160px; object-fit: cover; border-radius: var(--radius-md);"></div>`;
    } else {
        html += `<div style="width: 100%; height: 160px; background: var(--surface); display: flex; align-items: center; justify-content: center; border-radius: var(--radius-md); margin-bottom: 1rem;"><i data-lucide="utensils" style="opacity: 0.3; width: 48px; height: 48px;"></i></div>`;
    }
    item.customizationGroups.forEach((group, gIdx) => {
        html += `<div class="mb-4">
            <h4 class="font-bold mb-2">${group.name} ${group.required ? '<span class="text-danger">*</span>' : ''}</h4>
            <div class="flex flex-col gap-2">`;
        
        group.options.forEach((opt, oIdx) => {
            const inputType = group.selectionType === 'SINGLE' || group.maxSelection === 1 ? 'radio' : 'checkbox';
            const inputName = `cust_${group.id}`;
            const inputId = `cust_${group.id}_${oIdx}`;
            
            html += `<label class="flex items-center justify-between p-2 border border-border rounded-md" for="${inputId}">
                <div class="flex items-center gap-2">
                    <input type="${inputType}" name="${inputName}" id="${inputId}" value="${opt.name}" data-price="${opt.additionalPrice}" data-group-name="${group.name}">
                    <span>${opt.name}</span>
                </div>
                ${opt.additionalPrice > 0 ? `<span class="text-muted text-sm">+₹${opt.additionalPrice}</span>` : ''}
            </label>`;
        });
        
        html += `</div></div>`;
    });
    
    body.innerHTML = html;
    
    addBtn.onclick = () => {
        const customizations = [];
        let missingRequired = false;
        
        item.customizationGroups.forEach(group => {
            const inputs = body.querySelectorAll(`input[name="cust_${group.id}"]:checked`);
            if (group.required && inputs.length === 0) {
                missingRequired = true;
            }
            inputs.forEach(input => {
                customizations.push({
                    optionName: input.value,
                    additionalPrice: parseFloat(input.dataset.price || 0),
                    groupName: input.dataset.groupName
                });
            });
        });
        
        if (missingRequired) {
            if (window.showToast) showToast('Please select all required options', 'error');
            return;
        }
        
        this.addToCart(item, customizations);
        if (window.closeModal) closeModal('cust-modal');
    };
    
    if (window.openModal) openModal('cust-modal');
  }

  addToCart(item, customizations = []) {
    const extraPrice = customizations.reduce((sum, c) => sum + (c.additionalPrice || 0), 0);
    const unitPrice = item.price + extraPrice;

    const cartItem = {
      menuItemId: item.id,
      menuItemName: item.name,
      quantity: 1,
      unitPrice: unitPrice,
      totalPrice: unitPrice,
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
    this.renderCartModal();
    if (typeof showToast === 'function') showToast(`Added ${item.name} to cart`, 'success');
  }

  updateQuantity(cartId, delta) {
    const item = this.cart.find(c => c.cartId === cartId);
    if (!item) return;
    
    item.quantity += delta;
    if (item.quantity <= 0) {
      this.removeFromCart(cartId);
      return;
    }
    
    item.totalPrice = item.unitPrice * item.quantity;
    this.updateCartUI();
    this.renderCartModal();
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
              <div class="text-sm text-muted">${this.formatPrice(item.unitPrice)}</div>
            </div>
            <div class="flex items-center gap-3">
              <div style="display: flex; align-items: center; gap: 0.5rem; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 0.25rem;">
                <button class="btn-icon" onclick="customerApp.updateQuantity(${item.cartId}, -1)" style="padding: 2px;"><i data-lucide="minus" style="width:14px;height:14px;"></i></button>
                <span style="font-weight: 500; min-width: 1.5rem; text-align: center;">${item.quantity}</span>
                <button class="btn-icon" onclick="customerApp.updateQuantity(${item.cartId}, 1)" style="padding: 2px;"><i data-lucide="plus" style="width:14px;height:14px;"></i></button>
              </div>
              <span class="font-bold" style="min-width: 60px; text-align: right;">${this.formatPrice(item.unitPrice * item.quantity)}</span>
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
      sessionId: this.sessionId,
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

      // (WebSocket will be connected inside showOrderTracking)

      // Initialize notification manager for real-time updates
      if (typeof CustomerNotificationManager !== 'undefined') {
        this.notificationManager = new CustomerNotificationManager(this);
        const orderData = data.data;
        this.notificationManager.init(
          orderData.id,
          orderData.sessionId,
          orderData.customerSessionToken,
          orderData.queueNumber
        );

        // Save token data for reconnection
        localStorage.setItem(`dequeue_token_${this.vendorCode}`, JSON.stringify({
          orderId: orderData.id,
          sessionId: orderData.sessionId,
          customerToken: orderData.customerSessionToken
        }));
      }

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
          await this.loadVendor();
          
          const menuView = document.getElementById('menu-view');
          const thankYouView = document.getElementById('thank-you-view');
          
          if (data.data.status === 'CANCELLED') {
              document.getElementById('thank-you-title').textContent = 'Order Cancelled';
              document.getElementById('thank-you-message').textContent = 'Unfortunately, your order was cancelled. Please try ordering again.';
              document.getElementById('thank-you-icon').setAttribute('data-lucide', 'x-circle');
              document.getElementById('thank-you-icon').setAttribute('class', 'text-warning');
          } else {
              document.getElementById('thank-you-title').textContent = 'Thank You!';
              document.getElementById('thank-you-message').textContent = 'Your order has been successfully collected. Please visit us again!';
              document.getElementById('thank-you-icon').setAttribute('data-lucide', 'heart');
              document.getElementById('thank-you-icon').setAttribute('class', 'text-danger');
          }
          if (typeof lucide !== 'undefined') lucide.createIcons();
          
          if (menuView) menuView.classList.add('hidden');
          if (thankYouView) thankYouView.classList.remove('hidden');
          
          this.activeOrder = null;
        } else {
          await this.loadVendor();
          this.showOrderTracking();
          
          // Restore notification manager if we have stored token
          const storedToken = localStorage.getItem(`dequeue_token_${this.vendorCode}`);
          if (storedToken && typeof CustomerNotificationManager !== 'undefined' && !this.notificationManager) {
            try {
              const tokenData = JSON.parse(storedToken);
              this.notificationManager = new CustomerNotificationManager(this);
              this.notificationManager.init(
                tokenData.orderId,
                tokenData.sessionId,
                tokenData.customerToken,
                this.activeOrder.queueNumber
              );
            } catch(e) { console.error('Failed to restore notifications:', e); }
          }
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
    this.connectWebSocket();
    const menuView = document.getElementById('menu-view');
    const orderView = document.getElementById('order-view');
    if (menuView) menuView.classList.add('hidden');
    if (orderView) orderView.classList.remove('hidden');

    if (this.activeOrder) {
      const qnEl = document.getElementById('queue-number');
      if (qnEl) qnEl.textContent = this.activeOrder.queueNumber || 'N/A';

      this.updateStatusDisplay(this.activeOrder.status);
      this.subscribeToOrder(this.activeOrder.queueNumber);
    }
  }

  connectWebSocket() {
    if (this.stompClient && this.stompClient.connected) return;

    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;

    this.stompClient.connect({}, (frame) => {
      console.log('WebSocket connected');
      this.wsReconnectAttempts = 0;

      // Subscribe to vendor updates
      if (this.vendor && this.vendor.id) {
        this.subscribeToVendor(this.vendor.id);
      }

      // Subscribe to active order
      if (this.activeOrder && this.activeOrder.queueNumber) {
        this.subscribeToOrder(this.activeOrder.queueNumber);
      }

      // Notify notification manager of reconnection
      if (this.notificationManager) {
        this.notificationManager.onReconnected();
      }
    }, (error) => {
      console.error('WebSocket disconnected:', error);
      // Exponential backoff reconnect
      const attempts = this.wsReconnectAttempts || 0;
      const delay = Math.min(1000 * Math.pow(2, attempts), 30000);
      this.wsReconnectAttempts = attempts + 1;
      console.log(`Reconnecting in ${delay}ms (attempt ${this.wsReconnectAttempts})...`);
      setTimeout(() => this.connectWebSocket(), delay);
    });
  }
  
  subscribeToVendor(vendorId) {
      if (!this.stompClient || !this.stompClient.connected) return;
      this.stompClient.subscribe('/topic/vendor/' + vendorId, (msg) => {
          const event = JSON.parse(msg.body);
          if (event.status === 'PREPARING' || event.status === 'READY') {
              const banner = document.getElementById('running-queue-banner');
              const display = document.getElementById('running-queue-display');
              if (banner && display) {
                  banner.classList.remove('hidden');
                  display.textContent = event.queueNumber;
              }
          }
      });
  }

  subscribeToOrder(queueNumber) {
      if (!this.stompClient || !this.stompClient.connected) return;
      this.stompClient.subscribe('/topic/orders/' + queueNumber, (msg) => {
          const event = JSON.parse(msg.body);
          this.handleOrderUpdate(event);
      });
  }
  
  handleOrderUpdate(event) {
      if (!this.activeOrder) return;
      
      const status = event.status || event.orderStatus;
      if (!status) return;
      
      this.activeOrder.status = status;
      localStorage.setItem(`dequeue_order_${this.vendorCode}`, JSON.stringify(this.activeOrder));
      this.updateStatusDisplay(status);
      this.showBrowserNotification(status);
      
      if (status === 'COLLECTED' || status === 'CANCELLED') {
        localStorage.removeItem(`dequeue_order_${this.vendorCode}`);
        localStorage.removeItem(`dequeue_token_${this.vendorCode}`);
        if (this.notificationManager) {
          this.notificationManager.destroy();
          this.notificationManager = null;
        }
        const orderView = document.getElementById('order-view');
        const thankYouView = document.getElementById('thank-you-view');
        
        if (status === 'CANCELLED') {
            document.getElementById('thank-you-title').textContent = 'Order Cancelled';
            document.getElementById('thank-you-message').textContent = 'Unfortunately, your order was cancelled. Please try ordering again.';
            document.getElementById('thank-you-icon').setAttribute('data-lucide', 'x-circle');
            document.getElementById('thank-you-icon').setAttribute('class', 'text-warning');
        } else {
            document.getElementById('thank-you-title').textContent = 'Thank You!';
            document.getElementById('thank-you-message').textContent = 'Your order has been successfully collected. Please visit us again!';
            document.getElementById('thank-you-icon').setAttribute('data-lucide', 'heart');
            document.getElementById('thank-you-icon').setAttribute('class', 'text-danger');
        }
        if (typeof lucide !== 'undefined') lucide.createIcons();
        
        if (orderView) orderView.classList.add('hidden');
        if (thankYouView) thankYouView.classList.remove('hidden');
      }
  }

  async showBrowserNotification(status) {
      if (!('Notification' in window)) {
        console.warn('Browser notifications are not supported.');
        return;
      }
    
      console.log('Notification permission:', Notification.permission);
    
      if (Notification.permission === 'default') {
        try {
          const permission = await Notification.requestPermission();
          console.log('Notification permission result:', permission);
        } catch (err) {
          console.error('Notification permission failed:', err);
          return;
        }
      }
    
      if (Notification.permission !== 'granted') {
        console.warn('Browser notification permission is not granted.');
        return;
      }
    
      const notifications = {
        READY: {
          title: 'Order Ready! 🍔',
          body: `Your order #${this.activeOrder?.queueNumber || ''} is ready for collection.`,
          icon: '/images/icon-192.png'
        },
        PREPARING: {
          title: 'Order Preparing 👨‍🍳',
          body: `Your order #${this.activeOrder?.queueNumber || ''} is now being prepared.`,
          icon: '/images/icon-192.png'
        },
        CANCELLED: {
          title: 'Order Cancelled',
          body: `Your order #${this.activeOrder?.queueNumber || ''} has been cancelled.`,
          icon: '/images/icon-192.png'
        },
        COLLECTED: {
          title: 'Order Collected',
          body: 'Thank you! Your order has been collected.',
          icon: '/images/icon-192.png'
        }
      };
    
      const notification = notifications[status];
    
      if (!notification) return;
    
      try {
        const options = {
          body: notification.body,
          icon: notification.icon,
          tag: `dequeue-order-${this.activeOrder?.id || this.activeOrder?.queueNumber}`,
          renotify: true
        };

        // Android Chrome requires ServiceWorker for notifications. 
        // Fallback to 'new Notification' for desktop Safari/Firefox if SW is not ready.
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.ready.then(registration => {
                registration.showNotification(notification.title, options);
            }).catch(err => {
                new Notification(notification.title, options);
            });
        } else {
            new Notification(notification.title, options);
        }
      } catch (err) {
        console.error('Failed to create browser notification:', err);
      }
  }

  submitFeedback() {
      const text = document.getElementById('feedback-text')?.value;
      if (text) {
          if (typeof showToast === 'function') showToast('Thank you for your feedback!', 'success');
          document.getElementById('feedback-text').value = '';
      }
      this.startNewOrder();
  }
  
  startNewOrder() {
      this.activeOrder = null;
      const thankYouView = document.getElementById('thank-you-view');
      const menuView = document.getElementById('menu-view');
      if (thankYouView) thankYouView.classList.add('hidden');
      if (menuView) menuView.classList.remove('hidden');
  }

  updateStatusDisplay(status) {
    const steps = ['PENDING', 'PREPARING', 'READY'];
    const labels = { PENDING: 'Order Received', ACCEPTED: 'Order Accepted', PREPARING: 'Preparing', READY: 'Ready for Collection', COLLECTED: 'Collected', CANCELLED: 'Cancelled' };
    
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
    return `₹${Math.round(Number(amount) * 100) / 100}`;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.customerApp = new CustomerApp();
});
