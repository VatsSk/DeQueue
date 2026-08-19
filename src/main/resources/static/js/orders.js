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

    console.log('[DEBUG_FILTERS] userStr:', userStr);
    console.log('[DEBUG_FILTERS] user:', user);
    console.log('[DEBUG_FILTERS] permissions:', permissions);

    // Determine tabs based on action permissions
    let tabs = [{ id: 'ALL', label: 'All Active' }];

    if (isPlatformAdmin || permissions.includes('order.accept') || permissions.includes('order.pending')) {
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
    const hasPriceAccess = isPlatformAdmin || permissions.includes('order.accept') || permissions.includes('order.pending') || permissions.includes('order.complete');

    let html = '';
    filtered.forEach(order => {
      let borderColor = 'var(--border)';
      let badgeClass = 'badge-pending';
      let actionsHtml = '';

      if (order.status === 'PENDING') {
        badgeClass = 'badge-pending';
        if (isPlatformAdmin || permissions.includes('order.accept') || permissions.includes('order.pending')) {
          actionsHtml = `
              <button class="btn btn-danger flex-1" onclick="ordersApp.updateStatus('${order.id}', 'CANCELLED')"><i data-lucide="x" style="width:16px;height:16px;margin-right:4px;"></i> Reject</button>
              <button class="btn btn-primary flex-1" onclick="ordersApp.updateStatus('${order.id}', 'ACCEPTED')"><i data-lucide="check" style="width:16px;height:16px;margin-right:4px;"></i> Accept</button>
          `;
        }
      } else if (order.status === 'ACCEPTED') {
        borderColor = 'var(--warning)';
        badgeClass = 'badge-accepted';
        if (isPlatformAdmin || permissions.includes('order.prepare')) {
          actionsHtml = `<button class="btn btn-primary w-full" onclick="ordersApp.updateStatus('${order.id}', 'PREPARING')"><i data-lucide="chef-hat" style="width:16px;height:16px;margin-right:4px;"></i> Start Preparing</button>`;
        }
      } else if (order.status === 'PREPARING') {
        borderColor = 'var(--info)';
        badgeClass = 'badge-preparing';
        if (isPlatformAdmin || permissions.includes('order.ready')) {
          actionsHtml = `<button class="btn btn-primary w-full" onclick="ordersApp.updateStatus('${order.id}', 'READY')"><i data-lucide="bell-ring" style="width:16px;height:16px;margin-right:4px;"></i> Mark Ready</button>`;
        }
      } else if (order.status === 'READY') {
        borderColor = 'var(--success)';
        badgeClass = 'badge-ready';
        const canComplete = isPlatformAdmin || permissions.includes('order.complete');
        const canPrint = isPlatformAdmin || permissions.includes('order.print');

        if (canComplete && canPrint) {
          actionsHtml = `
            <button class="btn btn-secondary flex-1" onclick="ordersApp.printBill('${order.id}')" title="Print bill for customer">
              <i data-lucide="printer" style="width:16px;height:16px;margin-right:4px;"></i> Print
            </button>
            <button class="btn btn-success flex-1" onclick="ordersApp.updateStatus('${order.id}', 'COMPLETED')">
              <i data-lucide="check-circle-2" style="width:16px;height:16px;margin-right:4px;"></i> Complete
            </button>
          `;
        } else if (canComplete) {
          actionsHtml = `
            <button class="btn btn-success w-full" onclick="ordersApp.updateStatus('${order.id}', 'COMPLETED')">
              <i data-lucide="check-circle-2" style="width:16px;height:16px;margin-right:4px;"></i> Complete
            </button>
          `;
        } else if (canPrint) {
          actionsHtml = `
            <button class="btn btn-secondary w-full" onclick="ordersApp.printBill('${order.id}')">
              <i data-lucide="printer" style="width:16px;height:16px;margin-right:4px;"></i> Print Bill
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

      let itemsHtml = '';
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
                    customHtml += `<div class="text-xs text-muted mt-1" style="margin-left: 1.5rem; display: flex; gap: 4px;"><i data-lucide="plus" style="width:12px;height:12px;"></i> ${optionsArr.join(', ')}</div>`;
                }
            }
            if (item.specialInstructions) {
                customHtml += `<div class="text-xs text-warning mt-1 font-medium" style="margin-left: 1.5rem; display: flex; gap: 4px;"><i data-lucide="message-square" style="width:12px;height:12px;"></i> "${this._escHtml(item.specialInstructions)}"</div>`;
            }
            itemsHtml += `
                <div class="flex justify-between items-start mb-3 pb-2 border-b border-border last:border-0 last:mb-0 last:pb-0 text-sm">
                    <div class="flex-1">
                        <div class="flex items-start gap-2">
                            <span class="font-bold min-w-[20px] bg-surface-hover rounded px-1 text-center">${item.quantity}x</span> 
                            <span class="font-semibold">${this._escHtml(item.menuItemName)}</span>
                        </div>
                        ${customHtml}
                    </div>
                    ${hasPriceAccess ? `<span class="font-medium whitespace-nowrap ml-3">₹${item.totalPrice.toFixed(2)}</span>` : ''}
                </div>
            `;
        });
      } else if (order.customOrderText) {
          itemsHtml = `<div class="text-sm border p-3 rounded-md bg-surface-hover mb-2"><strong class="flex items-center gap-1 mb-1"><i data-lucide="pen-tool" style="width:14px;height:14px;"></i> Custom:</strong> ${this._escHtml(order.customOrderText)}</div>`;
      }

      let noteHtml = '';
      if (order.customerNote) {
          noteHtml = `<div class="text-sm bg-warning/10 text-warning-foreground mt-3 p-2.5 rounded-md border border-warning/20">
              <strong class="flex items-center gap-1 mb-0.5"><i data-lucide="alert-circle" style="width:14px;height:14px;"></i> Order Note:</strong> 
              ${this._escHtml(order.customerNote)}
          </div>`;
      }

      let metadataHtml = '';
      if (order.metadata && Object.keys(order.metadata).length > 0) {
          metadataHtml = `<div class="text-xs text-muted mt-3 grid grid-cols-2 gap-1 bg-surface-hover p-2 rounded-md">
            ${Object.entries(order.metadata).map(([k, v]) => `<div><span class="font-medium">${this._escHtml(k)}:</span> ${this._escHtml(v)}</div>`).join('')}
          </div>`;
      }

      let paymentHtml = '';
      if (hasPriceAccess) {
          paymentHtml = `<div class="flex items-center gap-1 text-xs font-semibold text-danger mt-3 mb-1"><i data-lucide="wallet" style="width:14px;height:14px;"></i> Unpaid (Pay at counter)</div>`;
      }

      html += `
        <div class="card p-0 flex flex-col justify-between overflow-hidden transition-shadow hover:shadow-md" style="border-left: 4px solid ${borderColor}; min-height: 250px;">
          <div class="p-4 flex-1 flex flex-col">
            <div class="flex justify-between items-start mb-4 pb-3 border-b border-border">
              <div>
                <span class="font-extrabold text-xl">#${order.queueNumber}</span>
                <div class="text-xs text-muted mt-0.5 flex items-center gap-1"><i data-lucide="clock" style="width:12px;height:12px;"></i> ${new Date(order.createdAt).toLocaleTimeString()}</div>
              </div>
              <span class="badge ${badgeClass} shadow-sm">${statusLabels[order.status] || order.status}</span>
            </div>
            
            <div class="order-items-list flex-1 overflow-y-auto pr-1" style="max-height: 250px;">
              ${itemsHtml}
              ${noteHtml}
              ${metadataHtml}
            </div>
            ${paymentHtml}
          </div>
          
          <div class="p-4 bg-surface-hover/50 border-t border-border mt-auto">
            ${hasPriceAccess ? `
            <div class="flex justify-between items-center mb-3 text-sm">
              <span class="font-medium text-muted">Total Amount</span>
              <span class="font-extrabold text-lg text-primary">₹${order.totalAmount.toFixed(2)}</span>
            </div>
            ` : ''}
            <div class="flex gap-2 w-full">
              ${actionsHtml}
            </div>
          </div>
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

  printBill(orderId) {
    const order = this.orders.find(o => o.id === orderId);
    if (!order) return;

    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : {};
    const shopName = user.shopName || 'DeQueue Shop';
    const address = user.address && user.address.street ? user.address.street : '';
    const phone = user.phone || '';
    const email = user.email || '';
    
    // Calculate values
    const subtotal = order.items ? order.items.reduce((sum, item) => sum + item.totalPrice, 0) : order.totalAmount;
    
    // Read from settings
    const settings = user.settings || {};
    const taxName = settings.taxName || 'Tax';
    const taxPct = settings.taxPercentage || 0;
    const chargeAmt = settings.additionalCharges || 0;
    const chargeName = settings.additionalChargeName || 'Service Charge';
    const gstNumber = settings.gstNumber || '';
    
    const taxValue = (subtotal * taxPct) / 100;
    let computedTotal = subtotal + taxValue + chargeAmt;

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
        <div><strong>Payment:</strong> UNPAID (Pay at Counter)</div>
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
        ${taxPct > 0 ? `<tr>
            <td>${this._escHtml(taxName)} (${taxPct}%)</td>
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
      
      ${order.customerNote ? `<div class="instruction"><strong>Instruction:</strong> ${this._escHtml(order.customerNote)}</div>` : ''}
      
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

    html2pdf().set(opt).from(tempDiv).save().then(() => {
        if (window.showToast) showToast('PDF downloaded', 'success');
    }).catch(err => {
        console.error('PDF generation failed', err);
        if (window.showToast) showToast('Failed to generate PDF', 'error');
    });
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
