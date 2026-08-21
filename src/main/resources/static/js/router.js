// Simple router/navigation utility
class Router {
    constructor() {
        this.currentPath = window.location.pathname;
        this.navConfig = [
            { path: 'dashboard.html', icon: 'layout-dashboard', label: 'Dashboard', permissions: ['report.view', 'order.view'] },
            { path: 'orders.html', icon: 'list-ordered', label: 'Orders', permissions: ['order.view'] },
            { path: 'menu.html', icon: 'utensils', label: 'Menu', permissions: ['menu.view'] },
            { path: 'categories.html', icon: 'tags', label: 'Categories', permissions: ['menu.view'] },
            { path: 'customizations.html', icon: 'settings-2', label: 'Customizations', permissions: ['menu.view'] },
            { path: 'departments.html', icon: 'briefcase', label: 'Departments', permissions: ['staff.view'] },
            { path: 'staff.html', icon: 'users', label: 'Staff', permissions: ['staff.view'] },
            { path: 'reports.html', icon: 'bar-chart-3', label: 'Reports', permissions: ['report.view'] },
            { path: 'qr.html', icon: 'qr-code', label: 'QR Code', permissions: ['qr.view'] },
            { path: 'settings.html', icon: 'settings', label: 'Settings', permissions: ['staff.view'] },
            { path: 'geofence.html', icon: 'map-pin', label: 'Geofencing', permissions: ['staff.view'] },
            { path: 'settlements.html', icon: 'wallet', label: 'Payments & Settlements', permissions: ['report.view', 'order.view'] },
            // { path: 'promotions.html', icon: 'tag', label: 'Promotions', permissions: ['menu.view'] },
//            { path: 'customer.html', icon: 'smartphone', label: 'Customer View', permissions: ['order.view', 'menu.view'] },
            { path: 'vendors.html', icon: 'building', label: 'Platform Vendors', permissions: [], platformAdminOnly: true }
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
        
        let isPlatformAdmin = false;
        if (user) {
            isPlatformAdmin = user.platformAdmin === true;
            console.log('[ROUTER] user.platformAdmin:', user.platformAdmin);
            console.log('[ROUTER] isPlatformAdmin:', isPlatformAdmin);
        }

        let html = '';
        this.navConfig.forEach(item => {
            let hasAccess = false;
            
            if (isPlatformAdmin) {
                hasAccess = true;
            } else if (!item.platformAdminOnly) {
                // If the user has any of the permissions required by the item, grant access.
                // If the item lists no permissions, default to allowing access.
                if (!item.permissions || item.permissions.length === 0) {
                    hasAccess = true;
                } else if (user && user.roles && Array.isArray(user.roles) && user.roles.includes('ROLE_VENDOR_ADMIN')) {
                    hasAccess = true;
                } else if (user && user.role === 'ROLE_VENDOR_ADMIN') {
                    hasAccess = true;
                } else if (user && user.effectivePermissions && Array.isArray(user.effectivePermissions)) {
                    hasAccess = item.permissions.some(p => user.effectivePermissions.includes(p));
                }
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

        const currentItem = this.navConfig.find(item => this.currentPath.includes(item.path));
        
        if (currentItem) {
            let allowed = false;
            if (isPlatformAdmin) {
                allowed = true;
            } else if (currentItem.platformAdminOnly) {
                allowed = false;
            } else if (!currentItem.permissions || currentItem.permissions.length === 0) {
                allowed = true;
            } else if (user && user.roles && Array.isArray(user.roles) && user.roles.includes('ROLE_VENDOR_ADMIN')) {
                allowed = true;
            } else if (user && user.role === 'ROLE_VENDOR_ADMIN') {
                allowed = true;
            } else if (user.effectivePermissions && Array.isArray(user.effectivePermissions)) {
                allowed = currentItem.permissions.some(p => user.effectivePermissions.includes(p));
            }
            
            if (!allowed) {
                // Redirect to a safe page if unauthorized
                const hasOrderView = user.effectivePermissions && user.effectivePermissions.includes('order.view');
                const fallback = hasOrderView ? 'orders.html' : 'dashboard.html';
                if (!this.currentPath.includes(fallback)) {
                    window.location.href = `/${fallback}`;
                } else {
                    document.querySelector('.page-content').innerHTML = `
                        <div class="card p-6" style="text-align: center; max-width: 400px; margin: 40px auto;">
                            <i data-lucide="shield-alert" style="width: 48px; height: 48px; color: var(--danger); margin: 0 auto 16px;"></i>
                            <h2 class="mb-4">Unauthorized Access</h2>
                            <p class="text-muted mb-4">You do not have the required permissions to access this page. Please contact your administrator.</p>
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

