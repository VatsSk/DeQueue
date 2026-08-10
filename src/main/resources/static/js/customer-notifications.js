/**
 * DeQueue Customer Notification Manager
 * Handles:
 * - WebSocket status updates with auto-reconnect + exponential backoff
 * - Service Worker registration + Web Push subscription
 * - In-page toast/banner notifications for status changes
 * - Event deduplication by eventId
 */
class CustomerNotificationManager {
  constructor(app) {
    this.app = app;
    this.processedEventIds = new Set();
    this.maxProcessedEvents = 100;
    this.reconnectAttempts = 0;
    this.maxReconnectDelay = 30000;
    this.baseReconnectDelay = 1000;
    this.customerToken = null;
    this.orderId = null;
    this.sessionId = null;
    this.pushSubscription = null;
    this.serviceWorkerRegistration = null;
    this.notificationPermissionAsked = false;
  }

  /**
   * Initialize notifications after order is placed.
   * @param {string} orderId 
   * @param {string} sessionId 
   * @param {string} customerToken - HMAC signed token from backend
   * @param {string} queueNumber
   */
  async init(orderId, sessionId, customerToken, queueNumber) {
    this.orderId = orderId;
    this.sessionId = sessionId;
    this.customerToken = customerToken;
    this.queueNumber = queueNumber;

    // Subscribe to STOMP topic for this order
    this.subscribeToOrderUpdates();

    // Register service worker and request push permission
    await this.setupPushNotifications();
  }

  /**
   * Subscribe to WebSocket STOMP topic for order updates.
   * Uses the existing STOMP client from the CustomerApp.
   */
  subscribeToOrderUpdates() {
    if (!this.app.stompClient || !this.app.stompClient.connected) {
      // Wait for connection and retry
      setTimeout(() => this.subscribeToOrderUpdates(), 1000);
      return;
    }

    // Subscribe to customer-specific secured topic
    if (this.sessionId && this.orderId) {
      this.app.stompClient.subscribe(
        '/topic/customer/' + this.sessionId + '/' + this.orderId,
        (msg) => this.handleWebSocketMessage(msg)
      );
    }

    // Also subscribe to the queueNumber-based topic (existing pattern)
    if (this.queueNumber) {
      this.app.stompClient.subscribe(
        '/topic/orders/' + this.queueNumber,
        (msg) => this.handleWebSocketMessage(msg)
      );
    }
  }

  /**
   * Handle incoming WebSocket message with deduplication.
   */
  handleWebSocketMessage(msg) {
    try {
      const event = JSON.parse(msg.body);

      // Deduplicate by eventId
      if (event.eventId && this.processedEventIds.has(event.eventId)) {
        return;
      }
      if (event.eventId) {
        this.processedEventIds.add(event.eventId);
        // Cap the set size
        if (this.processedEventIds.size > this.maxProcessedEvents) {
          const first = this.processedEventIds.values().next().value;
          this.processedEventIds.delete(first);
        }
      }

      // Resolve status (supporting both new OrderStatusEvent and legacy OrderEvent)
      const status = event.status || event.orderStatus;
      if (!status) return;
      
      // Update event for downstream handlers
      event.status = status;

      // Dispatch to app
      this.app.handleOrderUpdate(event);

      // Show in-page notification
      const message = event.message || `Your order is now ${status}`;
      this.showStatusNotification(status, message);

    } catch (e) {
      console.error('Failed to process WebSocket message:', e);
    }
  }

  /**
   * Show a beautiful in-page notification banner for status changes.
   */
  showStatusNotification(status, message) {
    // Remove existing notification banner if any
    const existing = document.querySelector('.order-notification-banner');
    if (existing) existing.remove();

    const icons = {
      ACCEPTED: { name: 'check-circle', class: 'icon-accepted' },
      PREPARING: { name: 'chef-hat', class: 'icon-preparing' },
      READY: { name: 'shopping-bag', class: 'icon-ready' },
      COLLECTED: { name: 'party-popper', class: 'icon-collected' },
      CANCELLED: { name: 'x-circle', class: 'icon-cancelled' }
    };
    const iconObj = icons[status] || { name: 'bell', class: 'icon-default' };

    const banner = document.createElement('div');
    banner.className = `order-notification-banner ${iconObj.class}`;
    banner.innerHTML = `
      <div class="notification-icon-wrapper">
        <i data-lucide="${iconObj.name}"></i>
      </div>
      <div class="notification-content">
        <div class="notification-title">${this.getNotificationTitle(status)}</div>
        <div class="notification-body">${message || ''}</div>
      </div>
      <button class="notification-close" onclick="this.parentElement.classList.remove('show'); setTimeout(() => this.parentElement.remove(), 400);">
        <i data-lucide="x"></i>
      </button>
      <div class="notification-progress"></div>
    `;

    document.body.appendChild(banner);
    if (typeof lucide !== 'undefined') lucide.createIcons({ root: banner });

    // Animate in
    requestAnimationFrame(() => {
      banner.classList.add('show');
    });

    // Auto-remove after 6 seconds
    setTimeout(() => {
      if (banner.parentElement) {
          banner.classList.remove('show');
          setTimeout(() => banner.remove(), 400);
      }
    }, 6000);
  }

  getNotificationTitle(status) {
    const titles = {
      ACCEPTED: 'Order Confirmed!',
      PREPARING: 'Being Prepared',
      READY: 'Ready for Pickup!',
      COLLECTED: 'Collected',
      CANCELLED: 'Order Cancelled'
    };
    return titles[status] || 'Order Update';
  }

  /**
   * Register service worker and set up push notifications.
   */
  async setupPushNotifications() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      console.log('Push notifications not supported');
      return;
    }

    try {
      // Request notification permission FIRST to preserve user gesture context
      if (!this.notificationPermissionAsked && Notification.permission === 'default') {
        this.notificationPermissionAsked = true;
        const permission = await Notification.requestPermission();
        if (permission !== 'granted') {
          console.log('Notification permission denied');
          return;
        }
      }

      if (Notification.permission !== 'granted') {
        return;
      }

      // Register service worker
      this.serviceWorkerRegistration = await navigator.serviceWorker.register('/sw.js');
      console.log('Service Worker registered');

      // Wait for it to be ready
      await navigator.serviceWorker.ready;

      // Get VAPID public key from backend
      const vapidResponse = await fetch('/api/v1/public/notifications/vapid-public-key');
      const vapidData = await vapidResponse.json();
      if (!vapidData.success || !vapidData.data.publicKey) {
        console.log('VAPID key not available, skipping push subscription');
        return;
      }

      const vapidPublicKey = vapidData.data.publicKey;

      // Check for existing subscription
      let subscription = await this.serviceWorkerRegistration.pushManager.getSubscription();

      if (!subscription) {
        // Create new push subscription
        const applicationServerKey = this.urlBase64ToUint8Array(vapidPublicKey);
        subscription = await this.serviceWorkerRegistration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: applicationServerKey
        });
      }

      this.pushSubscription = subscription;

      // Send subscription to backend
      const subJson = subscription.toJSON();
      const response = await fetch('/api/v1/public/notifications/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          orderId: this.orderId,
          sessionId: this.sessionId,
          customerToken: this.customerToken,
          endpoint: subscription.endpoint,
          p256dh: subJson.keys.p256dh,
          auth: subJson.keys.auth
        })
      });

      if (!response.ok) {
        throw new Error(`Server returned ${response.status} ${response.statusText}`);
      }

      console.log('Push subscription registered with backend');

    } catch (e) {
      console.error('Failed to setup push notifications:', e);
    }
  }

  /**
   * Fetch latest order status from backend (for reconnection scenarios).
   */
  async fetchLatestStatus() {
    if (!this.orderId || !this.sessionId || !this.customerToken) return null;
    try {
      const res = await fetch(
        `/api/v1/public/notifications/order-status?orderId=${this.orderId}&sessionId=${this.sessionId}&token=${encodeURIComponent(this.customerToken)}`
      );
      const data = await res.json();
      if (data.success && data.data) {
        return data.data;
      }
    } catch (e) {
      console.error('Failed to fetch latest order status:', e);
    }
    return null;
  }

  /**
   * Handle WebSocket reconnection.
   * Called by the CustomerApp's reconnect logic.
   */
  async onReconnected() {
    this.reconnectAttempts = 0;

    // Re-subscribe to topics
    this.subscribeToOrderUpdates();

    // Fetch latest status since we may have missed events
    const latestStatus = await this.fetchLatestStatus();
    if (latestStatus && latestStatus.status) {
      this.app.handleOrderUpdate({
        orderId: latestStatus.orderId,
        status: latestStatus.status,
        message: '',
        queueNumber: latestStatus.queueNumber || ''
      });
    }
  }

  /**
   * Convert base64url-encoded string to Uint8Array (for VAPID key).
   */
  urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }

  destroy() {
    this.processedEventIds.clear();
  }
}
