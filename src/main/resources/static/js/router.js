// Simple router/navigation utility
class Router {
    constructor() {
        this.currentPath = window.location.pathname;
        this.navConfig = [
            { path: 'dashboard.html', icon: 'layout-dashboard', label: 'Dashboard', roles: ['ADMIN', 'MANAGER', 'COUNTER_STAFF'] },
            { path: 'orders.html', icon: 'list-ordered', label: 'Orders', roles: ['ADMIN', 'MANAGER', 'KITCHEN_STAFF', 'COUNTER_STAFF'] },
            { path: 'menu.html', icon: 'utensils', label: 'Menu', roles: ['ADMIN', 'MANAGER'] },
            { path: 'categories.html', icon: 'tags', label: 'Categories', roles: ['ADMIN', 'MANAGER'] },
            { path: 'customizations.html', icon: 'settings-2', label: 'Customizations', roles: ['ADMIN', 'MANAGER'] },
            { path: 'departments.html', icon: 'briefcase', label: 'Departments', roles: ['ADMIN', 'MANAGER'] },
            { path: 'staff.html', icon: 'users', label: 'Staff', roles: ['ADMIN', 'MANAGER'] },
            { path: 'roles.html', icon: 'shield', label: 'Roles', roles: ['ADMIN', 'MANAGER'] },
            { path: 'reports.html', icon: 'bar-chart-3', label: 'Reports', roles: ['ADMIN', 'MANAGER'] },
            { path: 'qr.html', icon: 'qr-code', label: 'QR Code', roles: ['ADMIN', 'MANAGER', 'COUNTER_STAFF'] },
            { path: 'profile.html', icon: 'store', label: 'Shop Profile', roles: ['ADMIN', 'MANAGER'] },
            { path: 'settings.html', icon: 'settings', label: 'Settings', roles: ['ADMIN', 'MANAGER'] },
//            { path: 'customer.html', icon: 'smartphone', label: 'Customer View', roles: ['ADMIN', 'MANAGER', 'COUNTER_STAFF'] },
            { path: 'vendors.html', icon: 'building', label: 'Platform Vendors', roles: [], platformAdminOnly: true }
        ];
        this.init();
    }

    init() {
        const run = () => {
            this.renderSidebar();
            this.enforceAccess();
        };

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', run);
        } else {
            run();
        }
    }

    renderSidebar() {
        const navContainer = document.querySelector('.sidebar-nav');
        if (!navContainer) return;

        const userStr = localStorage.getItem('user');
        const user = userStr ? JSON.parse(userStr) : null;
        
        console.log('[ROUTER] renderSidebar called');
        console.log('[ROUTER] user:', user);
        
        // Fix role checking: user.roleNames is an array like ['Vendor Admin', 'Kitchen Staff']
        let isPlatformAdmin = false;
        let hasAdminRole = false;
        let hasManagerRole = false;
        let hasKitchenRole = false;
        let hasCounterRole = false;

        if (user) {
            isPlatformAdmin = user.platformAdmin === true;
            console.log('[ROUTER] user.platformAdmin:', user.platformAdmin);
            console.log('[ROUTER] isPlatformAdmin:', isPlatformAdmin);
            if (user.roleNames && Array.isArray(user.roleNames)) {
                hasAdminRole = user.roleNames.includes('Vendor Admin');
                hasManagerRole = user.roleNames.includes('Manager');
                hasKitchenRole = user.roleNames.includes('Kitchen Staff');
                hasCounterRole = user.roleNames.includes('Counter Staff');
                
                // Fallback: if backend doesn't provide roles but user is authenticated, default to Admin
                if (user.roleNames.length === 0) {
                    hasAdminRole = true;
                }
            } else {
                hasAdminRole = true;
            }
        } else {
            // Fallback for safety
            hasAdminRole = true; 
        }

        let html = '';
        this.navConfig.forEach(item => {
            let hasAccess = false;
            
            if (isPlatformAdmin) {
                hasAccess = true;
            } else if (!item.platformAdminOnly) {
                // Check if user has any role required by this item
                if (item.roles.includes('ADMIN') && hasAdminRole) hasAccess = true;
                if (item.roles.includes('MANAGER') && hasManagerRole) hasAccess = true;
                if (item.roles.includes('KITCHEN_STAFF') && hasKitchenRole) hasAccess = true;
                if (item.roles.includes('COUNTER_STAFF') && hasCounterRole) hasAccess = true;
            }

            console.log(`[ROUTER] ${item.label}: hasAccess=${hasAccess}, platformAdminOnly=${item.platformAdminOnly || false}`);

            if (hasAccess) {
                let itemPath = item.path;
                if (item.path === 'customer.html' && user && user.vendorCode) {
                    itemPath = `customer.html?vendor=${user.vendorCode}`;
                }
                const isActive = this.currentPath.includes(item.path) ? 'active' : '';
                html += `<a href="${itemPath}" class="nav-item ${isActive}"><i data-lucide="${item.icon}"></i> ${item.label}</a>`;
            }
        });
        navContainer.innerHTML = html;
        console.log('[ROUTER] Sidebar HTML set. Total links:', navContainer.querySelectorAll('.nav-item').length);
        console.log('[ROUTER] Generated HTML:', html);

        // Add close button to sidebar for mobile
        const brand = document.querySelector('.sidebar-brand');
        if (brand && !brand.querySelector('.close-sidebar')) {
            brand.innerHTML = `<i data-lucide="qr-code"></i> DeQueue <button class="close-sidebar btn-icon" style="margin-left:auto; display:none;" onclick="document.getElementById('sidebar').classList.remove('open')"><i data-lucide="x"></i></button>`;
        }

        // Add logout button to header if missing
        const header = document.querySelector('.header');
        if (header && !header.querySelector('.logout-btn')) {
            let rightSide = header.querySelector('.header-right');
            if (!rightSide) {
                rightSide = document.createElement('div');
                rightSide.className = 'flex items-center gap-4 header-right';
                header.appendChild(rightSide);
            }
            rightSide.innerHTML += `<button class="btn btn-icon logout-btn" title="Logout" onclick="auth.logout()"><i data-lucide="log-out"></i></button>`;
        }

        if (window.lucide) {
            lucide.createIcons();
        }
    }

    enforceAccess() {
        const userStr = localStorage.getItem('user');
        const user = userStr ? JSON.parse(userStr) : null;
        const isPlatformAdmin = user ? user.platformAdmin : false;
        
        if (isPlatformAdmin) return; // Full access
        
        if (!user) {
            if (!window.location.pathname.includes('/index.html') && !window.location.pathname.includes('/customer.html') && window.location.pathname !== '/') {
                window.location.href = '/index.html';
            }
            return;
        }

        let hasAdminRole = user.roleNames && user.roleNames.includes('Vendor Admin');
        let hasManagerRole = user.roleNames && user.roleNames.includes('Manager');
        let hasKitchenRole = user.roleNames && user.roleNames.includes('Kitchen Staff');
        let hasCounterRole = user.roleNames && user.roleNames.includes('Counter Staff');
        
        // Fallback: if backend doesn't provide roles but user is authenticated, default to Admin
        if (!user.roleNames || user.roleNames.length === 0) {
            hasAdminRole = true;
        }

        const currentItem = this.navConfig.find(item => this.currentPath.includes(item.path));
        
        if (currentItem) {
            let allowed = false;
            if (currentItem.roles.includes('ADMIN') && hasAdminRole) allowed = true;
            if (currentItem.roles.includes('MANAGER') && hasManagerRole) allowed = true;
            if (currentItem.roles.includes('KITCHEN_STAFF') && hasKitchenRole) allowed = true;
            if (currentItem.roles.includes('COUNTER_STAFF') && hasCounterRole) allowed = true;
            
            if (!allowed && currentItem.roles.length > 0) {
                // Redirect to a safe page if unauthorized
                const fallback = hasKitchenRole ? 'orders.html' : 'dashboard.html';
                if (!this.currentPath.includes(fallback)) {
                    window.location.href = `/${fallback}`;
                } else {
                    document.querySelector('.page-content').innerHTML = `
                        <div class="card p-6" style="text-align: center; max-width: 400px; margin: 40px auto;">
                            <i data-lucide="shield-alert" style="width: 48px; height: 48px; color: var(--danger); margin: 0 auto 16px;"></i>
                            <h2 class="mb-4">Unauthorized Access</h2>
                            <p class="text-muted mb-4">You do not have the required roles assigned to access this page. Please contact your administrator.</p>
                            <button class="btn btn-primary" onclick="auth.logout()">Logout</button>
                        </div>
                    `;
                    if (window.lucide) lucide.createIcons();
                }
            }
        }
    }

    navigate(path) {
        window.location.href = path;
    }
}

window.router = new Router();

