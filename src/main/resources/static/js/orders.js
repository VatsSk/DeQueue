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
    const isVendorAdmin = user && (
      (user.roleNames && Array.isArray(user.roleNames) && user.roleNames.some(r => typeof r === 'string' && (r.toUpperCase() === 'ROLE_VENDOR_ADMIN' || r.toUpperCase() === 'VENDOR_ADMIN'))) ||
      (user.roleName && typeof user.roleName === 'string' && (user.roleName.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.roleName.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.role && typeof user.role === 'string' && (user.role.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.role.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.role && user.role.name && (user.role.name.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.role.name.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.roles && Array.isArray(user.roles) && user.roles.some(r => {
        const rName = typeof r === 'string' ? r : (r.name || '');
        return rName.toUpperCase() === 'ROLE_VENDOR_ADMIN' || rName.toUpperCase() === 'VENDOR_ADMIN';
      }))
    );
    const hasAdminAccess = isPlatformAdmin || isVendorAdmin;

    // Determine tabs based on action permissions
    let tabs = [{ id: 'ALL', label: 'All Active' }];

    if (hasAdminAccess || permissions.includes('order.accept') || permissions.includes('order.pending')) {
      tabs.push({ id: 'PENDING', label: 'Pending' });
    }
    if (hasAdminAccess || permissions.includes('order.prepare')) {
      tabs.push({ id: 'ACCEPTED', label: 'Accepted' });
    }
    if (hasAdminAccess || permissions.includes('order.ready')) {
      tabs.push({ id: 'PREPARING', label: 'Preparing' });
    }
    if (hasAdminAccess || permissions.includes('order.complete')) {
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
      // /orders/active already returns full OrderResponse objects (including items)
      const res = await api.get('/orders/active');
      if (res.success && res.data) {
        this.orders = res.data;
        this.updateFilterCounts();
        this.renderOrders();
      }
    } catch (e) {
      console.error('Failed to fetch orders', e);
    }
  }

  updateFilterCounts() {
    const allCount = this.orders.length;
    const pendingCount = this.orders.filter(o => o.status === 'PENDING').length;
    const acceptedCount = this.orders.filter(o => o.status === 'ACCEPTED').length;
    const preparingCount = this.orders.filter(o => o.status === 'PREPARING').length;
    const readyCount = this.orders.filter(o => o.status === 'READY').length;

    const tabs = document.querySelectorAll('.filter-tab');
    tabs.forEach(tab => {
      const filterId = tab.getAttribute('data-filter');
      if (filterId === 'ALL') tab.innerText = `All Active (${allCount})`;
      else if (filterId === 'PENDING') tab.innerText = `Pending (${pendingCount})`;
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
    const isVendorAdmin = user && (
      (user.roleNames && Array.isArray(user.roleNames) && user.roleNames.some(r => typeof r === 'string' && (r.toUpperCase() === 'ROLE_VENDOR_ADMIN' || r.toUpperCase() === 'VENDOR_ADMIN'))) ||
      (user.roleName && typeof user.roleName === 'string' && (user.roleName.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.roleName.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.role && typeof user.role === 'string' && (user.role.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.role.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.role && user.role.name && (user.role.name.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.role.name.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.roles && Array.isArray(user.roles) && user.roles.some(r => {
        const rName = typeof r === 'string' ? r : (r.name || '');
        return rName.toUpperCase() === 'ROLE_VENDOR_ADMIN' || rName.toUpperCase() === 'VENDOR_ADMIN';
      }))
    );
    const isCounterStaff = user && (
      (user.roleNames && Array.isArray(user.roleNames) && user.roleNames.some(r => typeof r === 'string' && (r.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || r.toUpperCase() === 'VENDORCOUNTER_STAFF'))) ||
      (user.roleName && typeof user.roleName === 'string' && (user.roleName.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || user.roleName.toUpperCase() === 'VENDORCOUNTER_STAFF')) ||
      (user.role && typeof user.role === 'string' && (user.role.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || user.role.toUpperCase() === 'VENDORCOUNTER_STAFF')) ||
      (user.role && user.role.name && (user.role.name.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || user.role.name.toUpperCase() === 'VENDORCOUNTER_STAFF')) ||
      (user.roles && Array.isArray(user.roles) && user.roles.some(r => {
        const rName = typeof r === 'string' ? r : (r.name || '');
        return rName.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || rName.toUpperCase() === 'VENDORCOUNTER_STAFF';
      }))
    );
    const hasAdminAccess = isPlatformAdmin || isVendorAdmin;
    const hasPriceAccess = hasAdminAccess || permissions.includes('order.accept') || permissions.includes('order.pending') || permissions.includes('order.complete');

    const isKitchen = user && (
      (user.roleName && user.roleName.toUpperCase() === 'KITCHEN') ||
      (user.role && typeof user.role === 'string' && user.role.toUpperCase() === 'KITCHEN') ||
      (user.role && user.role.name && user.role.name.toUpperCase() === 'KITCHEN') ||
      (user.roles && Array.isArray(user.roles) && user.roles.some(r => (typeof r === 'string' && r.toUpperCase() === 'KITCHEN') || (r.name && r.name.toUpperCase() === 'KITCHEN')))
    );

    let html = '';
    filtered.forEach(order => {
      let borderColor = 'var(--border)';
      let badgeClass = 'badge-pending';
      let actionsHtml = '';

      if (order.status === 'PENDING') {
        badgeClass = 'badge-pending';
        if (hasAdminAccess || permissions.includes('order.accept') || permissions.includes('order.pending')) {
          actionsHtml = `
              <button class="btn btn-danger flex-1" onclick="ordersApp.updateStatus('${order.id}', 'CANCELLED')"><i data-lucide="x" style="width:16px;height:16px;margin-right:4px;"></i> Reject</button>
              <button class="btn btn-primary flex-1" onclick="ordersApp.updateStatus('${order.id}', 'ACCEPTED')"><i data-lucide="check" style="width:16px;height:16px;margin-right:4px;"></i> Accept</button>
          `;
        }
      } else if (order.status === 'ACCEPTED') {
        borderColor = 'var(--warning)';
        badgeClass = 'badge-accepted';
        if (hasAdminAccess || permissions.includes('order.prepare')) {
          actionsHtml = `<button class="btn btn-primary w-full" onclick="ordersApp.updateStatus('${order.id}', 'PREPARING')"><i data-lucide="chef-hat" style="width:16px;height:16px;margin-right:4px;"></i> Start Preparing</button>`;
        }
      } else if (order.status === 'PREPARING') {
        borderColor = 'var(--info)';
        badgeClass = 'badge-preparing';
        if (hasAdminAccess || permissions.includes('order.ready')) {
          actionsHtml = `<button class="btn btn-primary w-full" onclick="ordersApp.updateStatus('${order.id}', 'READY')"><i data-lucide="bell-ring" style="width:16px;height:16px;margin-right:4px;"></i> Mark Ready</button>`;
        }
      } else if (order.status === 'READY') {
        borderColor = 'var(--success)';
        badgeClass = 'badge-ready';
        const canComplete = hasAdminAccess || permissions.includes('order.complete');

        if (canComplete) {
          actionsHtml = `
            <button class="btn btn-success w-full" onclick="ordersApp.updateStatus('${order.id}', 'COMPLETED')">
              <i data-lucide="check-circle-2" style="width:16px;height:16px;margin-right:4px;"></i> Complete
            </button>
          `;
        }
      } else {
         actionsHtml = `<button class="btn btn-secondary w-full" disabled>${order.status}</button>`;
      }

      const statusLabels = {
        PENDING: 'Order Initiated',
        ACCEPTED: 'Order Accepted',
        PREPARING: 'Started Preparing',
        READY: 'Prepared',
        COMPLETED: 'Completed',
        CANCELLED: 'Cancelled'
      };

        let itemCount = order.items
            ? order.items.reduce((acc, it) => acc + it.quantity, 0)
            : 0;

        let itemsHtml = `
            <div class="flex justify-between items-center pb-3 border-b border-border mb-3">
                <div class="flex items-center gap-3">
                    <div class="bg-primary/10 text-primary p-2 rounded-md">
                        <i data-lucide="shopping-bag" style="width:20px;height:20px;"></i>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-[10px] text-muted-foreground uppercase tracking-wider font-bold mb-0.5">Order Items</span>
                        <span class="font-extrabold text-base text-foreground leading-none">${itemCount} Items</span>
                    </div>
                </div>
                <button class="btn btn-sm flex items-center gap-1.5 shadow-sm rounded-md px-3 py-1.5 transition" style="background-color: var(--info); color: white; border: none;" onclick="ordersApp.showOrderDetails('${order.id}')" title="View Full Order">
                    <i data-lucide="eye" style="width:14px;height:14px;"></i>
                    <span class="font-medium text-xs">View Items</span>
                </button>
            </div>
        `;

        if (order.customOrderText && (!order.items || order.items.length === 0)) {
            itemsHtml += `
            <div class="mb-3 bg-surface border border-border p-3 rounded-md shadow-sm">
                <span class="text-[10px] text-muted-foreground uppercase tracking-wider font-bold flex items-center gap-1 mb-1"><i data-lucide="pen-tool" style="width:12px;height:12px;"></i> Custom Request</span>
                <span class="text-sm font-medium leading-relaxed block text-foreground">${this._escHtml(order.customOrderText)}</span>
            </div>
            `;
        }

// Customer note
        let noteHtml = order.customerNote
            ? `
        <div class="mb-3 bg-warning/10 border-l-4 border-warning p-3 rounded-md shadow-sm">
            <span class="text-[10px] text-warning-foreground uppercase tracking-wider font-bold flex items-center gap-1 mb-1.5"><i data-lucide="user" style="width:12px;height:12px;"></i> User Message</span>
            <div class="text-sm font-medium text-warning-foreground leading-snug">
                ${this._escHtml(order.customerNote)}
            </div>
        </div>
      `
            : '';

// Metadata
        let metadataHtml = '';
        if (order.metadata && Object.keys(order.metadata).length > 0) {
            metadataHtml = `
        <div class="mb-3 grid grid-cols-2 gap-2 bg-surface-hover p-2.5 rounded-md border border-border/50">
            ${Object.entries(order.metadata)
                .map(([k, v]) => `
                    <div class="flex flex-col">
                        <span class="text-[10px] text-muted-foreground uppercase tracking-wider font-bold mb-0.5">${this._escHtml(k)}</span>
                        <span class="text-sm font-medium text-foreground leading-tight">${this._escHtml(v)}</span>
                    </div>
                `)
                .join('')}
        </div>
    `;
        }

// Payment status
        let paymentHtml = '';
        if (hasPriceAccess) {
            const method = (order.metadata && order.metadata.paymentMethod) || 'OFFLINE';
            if (method === 'CASHFREE') {
                paymentHtml = `
                <div class="mt-2">
                    <div class="text-sm font-bold bg-yellow-50 text-yellow-600 p-2 rounded-md border border-yellow-200 flex items-center justify-center gap-2 shadow-sm">
                        <i data-lucide="zap" style="width:16px;height:16px;fill:currentColor;"></i> Online Payment
                    </div>
                </div>`;
            } else {
                paymentHtml = `
                <div class="mt-2">
                    <div class="text-sm font-bold bg-danger/10 text-danger p-2 rounded-md border border-danger/20 flex items-center justify-center gap-2 shadow-sm">
                        <i data-lucide="wallet" style="width:16px;height:16px;"></i> Pay at Counter
                    </div>
                </div>`;
            }
        }

// Feedback 
        let feedbackHtml = '';
        if (order.status === 'COMPLETED' && (order.rating || order.feedback)) {
            let stars = '';
            const rating = order.rating || 0;
            for(let i = 1; i <= 5; i++) {
                stars += `<i data-lucide="star" style="width:12px;height:12px; margin-right:2px;" class="${i <= rating ? 'fill-warning text-warning' : 'text-muted/30'}"></i>`;
            }
            feedbackHtml = `
            <div class="mt-3 p-3 bg-indigo-50 border border-indigo-100 rounded-md shadow-sm">
                <div class="flex items-center justify-between mb-1.5">
                    <span class="text-[10px] text-indigo-800 uppercase tracking-wider font-bold flex items-center gap-1"><i data-lucide="message-square-heart" style="width:12px;height:12px;"></i> Customer Feedback</span>
                    <div class="flex items-center">${stars}</div>
                </div>
                ${order.feedback ? `<div class="text-sm font-medium text-indigo-900 mt-1 leading-snug">${this._escHtml(order.feedback)}</div>` : ''}
            </div>
            `;
        }

// Bill Icons (Always visible when Prepared or Completed)
        let billIconsHtml = '';
        if (order.status === 'READY' || order.status === 'COMPLETED') {
            const canPrint = hasAdminAccess || isCounterStaff || permissions.includes('order.print');
            if (canPrint) {
                billIconsHtml = `
                    <div class="absolute -bottom-4 right-4 bg-surface border border-border shadow-lg rounded-full flex items-center px-1.5 py-1 z-10">
                        <button class="btn-icon p-1.5 text-primary hover:bg-primary/10 rounded-full transition-colors flex items-center gap-1 font-bold text-xs pr-2" onclick="ordersApp.printBill('${order.id}', 'view')" title="View Bill">
                            <i data-lucide="receipt" style="width:14px;height:14px;"></i> Bill
                        </button>
                        <div class="w-px h-4 bg-border mx-0.5"></div>
                        <button class="btn-icon p-1.5 text-primary hover:bg-primary/10 rounded-full transition-colors" onclick="ordersApp.printBill('${order.id}', 'download')" title="Download Bill">
                            <i data-lucide="download" style="width:14px;height:14px;"></i>
                        </button>
                    </div>
                `;
            }
        }

// Final card
        html += `
    <div class="card p-0 flex flex-col transition-shadow hover:shadow-md relative"
         style="border-left:4px solid ${borderColor}; min-height:220px; height:fit-content; max-height:none; margin-bottom:16px;">

        <!-- Header -->
        <div class="p-4 flex-1">

            <div class="flex justify-between items-start mb-3 pb-2 border-b border-border">

                <div class="flex items-center">
                    <div>
                        <span class="font-extrabold ${isKitchen ? 'text-2xl' : 'text-xl'} text-primary">
                            #${order.queueNumber}
                        </span>

                        <div class="text-xs text-muted mt-1 flex items-center gap-1">
                            <i data-lucide="clock" style="width:12px;height:12px;"></i>
                            ${new Date(order.createdAt).toLocaleTimeString()}
                        </div>
                    </div>
                </div>

                <div class="flex flex-col items-end gap-2">
                    <span class="badge ${badgeClass} shadow-sm px-2 py-1">
                        ${statusLabels[order.status] || order.status}
                    </span>
                </div>
            </div>

            <!-- Items -->
            <div class="order-items-list overflow-y-auto"
                 style="max-height:250px;">

                ${itemsHtml}
                ${noteHtml}
                ${metadataHtml}
                ${paymentHtml}
                ${feedbackHtml}

            </div>
        </div>

        <!-- Footer -->
        <div class="p-4 bg-surface-hover/50 border-t border-border rounded-b-xl">

            ${
            hasPriceAccess
                ? `
                        <div class="flex justify-between items-center mb-3 text-sm">
                            <span class="font-medium text-muted">
                                Total Amount
                            </span>

                            <span class="font-extrabold text-lg text-primary">
                                ₹${order.totalAmount.toFixed(2)}
                            </span>
                        </div>
                      `
                : ''
        }

            <div class="flex gap-2 w-full">
                ${actionsHtml}
            </div>

        </div>
        ${billIconsHtml}
    </div>
`;
    });

    grid.innerHTML = html;
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  _escHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  printBill(orderId, action = 'download') {
    const order = this.orders.find(o => o.id === orderId);
    if (!order) return;

    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : {};
    const shopName = user.shopName || 'DeQueue Shop';
    const address = user.address && user.address.street ? user.address.street : '';
    const phone = user.phone || '';
    const email = user.email || '';
    
    // Read from order or fallback to settings
    const subtotal = order.subtotal != null ? order.subtotal : (order.items ? order.items.reduce((sum, item) => sum + item.totalPrice, 0) : order.totalAmount);
    
    const settings = user.settings || {};
    const taxName = order.taxName || settings.taxName || 'Tax';
    const taxValue = order.taxAmount != null ? order.taxAmount : ((subtotal * (settings.taxPercentage || 0)) / 100);
    const taxPct = settings.taxPercentage || 0; // only used for label if fallback
    
    const chargeName = order.serviceChargeName || settings.additionalChargeName || 'Service Charge';
    const chargeAmt = order.serviceChargeAmount != null ? order.serviceChargeAmount : (settings.additionalCharges || 0);
    
    const couponName = order.couponCode ? `Coupon (${order.couponCode})` : 'Coupon Discount';
    const couponDiscount = order.couponDiscount || 0;
    
    let computedTotal = order.totalAmount != null ? order.totalAmount : (subtotal + taxValue + chargeAmt - couponDiscount);
    const gstNumber = settings.gstNumber || '';

    const items = (order.items || []).map(i => `
      <tr>
        <td style="padding:8px 0;border-bottom:1px dashed #ccc;">
            ${this._escHtml(i.menuItemName)}
            ${i.selectedCustomizations && i.selectedCustomizations.length > 0 ? 
                '<br><small style="color:#666">+' + i.selectedCustomizations.map(c => c.selectedOptions.map(o => o.name).join(',')).join(',') + '</small>' : ''}
        </td>
        <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:center;">${i.quantity}</td>
        <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:right;">₹${Number(i.unitPrice || 0).toFixed(2)}</td>
        <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:right;">₹${Number(i.totalPrice || 0).toFixed(2)}</td>
      </tr>`).join('');

    let metadataHtml = '';
    if (order.metadata && Object.keys(order.metadata).length > 0) {
        metadataHtml = `<div class="divider"></div><div class="order-meta">` + Object.entries(order.metadata).map(([k,v]) => `<div><strong>${this._escHtml(k)}:</strong> ${this._escHtml(v)}</div>`).join('') + `</div>`;
    }

    const date = new Date().toLocaleString();

    const html = `
      <h2>${this._escHtml(shopName)}</h2>
      <div class="info">
        ${address ? address + '<br>' : ''}
        ${phone ? 'Ph: ' + phone + '<br>' : ''}
        ${email ? email + '<br>' : ''}
        ${gstNumber ? 'GSTIN: ' + this._escHtml(gstNumber) : ''}
      </div>
      
      <div class="divider"></div>
      
      <div class="order-meta">
        <div><strong>Order #:</strong> ${order.queueNumber}</div>
        <div><strong>Date:</strong> ${date}</div>
      </div>
      
      ${metadataHtml}
      
      <div class="divider"></div>
      
      <table>
        <thead><tr>
          <th>Item</th><th style="text-align:center">Qty</th>
          <th style="text-align:right">Price</th><th style="text-align:right">Total</th>
        </tr></thead>
        <tbody>${items}</tbody>
      </table>
      
      <table class="totals-table">
        <tr>
            <td>Subtotal</td>
            <td style="text-align:right">₹${subtotal.toFixed(2)}</td>
        </tr>
        ${couponDiscount > 0 ? `<tr>
            <td>${this._escHtml(couponName)}</td>
            <td style="text-align:right">-₹${couponDiscount.toFixed(2)}</td>
        </tr>` : ''}
        ${taxValue > 0 ? `<tr>
            <td>${this._escHtml(taxName)} ${order.taxAmount == null && taxPct > 0 ? '('+taxPct+'%)' : ''}</td>
            <td style="text-align:right">₹${taxValue.toFixed(2)}</td>
        </tr>` : ''}
        ${chargeAmt > 0 ? `<tr>
            <td>${this._escHtml(chargeName)}</td>
            <td style="text-align:right">₹${chargeAmt.toFixed(2)}</td>
        </tr>` : ''}
        <tr>
            <td class="grand-total" style="padding-top:10px">Total</td>
            <td class="grand-total" style="text-align:right;padding-top:10px">₹${computedTotal.toFixed(2)}</td>
        </tr>
      </table>
      
      <div class="divider"></div>
      
      <div class="footer">
        Thank you for visiting!<br>
        Powered by DeQueue
      </div>
    `;

    if (window.showToast) showToast('Generating PDF...', 'info');

    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = `<style>
        body{font-family:'Courier New', Courier, monospace;max-width:350px;margin:0 auto;padding:20px;color:#000;font-size:14px;line-height:1.4}
        h2{text-align:center;margin:0 0 5px 0;font-size:22px;text-transform:uppercase}
        .info{text-align:center;margin-bottom:15px;font-size:12px}
        .divider{border-top:1px dashed #000;margin:10px 0}
        .order-meta{margin-bottom:15px;font-size:13px}
        table{width:100%;border-collapse:collapse;margin-bottom:15px;font-size:13px}
        th{text-align:left;border-bottom:1px solid #000;padding-bottom:5px}
        .totals-table{width:100%;font-size:13px}
        .totals-table td{padding:3px 0}
        .totals-table .bold{font-weight:bold}
        .totals-table .grand-total{font-size:18px;font-weight:bold;border-top:1px dashed #000;padding-top:8px;margin-top:5px}
        .footer{text-align:center;font-size:11px;margin-top:20px}
        .instruction{font-size:12px;margin-top:10px;font-style:italic;}
      </style>
      <div style="padding:20px;max-width:350px;margin:0 auto;background:#fff;">${html}</div>`;

    const opt = {
      margin:       10,
      filename:     `bill-${order.queueNumber || 'order'}.pdf`,
      image:        { type: 'jpeg', quality: 0.98 },
      html2canvas:  { scale: 2 },
      jsPDF:        { unit: 'mm', format: 'a5', orientation: 'portrait' }
    };

    const pdfWorker = html2pdf().set(opt).from(tempDiv);
    
    if (action === 'view') {
        pdfWorker.output('bloburl').then((url) => {
            window.open(url, '_blank');
        }).catch(err => {
            console.error('PDF view failed', err);
            if (window.showToast) showToast('Failed to view PDF', 'error');
        });
    } else {
        pdfWorker.save().then(() => {
            if (window.showToast) showToast('PDF downloaded', 'success');
        }).catch(err => {
            console.error('PDF generation failed', err);
            if (window.showToast) showToast('Failed to generate PDF', 'error');
        });
    }
  }

  showOrderDetails(orderId) {
    const order = this.orders.find(o => o.id === orderId);
    if (!order) return;

    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;
    const permissions = user && user.effectivePermissions ? user.effectivePermissions : [];
    const isPlatformAdmin = user ? user.platformAdmin === true : false;
    const isVendorAdmin = user && (
      (user.roleNames && Array.isArray(user.roleNames) && user.roleNames.some(r => typeof r === 'string' && (r.toUpperCase() === 'ROLE_VENDOR_ADMIN' || r.toUpperCase() === 'VENDOR_ADMIN'))) ||
      (user.roleName && typeof user.roleName === 'string' && (user.roleName.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.roleName.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.role && typeof user.role === 'string' && (user.role.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.role.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.role && user.role.name && (user.role.name.toUpperCase() === 'ROLE_VENDOR_ADMIN' || user.role.name.toUpperCase() === 'VENDOR_ADMIN')) ||
      (user.roles && Array.isArray(user.roles) && user.roles.some(r => {
        const rName = typeof r === 'string' ? r : (r.name || '');
        return rName.toUpperCase() === 'ROLE_VENDOR_ADMIN' || rName.toUpperCase() === 'VENDOR_ADMIN';
      }))
    );
    const isCounterStaff = user && (
      (user.roleNames && Array.isArray(user.roleNames) && user.roleNames.some(r => typeof r === 'string' && (r.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || r.toUpperCase() === 'VENDORCOUNTER_STAFF'))) ||
      (user.roleName && typeof user.roleName === 'string' && (user.roleName.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || user.roleName.toUpperCase() === 'VENDORCOUNTER_STAFF')) ||
      (user.role && typeof user.role === 'string' && (user.role.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || user.role.toUpperCase() === 'VENDORCOUNTER_STAFF')) ||
      (user.role && user.role.name && (user.role.name.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || user.role.name.toUpperCase() === 'VENDORCOUNTER_STAFF')) ||
      (user.roles && Array.isArray(user.roles) && user.roles.some(r => {
        const rName = typeof r === 'string' ? r : (r.name || '');
        return rName.toUpperCase() === 'ROLE_VENDORCOUNTER_STAFF' || rName.toUpperCase() === 'VENDORCOUNTER_STAFF';
      }))
    );
    const hasAdminAccess = isPlatformAdmin || isVendorAdmin;
    
    const isKitchen = user && (
      (user.roleName && user.roleName.toUpperCase() === 'KITCHEN') ||
      (user.role && typeof user.role === 'string' && user.role.toUpperCase() === 'KITCHEN') ||
      (user.role && user.role.name && user.role.name.toUpperCase() === 'KITCHEN') ||
      (user.roles && Array.isArray(user.roles) && user.roles.some(r => (typeof r === 'string' && r.toUpperCase() === 'KITCHEN') || (r.name && r.name.toUpperCase() === 'KITCHEN')))
    );

    const canPrint = hasAdminAccess || isCounterStaff || permissions.includes('order.print');
    let titleHtml = `Order #${order.queueNumber}`;
    if (canPrint && (order.status === 'READY' || order.status === 'COMPLETED')) {
        titleHtml = `
            <div class="flex items-center gap-3">
                <span>Order #${order.queueNumber}</span>
                <div class="flex items-center gap-1 bg-primary/5 rounded-md border border-primary/20 px-1 py-0.5 shadow-sm">
                    <button class="btn-icon p-1.5 text-primary hover:bg-primary/10 rounded-md transition-colors" onclick="ordersApp.printBill('${order.id}', 'view')" title="View Bill">
                        <i data-lucide="file-text" style="width:16px;height:16px;"></i>
                    </button>
                    <button class="btn-icon p-1.5 text-primary hover:bg-primary/10 rounded-md transition-colors" onclick="ordersApp.printBill('${order.id}', 'download')" title="Download Bill">
                        <i data-lucide="download" style="width:16px;height:16px;"></i>
                    </button>
                </div>
            </div>
        `;
    }
    document.getElementById('order-modal-title').innerHTML = titleHtml;

    let tableNumber = null;
    let otherMetadata = {};
    if (order.metadata) {
        for (const [k, v] of Object.entries(order.metadata)) {
            if (k.toLowerCase().includes('table')) {
                tableNumber = v;
            } else {
                otherMetadata[k] = v;
            }
        }
    }

    let statusBadgeClass = 'badge-secondary';
    if (order.status === 'PENDING') statusBadgeClass = 'badge-pending';
    else if (order.status === 'ACCEPTED') statusBadgeClass = 'badge-accepted';
    else if (order.status === 'PREPARING') statusBadgeClass = 'badge-preparing';
    else if (order.status === 'READY') statusBadgeClass = 'badge-ready';

    let topHtml = '';
    const elapsedMinutes = Math.floor((new Date() - new Date(order.createdAt)) / 60000);
    
    if (tableNumber) {
        topHtml = `
          <div class="flex items-center justify-between bg-primary text-white p-4 rounded-xl shadow-md mb-5 bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4IiBoZWlnaHQ9IjgiPgo8cmVjdCB3aWR0aD0iNCIgaGVpZ2h0PSI0IiBmaWxsPSIjZmZmIiBmaWxsLW9wYWNpdHk9IjAuMDUiLz4KPC9zdmc+')]">
             <div class="flex flex-col">
                 <span class="text-[11px] uppercase font-bold text-primary-foreground/80 tracking-widest mb-0.5">Table Number</span>
                 <span class="text-4xl font-black leading-none drop-shadow-sm">${this._escHtml(tableNumber)}</span>
             </div>
             <div class="flex flex-col text-right items-end gap-1.5">
                 <span class="badge bg-white text-primary px-3 py-1 font-bold shadow-sm uppercase border-0">${order.status}</span>
                 <span class="text-xs font-semibold flex items-center gap-1 opacity-90"><i data-lucide="clock" style="width:12px;height:12px;"></i> ${elapsedMinutes >= 0 ? elapsedMinutes : 0} mins ago</span>
             </div>
          </div>
        `;
    } else {
        topHtml = `
          <div class="flex items-center justify-between bg-surface p-4 rounded-xl border border-border shadow-sm mb-5">
             <div class="flex flex-col">
                 <span class="text-[10px] uppercase font-bold text-muted-foreground tracking-wider mb-1">Time Elapsed</span>
                 <span class="text-sm font-bold text-foreground flex items-center gap-1.5"><i data-lucide="clock" style="width:14px;height:14px;"></i> ${elapsedMinutes >= 0 ? elapsedMinutes : 0} mins ago</span>
             </div>
             <div class="flex flex-col text-right items-end">
                 <span class="text-[10px] uppercase font-bold text-muted-foreground tracking-wider mb-1">Status</span>
                 <span class="badge ${statusBadgeClass} px-3 py-1 text-xs shadow-sm uppercase">${order.status}</span>
             </div>
          </div>
        `;
    }

    let itemsHtml = `<div class="mb-3 px-1">
          <h3 class="font-extrabold text-lg text-foreground tracking-tight flex items-center gap-2"><i data-lucide="shopping-cart" style="width:18px;height:18px;" class="text-primary"></i> Order Items</h3>
      </div>
      <div class="flex flex-col gap-3 mb-6">`;

    if (order.items && order.items.length > 0) {
      order.items.forEach(item => {
          let customHtml = '';
          if (item.selectedCustomizations && item.selectedCustomizations.length > 0) {
              let optionsArr = [];
              item.selectedCustomizations.forEach(c => {
                  if (c.selectedOptions) {
                      c.selectedOptions.forEach(opt => optionsArr.push(opt.name));
                  }
              });
              if (optionsArr.length > 0) {
                  customHtml += `<div class="flex flex-wrap gap-1.5 mt-2">
                      ${optionsArr.map(opt => `<span class="bg-surface text-muted-foreground border border-border px-2 py-0.5 rounded-md text-[11px] font-bold shadow-sm">+ ${this._escHtml(opt)}</span>`).join('')}
                  </div>`;
              }
          }
          if (item.specialInstructions) {
              customHtml += `<div class="mt-2 bg-warning/10 border-l-[3px] border-warning p-2 rounded-r-md text-xs font-bold text-warning-foreground flex items-start gap-1.5 shadow-sm">
                  <i data-lucide="alert-triangle" style="width:14px;height:14px;margin-top:1px;flex-shrink:0;"></i>
                  <span>${this._escHtml(item.specialInstructions)}</span>
              </div>`;
          }

          itemsHtml += `
            <div class="flex gap-3.5 p-3.5 bg-surface rounded-xl border border-border shadow-sm items-start transition hover:border-primary/30">
                <div class="flex-shrink-0 w-[42px] h-[42px] bg-primary/10 text-primary rounded-xl flex items-center justify-center font-black text-lg border border-primary/20 shadow-sm">
                    ${item.quantity}x
                </div>
                <div class="flex-1 min-w-0 flex flex-col justify-center min-h-[42px]">
                    <div class="flex justify-between items-start gap-2">
                        <h5 class="font-bold text-[15px] text-foreground leading-tight m-0 mt-0.5">${this._escHtml(item.menuItemName)}</h5>
                        ${!isKitchen ? `<span class="font-extrabold text-[15px] text-primary whitespace-nowrap mt-0.5">₹${item.totalPrice.toFixed(2)}</span>` : ''}
                    </div>
                    ${customHtml}
                </div>
            </div>
          `;
      });
    } else if (order.customOrderText) {
        itemsHtml += `
        <div class="p-4 bg-primary/5 rounded-xl border border-primary/20 flex gap-3.5 items-start shadow-sm">
            <div class="flex-shrink-0 w-[42px] h-[42px] bg-primary text-white rounded-xl flex items-center justify-center shadow-sm">
                <i data-lucide="pen-tool" style="width:20px;height:20px;"></i>
            </div>
            <div>
                <strong class="block mb-1 text-primary uppercase tracking-wider text-[11px] font-black">Custom Request</strong>
                <span class="text-foreground font-semibold text-sm leading-relaxed">${this._escHtml(order.customOrderText)}</span>
            </div>
        </div>`;
    }
    itemsHtml += `</div>`;

    let noteHtml = '';
    if (order.customerNote) {
        noteHtml = `
        <div class="mb-6 bg-warning/10 rounded-xl border-l-4 border-warning p-4 shadow-sm flex gap-3 items-start">
            <i data-lucide="message-square" class="text-warning-foreground mt-0.5" style="width:20px;height:20px;flex-shrink:0;"></i>
            <div>
                <strong class="block text-[11px] uppercase tracking-wider text-warning-foreground font-black mb-1">User Message</strong> 
                <span class="font-bold text-warning-foreground text-sm leading-snug">${this._escHtml(order.customerNote)}</span>
            </div>
        </div>`;
    }

    let metadataHtml = '';
    if (Object.keys(otherMetadata).length > 0) {
        metadataHtml = `<div class="mb-3 px-1">
          <h3 class="font-extrabold text-lg text-foreground tracking-tight flex items-center gap-2"><i data-lucide="info" style="width:18px;height:18px;" class="text-primary"></i> Order Information</h3>
      </div>`;
        metadataHtml += `<div class="grid grid-cols-2 gap-3 bg-surface p-4 rounded-xl border border-border shadow-sm mb-6">
          ${Object.entries(otherMetadata).map(([k, v]) => `
            <div class="flex flex-col gap-1">
                <span class="font-bold text-[10px] uppercase tracking-wider text-muted-foreground">${this._escHtml(k)}</span>
                <span class="font-semibold text-foreground text-sm">${this._escHtml(v)}</span>
            </div>
          `).join('')}
        </div>`;
    }

    let amountHtml = '';
    if (!isKitchen) {
       let breakdownHtml = '';
       if (order.subtotal != null) {
           breakdownHtml += `<div class="flex justify-between text-sm mb-2.5 text-muted-foreground"><span>Subtotal</span><span class="font-semibold text-foreground">₹${order.subtotal.toFixed(2)}</span></div>`;
       }
       if (order.couponDiscount != null && order.couponDiscount > 0) {
           breakdownHtml += `<div class="flex justify-between text-sm mb-2.5 text-success font-semibold"><span>Coupon Discount${order.couponCode ? ' ('+this._escHtml(order.couponCode)+')' : ''}</span><span>-₹${order.couponDiscount.toFixed(2)}</span></div>`;
       }
       if (order.taxAmount != null && order.taxAmount > 0) {
           breakdownHtml += `<div class="flex justify-between text-sm mb-2.5 text-muted-foreground"><span>${this._escHtml(order.taxName || 'Tax')}</span><span class="font-semibold text-foreground">₹${order.taxAmount.toFixed(2)}</span></div>`;
       }
       if (order.serviceChargeAmount != null && order.serviceChargeAmount > 0) {
           breakdownHtml += `<div class="flex justify-between text-sm mb-2.5 text-muted-foreground"><span>${this._escHtml(order.serviceChargeName || 'Service Charge')}</span><span class="font-semibold text-foreground">₹${order.serviceChargeAmount.toFixed(2)}</span></div>`;
       }

       amountHtml = `
       <div class="mt-2 bg-surface p-5 rounded-xl border border-border shadow-sm">
          ${breakdownHtml}
          <div class="flex justify-between items-center mt-3 pt-3 border-t border-dashed border-border/80">
             <span class="font-black text-muted-foreground uppercase tracking-widest text-[11px]">Total Amount</span>
             <span class="text-2xl font-black text-primary">₹${order.totalAmount.toFixed(2)}</span>
          </div>
       </div>`;
       
       if (order.paymentStatus !== 'PAID') {
           const method = (order.metadata && order.metadata.paymentMethod) || 'OFFLINE';
           if (method === 'CASHFREE') {
               amountHtml += `
               <div class="flex items-center justify-center gap-2 text-sm font-bold text-yellow-600 mt-3 bg-yellow-50 p-3.5 rounded-xl border border-yellow-200 shadow-sm uppercase tracking-wider">
                   <i data-lucide="zap" style="width:16px;height:16px;fill:currentColor;"></i> Online Payment
               </div>`;
           } else {
               amountHtml += `
               <div class="flex items-center justify-center gap-2 text-sm font-bold text-danger mt-3 bg-danger/10 p-3.5 rounded-xl border border-danger/20 shadow-sm uppercase tracking-wider">
                   <i data-lucide="wallet" style="width:16px;height:16px;"></i> Pay at counter
               </div>`;
           }
       }
    }

    document.getElementById('order-modal-body').innerHTML = `
      ${topHtml}
      ${itemsHtml}
      ${noteHtml}
      ${metadataHtml}
      ${amountHtml}
    `;

    if (typeof lucide !== 'undefined') lucide.createIcons();
    if (window.openModal) openModal('order-modal');
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
