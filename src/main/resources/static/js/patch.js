  updateCartUI() {
    this.saveCart();
    const cartBtn = document.getElementById('floating-cart');
    if (!cartBtn) return;

    const totalItems = this.cart.reduce((sum, item) => sum + item.quantity, 0);
    const subtotal = this.cart.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0);
    
    let finalTotal = subtotal;
    const settings = this.vendor?.settings || {};
    
    // Tax
    if (settings.taxPercentage && settings.taxPercentage > 0) {
        finalTotal += (subtotal * settings.taxPercentage) / 100;
    }
    // Additional charge
    if (settings.additionalCharges && settings.additionalCharges > 0) {
        finalTotal += settings.additionalCharges;
    }
    // Coupon
    if (this.appliedCoupon) {
        let discount = 0;
        if (this.appliedCoupon.type === 'PERCENTAGE') {
            discount = (subtotal * this.appliedCoupon.value) / 100;
        } else {
            discount = this.appliedCoupon.value;
        }
        finalTotal -= discount;
    }
    if (finalTotal < 0) finalTotal = 0;

    if (totalItems > 0) {
      document.getElementById('cart-count').innerText = `${totalItems} Item${totalItems > 1 ? 's' : ''}`;
      document.getElementById('cart-total').innerText = this.formatPrice(finalTotal);
      cartBtn.classList.add('visible');
    } else {
      cartBtn.classList.remove('visible');
    }
  }

  applyCoupon() {
    const input = document.getElementById('cart-coupon-input');
    if (!input) return;
    const code = input.value.trim().toUpperCase();
    if (!code) return;
    
    const settings = this.vendor?.settings || {};
    if (!settings.coupons) {
        if (typeof showToast === 'function') showToast('Invalid coupon code', 'error');
        return;
    }
    
    const coupon = settings.coupons.find(c => c.code.toUpperCase() === code);
    if (!coupon) {
        if (typeof showToast === 'function') showToast('Invalid coupon code', 'error');
        return;
    }
    
    this.appliedCoupon = coupon;
    this.updateCartUI();
    this.renderCartModal();
    if (typeof showToast === 'function') showToast('Coupon applied', 'success');
  }

  removeCoupon() {
    this.appliedCoupon = null;
    this.updateCartUI();
    this.renderCartModal();
  }

  renderCartModal() {
    const body = document.querySelector('#cart-modal-overlay .modal-body');
    if (!body) return;

    if (this.cart.length === 0) {
      body.innerHTML = `
        <div class="text-center py-8 text-muted">
          <i data-lucide="shopping-cart" style="width:40px;height:40px;opacity:0.3;margin:0 auto 0.75rem;display:block;"></i>
          <p>Your cart is empty</p>
        </div>`;
      if (typeof lucide !== 'undefined') lucide.createIcons();
      return;
    }

    const total = this.cart.reduce((s, i) => s + i.unitPrice * i.quantity, 0);
    let finalTotal = total;
    let taxHtml = '';
    let chargeHtml = '';
    let couponHtml = '';
    let couponInputHtml = '';
    
    // Tax and Additional Charges
    const settings = this.vendor?.settings || {};
    const taxPct = settings.taxPercentage || 0;
    const taxName = settings.taxName || 'Tax';
    const chargeAmt = settings.additionalCharges || 0;
    const chargeName = settings.additionalChargeName || 'Service Charge';
    
    let taxAmount = 0;
    if (taxPct > 0) {
        taxAmount = (total * taxPct) / 100;
        taxHtml = `<div class="flex justify-between text-sm mb-1 text-muted">
          <span>${this._escHtml(taxName)} (${taxPct}%)</span>
          <span>${this.formatPrice(taxAmount)}</span>
        </div>`;
    }
    
    if (chargeAmt > 0) {
        chargeHtml = `<div class="flex justify-between text-sm mb-1 text-muted">
          <span>${this._escHtml(chargeName)}</span>
          <span>${this.formatPrice(chargeAmt)}</span>
        </div>`;
    }
    
    // Coupons
    let couponDiscount = 0;
    if (settings.coupons && settings.coupons.length > 0) {
        // Render Coupon Input
        couponInputHtml = `
          <div class="mb-3">
             <div class="flex gap-2">
                 <input type="text" id="cart-coupon-input" class="form-control" placeholder="Enter coupon code" style="text-transform:uppercase" ${this.appliedCoupon ? 'disabled value="'+this._escHtml(this.appliedCoupon.code)+'"' : ''}>
                 ${this.appliedCoupon ? 
                   `<button class="btn btn-secondary" onclick="customerApp.removeCoupon()">Remove</button>` : 
                   `<button class="btn btn-secondary" onclick="customerApp.applyCoupon()">Apply</button>`}
             </div>
             ${this.appliedCoupon ? `<div class="text-xs text-success mt-1"><i data-lucide="check" style="width:12px;height:12px;display:inline"></i> Coupon applied</div>` : ''}
          </div>
        `;
        
        if (this.appliedCoupon) {
            if (this.appliedCoupon.type === 'PERCENTAGE') {
                couponDiscount = (total * this.appliedCoupon.value) / 100;
            } else {
                couponDiscount = this.appliedCoupon.value;
            }
            if (couponDiscount > total) couponDiscount = total;
            
            couponHtml = `<div class="flex justify-between text-sm mb-1 text-success font-medium">
              <span>Coupon (${this._escHtml(this.appliedCoupon.code)})</span>
              <span>-${this.formatPrice(couponDiscount)}</span>
            </div>`;
        }
    } else {
        // If vendor has no coupons enabled, clear any applied coupon
        this.appliedCoupon = null;
    }
    
    finalTotal = total - couponDiscount + taxAmount + chargeAmt;
    if (finalTotal < 0) finalTotal = 0;
    
    // Save computed payment values to instance so placeOrder can use them
    this._currentCheckout = {
        subtotal: total,
        taxAmount: taxAmount,
        taxName: taxPct > 0 ? taxName : null,
        serviceChargeAmount: chargeAmt > 0 ? chargeAmt : null,
        serviceChargeName: chargeAmt > 0 ? chargeName : null,
        couponCode: this.appliedCoupon ? this.appliedCoupon.code : null,
        couponDiscount: couponDiscount > 0 ? couponDiscount : null
    };

    let cfHtml = '';
    if (this.vendor?.settings?.customFields && this.vendor.settings.customFields.length > 0) {
        this.vendor.settings.customFields.forEach((cf, idx) => {
            cfHtml += `<div class="mb-3">
                <label class="text-sm font-bold mb-1 block">${this._escHtml(cf.name)} ${cf.required ? '<span class="text-danger">*</span>' : ''}</label>`;
            
            if (cf.type === 'TEXT') {
                cfHtml += `<input type="text" id="cf-${idx}" class="form-control" placeholder="Enter ${this._escHtml(cf.name)}">`;
            } else if (cf.type === 'DROPDOWN') {
                cfHtml += `<select id="cf-${idx}" class="form-control">
                    <option value="">Select...</option>
                    ${cf.options.map(o => `<option value="${this._escHtml(o)}">${this._escHtml(o)}</option>`).join('')}
                </select>`;
            } else if (cf.type === 'CHECKBOX') {
                cfHtml += `<div class="flex flex-col gap-1">
                    ${cf.options.map((o, oidx) => `
                        <label class="flex items-center gap-2">
                            <input type="checkbox" name="cf-${idx}" value="${this._escHtml(o)}"> <span class="text-sm">${this._escHtml(o)}</span>
                        </label>
                    `).join('')}
                </div>`;
            }
            cfHtml += `</div>`;
        });
    }

    body.innerHTML = \`
      <div class="cart-items" style="margin-bottom: 1.5rem;">
        \${this.cart.map(item => {
          // thumbnail: Cloudinary image or food emoji placeholder
          const thumb = item.image
            ? \\\`<img src="\${item.image}" alt="\${item.menuItemName}"
                style="width:64px;height:64px;border-radius:12px;object-fit:cover;flex-shrink:0;box-shadow:0 2px 4px rgba(0,0,0,0.05);">\\\`
            : \\\`<div style="width:64px;height:64px;border-radius:12px;background:var(--surface);border:1px solid var(--border);
                display:flex;align-items:center;justify-content:center;flex-shrink:0;font-size:1.8rem;box-shadow:0 2px 4px rgba(0,0,0,0.05);">
                🍽️
               </div>\\\`;

          // customization summary
          const custLines = item.customizations && item.customizations.length > 0
            ? \\\`<div style="display:flex; flex-wrap:wrap; gap:4px; margin-top:6px;">\${item.customizations.map(c => \\\`<span style="background:var(--primary); background-opacity:0.1; color:var(--primary); border:1px solid var(--primary); padding:2px 6px; border-radius:4px; font-size:10px; font-weight:600;">+ \${c.optionName || c.groupName}</span>\\\`).join('')}</div>\\\`
            : '';

          return \\\`
          <div class="cart-item-row" data-cart-id="\${item.cartId}" style="display:flex; gap:12px; padding:12px; background:var(--surface-hover); border-radius:12px; margin-bottom:12px; border:1px solid var(--border); box-shadow: 0 1px 2px rgba(0,0,0,0.03);">
            \${thumb}
            <div style="flex:1; display:flex; flex-direction:column; justify-content:center; min-width:0;">
              <div style="font-weight:700; font-size:1rem; line-height:1.2; color:var(--foreground); margin-bottom:4px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">\${item.menuItemName}</div>
              <div style="font-size:0.8rem; color:var(--text-muted); font-weight:500;">\${this.formatPrice(item.unitPrice)} each</div>
              \${custLines}
              \${item.instruction ? \\\`<div style="font-size:11px; color:#d97706; margin-top:6px; background:#fef3c7; padding:4px 8px; border-radius:4px; display:inline-block; border-left:2px solid #f59e0b;"><i data-lucide="pen-tool" style="width:10px;height:10px;display:inline;"></i> \${this._escHtml(item.instruction)}</div>\\\` : ''}
            </div>
            <div style="display:flex; flex-direction:column; justify-content:space-between; align-items:flex-end; min-width:90px;">
              <div style="font-weight:800; font-size:1.05rem; color:var(--primary);">\${this.formatPrice(item.unitPrice * item.quantity)}</div>
              <div style="display:flex; align-items:center; background:var(--surface); border:1px solid var(--border); border-radius:8px; padding:2px; margin-top:8px; box-shadow:0 1px 2px rgba(0,0,0,0.05);">
                <button onclick="customerApp.updateQuantity(\${item.cartId}, -1)" style="width:28px; height:28px; display:flex; align-items:center; justify-content:center; border:none; background:transparent; cursor:pointer; color:var(--danger);">
                  <i data-lucide="\${item.quantity === 1 ? 'trash-2' : 'minus'}" style="width:14px; height:14px;"></i>
                </button>
                <span style="font-weight:700; font-size:14px; min-width:32px; text-align:center; color:var(--foreground);">\${item.quantity}</span>
                <button onclick="customerApp.updateQuantity(\${item.cartId}, 1)" style="width:28px; height:28px; display:flex; align-items:center; justify-content:center; border:none; background:transparent; cursor:pointer; color:var(--success);">
                  <i data-lucide="plus" style="width:14px; height:14px;"></i>
                </button>
              </div>
            </div>
          </div>\\\`;
        }).join('')}
      </div>

      <div class="border-t border-border pt-3 mb-3">
        <div class="flex justify-between text-sm mb-1">
          <span>Subtotal</span>
          <span>\${this.formatPrice(total)}</span>
        </div>
        \${couponHtml}
        \${taxHtml}
        \${chargeHtml}
        <div class="flex justify-between font-bold text-lg mt-2 pt-2 border-t border-border">
          <span>Total</span>
          <span>\${this.formatPrice(finalTotal)}</span>
        </div>
      </div>
      
      \${couponInputHtml}

      <div id="cart-custom-fields" class="mb-3 \${cfHtml ? '' : 'hidden'}">
        \${cfHtml}
      </div>

      <textarea id="customer-note" class="form-control mb-3" placeholder="Any special instructions or allergies..." rows="2"
        style="resize:none;font-size:0.9rem;"></textarea>

      <div class="flex gap-2 items-center">
        <select id="pay-method-select" class="form-control" style="width: 150px; font-weight: 600; height: 100%; border: 1px solid var(--border); padding: 0.75rem; border-radius: 8px;">
          <option value="OFFLINE">Counter (Cash/Card)</option>
          <option value="CASHFREE">Pay Online (UPI)</option>
        </select>
        <button id="place-order-btn" class="btn btn-primary flex-1" onclick="customerApp.placeOrder()"
          style="padding: 1rem; font-size: 1rem; font-weight: 600; letter-spacing: 0.02em;">
          <i data-lucide="check-circle" style="width:18px;height:18px;display:inline;margin-right:0.4rem;"></i>
          Place Order &mdash; \${this.formatPrice(finalTotal)}
        </button>
      </div>
    \`;
    
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }
