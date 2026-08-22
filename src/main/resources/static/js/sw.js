// Service Worker for Scan2Skip Push Notifications

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(clients.claim());
});

// Handle push events from Web Push API
self.addEventListener('push', (event) => {
  let notifData = {};
  if (event.data) {
    try { notifData = event.data.json(); } catch (e) { notifData = { body: event.data.text() }; }
  }
  const promiseChain = self.registration.showNotification(notifData.title || 'Order Update!', {
    body: notifData.body || 'Your order status was updated.',
    icon: '/Scan2Skip_favicon.svg',
    badge: '/Scan2Skip_favicon.svg',
    tag: notifData.tag || 'order-update',
    data: notifData.data || {}
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
