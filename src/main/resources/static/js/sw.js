// Service Worker for DeQueue Push Notifications

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(clients.claim());
});

// Handle push events from Web Push API
self.addEventListener('push', (event) => {
  const promiseChain = self.registration.showNotification('Order Update!', {
    body: event.data ? event.data.text() : 'Your order status was updated.',
    tag: 'order-update'
  });
  event.waitUntil(promiseChain);
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
