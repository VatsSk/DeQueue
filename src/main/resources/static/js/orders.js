class Orders {
  constructor() {
    this.orders = [];
    this.currentFilter = 'ALL';
    this.init();
  }

  async init() {
    this.setupFilters();
    this.setupSearch();
    await this.fetchOrders();
    
    this.connectWebSocket();
  }

  connectWebSocket() {
    if (this.stompClient && this.stompClient.connected) return;
    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;
    
    this.stompClient.connect({}, () => {
        const userStr = localStorage.getItem('user');
        if (userStr) {
            try {
                const user = JSON.parse(userStr);
                if (user && user.vendorId) {
                    this.stompClient.subscribe('/topic/vendor/' + user.vendorId, (msg) => {
                        this.fetchOrders();
                    });
                }
            } catch (e) {
                console.error("WebSocket subscription error", e);
            }
        }
    }, (error) => {
        console.error("WebSocket disconnected", error);
    });
  }

  setupFilters() {
    const container = document.getElementById('orders-filters-container');
    if (!container) return;

    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;
    const permissions = user && user.effectivePermissions ? user.effectivePermissions : [];
    const isPlatformAdmin = user ? user.platformAdmin === true : false;

    // Determine tabs based on action permissions
    let tabs = [];
    tabs.push({ id: 'ALL', label: 'All' });

    if (isPlatformAdmin || permissions.includes('order.accept')) {
      tabs.push({ id: 'PENDING', label: 'Pending' });
    }
    if (isPlatformAdmin || permissions.includes('order.prepare')) {
      tabs.push({ id: 'ACCEPTED', label: 'Accepted' });
    }
    if (isPlatformAdmin || permissions.includes('order.ready')) {
      tabs.push({ id: 'PREPARING', label: 'Preparing' });
    }
    if (isPlatformAdmin || permissions.includes('order.complete')) {
      tabs.push({ id: 'READY', label: 'Ready' });
    }

    let html = '';
    tabs.forEach((tab, index) => {
      const activeClass = tab.id === this.currentFilter ? 'active' : '';
      html += `<button class="filter-tab ${activeClass}" data-filter="${tab.id}">${tab.label}</button>`;
    });
    container.innerHTML = html;

    // Attach click events
    const tabButtons = container.querySelectorAll('.filter-tab');
    tabButtons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        tabButtons.forEach(t => t.classList.remove('active'));
        e.target.classList.add('active');
        this.currentFilter = e.target.getAttribute('data-filter');
        this.renderOrders();
      });
    });
  }

  setupSearch() {
    const searchInput = document.querySelector('input[placeholder="Search Order #..."]');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.renderOrders(e.target.value.toLowerCase());
      });
    }
  }

  async fetchOrders() {
    try {
      // Get all active order summaries
      const res = await api.get('/orders/active');
      if (res.success && res.data) {
        // Fetch detailed order info for each active order
        const detailsPromises = res.data.map(summary => api.get(`/orders/${summary.id}`));
        const detailsRes = await Promise.all(detailsPromises);
        
        this.orders = detailsRes.filter(r => r.success).map(r => r.data);
        this.updateFilterCounts();
        this.renderOrders();
      }
    } catch (e) {
      console.error('Failed to fetch orders', e);
    }
  }

  updateFilterCounts() {
    const pendingCount = this.orders.filter(o => o.status === 'PENDING').length;
    const acceptedCount = this.orders.filter(o => o.status === 'ACCEPTED').length;
    const preparingCount = this.orders.filter(o => o.status === 'PREPARING').length;
    const readyCount = this.orders.filter(o => o.status === 'READY').length;

    const tabs = document.querySelectorAll('.filter-tab');
    tabs.forEach(tab => {
      const filterId = tab.getAttribute('data-filter');
      if (filterId === 'PENDING') tab.innerText = `Pending (${pendingCount})`;
      else if (filterId === 'ACCEPTED') tab.innerText = `Accepted (${acceptedCount})`;
      else if (filterId === 'PREPARING') tab.innerText = `Preparing (${preparingCount})`;
      else if (filterId === 'READY') tab.innerText = `Ready (${readyCount})`;
    });
  }

  renderOrders(searchQuery = '') {
    const grid = document.querySelector('.orders-grid');
    if (!grid) return;

    let filtered = this.orders;
    if (this.currentFilter !== 'ALL') {
      filtered = filtered.filter(o => o.status === this.currentFilter);
    }
    
    if (searchQuery) {
      filtered = filtered.filter(o => o.queueNumber.toLowerCase().includes(searchQuery));
    }

    if (filtered.length === 0) {
      grid.innerHTML = '<div class="text-muted p-4 col-span-full text-center">No orders match the current filter.</div>';
      return;
    }

    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;
    const permissions = user && user.effectivePermissions ? user.effectivePermissions : [];
    const isPlatformAdmin = user ? user.platformAdmin === true : false;

    let html = '';
    filtered.forEach(order => {
      let borderColor = 'var(--border)';
      let badgeClass = 'badge-pending';
      let actionsHtml = '';

      if (order.status === 'PENDING') {
        badgeClass = 'badge-pending';
        if (isPlatformAdmin || permissions.includes('order.accept')) {
          actionsHtml = `
              <button class="btn btn-danger" onclick="ordersApp.updateStatus('${order.id}', 'CANCELLED')">Reject</button>
              <button class="btn btn-primary" onclick="ordersApp.updateStatus('${order.id}', 'ACCEPTED')">Accept</button>
          `;
        }
      } else if (order.status === 'ACCEPTED') {
        borderColor = 'var(--warning)';
        badgeClass = 'badge-accepted';
        if (isPlatformAdmin || permissions.includes('order.prepare')) {
          actionsHtml = `<button class="btn btn-primary w-full" onclick="ordersApp.updateStatus('${order.id}', 'PREPARING')">Start Preparing</button>`;
        }
      } else if (order.status === 'PREPARING') {
        borderColor = 'var(--info)';
        badgeClass = 'badge-preparing';
        if (isPlatformAdmin || permissions.includes('order.ready')) {
          actionsHtml = `<button class="btn btn-primary w-full" onclick="ordersApp.updateStatus('${order.id}', 'READY')">Mark Ready</button>`;
        }
      } else if (order.status === 'READY') {
        borderColor = 'var(--success)';
        badgeClass = 'badge-ready';
        if (isPlatformAdmin || permissions.includes('order.complete')) {
          actionsHtml = `<button class="btn btn-success w-full" onclick="ordersApp.updateStatus('${order.id}', 'COMPLETED')">Mark Completed</button>`;
        }
      } else {
         actionsHtml = `<button class="btn btn-secondary w-full" disabled>${order.status}</button>`;
      }

      let itemsHtml = '';
      if (order.orderItems && order.orderItems.length > 0) {
        order.orderItems.forEach(item => {
            let customHtml = '';
            if (item.selectedCustomizations && item.selectedCustomizations.length > 0) {
                let optionsArr = [];
                item.selectedCustomizations.forEach(c => {
                    if (c.selectedOptions) {
                        c.selectedOptions.forEach(opt => optionsArr.push(opt.name));
                    }
                });
                if (optionsArr.length > 0) {
                    customHtml = `<div class="text-xs text-muted" style="margin-left: 0.5rem;">+ ${optionsArr.join(', ')}</div>`;
                }
            }
            itemsHtml += `
                <div class="flex justify-between items-start mb-1 text-sm">
                    <div>
                        <span class="font-medium">${item.quantity}x</span> ${this._escHtml(item.menuItemName)}
                        ${customHtml}
                    </div>
                    <span>₹${item.totalPrice.toFixed(2)}</span>
                </div>
            `;
        });
      } else if (order.customOrderText) {
          itemsHtml = `<div class="text-sm border p-2 rounded bg-light mb-2"><strong>Custom:</strong> ${this._escHtml(order.customOrderText)}</div>`;
      }

      let noteHtml = '';
      if (order.customerNote) {
          noteHtml = `<div class="text-xs text-warning mb-2">Note: ${this._escHtml(order.customerNote)}</div>`;
      }

      html += `
        <div class="card p-4 flex flex-col justify-between" style="border-top: 4px solid ${borderColor}; min-height: 250px;">
          <div>
            <div class="flex justify-between items-center mb-3">
              <div>
                <span class="font-bold text-lg">#${order.queueNumber}</span>
                <div class="text-xs text-muted">${new Date(order.createdAt).toLocaleTimeString()}</div>
              </div>
              <span class="badge ${badgeClass}">${order.status}</span>
            </div>
            
            <div class="order-items-list mb-4">
              ${itemsHtml}
              ${noteHtml}
            </div>
          </div>
          
          <div>
            <div class="border-t pt-3 flex justify-between items-center mb-3 text-sm font-bold">
              <span>Total</span>
              <span>₹${order.totalAmount.toFixed(2)}</span>
            </div>
            <div class="flex gap-2">
              ${actionsHtml}
            </div>
          </div>
        </div>
      `;
    });

    grid.innerHTML = html;
  }

  _escHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }


  async updateStatus(orderId, newStatus) {
    try {
      const res = await api.patch('/orders/' + orderId + '/status', { status: newStatus });
      if (res.success) {
        if (window.showToast) showToast(`Order marked as ${newStatus}`, 'success');
        await this.fetchOrders();
      }
    } catch (e) {
      console.error(e);
      if (window.showToast) showToast('Failed to update order status', 'error');
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.ordersApp = new Orders();
});
