// Customer App Logic - DeQueue Customer Ordering System
class CustomerApp {
  constructor() {
    this.vendorCode = this.getVendorCodeFromUrl();
    const storedCart = sessionStorage.getItem(`dequeue_cart_${this.vendorCode}`);
    this.cart = storedCart ? JSON.parse(storedCart) : [];
    this.vendor = null;
    this.menu = null;
    this.activeOrder = null;
    this.pollingInterval = null;
    this.notificationManager = null;
    this.sessionId = this.getOrCreateSessionId();
    // Geofence
    this.customerLat = null;
    this.customerLng = null;
    this.geofenceValidated = false;
    this.init();
  }

  getOrCreateSessionId() {
    let sid = localStorage.getItem('dequeue_customer_session');
    if (!sid) {
      sid = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2, 15);
      localStorage.setItem('dequeue_customer_session', sid);
    }
    return sid;
  }

  getVendorCodeFromUrl() {
    const path = window.location.pathname;
    const match = path.match(/\/v\/([^\/]+)/);
    if (match) return match[1];
    
    const params = new URLSearchParams(window.location.search);
    return params.get('vendor') || params.get('v');
  }

  async init() {
    if (!this.vendorCode) {
      this.showError('Invalid vendor link. Please scan the QR code again.');
      return;
    }

    // Check for active order in localStorage
    const stored = localStorage.getItem(`dequeue_order_${this.vendorCode}`);
    if (stored) {
      try {
        this.activeOrder = JSON.parse(stored);
        if (this.activeOrder && this.activeOrder.queueNumber) {
          await this.checkExistingOrder();
          return;
        }
      } catch (e) { /* ignore parse errors */ }
    }

    await this.loadVendor();
  }

  async loadVendor() {
    try {
      const res = await fetch(`/api/v1/public/vendors/${this.vendorCode}`);
      const data = await res.json();
      
      if (!data.success) {
        this.showError(data.message || 'Vendor not found');
        return;
      }

      this.vendor = data.data;
      
      // Fetch currently serving initially and start polling
      await this.updateCurrentlyServing();
      if (!this.pollingInterval) {
          this.pollingInterval = setInterval(() => this.updateCurrentlyServing(), 10000);
      }

      document.title = `${this.vendor.shopName} - Order | DeQueue`;

      // Update header
      const shopName = document.querySelector('.shop-info h1');
      if (shopName) shopName.textContent = this.vendor.shopName;

      if (this.vendor.logo) {
        const logoEl = document.querySelector('.shop-logo');
        if (logoEl) logoEl.innerHTML = `<img src="${this.vendor.logo}" alt="Logo" style="width:40px;height:40px;border-radius:50%;object-fit:cover;">`;
      }

      if (this.vendor.shopStatus !== 'OPEN') {
        this.showClosed();
        return;
      }

      const statusEl = document.querySelector('.shop-status');
      if (statusEl) {
        statusEl.className = 'shop-status open';
        statusEl.textContent = 'Accepting Orders';
      }

      await this.loadMenu();

      // If vendor has geofence enabled, prompt user for location
      if (this.vendor.settings && this.vendor.settings.enableGeofence) {
        this.promptGeofenceLocation();
      }

    } catch (err) {
      this.showError('Unable to load shop information. Please try again.');
      console.error(err);
    }
  }

  async updateCurrentlyServing() {
      try {
          const currRes = await fetch(`/api/v1/public/orders/${this.vendorCode}/currently-serving`);
          const currData = await currRes.json();
          const b = document.getElementById('running-queue-banner');
          const d = document.getElementById('running-queue-display');
          
          if (b && d) {
              if (currData.success && currData.data) {
                  d.textContent = currData.data;
                  b.classList.remove('hidden');
              } else {
                  b.classList.add('hidden');
              }
          }
      } catch (e) {
          // Silent fail for polling
      }
  }

  async loadMenu() {
    try {
      const res = await fetch(`/api/v1/public/menu/${this.vendorCode}/categories`);
      const data = await res.json();

      if (!data.success) {
        this.showError('Unable to load menu');
        return;
      }

      this.menu = data.data;
      this.renderCategories();
      this.renderMenuItems();
    } catch (err) {
      this.showError('Unable to load menu. Please try again.');
      console.error(err);
    }
  }

  renderCategories() {
    const container = document.querySelector('.category-pills');
    if (!container || !this.menu || !this.menu.categories) return;

    container.innerHTML = `<button class="category-pill active" data-category="all">All</button>`;
    this.menu.categories.forEach(cat => {
      container.innerHTML += `<button class="category-pill" data-category="${cat.id}">${cat.name}</button>`;
    });

    container.querySelectorAll('.category-pill').forEach(pill => {
      pill.addEventListener('click', (e) => {
        container.querySelectorAll('.category-pill').forEach(p => p.classList.remove('active'));
        e.target.classList.add('active');
        this.filterByCategory(e.target.dataset.category);
      });
    });
  }

  renderMenuItems(categoryId = 'all') {
    const grid = document.querySelector('.menu-grid');
    if (!grid || !this.menu) return;

    let items = [];
    if (this.menu.categories) {
      this.menu.categories.forEach(cat => {
        if (cat.items) {
          cat.items.forEach(item => {
            // Treat missing flags as true (robust against legacy DB entries)
            const isAvailable = item.available !== false;
            const isVisible = item.visible !== false;
            if (isAvailable && isVisible) {
              items.push({ ...item, categoryId: cat.id, categoryName: cat.name });
            }
          });
        }
      });
    }

    if (categoryId !== 'all') {
      items = items.filter(i => i.categoryId === categoryId);
    }

    if (items.length === 0) {
      grid.innerHTML = `<div class="text-center text-muted py-8" style="grid-column: 1/-1;">No items available</div>`;
      return;
    }

    grid.innerHTML = items.map(item => `
      <div class="menu-item-card" data-item-id="${item.id}">
        <div class="item-info">
          <div class="item-title">${item.name}</div>
          <div class="item-desc">${item.description || ''}</div>
          ${item.preparationTime ? `<div class="text-xs text-muted mt-1"><i data-lucide="clock" style="width:12px;height:12px;display:inline"></i> ~${item.preparationTime} min</div>` : ''}
          <div class="flex items-center justify-between mt-auto pt-2">
            <div class="item-price">${this.formatPrice(item.price)}</div>
            <button class="btn btn-primary add-to-cart-btn" style="padding: 0.25rem 0.75rem; min-height: 32px; font-size: 0.85rem;"
              onclick="customerApp.handleAddToCart('${item.id}')">Add</button>
          </div>
        </div>
        ${item.image
          ? `<img src="${item.image}" alt="${item.name}" class="item-image" loading="lazy">`
          : `<div class="item-image-placeholder"><i data-lucide="utensils"></i></div>`}
      </div>
    `).join('');

    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  filterByCategory(categoryId) {
    this.renderMenuItems(categoryId);
  }

  getItemById(itemId) {
    if (!this.menu || !this.menu.categories) return null;
    for (const cat of this.menu.categories) {
      if (cat.items) {
        const item = cat.items.find(i => i.id === itemId);
        if (item) return item;
      }
    }
    return null;
  }

  handleAddToCart(itemId) {
    const item = this.getItemById(itemId);
    if (!item) return;

    // Check if item has customization groups
    if (item.customizationGroups && item.customizationGroups.length > 0) {
      this.showCustomizationModal(item);
    } else {
      this.addToCart(item, []);
    }
  }

  showCustomizationModal(item) {
    const title = document.getElementById('cust-modal-title');
    const body = document.getElementById('cust-modal-body');
    const addBtn = document.getElementById('cust-modal-add-btn');
    if (!title || !body || !addBtn) return;

    title.innerText = `Customize ${item.name}`;
    
    let html = '';
    
    // Add image if requested
    if (item.image) {
        html += `<div style="margin-bottom: 1rem;"><img src="${item.image}" alt="${item.name}" style="width: 100%; height: 160px; object-fit: cover; border-radius: var(--radius-md);"></div>`;
    } else {
        html += `<div style="width: 100%; height: 160px; background: var(--surface); display: flex; align-items: center; justify-content: center; border-radius: var(--radius-md); margin-bottom: 1rem;"><i data-lucide="utensils" style="opacity: 0.3; width: 48px; height: 48px;"></i></div>`;
    }
    (item.customizationGroups || []).forEach((group, gIdx) => {
        html += `<div class="mb-4">
            <h4 class="font-bold mb-2">${group.name} ${group.required ? '<span class="text-danger">*</span>' : ''}</h4>
            <div class="flex flex-col gap-2">`;
        
        (group.options || []).forEach((opt, oIdx) => {
            const inputType = group.selectionType === 'SINGLE' || group.maxSelection === 1 ? 'radio' : 'checkbox';
            const inputName = `cust_${group.id}`;
            const inputId = `cust_${group.id}_${oIdx}`;
            const additionalPrice = opt.additionalPrice || 0;
            
            html += `<label class="flex items-center justify-between p-2 border border-border rounded-md" for="${inputId}">
                <div class="flex items-center gap-2">
                    <input type="${inputType}" name="${inputName}" id="${inputId}" value="${opt.name}" data-price="${additionalPrice}" data-group-name="${group.name}">
                    <span>${opt.name}</span>
                </div>
                ${additionalPrice > 0 ? `<span class="text-muted text-sm">+₹${additionalPrice}</span>` : ''}
            </label>`;
        });
        
        html += `</div></div>`;
    });
    
    body.innerHTML = html;
    
    addBtn.onclick = () => {
        const customizations = [];
        let missingRequired = false;
        
        (item.customizationGroups || []).forEach(group => {
            const inputs = body.querySelectorAll(`input[name="cust_${group.id}"]:checked`);
            if (group.required && inputs.length === 0) {
                missingRequired = true;
            }
             inputs.forEach(input => {
                customizations.push({
                    optionName: input.value,
                    additionalPrice: parseFloat(input.dataset.price || 0),
                    groupName: input.dataset.groupName,
                    groupId: group.id
                });
            });
        });
        
        if (missingRequired) {
            if (window.showToast) showToast('Please select all required options', 'error');
            return;
        }
        
        this.addToCart(item, customizations);
        if (window.closeModal) closeModal('cust-modal');
    };
    
    if (window.openModal) openModal('cust-modal');
  }

  addToCart(item, customizations = []) {
    const extraPrice = customizations.reduce((sum, c) => sum + (c.additionalPrice || 0), 0);
    const unitPrice = item.price + extraPrice;

    const cartItem = {
      menuItemId: item.id,
      menuItemName: item.name,
      image: item.image || null,         // ← store image for cart display
      quantity: 1,
      unitPrice: unitPrice,
      totalPrice: unitPrice,
      customizations: customizations,
      cartId: Date.now()
    };

    // Check if same item (without customizations) already in cart
    const existing = this.cart.find(c => c.menuItemId === item.id && customizations.length === 0);
    if (existing) {
      existing.quantity++;
      existing.totalPrice = existing.unitPrice * existing.quantity;
    } else {
      this.cart.push(cartItem);
    }

    this.updateCartUI();
    this.renderCartModal();
    if (typeof showToast === 'function') showToast(`Added ${item.name} to cart`, 'success');
  }


  updateQuantity(cartId, delta) {
    const item = this.cart.find(c => c.cartId === cartId);
    if (!item) return;
    
    item.quantity += delta;
    if (item.quantity <= 0) {
      this.removeFromCart(cartId);
      return;
    }
    
    item.totalPrice = item.unitPrice * item.quantity;
    this.updateCartUI();
    this.renderCartModal();
  }

  removeFromCart(cartId) {
    this.cart = this.cart.filter(c => c.cartId !== cartId);
    this.updateCartUI();
    this.renderCartModal();
  }

  saveCart() {
    if (this.vendorCode) {
      sessionStorage.setItem(`dequeue_cart_${this.vendorCode}`, JSON.stringify(this.cart));
    }
  }

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

    body.innerHTML = `
      <div class="cart-items" style="margin-bottom: 1rem;">
        ${this.cart.map(item => {
          // thumbnail: Cloudinary image or food emoji placeholder
          const thumb = item.image
            ? `<img src="${item.image}" alt="${item.menuItemName}"
                style="width:52px;height:52px;border-radius:10px;object-fit:cover;flex-shrink:0;">`
            : `<div style="width:52px;height:52px;border-radius:10px;background:var(--surface-hover);
                display:flex;align-items:center;justify-content:center;flex-shrink:0;font-size:1.5rem;">
                🍽️
               </div>`;

          // customization summary
          const custLines = item.customizations && item.customizations.length > 0
            ? item.customizations.map(c => `<span class="cart-cust-chip">${c.optionName || c.groupName || ''}</span>`).join('')
            : '';

          return `
          <div class="cart-item-row" data-cart-id="${item.cartId}">
            ${thumb}
            <div style="flex:1;min-width:0;">
              <div style="font-weight:600;font-size:0.95rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${item.menuItemName}</div>
              ${custLines ? `<div class="cart-cust-chips">${custLines}</div>` : ''}
              <div style="font-size:0.85rem;color:var(--text-muted);">${this.formatPrice(item.unitPrice)} each</div>
            </div>
            <div style="display:flex;flex-direction:column;align-items:flex-end;gap:0.35rem;flex-shrink:0;">
              <span style="font-weight:700;color:var(--primary);">${this.formatPrice(item.unitPrice * item.quantity)}</span>
              <div class="cart-qty-ctrl">
                <button class="btn-icon" onclick="customerApp.updateQuantity(${item.cartId}, -1)" style="padding:2px;width:26px;height:26px;min-height:26px;">
                  <i data-lucide="minus" style="width:12px;height:12px;"></i>
                </button>
                <span style="font-weight:600;min-width:1.25rem;text-align:center;">${item.quantity}</span>
                <button class="btn-icon" onclick="customerApp.updateQuantity(${item.cartId}, 1)" style="padding:2px;width:26px;height:26px;min-height:26px;">
                  <i data-lucide="plus" style="width:12px;height:12px;"></i>
                </button>
              </div>
            </div>
          </div>`;
        }).join('')}
      </div>

      <div class="border-t border-border pt-3 mb-3">
        <div class="flex justify-between text-sm mb-1">
          <span>Subtotal</span>
          <span>${this.formatPrice(total)}</span>
        </div>
        ${couponHtml}
        ${taxHtml}
        ${chargeHtml}
        <div class="flex justify-between font-bold text-lg mt-2 pt-2 border-t border-border">
          <span>Total</span>
          <span>${this.formatPrice(finalTotal)}</span>
        </div>
      </div>
      
      ${couponInputHtml}

      <!-- Payment Method Section -->
      <div class="card mt-3 mb-3" style="padding:1rem;background:rgba(99,102,241,.06);border:1px solid rgba(99,102,241,.2);">
        <div class="flex justify-between items-center mb-2">
          <span class="font-bold" style="font-size:.9rem;">Amount to Pay</span>
          <span class="font-bold text-primary" style="font-size:1.2rem;">${this.formatPrice(finalTotal)}</span>
        </div>
        <div style="font-size:.85rem;color:var(--text-muted);margin-bottom:.75rem;">Payment Method (select at counter)</div>
        <label class="flex items-center gap-2" style="font-size:.9rem;">
          <input type="radio" name="pay-method" value="counter" checked style="width:1.1rem;height:1.1rem;">
          <span>Pay at Counter</span>
        </label>
        <label class="flex items-center gap-2 mt-2 opacity-50" style="font-size:.9rem;" title="Coming soon">
          <input type="radio" name="pay-method" value="online" disabled style="width:1.1rem;height:1.1rem;">
          <span>Online Payment <span class="badge" style="font-size:.65rem;padding:2px 6px;">Coming Soon</span></span>
        </label>
      </div>

      <div id="cart-custom-fields" class="mb-3 ${cfHtml ? '' : 'hidden'}">
        ${cfHtml}
      </div>

      <textarea id="customer-note" class="form-control mb-3" placeholder="Any special instructions or allergies..." rows="2"
        style="resize:none;font-size:0.9rem;"></textarea>

      <button id="place-order-btn" class="btn btn-primary w-full" onclick="customerApp.placeOrder()"
        style="padding: 1rem; font-size: 1rem; font-weight: 600; letter-spacing: 0.02em;">
        <i data-lucide="check-circle" style="width:18px;height:18px;display:inline;margin-right:0.4rem;"></i>
        Place Order — ${this.formatPrice(finalTotal)}
      </button>
    `;
    
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  async placeOrder() {
    if (this.cart.length === 0) return;

    // ── Prevent double-submit immediately ──
    const placeBtn = document.getElementById('place-order-btn');
    if (placeBtn) {
      if (placeBtn.disabled) return; // already in-flight
      placeBtn.disabled = true;
      placeBtn.innerHTML = '<i data-lucide="loader-2" style="width:18px;height:18px;display:inline;margin-right:0.4rem;animation:spin 1s linear infinite;"></i> Placing Order…';
      if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    // Geofence check: if vendor requires it, validate user location first
    if (this.vendor && this.vendor.settings && this.vendor.settings.enableGeofence) {
      const allowed = await this.validateGeofence();
      if (!allowed) {
        if (placeBtn) { placeBtn.disabled = false; placeBtn.innerHTML = '<i data-lucide="check-circle" style="width:18px;height:18px;display:inline;margin-right:0.4rem;"></i> Place Order'; if (typeof lucide !== 'undefined') lucide.createIcons(); }
        return;
      }
    }

    const note = document.getElementById('customer-note')?.value || '';
    
    // Collect Custom Fields
    const metadata = {};
    if (this.vendor?.settings?.customFields) {
        let missingRequired = false;
        this.vendor.settings.customFields.forEach((cf, idx) => {
            const el = document.getElementById(`cf-${idx}`);
            if (cf.type === 'CHECKBOX') {
                const checked = Array.from(document.querySelectorAll(`input[name="cf-${idx}"]:checked`)).map(cb => cb.value);
                if (cf.required && checked.length === 0) missingRequired = true;
                if (checked.length > 0) metadata[cf.name] = checked.join(', ');
            } else {
                const val = el ? el.value.trim() : '';
                if (cf.required && !val) missingRequired = true;
                if (val) metadata[cf.name] = val;
            }
        });
        
        if (missingRequired) {
            if (placeBtn) { placeBtn.disabled = false; placeBtn.innerHTML = 'Place Order'; }
            if (window.showToast) showToast('Please fill all required fields', 'error');
            return;
        }
    }

    const orderData = {
      sessionId: this.sessionId,
      items: this.cart.map(item => {
        const groupedCusts = {};
        (item.customizations || []).forEach(c => {
            if (c.groupId) {
                if (!groupedCusts[c.groupId]) {
                    groupedCusts[c.groupId] = { groupId: c.groupId, selectedOptionNames: [] };
                }
                groupedCusts[c.groupId].selectedOptionNames.push(c.optionName);
            }
        });

        return {
          menuItemId: item.menuItemId,
          quantity: item.quantity,
          customizations: Object.values(groupedCusts)
        };
      }),
      customerNote: note,
      metadata: metadata,
      ...(this._currentCheckout || {})
    };

    try {
      const res = await fetch(`/api/v1/public/orders/${this.vendorCode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(orderData)
      });
      const data = await res.json();

      if (!data.success) {
        if (typeof showToast === 'function') showToast(data.message || 'Failed to place order', 'error');
        // Re-enable button so user can retry
        if (placeBtn) { placeBtn.disabled = false; placeBtn.innerHTML = '<i data-lucide="check-circle" style="width:18px;height:18px;display:inline;margin-right:0.4rem;"></i> Place Order'; if (typeof lucide !== 'undefined') lucide.createIcons(); }
        return;
      }

      this.activeOrder = data.data;

      // Initialize notification manager for real-time updates
      if (typeof CustomerNotificationManager !== 'undefined') {
        this.notificationManager = new CustomerNotificationManager(this);
        const orderData = data.data;
        this.notificationManager.init(
          orderData.id,
          orderData.sessionId,
          orderData.customerSessionToken,
          orderData.queueNumber
        );

        // Save token data for reconnection
        localStorage.setItem(`dequeue_token_${this.vendorCode}`, JSON.stringify({
          orderId: orderData.id,
          sessionId: orderData.sessionId,
          customerToken: orderData.customerSessionToken
        }));
      }

      localStorage.setItem(`dequeue_order_${this.vendorCode}`, JSON.stringify(this.activeOrder));
      this.cart = [];
      sessionStorage.removeItem(`dequeue_cart_${this.vendorCode}`);

      // Close cart modal and show tracking
      const floatingCart = document.getElementById('floating-cart');
      if (floatingCart) floatingCart.classList.remove('visible');
      if (typeof closeModal === 'function') closeModal('cart-modal');
      this.showOrderTracking();

    } catch (err) {
      if (typeof showToast === 'function') showToast('Failed to place order. Please try again.', 'error');
      console.error(err);
      // Re-enable button so user can retry
      if (placeBtn) { placeBtn.disabled = false; placeBtn.innerHTML = '<i data-lucide="check-circle" style="width:18px;height:18px;display:inline;margin-right:0.4rem;"></i> Place Order'; if (typeof lucide !== 'undefined') lucide.createIcons(); }
    }
  }


  async checkExistingOrder() {
    try {
      const res = await fetch(`/api/v1/public/orders/${this.vendorCode}/track/${this.activeOrder.queueNumber}`);
      
      // If the order genuinely doesn't exist (404), clear the session.
      if (res.status === 404) {
        localStorage.removeItem(`dequeue_order_${this.vendorCode}`);
        this.activeOrder = null;
        await this.loadVendor();
        return;
      }

      if (!res.ok) {
        throw new Error(`Failed to track order: ${res.status}`);
      }

      const data = await res.json();

      if (data.success) {
        this.activeOrder = data.data;
        if (data.data.status === 'COLLECTED' || data.data.status === 'CANCELLED') {
          localStorage.removeItem(`dequeue_order_${this.vendorCode}`);
          await this.loadVendor();
          
          const menuView = document.getElementById('menu-view');
          const thankYouView = document.getElementById('thank-you-view');
          
          if (data.data.status === 'CANCELLED') {
              document.getElementById('thank-you-title').textContent = 'Order Cancelled';
              document.getElementById('thank-you-message').textContent = 'Unfortunately, your order was cancelled. Please try ordering again.';
              document.getElementById('thank-you-icon').setAttribute('data-lucide', 'x-circle');
              document.getElementById('thank-you-icon').setAttribute('class', 'text-warning');
          } else {
              document.getElementById('thank-you-title').textContent = 'Thank You!';
              document.getElementById('thank-you-message').textContent = 'Your order has been successfully collected. Please visit us again!';
              document.getElementById('thank-you-icon').setAttribute('data-lucide', 'heart');
              document.getElementById('thank-you-icon').setAttribute('class', 'text-danger');
          }
          if (typeof lucide !== 'undefined') lucide.createIcons();
          
          if (menuView) menuView.classList.add('hidden');
          if (thankYouView) thankYouView.classList.remove('hidden');
          
          this.activeOrder = null;
        } else {
          await this.loadVendor();
          this.showOrderTracking();
          
          // Restore notification manager if we have stored token
          const storedToken = localStorage.getItem(`dequeue_token_${this.vendorCode}`);
          if (storedToken && typeof CustomerNotificationManager !== 'undefined' && !this.notificationManager) {
            try {
              const tokenData = JSON.parse(storedToken);
              this.notificationManager = new CustomerNotificationManager(this);
              this.notificationManager.init(
                tokenData.orderId,
                tokenData.sessionId,
                tokenData.customerToken,
                this.activeOrder.queueNumber
              );
            } catch(e) { console.error('Failed to restore notifications:', e); }
          }
        }
      } else {
        // Just log the error, don't clear the session unless it was a 404
        console.error('Track API returned success: false', data);
        await this.loadVendor();
        this.showOrderTracking(); // keep showing tracking view with last known state
      }
    } catch (err) {
      console.error('Error checking existing order:', err);
      // Don't wipe session on network errors or 500s. Just show the last known state.
      await this.loadVendor();
      if (this.activeOrder) {
          this.showOrderTracking();
      }
    }
  }

  async cancelOrder() {
    if (!this.activeOrder) return;
    
    const cancelBtn = document.getElementById('cancel-order-btn');
    if (cancelBtn) {
      if (cancelBtn.disabled) return;
      cancelBtn.disabled = true;
      cancelBtn.innerHTML = '<i data-lucide="loader-2" style="width:18px;height:18px;display:inline;margin-right:0.4rem;animation:spin 1s linear infinite;"></i> Cancelling…';
      if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    try {
      const storedToken = localStorage.getItem(`dequeue_token_${this.vendorCode}`);
      let sessionToken = '';
      if (storedToken) {
          try {
              sessionToken = JSON.parse(storedToken).customerToken || '';
          } catch(e) {}
      }

      const res = await fetch(`/api/v1/public/orders/${this.vendorCode}/cancel/${this.activeOrder.queueNumber}`, {
        method: 'POST',
        headers: { 
            'Content-Type': 'application/json',
            'X-Session-Token': sessionToken
        }
      });
      const data = await res.json();

      if (!data.success) {
        if (typeof showToast === 'function') showToast(data.message || 'Failed to cancel order', 'error');
        if (cancelBtn) {
            cancelBtn.disabled = false;
            cancelBtn.innerHTML = '<i data-lucide="x-circle" style="width:18px;height:18px;display:inline;margin-right:0.4rem;"></i> Cancel Order';
            if (typeof lucide !== 'undefined') lucide.createIcons();
        }
        return;
      }
      
      // Order cancelled successfully. The WebSocket will pick up the CANCELLED status,
      // but we can manually handle it here just in case.
      this.handleOrderUpdate(data.data);

    } catch (err) {
      console.error('Failed to cancel order:', err);
      if (typeof showToast === 'function') showToast('Failed to cancel order', 'error');
      if (cancelBtn) {
          cancelBtn.disabled = false;
          cancelBtn.innerHTML = '<i data-lucide="x-circle" style="width:18px;height:18px;display:inline;margin-right:0.4rem;"></i> Cancel Order';
          if (typeof lucide !== 'undefined') lucide.createIcons();
      }
    }
  }

  showOrderTracking() {
    this.connectWebSocket();
    const menuView = document.getElementById('menu-view');
    const orderView = document.getElementById('order-view');
    if (menuView) menuView.classList.add('hidden');
    if (orderView) orderView.classList.remove('hidden');

    if (this.activeOrder) {
      const qnEl = document.getElementById('queue-number');
      if (qnEl) qnEl.textContent = this.activeOrder.queueNumber || 'N/A';

      // Show amount-to-pay banner
      const amtBanner = document.getElementById('amount-to-pay-banner');
      const amtValue = document.getElementById('amount-to-pay-value');
      if (amtBanner && amtValue && this.activeOrder.totalAmount != null) {
        amtValue.textContent = this.formatPrice(this.activeOrder.totalAmount);
        amtBanner.classList.remove('hidden');
      }

      this.updateStatusDisplay(this.activeOrder.status);
      this.subscribeToOrder(this.activeOrder.queueNumber);
    }
  }

  connectWebSocket() {
    if (this.stompClient && this.stompClient.connected) return;

    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;

    this.stompClient.connect({}, (frame) => {
      console.log('WebSocket connected');
      this.wsReconnectAttempts = 0;

      // Subscribe to vendor updates
      if (this.vendor && this.vendor.id) {
        this.subscribeToVendor(this.vendor.id);
      }

      // Subscribe to active order
      if (this.activeOrder && this.activeOrder.queueNumber) {
        this.subscribeToOrder(this.activeOrder.queueNumber);
      }

      // Notify notification manager of reconnection
      if (this.notificationManager) {
        this.notificationManager.onReconnected();
      }
    }, (error) => {
      console.error('WebSocket disconnected:', error);
      // Exponential backoff reconnect
      const attempts = this.wsReconnectAttempts || 0;
      const delay = Math.min(1000 * Math.pow(2, attempts), 30000);
      this.wsReconnectAttempts = attempts + 1;
      console.log(`Reconnecting in ${delay}ms (attempt ${this.wsReconnectAttempts})...`);
      setTimeout(() => this.connectWebSocket(), delay);
    });
  }
  
  subscribeToVendor(vendorId) {
      if (!this.stompClient || !this.stompClient.connected) return;
      this.stompClient.subscribe('/topic/vendor/' + vendorId, (msg) => {
          const event = JSON.parse(msg.body);
          if (event.status === 'PREPARING' || event.status === 'READY') {
              const banner = document.getElementById('running-queue-banner');
              const display = document.getElementById('running-queue-display');
              if (banner && display) {
                  banner.classList.remove('hidden');
                  display.textContent = event.queueNumber;
              }
          }
      });
  }

  subscribeToOrder(queueNumber) {
      if (!this.stompClient || !this.stompClient.connected) return;
      this.stompClient.subscribe('/topic/orders/' + queueNumber, (msg) => {
          const event = JSON.parse(msg.body);
          this.handleOrderUpdate(event);
      });
  }
  
  handleOrderUpdate(event) {
      if (!this.activeOrder) return;
      
      const status = event.status || event.orderStatus;
      if (!status) return;
      
      this.activeOrder.status = status;
      // Update total from event if available
      if (event.totalAmount != null) this.activeOrder.totalAmount = event.totalAmount;
      localStorage.setItem(`dequeue_order_${this.vendorCode}`, JSON.stringify(this.activeOrder));
      this.updateStatusDisplay(status);
      this.showBrowserNotification(status);
      
      if (status === 'COMPLETED' || status === 'COLLECTED' || status === 'CANCELLED') {
        localStorage.removeItem(`dequeue_order_${this.vendorCode}`);
        localStorage.removeItem(`dequeue_token_${this.vendorCode}`);
        if (this.notificationManager) {
          this.notificationManager.destroy();
          this.notificationManager = null;
        }
        const orderView = document.getElementById('order-view');
        const thankYouView = document.getElementById('thank-you-view');
        
        if (status === 'CANCELLED') {
            document.getElementById('thank-you-title').textContent = 'Order Cancelled';
            document.getElementById('thank-you-message').textContent = 'Unfortunately, your order was cancelled. Please try ordering again.';
            document.getElementById('thank-you-icon').setAttribute('data-lucide', 'x-circle');
            document.getElementById('thank-you-icon').setAttribute('class', 'text-warning');
            const invoiceActions = document.getElementById('invoice-actions');
            if (invoiceActions) invoiceActions.classList.add('hidden');
        } else {
            document.getElementById('thank-you-title').textContent = 'Thank You!';
            document.getElementById('thank-you-message').textContent = 'Your order has been successfully completed. Please visit us again!';
            document.getElementById('thank-you-icon').setAttribute('data-lucide', 'heart');
            document.getElementById('thank-you-icon').setAttribute('class', 'text-danger');
            // Show invoice
            const invoiceActions = document.getElementById('invoice-actions');
            const invoiceTotalDisplay = document.getElementById('invoice-total-display');
            if (invoiceActions) invoiceActions.classList.remove('hidden');
            if (invoiceTotalDisplay && this.activeOrder.totalAmount != null) {
                invoiceTotalDisplay.textContent = this.formatPrice(this.activeOrder.totalAmount);
            }
            // Store completed order for invoice generation
            this._completedOrder = { ...this.activeOrder };
        }
        if (typeof lucide !== 'undefined') lucide.createIcons();
        
        if (orderView) orderView.classList.add('hidden');
        if (thankYouView) thankYouView.classList.remove('hidden');
        
        this.activeOrder = null;
      }
  }

  async showBrowserNotification(status) {
      if (!('Notification' in window)) {
        console.warn('Browser notifications are not supported.');
        return;
      }
    
      console.log('Notification permission:', Notification.permission);
    
      if (Notification.permission === 'default') {
        try {
          const permission = await Notification.requestPermission();
          console.log('Notification permission result:', permission);
        } catch (err) {
          console.error('Notification permission failed:', err);
          return;
        }
      }
    
      if (Notification.permission !== 'granted') {
        console.warn('Browser notification permission is not granted.');
        return;
      }
    
      const notifications = {
        READY: {
          title: 'Order Ready! 🍔',
          body: `Your order #${this.activeOrder?.queueNumber || ''} is ready for collection.`,
          icon: '/images/icon-192.png'
        },
        PREPARING: {
          title: 'Order Preparing 👨‍🍳',
          body: `Your order #${this.activeOrder?.queueNumber || ''} is now being prepared.`,
          icon: '/images/icon-192.png'
        },
        CANCELLED: {
          title: 'Order Cancelled',
          body: `Your order #${this.activeOrder?.queueNumber || ''} has been cancelled.`,
          icon: '/images/icon-192.png'
        },
        COLLECTED: {
          title: 'Order Collected',
          body: 'Thank you! Your order has been collected.',
          icon: '/images/icon-192.png'
        }
      };
    
      const notification = notifications[status];
    
      if (!notification) return;
    
      try {
        const options = {
          body: notification.body,
          icon: notification.icon,
          tag: `dequeue-order-${this.activeOrder?.id || this.activeOrder?.queueNumber}`,
          renotify: true
        };

        // Android Chrome requires ServiceWorker for notifications. 
        // Fallback to 'new Notification' for desktop Safari/Firefox if SW is not ready.
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.ready.then(registration => {
                registration.showNotification(notification.title, options);
            }).catch(err => {
                new Notification(notification.title, options);
            });
        } else {
            new Notification(notification.title, options);
        }
      } catch (err) {
        console.error('Failed to create browser notification:', err);
      }
  }

  submitFeedback() {
      const text = document.getElementById('feedback-text')?.value;
      if (text) {
          if (typeof showToast === 'function') showToast('Thank you for your feedback!', 'success');
          document.getElementById('feedback-text').value = '';
      }
      this.startNewOrder();
  }
  
  startNewOrder() {
      this.activeOrder = null;
      this.cart = [];
      sessionStorage.removeItem(`dequeue_cart_${this.vendorCode}`);
      this.updateCartUI();
      this.geofenceValidated = false; // reset for next order attempt
      const thankYouView = document.getElementById('thank-you-view');
      const menuView = document.getElementById('menu-view');
      if (thankYouView) thankYouView.classList.add('hidden');
      if (menuView) menuView.classList.remove('hidden');
  }

  // ─── Geofence helpers ────────────────────────────────────────────────────

  /**
   * Shows a soft location banner below the category pills asking the customer
   * to allow location. Does NOT block — ordering will re-validate when they try.
   */
  promptGeofenceLocation() {
    // Inject a location notice if not already present
    const existing = document.getElementById('gf-location-notice');
    if (existing) return;

    const notice = document.createElement('div');
    notice.id = 'gf-location-notice';
    notice.style.cssText = [
      'display:flex', 'align-items:center', 'gap:.75rem',
      'background:rgba(99,102,241,.08)', 'border:1px solid rgba(99,102,241,.25)',
      'border-radius:10px', 'padding:.75rem 1rem', 'margin:.75rem 0',
      'font-size:.85rem', 'color:var(--text-color,#111)', 'cursor:pointer',
      'transition:background .2s'
    ].join(';');
    notice.innerHTML = `
      <i data-lucide="map-pin" style="width:18px;height:18px;color:#6366f1;flex-shrink:0;"></i>
      <span><strong>Location required.</strong> This shop requires you to be nearby to order.
        <span id="gf-share-btn" style="color:#6366f1;font-weight:600;text-decoration:underline;cursor:pointer;margin-left:.25rem;">Share my location</span>
      </span>
      <i id="gf-location-icon" data-lucide="circle" style="width:14px;height:14px;margin-left:auto;color:#9ca3af;flex-shrink:0;"></i>
    `;

    const categoryPills = document.querySelector('.category-pills');
    if (categoryPills && categoryPills.parentNode) {
      categoryPills.parentNode.insertBefore(notice, categoryPills);
    } else {
      const menuView = document.getElementById('menu-view');
      if (menuView) menuView.prepend(notice);
    }

    if (typeof lucide !== 'undefined') lucide.createIcons();

    document.getElementById('gf-share-btn').addEventListener('click', () => {
      this._requestLocationForGeofence((ok) => {
        this._updateGeofenceNotice(ok);
      });
    });

    // Proactively try to get location silently
    this._requestLocationForGeofence((ok) => {
      this._updateGeofenceNotice(ok);
    });
  }

  _requestLocationForGeofence(callback) {
    if (!navigator.geolocation) { callback(false); return; }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.customerLat = pos.coords.latitude;
        this.customerLng = pos.coords.longitude;
        callback(true);
      },
      () => { callback(false); },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 30000 }
    );
  }

  _updateGeofenceNotice(locationObtained) {
    const notice = document.getElementById('gf-location-notice');
    const icon   = document.getElementById('gf-location-icon');
    const shareBtn = document.getElementById('gf-share-btn');
    if (!notice) return;

    if (locationObtained) {
      notice.style.background = 'rgba(34,197,94,.08)';
      notice.style.borderColor = 'rgba(34,197,94,.25)';
      notice.innerHTML = `
        <i data-lucide="check-circle-2" style="width:18px;height:18px;color:#22c55e;flex-shrink:0;"></i>
        <span style="color:var(--text-color,#111)"><strong style="color:#16a34a">Location shared.</strong> You&rsquo;ll be verified when you place your order.</span>
      `;
    } else {
      if (shareBtn) shareBtn.textContent = 'Try again';
      if (icon) {
        icon.setAttribute('data-lucide', 'alert-circle');
        icon.style.color = '#f59e0b';
      }
    }
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  /**
   * Called from placeOrder when geofence is enabled.
   * Returns true if customer is within range, false if blocked.
   */
  async validateGeofence() {
    // If we already have coordinates, skip re-requesting
    if (this.customerLat === null || this.customerLng === null) {
      // Try to get location now (blocking)
      const obtained = await new Promise((resolve) => {
        if (!navigator.geolocation) { resolve(false); return; }
        navigator.geolocation.getCurrentPosition(
          (pos) => { this.customerLat = pos.coords.latitude; this.customerLng = pos.coords.longitude; resolve(true); },
          () => resolve(false),
          { enableHighAccuracy: true, timeout: 12000, maximumAge: 30000 }
        );
      });

      if (!obtained) {
        if (typeof showToast === 'function') {
          showToast('Location required. Please allow location access to order from this shop.', 'error');
        }
        return false;
      }
    }

    try {
      const res = await fetch('/api/v1/geofence/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          vendorCode: this.vendorCode,
          customerLatitude: this.customerLat,
          customerLongitude: this.customerLng
        })
      });

      if (!res.ok) {
         console.error('Geofence API failed with status:', res.status);
         if (typeof showToast === 'function') {
           showToast('Unable to verify your location. Please try again.', 'error');
         }
         return false;
      }

      const data = await res.json();

      if (data.success && data.data) {
        if (data.data.withinRange) {
          return true;
        } else {
          const dist = Math.round(data.data.distance);
          const max  = Math.round(data.data.maxRadius);
          if (typeof showToast === 'function') {
            showToast(`You are ${dist}m away. This shop only accepts orders within ${max}m.`, 'error');
          }
          return false;
        }
      }
    } catch (e) {
      console.error('Geofence validation network error:', e);
      // On genuine network error, allow ordering (fail-open)
      return true;
    }

    return false;
  }

  updateStatusDisplay(status) {
    // 5 steps in order
    const STEPS = ['PENDING', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED'];
    const currentIdx = STEPS.indexOf(status);

    // Update each timeline step
    STEPS.forEach((step, idx) => {
      const el = document.getElementById(`step-${step}`);
      if (!el) return;
      el.classList.remove('done', 'active');
      if (idx < currentIdx) el.classList.add('done');
      else if (idx === currentIdx) el.classList.add('active');
    });

    // Update badge
    const labels = {
      PENDING: 'Order Initiated',
      ACCEPTED: 'Order Accepted',
      PREPARING: 'Started Preparing',
      READY: 'Prepared — Ready for Collection',
      COMPLETED: 'Order Completed',
      CANCELLED: 'Cancelled'
    };
    const badgeClasses = {
      PENDING: 'badge-pending',
      ACCEPTED: 'badge-accepted',
      PREPARING: 'badge-preparing',
      READY: 'badge-ready',
      COMPLETED: 'badge-success',
      CANCELLED: 'badge-danger'
    };
    const badge = document.getElementById('order-status-badge');
    if (badge) {
      badge.textContent = labels[status] || status;
      badge.className = `badge ${badgeClasses[status] || 'badge-pending'} text-lg px-4 py-2 mt-4`;
    }

    // Show/hide cancel button (only for PENDING)
    const cancelContainer = document.getElementById('cancel-order-container');
    if (cancelContainer) {
      cancelContainer.classList.toggle('hidden', status !== 'PENDING');
    }
    
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  generateInvoiceHTML(order) {
    const shopName = this.vendor?.shopName || 'DeQueue Shop';
    const address = this.vendor?.address && this.vendor.address.street ? this.vendor.address.street : '';
    const phone = this.vendor?.phone || '';
    const email = this.vendor?.email || '';

    // Fallback to settings if not populated on order
    const settings = this.vendor?.settings || {};
    
    const subtotal = order.subtotal != null ? order.subtotal : (order.items ? order.items.reduce((sum, item) => sum + item.totalPrice, 0) : order.totalAmount);
    
    const taxName = order.taxName || settings.taxName || 'Tax';
    const taxValue = order.taxAmount != null ? order.taxAmount : ((subtotal * (settings.taxPercentage || 0)) / 100);
    const taxPct = settings.taxPercentage || 0; // used for fallback label
    
    const chargeName = order.serviceChargeName || settings.additionalChargeName || 'Service Charge';
    const chargeAmt = order.serviceChargeAmount != null ? order.serviceChargeAmount : (settings.additionalCharges || 0);
    
    const couponName = order.couponCode ? `Coupon (${order.couponCode})` : 'Coupon Discount';
    const couponDiscount = order.couponDiscount || 0;
    
    const computedTotal = order.totalAmount != null ? order.totalAmount : (subtotal + taxValue + chargeAmt - couponDiscount);
    const gstNumber = settings.gstNumber || '';

    const items = (order.items || []).map(i => `
      <tr>
        <td style="padding:8px 0;border-bottom:1px dashed #ccc;">
            ${this._escHtml(i.menuItemName)}
            ${i.selectedCustomizations && i.selectedCustomizations.length > 0 ? 
                '<br><small style="color:#666">+' + i.selectedCustomizations.map(c => c.selectedOptions.map(o => o.name).join(',')).join(',') + '</small>' : ''}
        </td>
        <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:center;">${i.quantity}</td>
        <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:right;">₹${Number(i.unitPrice || 0).toFixed(2)}</td>
        <td style="padding:8px 0;border-bottom:1px dashed #ccc;text-align:right;">₹${Number(i.totalPrice || 0).toFixed(2)}</td>
      </tr>`).join('');

    let metadataHtml = '';
    if (order.metadata && Object.keys(order.metadata).length > 0) {
        metadataHtml = `<div class="divider"></div><div class="order-meta">` + Object.entries(order.metadata).map(([k,v]) => `<div><strong>${this._escHtml(k)}:</strong> ${this._escHtml(v)}</div>`).join('') + `</div>`;
    }

    const date = order.completedAt ? new Date(order.completedAt).toLocaleString() : new Date().toLocaleString();

    return `<!DOCTYPE html><html><head><meta charset="UTF-8">
      <title>Invoice — ${this._escHtml(shopName)}</title>
      <style>
        body{font-family:'Courier New', Courier, monospace;max-width:350px;margin:0 auto;padding:20px;color:#000;font-size:14px;line-height:1.4}
        h2{text-align:center;margin:0 0 5px 0;font-size:22px;text-transform:uppercase}
        .info{text-align:center;margin-bottom:15px;font-size:12px}
        .divider{border-top:1px dashed #000;margin:10px 0}
        .order-meta{margin-bottom:15px;font-size:13px}
        table{width:100%;border-collapse:collapse;margin-bottom:15px;font-size:13px}
        th{text-align:left;border-bottom:1px solid #000;padding-bottom:5px}
        .totals-table{width:100%;font-size:13px}
        .totals-table td{padding:3px 0}
        .totals-table .bold{font-weight:bold}
        .totals-table .grand-total{font-size:18px;font-weight:bold;border-top:1px dashed #000;padding-top:8px;margin-top:5px}
        .footer{text-align:center;font-size:11px;margin-top:20px}
        .instruction{font-size:12px;margin-top:10px;font-style:italic;}
        @media print{body{max-width:100%;padding:0;} .no-print{display:none}}
      </style></head><body>
      <h2>${this._escHtml(shopName)}</h2>
      <div class="info">
        ${address ? address + '<br>' : ''}
        ${phone ? 'Ph: ' + phone + '<br>' : ''}
        ${email ? email + '<br>' : ''}
        ${gstNumber ? 'GSTIN: ' + this._escHtml(gstNumber) : ''}
      </div>
      
      <div class="divider"></div>
      
      <div class="order-meta">
        <div><strong>Order #:</strong> ${order.queueNumber}</div>
        <div><strong>Date:</strong> ${date}</div>
        <div><strong>Payment:</strong> UNPAID (Pay at Counter)</div>
      </div>
      
      ${metadataHtml}
      
      <div class="divider"></div>
      
      <table>
        <thead><tr>
          <th>Item</th><th style="text-align:center">Qty</th>
          <th style="text-align:right">Price</th><th style="text-align:right">Total</th>
        </tr></thead>
        <tbody>${items}</tbody>
      </table>
      
      <table class="totals-table">
        <tr>
            <td>Subtotal</td>
            <td style="text-align:right">₹${subtotal.toFixed(2)}</td>
        </tr>
        ${couponDiscount > 0 ? `<tr>
            <td>${this._escHtml(couponName)}</td>
            <td style="text-align:right">-₹${couponDiscount.toFixed(2)}</td>
        </tr>` : ''}
        ${taxValue > 0 ? `<tr>
            <td>${this._escHtml(taxName)} ${order.taxAmount == null && taxPct > 0 ? '('+taxPct+'%)' : ''}</td>
            <td style="text-align:right">₹${taxValue.toFixed(2)}</td>
        </tr>` : ''}
        ${chargeAmt > 0 ? `<tr>
            <td>${this._escHtml(chargeName)}</td>
            <td style="text-align:right">₹${chargeAmt.toFixed(2)}</td>
        </tr>` : ''}
        <tr>
            <td class="grand-total" style="padding-top:10px">Total</td>
            <td class="grand-total" style="text-align:right;padding-top:10px">₹${computedTotal.toFixed(2)}</td>
        </tr>
      </table>
      
      ${order.customerNote ? `<div class="instruction"><strong>Instruction:</strong> ${this._escHtml(order.customerNote)}</div>` : ''}
      
      <div class="divider"></div>
      
      <div class="footer">
        Thank you for visiting!<br>
        Powered by DeQueue
      </div>
      <script>window.onload=()=>window.print()<\/script>
    </body></html>`;
  }

  viewInvoice() {
    const order = this._completedOrder;
    if (!order) { if (window.showToast) showToast('No invoice available', 'error'); return; }
    const html = this.generateInvoiceHTML(order);
    const win = window.open('', '_blank');
    if (win) { win.document.write(html); win.document.close(); }
  }

  downloadInvoice() {
    const order = this._completedOrder;
    if (!order) { if (window.showToast) showToast('No invoice available', 'error'); return; }
    
    if (window.showToast) showToast('Generating PDF...', 'info');

    // Create a temporary container for html2pdf
    const tempDiv = document.createElement('div');
    
    // We strip the outer HTML/HEAD tags and print script from the generated HTML
    // so html2pdf can render just the body content.
    let contentHtml = this.generateInvoiceHTML(order);
    contentHtml = contentHtml.replace(/<script>.*?<\/script>/g, '');
    const bodyMatch = contentHtml.match(/<body[^>]*>([\s\S]*?)<\/body>/i);
    const styleMatch = contentHtml.match(/<style[^>]*>([\s\S]*?)<\/style>/i);
    
    tempDiv.innerHTML = (styleMatch ? `<style>${styleMatch[1]}</style>` : '') + 
                        `<div style="padding:20px;max-width:350px;margin:0 auto;background:#fff;">${bodyMatch ? bodyMatch[1] : contentHtml}</div>`;
    
    const opt = {
      margin:       10,
      filename:     `invoice-${order.queueNumber || 'order'}.pdf`,
      image:        { type: 'jpeg', quality: 0.98 },
      html2canvas:  { scale: 2 },
      jsPDF:        { unit: 'mm', format: 'a5', orientation: 'portrait' }
    };

    if (window.html2pdf) {
        html2pdf().set(opt).from(tempDiv).save().then(() => {
            if (window.showToast) showToast('PDF downloaded', 'success');
        }).catch(err => {
            console.error('PDF generation error:', err);
            if (window.showToast) showToast('Failed to generate PDF', 'error');
        });
    } else {
        // Fallback to HTML if library failed to load
        const blob = new Blob([this.generateInvoiceHTML(order)], { type: 'text/html' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `invoice-${order.queueNumber || 'order'}.html`;
        a.click();
        URL.revokeObjectURL(a.href);
    }
  }

  showClosed() {
    const menuView = document.getElementById('menu-view');
    if (menuView) {
      menuView.innerHTML = `
        <div class="flex flex-col items-center justify-center text-center py-16">
          <div class="inline-block p-4 rounded-full bg-surface mb-4">
            <i data-lucide="store" class="text-muted" style="width:48px;height:48px;"></i>
          </div>
          <h2 class="text-xl font-bold mb-2">Shop is Currently Closed</h2>
          <p class="text-muted">Please check back during business hours.</p>
        </div>
      `;
      if (typeof lucide !== 'undefined') lucide.createIcons();
    }
    const statusEl = document.querySelector('.shop-status');
    if (statusEl) {
      statusEl.className = 'shop-status closed';
      statusEl.textContent = 'Closed';
    }
  }

  showError(message) {
    const app = document.querySelector('.customer-app');
    if (app) {
      app.innerHTML = `
        <div class="flex flex-col items-center justify-center text-center min-h-screen p-8">
          <div class="inline-block p-4 rounded-full bg-surface mb-4">
            <i data-lucide="alert-circle" class="text-danger" style="width:48px;height:48px;"></i>
          </div>
          <h2 class="text-xl font-bold mb-2">Oops!</h2>
          <p class="text-muted">${message}</p>
        </div>
      `;
      if (typeof lucide !== 'undefined') lucide.createIcons();
    }
  }

  formatPrice(amount) {
    return `₹${Math.round(Number(amount) * 100) / 100}`;
  }

  _escHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.customerApp = new CustomerApp();
});
