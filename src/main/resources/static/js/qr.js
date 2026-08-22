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
        qrData.qrImageUrl = `https://quickchart.io/qr?text=${encodeURIComponent(dynamicUrl)}&size=400&margin=0`;
        
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
        
        // 1. Load QR Image directly with CORS configuration
        const qrUrl = this.qrData.qrImageUrl;
        const qrImg = new Image();
        qrImg.crossOrigin = 'Anonymous';
        await new Promise((resolve, reject) => {
            qrImg.onload = resolve;
            qrImg.onerror = () => reject(new Error('Failed to load QR image'));
            qrImg.src = qrUrl;
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
        ctx.fillStyle = '#14b8a6'; // Teal color
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
        
        // Load local PNG logo for the center
        const qrLogoImg = new Image();
        await new Promise((resolve, reject) => {
            qrLogoImg.onload = resolve;
            qrLogoImg.onerror = reject;
            qrLogoImg.src = window.qrLogoB64 || 'images/qr_logo.png';
        });
        
        // Draw center logo container
        const logoW = 110;
        const logoH = 65;
        const logoX = qrX + (qrSize - logoW) / 2;
        const logoY = qrY + (qrSize - logoH) / 2;
        
        ctx.fillStyle = '#ffffff';
        ctx.shadowColor = 'rgba(0, 0, 0, 0.15)';
        ctx.shadowBlur = 8;
        ctx.shadowOffsetY = 2;
        
        const lr = 12;
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
        
        // Draw logo image (stretch it as requested)
        ctx.drawImage(qrLogoImg, logoX + 12, logoY + 12, logoW - 24, logoH - 24);
        
        // "SCAN ME" text under QR
        ctx.fillStyle = '#1e293b';
        ctx.font = 'bold 42px sans-serif';
        ctx.textBaseline = 'alphabetic';
        ctx.fillText('SCAN TO ORDER', width / 2, qrY + qrSize + 60);
        
        // Step-by-step instructions
        ctx.font = '28px sans-serif';
        ctx.fillStyle = '#64748b';
        ctx.fillText('1. Scan QR  •  2. Select Items  •  3. Collect Order', width / 2, qrY + qrSize + 160);
        
        // Footer (Scan2Skip Branding)
        ctx.fillStyle = '#94a3b8';
        ctx.font = '500 24px sans-serif';
        ctx.fillText('Powered by', width / 2, height - 85);

        ctx.fillStyle = '#14b8a6';
        ctx.font = 'bold 42px sans-serif';
        ctx.fillText('Scan2Skip', width / 2, height - 40);
        
        // 4. Download
        const finalUrl = canvas.toDataURL('image/png');
        const a = document.createElement('a');
        a.href = finalUrl;
        a.download = `${user.shopName.replace(/\\s+/g, '_')}_Scan2Skip_Poster.png`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        
        
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
  async downloadCard() {
    const btn = document.querySelector('button[onclick="window.qrApp.downloadCard()"]');
    const origText = btn ? btn.innerHTML : '<i data-lucide="credit-card"></i> Download Card';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i data-lucide="loader-2" class="animate-spin"></i> Generating Card...';
        if (window.lucide) lucide.createIcons();
    }

    try {
        const cardElement = document.querySelector('.qr-card');
        const actionsGroup = document.querySelector('.action-group');
        const userStr = localStorage.getItem('user');
        const user = userStr ? JSON.parse(userStr) : { shopName: 'Your Shop' };
        
        if (!cardElement) throw new Error("Card element not found");

        // Hide action buttons temporarily
        if (actionsGroup) actionsGroup.style.display = 'none';

        // Add 'Powered by Scan2Skip' temporarily
        const footer = document.createElement('div');
        footer.id = 'temp-card-footer';
        footer.style.textAlign = 'center';
        footer.style.marginTop = '1.5rem';
        footer.style.color = 'var(--text-muted, #94a3b8)';
        footer.style.fontSize = '14px';
        footer.style.fontWeight = '500';
        footer.innerHTML = `Powered by <strong style="color: var(--primary, #14b8a6);">Scan2Skip</strong>`;
        
        const infoSide = document.querySelector('.qr-info-side');
        if (infoSide) infoSide.appendChild(footer);

        // Wait a small moment to ensure DOM paints
        await new Promise(r => setTimeout(r, 100));

        // Use html2canvas to capture exactly what is visible
        const canvas = await html2canvas(cardElement, {
            scale: 3, // High resolution
            useCORS: true,
            backgroundColor: window.getComputedStyle(cardElement).backgroundColor
        });

        // Restore the DOM
        if (actionsGroup) actionsGroup.style.display = 'flex';
        const addedFooter = document.getElementById('temp-card-footer');
        if (addedFooter) addedFooter.remove();

        // Download
        const finalUrl = canvas.toDataURL('image/png');
        const a = document.createElement('a');
        a.href = finalUrl;
        a.download = `${user.shopName.replace(/\\s+/g, '_')}_Scan2Skip_Card.png`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);

        if (window.showToast) showToast('Card Downloaded!', 'success');
        
    } catch(err) {
        console.error(err);
        
        // Ensure DOM is restored in case of error
        const actionsGroup = document.querySelector('.action-group');
        if (actionsGroup) actionsGroup.style.display = 'flex';
        const addedFooter = document.getElementById('temp-card-footer');
        if (addedFooter) addedFooter.remove();
        
        if (window.showToast) showToast('Failed to generate card', 'error');
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
