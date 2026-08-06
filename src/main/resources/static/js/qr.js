class QrCode {
  constructor() {
    this.init();
  }

  async init() {
    console.log('QR Code initialized');
    try {
      const response = await api.get('/qr');
      if (response.success && response.data) {
        const qrData = response.data;
        const img = document.querySelector('img[alt="QR Code"]');
        if (img) {
          // If the backend gives us a direct image URL, use it. Otherwise use the link to generate one.
          img.src = qrData.qrImageUrl || `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(qrData.qrUrl)}`;
        }
        
        // Update the shop name from the logged-in user session
        const userStr = localStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            const title = document.querySelector('h3');
            if (title) title.textContent = user.shopName || 'Your Shop';
        }
      }
    } catch (err) {
      console.error('Failed to load QR code:', err);
      if (window.showToast) showToast('Failed to load your QR Code', 'error');
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.qrApp = new QrCode();
});
