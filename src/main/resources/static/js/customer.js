// Customer App Logic - DeQueue Customer Ordering System
class CustomerApp {
  constructor() {
    this.vendorCode = this.getVendorCodeFromUrl();
    const storedCart = sessionStorage.getItem(`dequeue_cart_${this.vendorCode}`);
    this.cart = storedCart ? JSON.parse(storedCart) : [];
    this.vendor = null;
    this.menu = null;
    this.currentCategory = 'popular';
    this.searchQuery = '';
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

      const params = new URLSearchParams(window.location.search);
      if (params.get('payment_success') === 'true') {
         const pendingStr = sessionStorage.getItem('dequeue_pending_checkout_' + this.vendorCode);
         if (pendingStr) {
             const pendingData = JSON.parse(pendingStr);
             window.history.replaceState({}, document.title, window.location.pathname + '?vendor=' + this.vendorCode);
             this.placeOrder(true, pendingData);
             return;
         }
      }

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
      this.renderHero();
      this.renderMenuItems('popular');
    } catch (err) {
      this.showError('Unable to load menu. Please try again.');
      console.error(err);
    }
  }

  renderHero() {
    const hero = document.getElementById('menu-hero');
    if (!hero || !this.vendor) return;
    const allItems = [];
    (this.menu?.categories || []).forEach(cat => (cat.items || []).forEach(item => { if (item.available !== false && item.visible !== false && item.image) allItems.push(item); }));
    
    const defaultImage = this.vendor.logo || '';
    
    this.heroSlides = [
      { title: 'Good food, good mood! 😊', sub: `Freshly made at ${this.vendor.shopName}`, image: allItems[0]?.image || defaultImage },
      { title: 'Taste the best! 🌟', sub: 'Handpicked ingredients for you', image: allItems[1]?.image || allItems[0]?.image || defaultImage },
      { title: 'Craving something delicious? 😋', sub: 'We have got you covered!', image: allItems[2]?.image || allItems[0]?.image || defaultImage }
    ];
    
    this.currentHeroIndex = 0;
    
    const updateHero = () => {
        const slide = this.heroSlides[this.currentHeroIndex];
        const image = slide.image;
        hero.style.backgroundImage = image ? `linear-gradient(90deg, rgba(255,248,238,.98) 0%, rgba(255,248,238,.84) 42%, rgba(255,248,238,.10) 100%), url("${image}")` : '';
        const title = hero.querySelector('[data-hero-title]'); const sub = hero.querySelector('[data-hero-subtitle]');
        if (title) title.textContent = slide.title;
        if (sub) sub.textContent = slide.sub;
        
        const dots = hero.querySelectorAll('.hero-dots span');
        dots.forEach((dot, index) => {
            if (index === this.currentHeroIndex) dot.classList.add('active');
            else dot.classList.remove('active');
        });
    };
    
    updateHero();
    
    if (this.heroInterval) clearInterval(this.heroInterval);
    this.heroInterval = setInterval(() => {
        this.currentHeroIndex = (this.currentHeroIndex + 1) % this.heroSlides.length;
        updateHero();
    }, 4000);
  }

  renderCategories() {
    const container = document.querySelector('.category-pills'); const sheet = document.getElementById('category-sheet-list'); const filterLabel = document.getElementById('category-filter-label');
    if (!container || !sheet || !this.menu || !this.menu.categories) return;
    const categories = [{ id: 'popular', name: 'Popular', icon: 'sparkles' }, ...this.menu.categories.map(cat => ({ id: cat.id, name: cat.name, icon: 'utensils' }))];
    container.innerHTML = '';
    sheet.innerHTML = categories.map(cat => `<button type="button" class="category-sheet-item ${cat.id === this.currentCategory ? 'active' : ''}" data-category="${this._escHtml(cat.id)}"><span class="category-sheet-icon"><i data-lucide="${cat.icon}"></i></span><span class="category-sheet-copy"><strong>${this._escHtml(cat.name)}</strong><small>${cat.id === 'popular' ? 'Customer favourites' : 'Browse this category'}</small></span><i data-lucide="check" class="category-sheet-check"></i></button>`).join('');
    if (filterLabel) { const selected = categories.find(c => c.id === this.currentCategory) || categories[0]; filterLabel.textContent = selected.name; }
    sheet.querySelectorAll('.category-sheet-item').forEach(btn => btn.addEventListener('click', () => { this.currentCategory = btn.dataset.category || 'popular'; this.renderCategories(); this.renderMenuItems(this.currentCategory); this.closeCategorySheet(); }));
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }
  renderMenuItems(categoryId = this.currentCategory || 'popular') {
    const grid = document.querySelector('.menu-grid'); if (!grid || !this.menu) return;
    this.currentCategory = categoryId || 'popular'; let items = [];
    (this.menu.categories || []).forEach(cat => (cat.items || []).forEach(item => { if (item.available !== false && item.visible !== false) items.push({ ...item, categoryId: cat.id, categoryName: cat.name }); }));
    if (this.currentCategory === 'popular') { items = items.filter(i => i.tags && i.tags.includes('Popular')); }
    else if (this.currentCategory !== 'all') items = items.filter(i => i.categoryId === this.currentCategory);
    const query = (this.searchQuery || '').trim().toLowerCase(); if (query) items = items.filter(i => String(i.name || '').toLowerCase().includes(query) || String(i.description || '').toLowerCase().includes(query) || String(i.categoryName || '').toLowerCase().includes(query));
    const title = document.getElementById('menu-section-title'); const subtitle = document.getElementById('menu-section-subtitle'); const selectedCategory = this.getCurrentCategoryName();
    if (title) title.innerHTML = `${this._escHtml(selectedCategory)} <span class="section-sparkle">✦</span>`;
    if (subtitle) subtitle.textContent = query ? `${items.length} result${items.length === 1 ? '' : 's'} for “${this._escHtml(query)}”` : (this.currentCategory === 'popular' ? 'Our most loved picks for you' : `Explore ${selectedCategory.toLowerCase()}`);
    if (!items.length) { grid.innerHTML = `<div class="empty-menu-state"><div class="empty-menu-icon"><i data-lucide="search-x"></i></div><h3>No items found</h3><p>Try another search or choose a different category.</p><button class="btn-soft" type="button" onclick="customerApp.clearMenuSearch()">Clear search</button></div>`; if (typeof lucide !== 'undefined') lucide.createIcons(); return; }
    grid.innerHTML = items.map((item, index) => {
      const qty = this.getMenuItemQuantity(item.id); const isBestSeller = item.tags && item.tags.includes('Best Seller'); const simple = !item.customizationGroups || item.customizationGroups.length === 0;
      const media = item.image ? `<img src="${this._escHtml(item.image)}" alt="${this._escHtml(item.name)}" class="item-image" loading="lazy">` : `<div class="item-image-placeholder"><i data-lucide="utensils"></i></div>`;
      const controls = simple && qty > 0 ? `<div class="menu-quantity-control" onclick="event.stopPropagation()"><button type="button" onclick="customerApp.changeMenuItemQuantity('${this._escHtml(item.id)}', -1)"><i data-lucide="${qty === 1 ? 'trash-2' : 'minus'}"></i></button><strong>${qty}</strong><button type="button" onclick="customerApp.changeMenuItemQuantity('${this._escHtml(item.id)}', 1)"><i data-lucide="plus"></i></button></div>` : `<button class="menu-add-btn" type="button" onclick="event.stopPropagation(); customerApp.handleAddToCart('${this._escHtml(item.id)}')"><i data-lucide="plus"></i><span>${simple ? 'Add' : 'Customize'}</span></button>`;
      return `<article class="menu-item-card ${index === 0 && this.currentCategory === 'popular' ? 'featured-card' : ''}" data-item-id="${this._escHtml(item.id)}"><div class="item-media-wrap">${media}${isBestSeller ? `<span class="bestseller-badge"><i data-lucide="star"></i> Bestseller</span>` : ''}<button class="favorite-btn" type="button" onclick="event.stopPropagation(); customerApp.toggleFavorite('${this._escHtml(item.id)}', this)"><i data-lucide="heart"></i></button></div><div class="item-info"><div class="item-category-label">${this._escHtml(item.categoryName || '')}</div><h3 class="item-title">${this._escHtml(item.name)}</h3><p class="item-desc">${this._escHtml(item.description || 'Freshly prepared for you.')}</p><div class="item-meta-row"><div><div class="item-price">${this.formatPrice(item.price)}</div>${item.preparationTime ? `<div class="prep-time"><i data-lucide="clock"></i> ${this._escHtml(item.preparationTime)} min</div>` : ''}</div>${controls}</div></div></article>`;
    }).join('');
    this.syncFavoriteButtons(); if (typeof lucide !== 'undefined') lucide.createIcons();
  }
  getCurrentCategoryName() { if (this.currentCategory === 'popular') return 'Popular'; const cat = this.menu?.categories?.find(c => c.id === this.currentCategory); return cat?.name || 'Popular'; }
  getMenuItemQuantity(itemId) { return this.cart.filter(item => item.menuItemId === itemId).reduce((sum, item) => sum + item.quantity, 0); }
  changeMenuItemQuantity(itemId, delta) { if (delta > 0) return this.handleAddToCart(itemId); const matches = this.cart.filter(item => item.menuItemId === itemId).sort((a,b)=>b.cartId-a.cartId); if (matches.length) this.updateQuantity(matches[0].cartId, -1); }
  clearMenuSearch() { this.searchQuery=''; const input=document.getElementById('menu-search'); const clear=document.getElementById('clear-search-btn'); if(input) input.value=''; if(clear) clear.classList.add('hidden'); this.renderMenuItems(this.currentCategory); }
  filterMenu(query) { this.searchQuery=query||''; const clear=document.getElementById('clear-search-btn'); if(clear) clear.classList.toggle('hidden', !this.searchQuery); this.renderMenuItems(this.currentCategory); }
  toggleFavorite(itemId, button) { const key=`dequeue_favorites_${this.vendorCode}`; let favorites=[]; try{favorites=JSON.parse(localStorage.getItem(key)||'[]')}catch(e){} const exists=favorites.includes(itemId); favorites=exists?favorites.filter(id=>id!==itemId):[...favorites,itemId]; localStorage.setItem(key,JSON.stringify(favorites)); if(button) button.classList.toggle('is-favorite',!exists); if(typeof lucide!=='undefined') lucide.createIcons(); }
  syncFavoriteButtons() { let favorites=[]; try{favorites=JSON.parse(localStorage.getItem(`dequeue_favorites_${this.vendorCode}`)||'[]')}catch(e){} document.querySelectorAll('.favorite-btn').forEach(btn=>{const card=btn.closest('.menu-item-card'); if(card) btn.classList.toggle('is-favorite',favorites.includes(card.dataset.itemId));}); }
  setMenuChromeVisible(visible) {
    const fab = document.getElementById('category-filter-fab');
    const sheet = document.getElementById('category-sheet-overlay');
    const menuView = document.getElementById('menu-view');
    const shouldShow = Boolean(visible && menuView && !menuView.classList.contains('hidden'));
    if (fab) fab.classList.toggle('is-hidden', !shouldShow);
    if (!shouldShow && sheet) {
      sheet.classList.remove('is-open');
      document.body.classList.remove('sheet-open');
    }
  }

  openCategorySheet() {
    const menuView = document.getElementById('menu-view');
    if (!menuView || menuView.classList.contains('hidden')) return;
    document.getElementById('category-sheet-overlay')?.classList.add('is-open');
    document.body.classList.add('sheet-open');
  }
  closeCategorySheet() {
    document.getElementById('category-sheet-overlay')?.classList.remove('is-open');
    document.body.classList.remove('sheet-open');
  }

  filterByCategory(categoryId) { this.currentCategory=categoryId||'popular'; this.renderCategories(); this.renderMenuItems(this.currentCategory); }

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
    const title=document.getElementById('cust-modal-title'), body=document.getElementById('cust-modal-body'), addBtn=document.getElementById('cust-modal-add-btn'); if(!title||!body||!addBtn)return;
    title.innerText=`Customize ${item.name}`; let html=`<div class="customize-item-intro">${item.image?`<img src="${this._escHtml(item.image)}" alt="${this._escHtml(item.name)}">`:`<div class="customize-placeholder"><i data-lucide="utensils"></i></div>`}<div><span class="section-kicker">YOUR CHOICE</span><h4>${this._escHtml(item.name)}</h4><strong>${this.formatPrice(item.price)}</strong></div></div>`;
    (item.customizationGroups||[]).forEach(group=>{const required=group.required?'<span class="required-dot">Required</span>':'<span class="optional-dot">Optional</span>'; html+=`<section class="customize-group"><div class="customize-group-head"><div><h4>${this._escHtml(group.name)}</h4><small>${group.selectionType==='SINGLE'||group.maxSelection===1?'Choose one':'Choose any'}</small></div>${required}</div><div class="customize-options">`; (group.options||[]).forEach((opt,oIdx)=>{const inputType=group.selectionType==='SINGLE'||group.maxSelection===1?'radio':'checkbox', inputName=`cust_${group.id}`, inputId=`cust_${group.id}_${oIdx}`, price=Number(opt.additionalPrice||0); html+=`<label class="customize-option" for="${inputId}"><span class="customize-option-left"><input type="${inputType}" name="${inputName}" id="${inputId}" value="${this._escHtml(opt.name)}" data-price="${price}" data-group-name="${this._escHtml(group.name)}"><span class="custom-radio-check"></span><span>${this._escHtml(opt.name)}</span></span><strong>${price>0?`+${this.formatPrice(price)}`:'Included'}</strong></label>`;}); html+=`</div></section>`;});
    body.innerHTML=html; const updateButton=()=>{let extra=0;body.querySelectorAll('input[type="radio"]:checked,input[type="checkbox"]:checked').forEach(i=>extra+=Number(i.dataset.price||0));const priceEl=addBtn.querySelector('strong');if(priceEl)priceEl.textContent=this.formatPrice(item.price+extra);}; body.querySelectorAll('input').forEach(i=>i.addEventListener('change',updateButton)); updateButton();
    addBtn.onclick=()=>{const customizations=[];let missingRequired=false;(item.customizationGroups||[]).forEach(group=>{const inputs=body.querySelectorAll(`input[name="cust_${group.id}"]:checked`);if(group.required&&inputs.length===0)missingRequired=true;inputs.forEach(input=>customizations.push({optionName:input.value,additionalPrice:parseFloat(input.dataset.price||0),groupName:input.dataset.groupName,groupId:group.id}));});if(missingRequired){if(window.showToast)showToast('Please select all required options','error');return;}this.addToCart(item,customizations);if(window.closeModal)closeModal('cust-modal');}; if(window.openModal)openModal('cust-modal'); if(typeof lucide!=='undefined')lucide.createIcons();
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
    this.saveCart();const cartBtn=document.getElementById('floating-cart');if(!cartBtn)return;const totalItems=this.cart.reduce((sum,item)=>sum+item.quantity,0),subtotal=this.cart.reduce((sum,item)=>sum+item.unitPrice*item.quantity,0),settings=this.vendor?.settings||{};let finalTotal=subtotal;if(settings.taxPercentage&&settings.taxPercentage>0)finalTotal+=subtotal*settings.taxPercentage/100;if(settings.additionalCharges&&settings.additionalCharges>0)finalTotal+=settings.additionalCharges;if(this.appliedCoupon)finalTotal-=this.appliedCoupon.type==='PERCENTAGE'?subtotal*this.appliedCoupon.value/100:this.appliedCoupon.value;if(finalTotal<0)finalTotal=0;if(totalItems>0){const count=document.getElementById('cart-count'),total=document.getElementById('cart-total'),badge=document.getElementById('cart-badge');if(count)count.innerHTML=`<strong>${totalItems}</strong> item${totalItems>1?'s':''}`;if(total)total.textContent=this.formatPrice(finalTotal);if(badge)badge.textContent=totalItems;cartBtn.classList.add('visible');}else cartBtn.classList.remove('visible');
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
    const body=document.querySelector('#cart-modal-overlay .modal-body'); if(!body)return;
    if(!this.cart.length){body.innerHTML=`<div class="empty-cart-state"><div class="empty-cart-icon"><i data-lucide="shopping-cart"></i></div><h3>Your cart is waiting</h3><p>Add something delicious and it will appear here.</p><button class="btn-primary-lg" onclick="closeModal('cart-modal')">Browse Menu</button></div>`;if(typeof lucide!=='undefined')lucide.createIcons();return;}
    const total=this.cart.reduce((s,i)=>s+i.unitPrice*i.quantity,0), settings=this.vendor?.settings||{}, taxPct=Number(settings.taxPercentage||0), taxName=settings.taxName||'Tax', chargeAmt=Number(settings.additionalCharges||0), chargeName=settings.additionalChargeName||'Service Charge', taxAmount=taxPct>0?total*taxPct/100:0;
    let couponDiscount=0;if(this.appliedCoupon)couponDiscount=this.appliedCoupon.type==='PERCENTAGE'?total*this.appliedCoupon.value/100:this.appliedCoupon.value;couponDiscount=Math.min(couponDiscount,total);const finalTotal=Math.max(0,total-couponDiscount+taxAmount+chargeAmt);
    this._currentCheckout={subtotal:total,taxAmount,taxName:taxPct>0?taxName:null,serviceChargeAmount:chargeAmt>0?chargeAmt:null,serviceChargeName:chargeAmt>0?chargeName:null,couponCode:this.appliedCoupon?this.appliedCoupon.code:null,couponDiscount:couponDiscount>0?couponDiscount:null,finalTotal};
    let cfHtml='';
    const enabledFields = (this.vendor?.settings?.customFields||[]).filter(cf => cf.enabled !== false);
    // Sort fields by displayOrder
    enabledFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    
    enabledFields.forEach((cf) => {
        const idStr = this._escHtml(cf.id);
        const labelStr = this._escHtml(cf.label);
        const isRequired = cf.required;
        const type = cf.type || 'text';
        const conditionsData = cf.conditions ? JSON.stringify(cf.conditions).replace(/"/g, '&quot;') : '[]';
        
        cfHtml += `<div class="checkout-field cf-container" id="cf-container-${idStr}" data-id="${idStr}" data-required="${isRequired}" data-conditions="${conditionsData}">`;
        cfHtml += `<label>${labelStr} ${isRequired ? '<span>*</span>' : ''}</label>`;
        
        const onchangeAttr = `onchange="customerApp.evaluateCustomFields()" oninput="customerApp.evaluateCustomFields()"`;
        
        if (type === 'text') {
            cfHtml += `<input type="text" id="cf-${idStr}" name="cf-${idStr}" class="checkout-input cf-input" placeholder="Enter ${labelStr}" ${onchangeAttr}>`;
        } else if (type === 'number') {
            cfHtml += `<input type="number" id="cf-${idStr}" name="cf-${idStr}" class="checkout-input cf-input" placeholder="Enter ${labelStr}" ${onchangeAttr}>`;
        } else if (type === 'dropdown') {
            cfHtml += `<select id="cf-${idStr}" name="cf-${idStr}" class="checkout-input cf-input" ${onchangeAttr}><option value="">Select ${labelStr}</option>`;
            (cf.options || []).forEach(o => {
                cfHtml += `<option value="${this._escHtml(o.value)}">${this._escHtml(o.label)}</option>`;
            });
            cfHtml += `</select>`;
        } else if (type === 'radio') {
            cfHtml += `<div class="checkout-choice-grid cf-input" data-type="radio" id="cf-${idStr}">`;
            (cf.options || []).forEach(o => {
                cfHtml += `<label class="choice-check"><input type="radio" name="cf-${idStr}" value="${this._escHtml(o.value)}" ${onchangeAttr}><span>${this._escHtml(o.label)}</span></label>`;
            });
            cfHtml += `</div>`;
        } else if (type === 'checkbox') {
            cfHtml += `<div class="checkout-choice-grid cf-input" data-type="checkbox" id="cf-${idStr}">`;
            (cf.options || []).forEach(o => {
                cfHtml += `<label class="choice-check"><input type="checkbox" name="cf-${idStr}" value="${this._escHtml(o.value)}" ${onchangeAttr}><span>${this._escHtml(o.label)}</span></label>`;
            });
            cfHtml += `</div>`;
        }
        cfHtml += `</div>`;
    });
    const paymentOnline=!!settings.enableOnlinePayment,paymentValue=document.getElementById('pay-method-select')?.value||'OFFLINE',onlineSelected=paymentValue==='ONLINE';
    const couponSection=settings.coupons?.length?`<div class="checkout-section coupon-section"><div class="checkout-section-heading"><div><span class="section-kicker">SAVE MORE</span><h4>Have a coupon?</h4></div><i data-lucide="ticket-percent"></i></div><div class="coupon-row"><input type="text" id="cart-coupon-input" class="checkout-input" placeholder="Enter coupon code" value="${this.appliedCoupon?this._escHtml(this.appliedCoupon.code):''}" ${this.appliedCoupon?'disabled':''}>${this.appliedCoupon?`<button class="coupon-action remove" onclick="customerApp.removeCoupon()">Remove</button>`:`<button class="coupon-action" onclick="customerApp.applyCoupon()">Apply</button>`}</div>${this.appliedCoupon?`<div class="coupon-success"><i data-lucide="check-circle-2"></i> ${this._escHtml(this.appliedCoupon.code)} applied — you saved ${this.formatPrice(couponDiscount)}</div>`:''}</div>`:'';
    body.innerHTML=`<div class="checkout-wrap"><section class="checkout-section cart-items-section"><div class="checkout-section-heading"><div><span class="section-kicker">YOUR ORDER</span><h4>${this.cart.reduce((s,i)=>s+i.quantity,0)} items</h4></div><span class="mini-total">${this.formatPrice(total)}</span></div><div class="checkout-items">${this.cart.map(item=>{const thumb=item.image?`<img src="${this._escHtml(item.image)}" alt="${this._escHtml(item.menuItemName)}">`:`<div class="cart-thumb-placeholder"><i data-lucide="utensils"></i></div>`;const cust=item.customizations?.length?`<div class="cart-custom-tags">${item.customizations.map(c=>`<span>${this._escHtml(c.optionName||c.groupName)}</span>`).join('')}</div>`:'';return `<div class="checkout-item"><div class="checkout-item-thumb">${thumb}</div><div class="checkout-item-main"><h5>${this._escHtml(item.menuItemName)}</h5><div class="checkout-item-unit">${this.formatPrice(item.unitPrice)} each</div>${cust}</div><div class="checkout-item-side"><strong>${this.formatPrice(item.unitPrice*item.quantity)}</strong><div class="checkout-qty"><button onclick="customerApp.updateQuantity(${item.cartId},-1)"><i data-lucide="${item.quantity===1?'trash-2':'minus'}"></i></button><span>${item.quantity}</span><button onclick="customerApp.updateQuantity(${item.cartId},1)"><i data-lucide="plus"></i></button></div></div></div>`}).join('')}</div></section>${couponSection}${cfHtml?`<section class="checkout-section"><div class="checkout-section-heading"><div><span class="section-kicker">ORDER DETAILS</span><h4>Almost there</h4></div><i data-lucide="clipboard-list"></i></div>${cfHtml}</section>`:''}<section class="checkout-section"><div class="checkout-section-heading"><div><span class="section-kicker">NOTE</span><h4>Anything we should know?</h4></div><i data-lucide="message-square-text"></i></div><textarea id="customer-note" class="checkout-textarea" placeholder="Less spicy, extra sauce, no onions..." rows="3"></textarea></section><section class="checkout-section bill-section"><div class="checkout-section-heading"><div><span class="section-kicker">BILL DETAILS</span><h4>Summary</h4></div><i data-lucide="receipt"></i></div><div class="bill-lines"><div><span>Item total</span><strong>${this.formatPrice(total)}</strong></div>${couponDiscount>0?`<div class="discount-line"><span>Coupon (${this._escHtml(this.appliedCoupon.code)})</span><strong>−${this.formatPrice(couponDiscount)}</strong></div>`:''}${taxAmount>0?`<div><span>${this._escHtml(taxName)} <small>${taxPct}%</small></span><strong>${this.formatPrice(taxAmount)}</strong></div>`:''}${chargeAmt>0?`<div><span>${this._escHtml(chargeName)}</span><strong>${this.formatPrice(chargeAmt)}</strong></div>`:''}</div><div class="bill-total"><span>Total to pay</span><strong>${this.formatPrice(finalTotal)}</strong></div><div class="bill-note"><i data-lucide="shield-check"></i> Final amount includes applicable taxes and charges.</div></section><section class="checkout-section payment-section"><div class="checkout-section-heading"><div><span class="section-kicker">PAYMENT</span><h4>How would you like to pay?</h4></div><i data-lucide="wallet-cards"></i></div><div class="payment-options"><label class="payment-card ${!onlineSelected?'selected':''}"><input type="radio" name="payment-ui" value="OFFLINE" ${!onlineSelected?'checked':''} onchange="customerApp._onPayMethodChange('OFFLINE')"><span class="payment-icon counter"><i data-lucide="store"></i></span><span class="payment-copy"><strong>Pay at Counter</strong><small>Pay when collecting your order</small></span><span class="payment-check"><i data-lucide="check"></i></span></label>${paymentOnline?`<label class="payment-card ${onlineSelected?'selected':''}"><input type="radio" name="payment-ui" value="ONLINE" ${onlineSelected?'checked':''} onchange="customerApp._onPayMethodChange('ONLINE')"><span class="payment-icon online"><i data-lucide="smartphone"></i></span><span class="payment-copy"><strong>Pay Online</strong><small>UPI, cards & net banking via Cashfree</small></span><span class="payment-check"><i data-lucide="check"></i></span></label>`:''}</div><select id="pay-method-select" class="sr-only-payment-select" aria-hidden="true"><option value="OFFLINE" ${!onlineSelected?'selected':''}>OFFLINE</option>${paymentOnline?`<option value="ONLINE" ${onlineSelected?'selected':''}>ONLINE</option>`:''}</select><div id="cash-payment-info" class="payment-info ${onlineSelected?'hidden':''}"><i data-lucide="wallet"></i><div><strong>Pay ${this.formatPrice(finalTotal)} at the counter</strong><span>Show your order number when collecting.</span></div></div><div id="upi-qr-panel" class="payment-info online-info ${onlineSelected?'':'hidden'}"><i data-lucide="lock-keyhole"></i><div><strong>Secure online payment</strong><span>You’ll be redirected to Cashfree to complete payment.</span></div></div></section></div><div class="checkout-bottom-spacer"></div>`;
    if(typeof lucide!=='undefined')lucide.createIcons();this.syncPaymentCards();this.evaluateCustomFields();const checkoutTotal=document.getElementById('checkout-button-total');if(checkoutTotal)checkoutTotal.textContent=this.formatPrice(finalTotal);const checkoutLabel=document.querySelector('.checkout-btn-label');if(checkoutLabel)checkoutLabel.textContent=onlineSelected?'Continue to Payment':'Place Order';
  }
  _onPayMethodChange(value) {
    const select=document.getElementById('pay-method-select');if(select)select.value=value;const qrPanel=document.getElementById('upi-qr-panel');const cashInfo=document.getElementById('cash-payment-info');if(qrPanel)qrPanel.classList.toggle('hidden',value!=='ONLINE');if(cashInfo)cashInfo.classList.toggle('hidden',value!=='OFFLINE');this.syncPaymentCards();const label=document.querySelector('.checkout-btn-label');if(label)label.textContent=value==='ONLINE'?'Continue to Payment':'Place Order';const total=document.getElementById('checkout-button-total');if(total&&this._currentCheckout)total.textContent=this.formatPrice(this._currentCheckout.finalTotal||0);
  }

  syncPaymentCards() { const selected=document.getElementById('pay-method-select')?.value||'OFFLINE';document.querySelectorAll('.payment-card').forEach(card=>{const radio=card.querySelector('input[type="radio"]');card.classList.toggle('selected',radio?.value===selected);}); }

  evaluateCustomFields() {
      // Gather current values
      const currentValues = {};
      document.querySelectorAll('.cf-container').forEach(container => {
          const id = container.getAttribute('data-id');
          const inputs = container.querySelectorAll('.cf-input');
          if (!inputs.length) return;
          const inputEl = inputs[0];
          
          if (inputEl.tagName === 'SELECT' || inputEl.tagName === 'INPUT') {
              currentValues[id] = inputEl.value;
          } else if (inputEl.classList.contains('checkout-choice-grid')) {
              const type = inputEl.getAttribute('data-type');
              if (type === 'radio') {
                  const checked = container.querySelector(`input[name="cf-${id}"]:checked`);
                  currentValues[id] = checked ? checked.value : '';
              } else if (type === 'checkbox') {
                  const checked = Array.from(container.querySelectorAll(`input[name="cf-${id}"]:checked`)).map(el => el.value);
                  currentValues[id] = checked.join(', ');
              }
          }
      });

      // Evaluate visibility
      document.querySelectorAll('.cf-container').forEach(container => {
          const conditionsStr = container.getAttribute('data-conditions');
          if (!conditionsStr || conditionsStr === '[]') return; // no conditions
          
          try {
              const conditions = JSON.parse(conditionsStr);
              let isVisible = true;
              
              for (const cond of conditions) {
                  const actualVal = currentValues[cond.fieldId] || '';
                  const expectedVal = cond.value || '';
                  
                  if (cond.operator === 'equals') {
                      if (actualVal !== expectedVal) isVisible = false;
                  } else if (cond.operator === 'not_equals') {
                      if (actualVal === expectedVal) isVisible = false;
                  }
              }
              
              if (isVisible) {
                  container.style.display = 'block';
              } else {
                  container.style.display = 'none';
                  // Clear values if hidden
                  const inputs = container.querySelectorAll('.cf-input');
                  if (inputs.length) {
                      const inputEl = inputs[0];
                      if (inputEl.tagName === 'SELECT' || (inputEl.tagName === 'INPUT' && (inputEl.type === 'text' || inputEl.type === 'number'))) {
                          inputEl.value = '';
                      } else if (inputEl.classList.contains('checkout-choice-grid')) {
                          container.querySelectorAll(`input[name="cf-${container.getAttribute('data-id')}"]`).forEach(el => el.checked = false);
                      }
                  }
              }
          } catch (e) {
              console.error('Failed to parse conditions', e);
          }
      });
  }

  async placeOrder(isFromPayment = false, pendingData = null) {
    if (this.cart.length === 0) return;

    let paymentSource = 'CASH'; // Default payment source
    let note = '';
    let metadata = {};
    let customFields = {};

    if (isFromPayment && pendingData) {
        paymentSource = 'CASHFREE'; // Online payment confirmed
        note = pendingData.note;
        metadata = pendingData.metadata;
        customFields = pendingData.customFields || {};
        this._currentCheckout = pendingData.checkout;
    } else {
        const paySelect = document.getElementById('pay-method-select');
        const selectedMethod = paySelect ? paySelect.value : 'OFFLINE';
        
        // Custom Fields Extraction
        let hasError = false;
        // Evaluate one last time before extracting
        this.evaluateCustomFields();
        
        document.querySelectorAll('.cf-container').forEach(container => {
            if (container.style.display === 'none') return;
            
            const id = container.getAttribute('data-id');
            const isRequired = container.getAttribute('data-required') === 'true';
            const inputs = container.querySelectorAll('.cf-input');
            if (!inputs.length) return;
            const inputEl = inputs[0];
            let val = '';
            
            if (inputEl.tagName === 'SELECT' || (inputEl.tagName === 'INPUT' && (inputEl.type === 'text' || inputEl.type === 'number'))) {
                val = inputEl.value;
            } else if (inputEl.classList.contains('checkout-choice-grid')) {
                const type = inputEl.getAttribute('data-type');
                if (type === 'radio') {
                    const checked = container.querySelector(`input[name="cf-${id}"]:checked`);
                    val = checked ? checked.value : '';
                } else if (type === 'checkbox') {
                    const checked = Array.from(container.querySelectorAll(`input[name="cf-${id}"]:checked`)).map(el => el.value);
                    val = checked.join(', ');
                }
            }
            
            if (isRequired && (!val || val.trim() === '')) {
                const label = container.querySelector('label').innerText.replace('*', '').trim();
                if (typeof showToast === 'function') showToast(`Please provide ${label}`, 'error');
                hasError = true;
                return;
            }
            
            if (val && val.trim() !== '') {
                customFields[id] = val;
            }
        });
        
        if (hasError) return;
        if (selectedMethod === 'ONLINE') {
            paymentSource = 'CASHFREE';
        } else {
            paymentSource = 'CASH'; // For counter payment
        }

        const placeBtn = document.getElementById('place-order-btn');
        if (placeBtn) {
            if (placeBtn.disabled) return;
            placeBtn.disabled = true;
            placeBtn.innerHTML = '<i data-lucide="loader-2" style="width:18px;height:18px;display:inline;margin-right:0.4rem;animation:spin 1s linear infinite;"></i> Placing Order...';
            if (typeof lucide !== 'undefined') lucide.createIcons();
        }

        if (this.vendor && this.vendor.settings && this.vendor.settings.enableGeofence) {
            const allowed = await this.validateGeofence();
            if (!allowed) {
                if (placeBtn) { placeBtn.disabled = false; placeBtn.innerHTML = '<i data-lucide="check-circle" style="width:18px;height:18px;display:inline;margin-right:0.4rem;"></i> Place Order'; if (typeof lucide !== 'undefined') lucide.createIcons(); }
                return;
            }
        }

        note = document.getElementById('customer-note')?.value || '';

        // Old custom field logic removed here

        // Store payment source in metadata for order tracking
        metadata['paymentSource'] = paymentSource;
        if (this._currentCheckout) {
            this._currentCheckout.paymentSource = paymentSource;
        }

        // Navigate to Cashfree simulated payment process for online payment
        if (paymentSource === 'CASHFREE') {
            const amount = (this._currentCheckout?.finalTotal || this.cart.reduce((s, c) => s + c.totalPrice, 0)).toFixed(2);
            sessionStorage.setItem('dequeue_pending_checkout_' + this.vendorCode, JSON.stringify({
                metadata,
                customFields, // <--- Add customFields
                checkout: this._currentCheckout,
                note: note
            }));
            window.location.href = `/cashfree.html?amount=${amount}&vendorCode=${this.vendorCode}`;
            return;
        }
    }

    const orderData = {
      sessionId: this.sessionId,
      paymentSource: paymentSource, // Explicitly set payment source
      metadata: metadata,
      customFields: customFields, // <--- Add customFields
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
        const orderResult = data.data;
        this.notificationManager.init(
          orderResult.id,
          orderResult.sessionId,
          orderResult.customerSessionToken,
          orderResult.queueNumber
        );

        // Save token data for reconnection
        localStorage.setItem(`dequeue_token_${this.vendorCode}`, JSON.stringify({
          orderId: orderResult.id,
          sessionId: orderResult.sessionId,
          customerToken: orderResult.customerSessionToken
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
        if (data.data.status === 'COMPLETED' || data.data.status === 'CANCELLED') {
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
              document.getElementById('thank-you-message').textContent = 'Your order has been successfully completed. Please visit us again!';
              document.getElementById('thank-you-icon').setAttribute('data-lucide', 'heart');
              document.getElementById('thank-you-icon').setAttribute('class', 'text-danger');
              // Save completed order for feedback
              this.completedOrder = data.data;
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
    this.setMenuChromeVisible(false);

    if (this.activeOrder) {
      const qnEl = document.getElementById('queue-number');
      if (qnEl) qnEl.textContent = this.activeOrder.queueNumber || 'N/A';
      const statusOrder = document.getElementById('status-order-number');
      if (statusOrder) statusOrder.textContent = this.activeOrder.queueNumber || 'N/A';

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
        this.setMenuChromeVisible(false);
        
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
          body: `Your order #${this.activeOrder?.queueNumber || ''} is ready for collection.`
        },
        PREPARING: {
          title: 'Order Preparing 👨‍🍳',
          body: `Your order #${this.activeOrder?.queueNumber || ''} is now being prepared.`
        },
        CANCELLED: {
          title: 'Order Cancelled',
          body: `Your order #${this.activeOrder?.queueNumber || ''} has been cancelled.`
        },
        COLLECTED: {
          title: 'Order Collected',
          body: 'Thank you! Your order has been collected.'
        }
      };
    
      const notification = notifications[status];
    
      if (!notification) return;
    
      try {
        const options = {
          body: notification.body,
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

  setRating(val) {
      this.currentRating = val;
      const stars = document.querySelectorAll('#star-rating .star-button');
      const labels = { 1: 'Not great', 2: 'Could be better', 3: 'It was good', 4: 'Really liked it', 5: 'Loved it! ❤️' };
      stars.forEach((button, index) => {
          const selected = index < val;
          button.classList.toggle('selected', selected);
          button.setAttribute('aria-checked', selected && index === val - 1 ? 'true' : 'false');
      });
      const ratingLabel = document.getElementById('rating-label');
      if (ratingLabel) ratingLabel.textContent = `${val}/5 · ${labels[val] || 'Thank you!'}`;
      if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  async submitFeedback() {
      const text = document.getElementById('feedback-text')?.value || '';
      const rating = this.currentRating || 0;
      
      if (!rating && !text) {
          if (typeof showToast === 'function') showToast('Please provide a rating or some text.', 'warning');
          return;
      }
      
      const feedbackOrder = this.completedOrder || this._completedOrder;
      if (feedbackOrder && feedbackOrder.queueNumber) {
          try {
              const res = await fetch(`/api/v1/public/orders/${this.vendorCode}/feedback/${feedbackOrder.queueNumber}`, {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ rating: rating, feedback: text })
              });
              const data = await res.json();
              if (data.success) {
                  if (typeof showToast === 'function') showToast('Thank you for your feedback!', 'success');
              } else {
                  if (typeof showToast === 'function') showToast('Feedback submitted.', 'info');
              }
          } catch(e) {
              console.error('Failed to submit feedback', e);
          }
      } else {
          if (typeof showToast === 'function') showToast('Thank you for your feedback!', 'success');
      }
      
      if (document.getElementById('feedback-text')) {
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
      this.setMenuChromeVisible(true);
      this.closeCategorySheet();
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
    const shortStatus = document.getElementById('order-status-short');
    if (shortStatus) shortStatus.textContent = labels[status] || status;

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
