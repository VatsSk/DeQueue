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
    const tabs = document.querySelectorAll('.filter-tab');
    if (!tabs.length) return;
    
    tabs.forEach(tab => {
      tab.addEventListener('click', (e) => {
        tabs.forEach(t => t.classList.remove('active'));
        e.target.classList.add('active');
        
        const filterText = e.target.innerText.toLowerCase();
        if (filterText.includes('all')) this.currentFilter = 'ALL';
        else if (filterText.includes('pending')) this.currentFilter = 'PENDING';
        else if (filterText.includes('preparing')) this.currentFilter = 'PREPARING';
        else if (filterText.includes('ready')) this.currentFilter = 'READY';
        
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
    const preparingCount = this.orders.filter(o => o.status === 'PREPARING').length;
    const readyCount = this.orders.filter(o => o.status === 'READY').length;

    const tabs = document.querySelectorAll('.filter-tab');
    tabs.forEach(tab => {
      const text = tab.innerText.toLowerCase();
      if (text.includes('pending')) tab.innerText = `Pending (${pendingCount})`;
      else if (text.includes('preparing')) tab.innerText = `Preparing (${preparingCount})`;
      else if (text.includes('ready')) tab.innerText = `Ready (${readyCount})`;
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

    let html = '';
    filtered.forEach(order => {
      let borderColor = 'var(--border)';
      let badgeClass = 'badge-pending';
      let actionsHtml = '';

      if (order.status === 'PENDING') {
        badgeClass = 'badge-pending';
        actionsHtml = `
            <button class="btn btn-danger" onclick="ordersApp.updateStatus('${order.id}', 'CANCELLED')">Reject</button>
            <button class="btn btn-primary" onclick="ordersApp.updateStatus('${order.id}', 'PREPARING')">Accept</button>
        `;
      } else if (order.status === 'PREPARING') {
        borderColor = 'var(--info)';
        badgeClass = 'badge-preparing';
        actionsHtml = `<button class="btn btn-primary w-full" onclick="ordersApp.updateStatus('${order.id}', 'READY')">Mark Ready</button>`;
      } else if (order.status === 'READY') {
        borderColor = 'var(--success)';
        badgeClass = 'badge-ready';
        actionsHtml = `<button class="btn btn-success w-full" onclick="ordersApp.updateStatus('${order.id}', 'COLLECTED')">Mark Completed</button>`;
      } else {
         actionsHtml = `<button class="btn btn-secondary w-full" disabled>${order.status}</button>`;
      }

      let itemsHtml = '';
      if (order.items && order.items.length > 0) {
        order.items.forEach(item => {
            let customHtml = '';
            if (item.customizations && item.customizations.length > 0) {
                const customStr = item.customizations.map(c => `- ${c.optionName} (+₹${c.additionalPrice})`).join('<br>');
                customHtml = `<div class="item-customizations">${customStr}</div>`;
            }
            itemsHtml += `
                <div class="order-item">
                    <div class="item-main">
                        <span>${item.quantity}x ${item.menuItemName}</span>
                        <span>₹${item.unitPrice * item.quantity}</span>
                    </div>
                    ${customHtml}
                </div>
            `;
        });
      }

      html += `
        <div class="order-card ${order.status !== 'PENDING' ? 'border-l-4' : ''}" style="${order.status !== 'PENDING' ? 'border-left-color: ' + borderColor + ';' : ''}">
            <div class="order-card-header">
                <div class="order-queue-num">${order.queueNumber}</div>
                <div class="text-right">
                    <span class="badge ${badgeClass} mb-1 block">${order.status}</span>
                    <div class="order-time">${window.getTimeAgo ? window.getTimeAgo(order.createdAt) : new Date(order.createdAt).toLocaleTimeString()}</div>
                </div>
            </div>
            <div class="order-items">
                ${itemsHtml}
            </div>
            <div class="order-card-footer">
                <div class="order-total">
                    <span>Total</span>
                    <span>₹${order.totalAmount}</span>
                </div>
                <div class="order-actions ${order.status === 'PENDING' ? 'two-col' : ''}">
                    ${actionsHtml}
                </div>
            </div>
        </div>
      `;
    });

    grid.innerHTML = html;
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
