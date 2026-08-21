class Orders {
  constructor() {
    this.orders = [];
    this.currentFilter = 'ALL';
    this.currentView = localStorage.getItem('dequeueOrdersView') || 'cards';
    this.searchQuery = '';
    this.tableFilter = 'ALL';
    this.typeFilter = 'ALL';
    this.paymentFilter = 'ALL';
    this.sortFilter = 'WAITING';
    this.stompClient = null;
    this.init();
  }

  async init() {
    this.setupViewSwitcher();
    this.setupFilters();
    this.setupSecondaryFilters();
    this.setupSearch();
    await this.fetchOrders();
    this.connectWebSocket();
    this.applyView();
  }

  getUser() {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null');
    } catch (_) {
      return null;
    }
  }

  permissions() {
    const user = this.getUser();
    return user && Array.isArray(user.effectivePermissions) ? user.effectivePermissions : [];
  }

  isAdmin() {
    const user = this.getUser();
    if (!user) return false;
    if (user.platformAdmin === true) return true;
    return this.roleMatches(user, ['ROLE_VENDOR_ADMIN', 'VENDOR_ADMIN']);
  }

  isCounterStaff() {
    const user = this.getUser();
    return user ? this.roleMatches(user, ['ROLE_VENDORCOUNTER_STAFF', 'VENDORCOUNTER_STAFF']) : false;
  }

  isKitchen() {
    const user = this.getUser();
    return user ? this.roleMatches(user, ['KITCHEN']) : false;
  }

  roleMatches(user, names) {
    const wanted = names.map(x => x.toUpperCase());
    const values = [];
    if (Array.isArray(user.roleNames)) values.push(...user.roleNames);
    if (user.roleName) values.push(user.roleName);
    if (user.role) values.push(typeof user.role === 'string' ? user.role : user.role.name);
    if (Array.isArray(user.roles)) values.push(...user.roles.map(r => typeof r === 'string' ? r : r.name));
    return values.some(v => v && wanted.includes(String(v).toUpperCase()));
  }

  hasPermission(name) {
    return this.isAdmin() || this.permissions().includes(name);
  }

  connectWebSocket() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    if (this.stompClient && this.stompClient.connected) return;

    try {
      const socket = new SockJS('/ws');
      this.stompClient = Stomp.over(socket);
      this.stompClient.debug = null;
      this.stompClient.connect({}, () => {
        const user = this.getUser();
        if (user && user.vendorId) {
          this.stompClient.subscribe('/topic/vendor/' + user.vendorId, () => {
            this.fetchOrders();
          });
        }
      }, error => console.error('WebSocket disconnected', error));
    } catch (e) {
      console.error('WebSocket connection failed', e);
    }
  }

  setupViewSwitcher() {
    document.querySelectorAll('.view-switch').forEach(btn => {
      btn.addEventListener('click', () => {
        this.currentView = btn.dataset.view;
        localStorage.setItem('dequeueOrdersView', this.currentView);
        document.querySelectorAll('.view-switch').forEach(x => x.classList.toggle('active', x === btn));
        this.applyView();
      });
    });
  }

  applyView() {
    const cards = document.getElementById('orders-grid');
    const table = document.getElementById('orders-table-wrap');
    if (!cards || !table) return;
    const isCards = this.currentView === 'cards';
    cards.hidden = !isCards;
    table.hidden = isCards;
    this.renderCurrent();
  }

  setupFilters() {
    const container = document.getElementById('orders-filters-container');
    if (!container) return;

    const tabs = [{ id: 'ALL', label: 'All Active', icon: 'layers-3' }];

    if (this.hasPermission('order.accept') || this.hasPermission('order.pending')) {
      tabs.push({ id: 'PENDING', label: 'Pending', icon: 'hourglass' });
    }
    if (this.hasPermission('order.prepare')) tabs.push({ id: 'ACCEPTED', label: 'Accepted', icon: 'check-square-2' });
    if (this.hasPermission('order.ready')) tabs.push({ id: 'PREPARING', label: 'Preparing', icon: 'chef-hat' });
    if (this.hasPermission('order.complete')) tabs.push({ id: 'READY', label: 'Ready', icon: 'circle-check' });

    // Completed is a history filter. It is intentionally separate from All Active.
    tabs.push({ id: 'COMPLETED_24H', label: 'Completed', icon: 'history', window: '24h', completed: true });

    container.innerHTML = tabs.map(tab => `
      <button type="button" class="filter-tab ${tab.id === this.currentFilter ? 'active' : ''} ${tab.completed ? 'completed-tab' : ''}"
              data-filter="${tab.id}">
        <i data-lucide="${tab.icon}"></i>
        <span>${tab.label}</span>
        ${tab.window ? `<span class="filter-window">${tab.window}</span>` : ''}
        <span class="filter-count" data-count="${tab.id}">0</span>
      </button>
    `).join('');

    container.querySelectorAll('.filter-tab').forEach(btn => {
      btn.addEventListener('click', () => {
        this.currentFilter = btn.dataset.filter;
        container.querySelectorAll('.filter-tab').forEach(x => x.classList.toggle('active', x === btn));
        this.renderCurrent();
      });
    });

    if (window.lucide) lucide.createIcons();
  }

  setupSearch() {
    const input = document.getElementById('order-search');
    if (!input) return;
    input.addEventListener('input', e => {
      this.searchQuery = e.target.value.trim().toLowerCase();
      this.renderCurrent();
    });
  }

  setupSecondaryFilters() {
    const table = document.getElementById('table-filter');
    const type = document.getElementById('type-filter');
    const payment = document.getElementById('payment-filter');
    const sort = document.getElementById('sort-filter');
    const reset = document.getElementById('clear-order-filters');

    if (table) table.addEventListener('change', e => { this.tableFilter = e.target.value; this.renderCurrent(); });
    if (type) type.addEventListener('change', e => { this.typeFilter = e.target.value; this.renderCurrent(); });
    if (payment) payment.addEventListener('change', e => { this.paymentFilter = e.target.value; this.renderCurrent(); });
    if (sort) sort.addEventListener('change', e => { this.sortFilter = e.target.value; this.renderCurrent(); });

    if (reset) reset.addEventListener('click', () => {
      this.searchQuery = '';
      this.tableFilter = this.typeFilter = this.paymentFilter = 'ALL';
      this.sortFilter = 'WAITING';
      if (document.getElementById('order-search')) document.getElementById('order-search').value = '';
      if (table) table.value = 'ALL';
      if (type) type.value = 'ALL';
      if (payment) payment.value = 'ALL';
      if (sort) sort.value = 'WAITING';
      this.renderCurrent();
    });
  }

  async fetchOrders() {
    try {
      // Preserve existing API behavior: /orders/active returns full OrderResponse objects.
      const res = await api.get('/orders/active');
      if (res.success && Array.isArray(res.data)) {
        this.orders = res.data;
        this.populateTableFilter();
        this.updateFilterCounts();
        this.renderCurrent();
      }
    } catch (e) {
      console.error('Failed to fetch orders', e);
      this.renderEmpty('Unable to load orders', 'Please try again or check your connection.', 'wifi-off');
    }
  }

  populateTableFilter() {
    const select = document.getElementById('table-filter');
    if (!select) return;
    const current = this.tableFilter;
    const tables = [...new Set(this.orders.map(o => this.getTable(o)).filter(Boolean).map(String))].sort((a,b) => a.localeCompare(b, undefined, {numeric:true}));
    select.innerHTML = `<option value="ALL">All Tables</option>` + tables.map(t => `<option value="${this.esc(t)}">${this.esc(t)}</option>`).join('');
    select.value = tables.includes(current) ? current : 'ALL';
  }

  isCompletedWithin24h(order) {
    if (!order || order.status !== 'COMPLETED') return false;
    const date = new Date(order.completedAt || order.updatedAt || order.createdAt);
    if (Number.isNaN(date.getTime())) return false;
    return (Date.now() - date.getTime()) <= 24 * 60 * 60 * 1000;
  }

  activeOrders() {
    // "All Active" explicitly excludes completed and cancelled orders.
    return this.orders.filter(o => ['PENDING','ACCEPTED','PREPARING','READY'].includes(o.status));
  }

  updateFilterCounts() {
    const counts = {
      ALL: this.activeOrders().length,
      PENDING: this.orders.filter(o => o.status === 'PENDING').length,
      ACCEPTED: this.orders.filter(o => o.status === 'ACCEPTED').length,
      PREPARING: this.orders.filter(o => o.status === 'PREPARING').length,
      READY: this.orders.filter(o => o.status === 'READY').length,
      COMPLETED_24H: this.orders.filter(o => this.isCompletedWithin24h(o)).length
    };
    Object.entries(counts).forEach(([id, count]) => {
      const el = document.querySelector(`[data-count="${id}"]`);
      if (el) el.textContent = count;
    });

    const summary = document.getElementById('orders-summary');
    if (!summary) return;
    summary.innerHTML = [
      ['ALL','Active',counts.ALL,'active'],
      ['PENDING','Pending',counts.PENDING,'pending'],
      ['ACCEPTED','Accepted',counts.ACCEPTED,'accepted'],
      ['PREPARING','Preparing',counts.PREPARING,'preparing'],
      ['READY','Ready',counts.READY,'ready']
    ].map(([id,label,count,cls]) => `
      <button type="button" class="summary-card ${cls} ${this.currentFilter === id ? 'active' : ''}" data-summary="${id}">
        <div class="summary-value">${count}</div><div class="summary-label">${label}</div>
      </button>
    `).join('');

    summary.querySelectorAll('[data-summary]').forEach(btn => btn.addEventListener('click', () => {
      this.currentFilter = btn.dataset.summary;
      document.querySelectorAll('.filter-tab').forEach(x => x.classList.toggle('active', x.dataset.filter === this.currentFilter));
      this.renderCurrent();
    }));
  }

  getFilteredOrders() {
    let list = [...this.orders];

    if (this.currentFilter === 'ALL') {
      list = this.activeOrders();
    } else if (this.currentFilter === 'COMPLETED_24H') {
      list = list.filter(o => this.isCompletedWithin24h(o));
    } else {
      list = list.filter(o => o.status === this.currentFilter);
    }

    if (this.searchQuery) {
      list = list.filter(o => {
        const q = this.searchQuery;
        return String(o.queueNumber || '').toLowerCase().includes(q)
          || String(this.getTable(o) || '').toLowerCase().includes(q);
      });
    }

    if (this.tableFilter !== 'ALL') list = list.filter(o => String(this.getTable(o)) === String(this.tableFilter));
    if (this.typeFilter !== 'ALL') list = list.filter(o => this.getOrderType(o) === this.typeFilter);
    if (this.paymentFilter !== 'ALL') list = list.filter(o => this.matchesPayment(o, this.paymentFilter));

    list.sort((a,b) => {
      if (this.sortFilter === 'VALUE') return Number(b.totalAmount || 0) - Number(a.totalAmount || 0);
      if (this.sortFilter === 'NEWEST') return new Date(b.createdAt) - new Date(a.createdAt);
      if (this.sortFilter === 'OLDEST') return new Date(a.createdAt) - new Date(b.createdAt);
      return new Date(a.createdAt) - new Date(b.createdAt); // longest waiting first
    });
    return list;
  }

  renderCurrent() {
    this.updateFilterCounts();
    const list = this.getFilteredOrders();
    if (this.currentView === 'cards') this.renderCards(list);
    else this.renderTable(list);
    if (window.lucide) lucide.createIcons();
  }

  renderCards(list) {
    const grid = document.getElementById('orders-grid');
    if (!grid) return;
    if (!list.length) {
      grid.className = 'orders-grid empty';
      grid.innerHTML = this.emptyMarkup();
      return;
    }
    grid.className = 'orders-grid';
    grid.innerHTML = list.map(order => this.cardMarkup(order)).join('');
    if (window.lucide) lucide.createIcons();
  }

  renderTable(list) {
    const wrap = document.getElementById('orders-table-wrap');
    if (!wrap) return;
    if (!list.length) {
      wrap.innerHTML = this.emptyMarkup();
      return;
    }

    const rows = list.map(order => {
      const canPrint = this.canPrint();
      const action = this.primaryAction(order);
      return `
        <tr>
          <td><span class="table-order-number">#${this.esc(order.queueNumber)}</span></td>
          <td>${this.esc(this.getTable(order) || '—')}</td>
          <td>${this.esc(this.orderTypeLabel(order))}</td>
          <td>${this.itemCount(order)} item${this.itemCount(order) === 1 ? '' : 's'}</td>
          <td>${this.statusPill(order.status)}</td>
          <td class="${this.waitingMinutes(order) >= 15 ? 'text-danger' : ''}">${this.waitingLabel(order)}</td>
          <td class="font-bold">${this.hasPriceAccess() ? '₹' + Number(order.totalAmount || 0).toFixed(2) : '—'}</td>
          <td>${this.paymentBadge(order)}</td>
          <td>
            <div class="table-actions">
              <button class="table-btn" onclick="ordersApp.showOrderDetails('${this.escAttr(order.id)}')" title="View order"><i data-lucide="eye"></i> View</button>
              ${action ? `<button class="table-btn primary" onclick="ordersApp.updateStatus('${this.escAttr(order.id)}','${action.status}')">${action.label}</button>` : ''}
              ${canPrint && ['READY','COMPLETED'].includes(order.status) ? `
                <button class="table-btn success" onclick="ordersApp.printBill('${this.escAttr(order.id)}','view')" title="View Bill"><i data-lucide="receipt"></i></button>
                <button class="table-btn success" onclick="ordersApp.printBill('${this.escAttr(order.id)}','download')" title="Download Bill"><i data-lucide="download"></i></button>` : ''}
            </div>
          </td>
        </tr>`;
    }).join('');

    wrap.innerHTML = `
      <table class="orders-table">
        <thead><tr>
          <th>Order</th><th>Table</th><th>Type</th><th>Items</th><th>Status</th>
          <th>Waiting</th><th>Amount</th><th>Payment</th><th>Actions</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <div class="completed-info">
        <i data-lucide="info"></i>
        ${this.currentFilter === 'COMPLETED_24H' ? 'Showing completed orders from the last 24 hours only.' : `Showing ${list.length} order${list.length === 1 ? '' : 's'}.`}
      </div>`;
    if (window.lucide) lucide.createIcons();
  }

  cardMarkup(order) {
    const status = String(order.status || '').toLowerCase();
    const action = this.primaryAction(order);
    const canPrint = this.canPrint();
    const items = Array.isArray(order.items) ? order.items : [];

    const note = order.customerNote ? `
      <div class="customer-note">
        <div class="customer-note-title"><i data-lucide="message-square-warning"></i> Customer Note</div>
        <div class="customer-note-text">${this.esc(order.customerNote)}</div>
      </div>` : '';

    const payment = this.hasPriceAccess() ? `<div class="payment-line">${this.paymentBadge(order)}</div>` : '';

    let actions = '';
    if (order.status === 'PENDING' && (this.hasPermission('order.accept') || this.hasPermission('order.pending'))) {
      actions = `<div class="order-actions two">
        <button class="order-action danger" onclick="ordersApp.updateStatus('${this.escAttr(order.id)}','CANCELLED')"><i data-lucide="x"></i> Reject</button>
        <button class="order-action primary" onclick="ordersApp.updateStatus('${this.escAttr(order.id)}','ACCEPTED')"><i data-lucide="check"></i> Accept Order</button>
      </div>`;
    } else if (action) {
      actions = `<div class="order-actions">
        <button class="order-action ${order.status === 'READY' ? 'success' : 'primary'}" onclick="ordersApp.updateStatus('${this.escAttr(order.id)}','${action.status}')">
          <i data-lucide="${action.icon}"></i> ${action.label}
        </button>
      </div>`;
    }

    const bill = canPrint && ['READY','COMPLETED'].includes(order.status) ? `
      <div class="bill-actions">
        <button class="bill-btn" onclick="ordersApp.printBill('${this.escAttr(order.id)}','view')"><i data-lucide="eye"></i> View Bill</button>
        <button class="bill-btn" onclick="ordersApp.printBill('${this.escAttr(order.id)}','download')"><i data-lucide="printer"></i> Print / PDF</button>
      </div>` : '';

    return `
      <article class="order-card status-${status}">
        <div class="order-card-head">
          <div class="order-card-status-row">
            ${this.statusPill(order.status)}
            <span class="waiting-time ${this.waitingMinutes(order) >= 15 ? 'attention' : ''}">${this.waitingLabel(order)}</span>
          </div>
          <div class="order-number-row">
            <div class="order-number">#${this.esc(order.queueNumber)}</div>
            <button class="order-menu-btn" onclick="ordersApp.openMenu(event,'${this.escAttr(order.id)}')" title="More actions"><i data-lucide="more-vertical"></i></button>
          </div>
          <div class="order-context">
            <span><i data-lucide="map-pin"></i>${this.esc(this.getTable(order) || 'No table')}</span>
            <span><i data-lucide="${this.getOrderType(order) === 'TAKEAWAY' ? 'shopping-bag' : 'utensils'}"></i>${this.esc(this.orderTypeLabel(order))}</span>
          </div>
        </div>

        <div class="order-card-body">
          <div class="items-summary">
            <span class="items-count">${this.itemCount(order)} Item${this.itemCount(order) === 1 ? '' : 's'}</span>
            <button class="view-items-btn" onclick="ordersApp.showOrderDetails('${this.escAttr(order.id)}')"><i data-lucide="eye"></i> Show items</button>
          </div>
          ${!items.length && order.customOrderText ? `<div class="text-muted text-sm" style="margin-top:.5rem">${this.esc(order.customOrderText)}</div>` : ''}
          ${note}
          ${payment}
        </div>

        <div class="order-card-footer">
          ${this.hasPriceAccess() ? `<div class="total-row"><span class="total-label">Total</span><span class="total-value">₹${Number(order.totalAmount || 0).toFixed(2)}</span></div>` : ''}
          ${actions}
          ${bill}
        </div>
      </article>`;
  }

  statusPill(status) {
    const map = {
      PENDING:['pending','hourglass','Pending'],
      ACCEPTED:['accepted','check-square-2','Accepted'],
      PREPARING:['preparing','chef-hat','Preparing'],
      READY:['ready','circle-check','Ready'],
      COMPLETED:['completed','check-check','Completed'],
      CANCELLED:['completed','x-circle','Cancelled']
    };
    const x = map[status] || ['completed','circle','Unknown'];
    return `<span class="status-pill ${x[0]}"><i data-lucide="${x[1]}"></i>${x[2]}</span>`;
  }

  primaryAction(order) {
    if (order.status === 'ACCEPTED' && this.hasPermission('order.prepare')) return {status:'PREPARING',label:'Start Preparing',icon:'chef-hat'};
    if (order.status === 'PREPARING' && this.hasPermission('order.ready')) return {status:'READY',label:'Mark Ready',icon:'bell-ring'};
    if (order.status === 'READY' && this.hasPermission('order.complete')) return {status:'COMPLETED',label:'Complete Order',icon:'check-circle-2'};
    return null;
  }

  canPrint() { return this.isAdmin() || this.isCounterStaff() || this.permissions().includes('order.print'); }
  hasPriceAccess() { return this.isAdmin() || this.permissions().some(p => ['order.accept','order.pending','order.complete'].includes(p)); }

  getTable(order) {
    if (!order) return null;
    let key;
    if (order.customFields) {
        key = Object.keys(order.customFields).find(k => k.toLowerCase().includes('table'));
        if (key) return order.customFields[key];
    }
    if (order.metadata) {
        key = Object.keys(order.metadata).find(k => k.toLowerCase().includes('table'));
        if (key) return order.metadata[key];
    }
    return null;
  }

  getOrderType(order) {
    let raw = '';
    if (order.customFields) {
        const typeKey = Object.keys(order.customFields).find(k => k.toLowerCase().includes('order') && k.toLowerCase().includes('type'));
        if (typeKey) raw = order.customFields[typeKey];
    }
    if (!raw && order.metadata) {
        raw = order.metadata.orderType || '';
    }
    if (!raw) raw = order.orderType || '';
    
    raw = String(raw).toUpperCase().replace(/[-\s]/g,'_');
    if (raw.includes('TAKE')) return 'TAKEAWAY';
    if (raw.includes('DELIVERY')) return 'DELIVERY';
    return 'DINE_IN';
  }

  orderTypeLabel(order) {
    return {DINE_IN:'Dine-in',TAKEAWAY:'Takeaway',DELIVERY:'Delivery'}[this.getOrderType(order)] || 'Order';
  }

  paymentMethod(order) {
    const method = String(order?.metadata?.paymentMethod || '').toUpperCase();
    return method === 'CASHFREE' ? 'ONLINE' : 'OFFLINE';
  }

  matchesPayment(order, filter) {
    if (filter === 'ONLINE') return this.paymentMethod(order) === 'ONLINE';
    if (filter === 'OFFLINE') return this.paymentMethod(order) === 'OFFLINE';
    if (filter === 'PAID') return String(order.paymentStatus || '').toUpperCase() === 'PAID';
    if (filter === 'UNPAID') return String(order.paymentStatus || '').toUpperCase() !== 'PAID';
    return true;
  }

  paymentBadge(order) {
    const paid = String(order.paymentStatus || '').toUpperCase() === 'PAID';
    const online = this.paymentMethod(order) === 'ONLINE';
    if (paid) return `<span class="payment-badge">${online ? '✓ Paid · Online' : '✓ Paid · Offline'}</span>`;
    return `<span class="payment-badge unpaid">${online ? 'Online payment' : 'Pay at counter'}</span>`;
  }

  itemCount(order) {
    return (order.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0);
  }

  waitingMinutes(order) {
    const date = new Date(order.createdAt);
    if (Number.isNaN(date.getTime())) return 0;
    return Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
  }

  waitingLabel(order) {
    const m = this.waitingMinutes(order);
    if (m < 1) return 'Just now';
    if (m < 60) return `${m}m`;
    const h = Math.floor(m / 60);
    return `${h}h ${m % 60}m`;
  }

  showOrderDetails(orderId) {
    const order = this.orders.find(o => o.id === orderId);
    if (!order) return;

    const table = this.getTable(order);
    const items = order.items || [];
    const itemHtml = items.length ? items.map(item => {
      const custom = (item.selectedCustomizations || []).flatMap(c => c.selectedOptions || []).map(o => `<span class="custom-chip">+ ${this.esc(o.name)}</span>`).join('');
      const instruction = item.specialInstructions ? `<div class="instruction">${this.esc(item.specialInstructions)}</div>` : '';
      return `<div class="details-item">
        <div class="qty-box">${Number(item.quantity || 0)}×</div>
        <div class="details-item-main">
          <div style="display:flex;justify-content:space-between;gap:.5rem"><div class="details-item-name">${this.esc(item.menuItemName)}</div>${this.hasPriceAccess() ? `<div class="details-item-price">₹${Number(item.totalPrice || 0).toFixed(2)}</div>` : ''}</div>
          ${custom ? `<div class="custom-chips">${custom}</div>` : ''}${instruction}
        </div>
      </div>`;
    }).join('') : `<div class="details-item">${this.esc(order.customOrderText || 'No item details available')}</div>`;

    let price = '';
    if (!this.isKitchen() && this.hasPriceAccess()) {
      price = `<div class="details-price">
        ${order.subtotal != null ? `<div class="price-row"><span>Subtotal</span><strong>₹${Number(order.subtotal).toFixed(2)}</strong></div>` : ''}
        ${order.couponDiscount > 0 ? `<div class="price-row discount"><span>Coupon${order.couponCode ? ' ('+this.esc(order.couponCode)+')' : ''}</span><strong>-₹${Number(order.couponDiscount).toFixed(2)}</strong></div>` : ''}
        ${order.taxAmount > 0 ? `<div class="price-row"><span>${this.esc(order.taxName || 'Tax')}</span><strong>₹${Number(order.taxAmount).toFixed(2)}</strong></div>` : ''}
        ${order.serviceChargeAmount > 0 ? `<div class="price-row"><span>${this.esc(order.serviceChargeName || 'Service Charge')}</span><strong>₹${Number(order.serviceChargeAmount).toFixed(2)}</strong></div>` : ''}
        <div class="price-row total"><span>Total</span><strong>₹${Number(order.totalAmount || 0).toFixed(2)}</strong></div>
        <div style="margin-top:.65rem">${this.paymentBadge(order)}</div>
      </div>`;
    }

    const billActions = this.canPrint() && ['READY','COMPLETED'].includes(order.status) ? `
      <div class="details-bill-actions">
        <button onclick="ordersApp.printBill('${this.escAttr(order.id)}','view')"><i data-lucide="eye"></i> View Bill</button>
        <button onclick="ordersApp.printBill('${this.escAttr(order.id)}','download')"><i data-lucide="printer"></i> Print / PDF</button>
      </div>` : '';

    let customFieldsHtml = '';
    const displayCustomFields = order.customFields || {};
    if (Object.keys(displayCustomFields).length > 0) {
        customFieldsHtml = '<div class="details-section" style="margin-top: 1rem;"><div class="details-section-title">Order Information</div>';
        for (const [k, v] of Object.entries(displayCustomFields)) {
            let label = k.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
            if (this.getUser()?.settings && this.getUser()?.settings.customFields) {
                const cfDef = this.getUser()?.settings.customFields.find(f => f.id === k);
                if (cfDef && cfDef.label) label = cfDef.label;
            }
            customFieldsHtml += `<div style="display:flex; justify-content:space-between; padding: 4px 0; border-bottom: 1px solid #f1f5f9; font-size: 0.9rem;">
                <span style="color: #64748b;">${this.esc(label)}</span>
                <span style="font-weight: 500;">${this.esc(v)}</span>
            </div>`;
        }
        customFieldsHtml += '</div>';
    }

    document.getElementById('order-modal-title').innerHTML = `Order #${this.esc(order.queueNumber)} ${this.statusPill(order.status)}`;
    document.getElementById('order-modal-body').innerHTML = `
      <div class="order-details-body">
        <div class="details-hero">
          <div><div class="details-table-number">${this.esc(table || '-')}</div><div class="details-meta">${this.esc(this.orderTypeLabel(order))} &bull; Placed ${this.waitingLabel(order)} ago</div></div>
          ${this.statusPill(order.status)}
        </div>
        <div class="details-section"><div class="details-section-title">Items (${this.itemCount(order)})</div>${itemHtml}</div>
        ${customFieldsHtml}
        ${order.customerNote ? `<div class="details-note"><strong>Customer note</strong><br>${this.esc(order.customerNote)}</div>` : ''}
        ${price}${billActions}
      </div>`;

    if (window.lucide) lucide.createIcons();
    if (window.openModal) window.openModal('order-modal');
    else document.getElementById('order-modal-overlay')?.classList.add('active');
  }

  openMenu(event, orderId) {
    event.stopPropagation();
    document.querySelector('.order-context-menu')?.remove();
    const order = this.orders.find(o => o.id === orderId);
    if (!order) return;
    const menu = document.createElement('div');
    menu.className = 'order-context-menu';
    menu.innerHTML = `
      <button onclick="ordersApp.showOrderDetails('${this.escAttr(orderId)}');this.parentElement.remove()"><i data-lucide="eye"></i> View Order</button>
      ${this.canPrint() && ['READY','COMPLETED'].includes(order.status) ? `
        <button onclick="ordersApp.printBill('${this.escAttr(orderId)}','view');this.parentElement.remove()"><i data-lucide="receipt"></i> View Bill</button>
        <button onclick="ordersApp.printBill('${this.escAttr(orderId)}','download');this.parentElement.remove()"><i data-lucide="download"></i> Download PDF</button>` : ''}
    `;
    document.body.appendChild(menu);
    const rect = event.currentTarget.getBoundingClientRect();
    menu.style.left = `${Math.min(rect.right - 165, window.innerWidth - 175)}px`;
    menu.style.top = `${Math.min(rect.bottom + 5, window.innerHeight - 120)}px`;
    if (window.lucide) lucide.createIcons();
    setTimeout(() => document.addEventListener('click', () => menu.remove(), {once:true}), 0);
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

  printBill(orderId, action = 'download') {
    const order = this.orders.find(o => o.id === orderId);
    if (!order) return;

    const user = this.getUser() || {};
    const shopName = user.shopName || 'DeQueue Shop';
    const address = user.address?.street || '';
    const phone = user.phone || '';
    const email = user.email || '';
    const subtotal = order.subtotal != null ? Number(order.subtotal) : (order.items || []).reduce((sum,item) => sum + Number(item.totalPrice || 0), 0);
    const settings = user.settings || {};
    const taxName = order.taxName || settings.taxName || 'Tax';
    const taxValue = order.taxAmount != null ? Number(order.taxAmount) : ((subtotal * Number(settings.taxPercentage || 0)) / 100);
    const taxPct = Number(settings.taxPercentage || 0);
    const chargeName = order.serviceChargeName || settings.additionalChargeName || 'Service Charge';
    const chargeAmt = order.serviceChargeAmount != null ? Number(order.serviceChargeAmount) : Number(settings.additionalCharges || 0);
    const couponName = order.couponCode ? `Coupon (${order.couponCode})` : 'Coupon Discount';
    const couponDiscount = Number(order.couponDiscount || 0);
    const computedTotal = order.totalAmount != null ? Number(order.totalAmount) : subtotal + taxValue + chargeAmt - couponDiscount;
    const gstNumber = settings.gstNumber || '';

    const items = (order.items || []).map(i => `<tr>
      <td style="padding:8px 0;border-bottom:1px dashed #ccc;">${this.esc(i.menuItemName)}
        ${(i.selectedCustomizations || []).length ? '<br><small style="color:#666">+' + i.selectedCustomizations.map(c => (c.selectedOptions || []).map(o => this.esc(o.name)).join(',')).join(',') + '</small>' : ''}
      </td>
      <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:center;">${Number(i.quantity || 0)}</td>
      <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:right;">₹${Number(i.unitPrice || 0).toFixed(2)}</td>
      <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:right;">₹${Number(i.totalPrice || 0).toFixed(2)}</td>
    </tr>`).join('');

    const html = `<h2>${this.esc(shopName)}</h2>
      <div class="info">${address ? this.esc(address)+'<br>' : ''}${phone ? 'Ph: '+this.esc(phone)+'<br>' : ''}${email ? this.esc(email)+'<br>' : ''}${gstNumber ? 'GSTIN: '+this.esc(gstNumber) : ''}</div>
      <div class="divider"></div>
      <div class="order-meta"><div><strong>Order #:</strong> ${this.esc(order.queueNumber)}</div><div><strong>Date:</strong> ${new Date().toLocaleString()}</div></div>
      <div class="divider"></div>
      <table><thead><tr><th>Item</th><th style="text-align:center">Qty</th><th style="text-align:right">Price</th><th style="text-align:right">Total</th></tr></thead><tbody>${items}</tbody></table>
      <table class="totals-table">
        <tr><td>Subtotal</td><td style="text-align:right">₹${subtotal.toFixed(2)}</td></tr>
        ${couponDiscount > 0 ? `<tr><td>${this.esc(couponName)}</td><td style="text-align:right">-₹${couponDiscount.toFixed(2)}</td></tr>` : ''}
        ${taxValue > 0 ? `<tr><td>${this.esc(taxName)} ${order.taxAmount == null && taxPct > 0 ? '('+taxPct+'%)' : ''}</td><td style="text-align:right">₹${taxValue.toFixed(2)}</td></tr>` : ''}
        ${chargeAmt > 0 ? `<tr><td>${this.esc(chargeName)}</td><td style="text-align:right">₹${chargeAmt.toFixed(2)}</td></tr>` : ''}
        <tr><td class="grand-total">Total</td><td class="grand-total" style="text-align:right">₹${computedTotal.toFixed(2)}</td></tr>
      </table><div class="divider"></div><div class="footer">Thank you for visiting!<br>Powered by DeQueue</div>`;

    const fullHtml = `<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Invoice</title><style>
      .invoice-receipt {font-family:'Courier New',Courier,monospace;max-width:350px;margin:0 auto;padding:20px;color:#000;font-size:14px;line-height:1.4}
      .invoice-receipt h2{text-align:center;margin:0 0 5px;font-size:22px;text-transform:uppercase}.invoice-receipt .info{text-align:center;margin-bottom:15px;font-size:12px}
      .invoice-receipt .divider{border-top:1px dashed #000;margin:10px 0}.invoice-receipt .order-meta{margin-bottom:15px;font-size:13px}
      .invoice-receipt table{width:100%;border-collapse:collapse;margin-bottom:15px;font-size:13px}.invoice-receipt th{text-align:left;border-bottom:1px solid #000;padding-bottom:5px}
      .invoice-receipt td{color:#000;}
      .invoice-receipt .totals-table td{padding:3px 0}.invoice-receipt .grand-total{font-size:18px;font-weight:bold;border-top:1px dashed #000;padding-top:8px}.invoice-receipt .footer{text-align:center;font-size:11px;margin-top:20px}
      @media print{ .invoice-receipt {max-width:100%;padding:0;} }
    </style></head><body style="background:#fff;margin:0;padding:0;"><div class="invoice-receipt" style="padding:20px;max-width:350px;margin:0 auto;background:#fff;">${html}</div></body></html>`;

    if (action === 'view') {
      let overlay = document.getElementById('invoice-modal-overlay');
      if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'invoice-modal-overlay';
        overlay.className = 'modal-overlay';
        overlay.innerHTML = `
          <div class="modal" style="max-width: 450px; margin: auto; display: flex; flex-direction: column; height: 85vh; max-height: 800px; padding: 0; overflow: hidden; background: #fff; border-radius: 12px;">
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 16px; border-bottom: 1px solid #eee;">
              <h3 style="margin:0; font-size: 18px; font-weight: bold; color: var(--dq-text-main);">Order Receipt</h3>
              <button class="btn-icon" style="color: var(--dq-text-main);" onclick="this.closest('.modal-overlay').classList.remove('active')"><i data-lucide="x"></i></button>
            </div>
            <div style="flex: 1; padding: 0; background: #f4f2f0; display: flex; justify-content: center; align-items: flex-start; padding-top: 20px; overflow-y: auto;">
              <iframe id="invoice-iframe" style="width: 350px; height: 100%; min-height: 500px; border: 1px solid #ddd; background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.05); border-radius: 4px;"></iframe>
            </div>
            <div style="padding: 16px; border-top: 1px solid #eee; display: flex; gap: 12px; justify-content: flex-end; background: #fff;">
              <button class="btn btn-secondary" onclick="document.getElementById('invoice-iframe').contentWindow.print()" style="flex: 1;"><i data-lucide="printer" style="width:16px;height:16px;margin-right:6px;display:inline;vertical-align:text-bottom;"></i> Print</button>
              <button class="btn btn-primary" id="invoice-download-btn" style="flex: 1;"><i data-lucide="download" style="width:16px;height:16px;margin-right:6px;display:inline;vertical-align:text-bottom;"></i> Download</button>
            </div>
          </div>
        `;
        document.body.appendChild(overlay);
        if (window.lucide) lucide.createIcons();
      }
      
      const iframe = document.getElementById('invoice-iframe');
      iframe.srcdoc = fullHtml;
      
      document.getElementById('invoice-download-btn').onclick = () => {
         this.printBill(orderId, 'download');
      };
      
      setTimeout(() => overlay.classList.add('active'), 50);
      return;
    }
    
    // Download PDF Action
    if (window.showToast) showToast('Generating PDF...', 'info');
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = fullHtml;

    const worker = html2pdf().set({
      margin:10,
      filename:`bill-${order.queueNumber || 'order'}.pdf`,
      image:{type:'jpeg',quality:.98},
      html2canvas:{scale:2},
      jsPDF:{unit:'mm',format:'a5',orientation:'portrait'}
    }).from(tempDiv);

    worker.save().then(() => {
      if (window.showToast) showToast('PDF downloaded','success');
    }).catch(err => {
      console.error('PDF generation failed',err);
      if (window.showToast) showToast('Failed to generate PDF','error');
    });
  }

  renderEmpty(title, text, icon='inbox') {
    const markup = `<div class="empty-orders"><i data-lucide="${icon}"></i><h4>${this.esc(title)}</h4><div>${this.esc(text)}</div></div>`;
    const grid = document.getElementById('orders-grid');
    const table = document.getElementById('orders-table-wrap');
    if (this.currentView === 'cards' && grid) { grid.className='orders-grid empty'; grid.innerHTML=markup; }
    if (this.currentView === 'table' && table) table.innerHTML=markup;
    if (window.lucide) lucide.createIcons();
  }

  emptyMarkup() {
    const completed = this.currentFilter === 'COMPLETED_24H';
    return `<div class="empty-orders"><i data-lucide="${completed ? 'history' : 'inbox'}"></i><h4>${completed ? 'No completed orders in the last 24 hours' : 'No orders found'}</h4><div>${completed ? 'Completed history older than 24 hours is intentionally hidden here.' : 'Try changing the status, search, or secondary filters.'}</div></div>`;
  }

  esc(value) {
    if (value == null) return '';
    return String(value).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#039;');
  }

  escAttr(value) { return this.esc(value); }
}

document.addEventListener('DOMContentLoaded', () => {
  window.ordersApp = new Orders();
});
