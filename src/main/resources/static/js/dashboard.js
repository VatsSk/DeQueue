class Dashboard {
  constructor() {
    this.init();
  }

  init() {
    this.refreshData();
    this.startAutoRefresh();
    this.connectWebSocket();
  }

  connectWebSocket() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    
    const userStr = localStorage.getItem('user');
    if (!userStr) return;
    const user = JSON.parse(userStr);
    if (!user || !user.id) return;
    
    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;
    
    this.stompClient.connect({}, () => {
      this.stompClient.subscribe('/topic/vendor/' + user.vendorId, (msg) => {
        // Just refresh the dashboard data whenever there's a vendor update
        this.refreshData();
      });
    }, (error) => {
      console.error("WebSocket disconnected, retrying...", error);
      setTimeout(() => this.connectWebSocket(), 5000);
    });
  }

  startAutoRefresh() {
    // Poll every 10 seconds
    setInterval(() => {
      this.refreshData();
    }, 10000);
  }

  async refreshData() {
    try {
      const res = await api.get('/dashboard');
      if (res.success && res.data) {
        this.renderStats(res.data.todayStats);
        this.renderLiveQueue(res.data.recentOrders);
        this.renderWaitTime(res.data.averageWaitTime);
      }
    } catch (e) {
      console.error('Failed to load dashboard data', e);
    }
  }

  renderStats(stats) {
    if (!stats) return;
    
    // Total Orders
    const totalOrdersEl = document.getElementById('stat-total-orders');
    if (totalOrdersEl) totalOrdersEl.innerText = stats.totalOrders;

    // Pending
    const pendingEl = document.getElementById('stat-pending-orders');
    if (pendingEl) pendingEl.innerText = stats.pendingOrders;

    // Preparing
    const preparingEl = document.getElementById('stat-preparing-orders');
    if (preparingEl) preparingEl.innerText = stats.preparingOrders;

    // Ready
    const readyEl = document.getElementById('stat-ready-orders');
    if (readyEl) readyEl.innerText = stats.readyOrders;
  }

  renderLiveQueue(orders) {
    if (!orders) return;
    
    // Sort orders by createdAt or status logic
    const readyOrders = orders.filter(o => o.status === 'READY');
    const preparingOrders = orders.filter(o => o.status === 'PREPARING');
    const pendingOrders = orders.filter(o => o.status === 'PENDING');

    const queueDisplay = document.querySelector('.queue-display');
    if (queueDisplay) {
        let html = '';
        
        // Show up to 1 Now Serving (Ready)
        if (readyOrders.length > 0) {
            html += `<div class="queue-box serving"><h4>Now Serving</h4><div class="number">${readyOrders[0].queueNumber}</div></div>`;
        } else {
            html += `<div class="queue-box serving"><h4>Now Serving</h4><div class="number">-</div></div>`;
        }

        // Show Next (Pending)
        if (pendingOrders.length > 0) {
            html += `<div class="queue-box"><h4>Next</h4><div class="number text-muted">${pendingOrders[0].queueNumber}</div></div>`;
        } else {
            html += `<div class="queue-box"><h4>Next</h4><div class="number text-muted">-</div></div>`;
        }

        // Show Preparing (up to 2)
        for (let i = 0; i < 2; i++) {
            if (preparingOrders.length > i) {
                html += `<div class="queue-box"><h4>Preparing</h4><div class="number text-muted">${preparingOrders[i].queueNumber}</div></div>`;
            } else {
                html += `<div class="queue-box"><h4>Preparing</h4><div class="number text-muted">-</div></div>`;
            }
        }
        
        queueDisplay.innerHTML = html;
    }

    // Recent Orders list
    const recentList = document.getElementById('recent-orders-list');
    if (recentList) {
        let html = '';
        if (orders.length === 0) {
            html = '<div class="text-center text-muted p-2">No orders yet</div>';
        } else {
            // Just show top 3 most recent
            orders.slice(0, 3).forEach(o => {
                let badgeClass = 'badge-pending';
                if (o.status === 'PREPARING') badgeClass = 'badge-preparing';
                if (o.status === 'READY') badgeClass = 'badge-ready';
                if (o.status === 'COLLECTED' || o.status === 'CANCELLED') badgeClass = 'badge-secondary';

                const timeStr = window.getTimeAgo ? window.getTimeAgo(o.createdAt) : new Date(o.createdAt).toLocaleTimeString();
                
                html += `
                <div class="flex justify-between items-center border-b pb-2 mb-2 last:border-0 last:pb-0 last:mb-0" style="border-color: var(--border);">
                    <div><span class="font-bold">${o.queueNumber}</span> - ${o.itemCount} Items</div>
                    <span class="badge ${badgeClass}">${timeStr}</span>
                </div>
                `;
            });
        }
        recentList.innerHTML = html;
    }
  }

  renderWaitTime(waitTime) {
    const el = document.getElementById('avg-wait-time');
    if (el) el.innerHTML = `${waitTime || 0}<span style="font-size: 1.5rem">m</span>`;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.dashboard = new Dashboard();
});
