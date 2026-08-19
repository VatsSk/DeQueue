self.addEventListener('push', function(event) {
    if (!event.data) {
        console.log('Push event but no data');
        return;
    }

    try {
        const data = event.data.json();
        
        const title = data.title || 'DeQueue Notification';
        const options = {
            body: data.body || 'You have a new notification.',
            icon: '/logo.jpg', // Using the generated logo
            badge: '/logo.jpg',
            data: data
        };

        event.waitUntil(
            self.registration.showNotification(title, options)
        );
    } catch (e) {
        console.error('Error parsing push data', e);
    }
});

self.addEventListener('notificationclick', function(event) {
    event.notification.close();

    // Open the app or focus the existing window
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function(clientList) {
            for (let i = 0; i < clientList.length; i++) {
                let client = clientList[i];
                if (client.url.includes('/') && 'focus' in client) {
                    return client.focus();
                }
            }
            if (clients.openWindow) {
                return clients.openWindow('/');
            }
        })
    );
});
