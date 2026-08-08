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
        this.qrData = qrData;
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

  copyLink() {
    if (!this.qrData || !this.qrData.qrUrl) {
      if (window.showToast) showToast('Link not available', 'error');
      return;
    }
    navigator.clipboard.writeText(this.qrData.qrUrl).then(() => {
      if (window.showToast) showToast('Link Copied', 'success');
    }).catch(err => {
      console.error('Failed to copy', err);
      if (window.showToast) showToast('Failed to copy link', 'error');
    });
  }

  async downloadPng() {
    const img = document.querySelector('img[alt="QR Code"]');
    if (!img || !img.src) return;
    
    try {
        const response = await fetch(img.src);
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'DeQueue-QRCode.png';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        if (window.showToast) showToast('QR Code Downloaded', 'success');
    } catch(err) {
        // Fallback for CORS issues
        const a = document.createElement('a');
        a.href = img.src;
        a.download = 'DeQueue-QRCode.png';
        a.target = '_blank';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.qrApp = new QrCode();
});
