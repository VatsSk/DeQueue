// Service Worker for DeQueue Push Notifications

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(clients.claim());
});

// Handle push events from Web Push API
self.addEventListener('push', (event) => {
  if (!event.data) return;

  try {
    const data = event.data.json();
    const title = data.title || '🔔 Order Update';
    const options = {
      body: data.body || 'Your order status has changed.',
      icon: '/images/icon-192.png',
      badge: '/images/badge-72.png',
      tag: 'order-' + (data.orderId || 'unknown'),
      renotify: true,
      data: {
        orderId: data.orderId,
        status: data.status,
        timestamp: data.timestamp,
        url: self.location.origin
      },
      actions: [
        { action: 'view', title: 'View Order' }
      ],
      vibrate: [200, 100, 200]
    };

    event.waitUntil(
      self.registration.showNotification(title, options)
    );
  } catch (e) {
    console.error('Push event error:', e);
    // Fallback if JSON parsing fails
    event.waitUntil(
      self.registration.showNotification('🔔 Order Update', {
        body: event.data.text(),
        tag: 'order-fallback'
      })
    );
  }
});

// Handle notification click
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const data = event.notification.data || {};
  const urlToOpen = data.url || self.location.origin;

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // Focus existing window if available
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          return client.focus();
        }
      }
      // Open new window if no existing one
      return clients.openWindow(urlToOpen);
    })
  );
});
