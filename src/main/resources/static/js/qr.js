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
        
        // Dynamically set the URL to point to the actual hosted domain
        const dynamicUrl = `${window.location.origin}/customer.html?vendor=${qrData.vendorCode}`;
        qrData.qrUrl = dynamicUrl;
        qrData.qrImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=400x400&ecc=H&data=${encodeURIComponent(dynamicUrl)}`;
        
        this.qrData = qrData;
        const img = document.querySelector('img[alt="QR Code"]');
        if (img) {
          img.src = qrData.qrImageUrl;
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
    const btn = document.querySelector('.btn-primary');
    const origText = btn ? btn.innerHTML : 'Download PNG';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i data-lucide="loader-2" class="animate-spin"></i> Generating Poster...';
        if (window.lucide) lucide.createIcons();
    }

    try {
        const userStr = localStorage.getItem('user');
        const user = userStr ? JSON.parse(userStr) : { shopName: 'Your Shop' };
        
        // 1. Fetch QR Image as Blob to avoid Canvas CORS taint
        const qrUrl = this.qrData.qrImageUrl;
        const response = await fetch(qrUrl);
        const blob = await response.blob();
        
        // 2. Load it into an Image object
        const qrImg = new Image();
        const objectUrl = window.URL.createObjectURL(blob);
        await new Promise((resolve) => {
            qrImg.onload = resolve;
            qrImg.src = objectUrl;
        });
        
        // 3. Create Canvas
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        
        // Canvas dimensions (Poster size)
        const width = 800;
        const height = 1200;
        canvas.width = width;
        canvas.height = height;
        
        // Background
        ctx.fillStyle = '#f8fafc';
        ctx.fillRect(0, 0, width, height);
        
        // Top Accent Shape
        ctx.fillStyle = '#FF5A5F'; // Primary color
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.lineTo(width, 0);
        ctx.lineTo(width, 350);
        ctx.quadraticCurveTo(width / 2, 450, 0, 350);
        ctx.fill();
        
        // Shop Name
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 72px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(user.shopName, width / 2, 140);
        
        // Tagline
        ctx.font = '36px sans-serif';
        ctx.fillText('Order & Pay Without Waiting', width / 2, 210);
        
        // QR Code Container (white card with shadow)
        const qrSize = 440;
        const qrX = (width - qrSize) / 2;
        const qrY = 380;
        
        ctx.shadowColor = 'rgba(0, 0, 0, 0.15)';
        ctx.shadowBlur = 40;
        ctx.shadowOffsetX = 0;
        ctx.shadowOffsetY = 20;
        
        // Draw rounded rectangle card
        const cx = qrX - 40;
        const cy = qrY - 40;
        const cw = qrSize + 80;
        const ch = qrSize + 140;
        const r = 24;
        
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.moveTo(cx + r, cy);
        ctx.lineTo(cx + cw - r, cy);
        ctx.quadraticCurveTo(cx + cw, cy, cx + cw, cy + r);
        ctx.lineTo(cx + cw, cy + ch - r);
        ctx.quadraticCurveTo(cx + cw, cy + ch, cx + cw - r, cy + ch);
        ctx.lineTo(cx + r, cy + ch);
        ctx.quadraticCurveTo(cx, cy + ch, cx, cy + ch - r);
        ctx.lineTo(cx, cy + r);
        ctx.quadraticCurveTo(cx, cy, cx + r, cy);
        ctx.closePath();
        ctx.fill();
        
        // Reset Shadow
        ctx.shadowColor = 'transparent';
        
        // Draw QR Image
        ctx.drawImage(qrImg, qrX, qrY, qrSize, qrSize);
        
        // Draw scan2skip branding in the center of QR
        const logoW = 140;
        const logoH = 44;
        const logoX = qrX + (qrSize - logoW) / 2;
        const logoY = qrY + (qrSize - logoH) / 2;
        
        ctx.fillStyle = '#ffffff';
        ctx.shadowColor = 'rgba(0, 0, 0, 0.15)';
        ctx.shadowBlur = 8;
        ctx.shadowOffsetY = 2;
        
        const lr = 8;
        ctx.beginPath();
        ctx.moveTo(logoX + lr, logoY);
        ctx.lineTo(logoX + logoW - lr, logoY);
        ctx.quadraticCurveTo(logoX + logoW, logoY, logoX + logoW, logoY + lr);
        ctx.lineTo(logoX + logoW, logoY + logoH - lr);
        ctx.quadraticCurveTo(logoX + logoW, logoY + logoH, logoX + logoW - lr, logoY + logoH);
        ctx.lineTo(logoX + lr, logoY + logoH);
        ctx.quadraticCurveTo(logoX, logoY + logoH, logoX, logoY + logoH - lr);
        ctx.lineTo(logoX, logoY + lr);
        ctx.quadraticCurveTo(logoX, logoY, logoX + lr, logoY);
        ctx.closePath();
        ctx.fill();
        
        ctx.shadowColor = 'transparent';
        
        ctx.fillStyle = '#FF5A5F';
        ctx.font = 'bold 22px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('scan2skip', qrX + qrSize / 2, qrY + qrSize / 2);

        // Reset textBaseline
        ctx.textBaseline = 'alphabetic';
        
        // "SCAN ME" text under QR
        ctx.fillStyle = '#1e293b';
        ctx.font = 'bold 42px sans-serif';
        ctx.fillText('SCAN TO ORDER', width / 2, qrY + qrSize + 60);
        
        // Step-by-step instructions
        ctx.font = '28px sans-serif';
        ctx.fillStyle = '#64748b';
        ctx.fillText('1. Scan QR  •  2. Select Items  •  3. Collect Order', width / 2, qrY + qrSize + 160);
        
        // Footer (DeQueue Branding)
        ctx.fillStyle = '#FF5A5F';
        ctx.font = 'bold 48px sans-serif';
        ctx.fillText('Scan2Skip', width / 2, height - 100);
        
        ctx.fillStyle = '#94a3b8';
        ctx.font = '24px sans-serif';
        ctx.fillText('Powered by Scan2Skip.com', width / 2, height - 50);
        
        // 4. Download
        const finalUrl = canvas.toDataURL('image/png');
        const a = document.createElement('a');
        a.href = finalUrl;
        a.download = `${user.shopName.replace(/\\s+/g, '_')}_Scan2Skip_Poster.png`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        
        window.URL.revokeObjectURL(objectUrl);
        if (window.showToast) showToast('Poster Downloaded!', 'success');
        
    } catch(err) {
        console.error(err);
        if (window.showToast) showToast('Failed to generate poster', 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = origText;
            if (window.lucide) lucide.createIcons();
        }
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.qrApp = new QrCode();
});
