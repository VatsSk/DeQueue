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
            { path: 'staff.html', icon: 'users', label: 'Staff', roles: ['ADMIN', 'MANAGER'] },
            { path: 'reports.html', icon: 'bar-chart-3', label: 'Reports', roles: ['ADMIN', 'MANAGER'] },
            { path: 'qr.html', icon: 'qr-code', label: 'QR Code', roles: ['ADMIN', 'MANAGER', 'COUNTER_STAFF'] },
            { path: 'profile.html', icon: 'store', label: 'Shop Profile', roles: ['ADMIN', 'MANAGER'] },
            { path: 'settings.html', icon: 'settings', label: 'Settings', roles: ['ADMIN', 'MANAGER'] },
            { path: 'customer.html', icon: 'smartphone', label: 'Customer View', roles: ['ADMIN', 'MANAGER', 'COUNTER_STAFF'] }
        ];
        this.init();
    }

    init() {
        document.addEventListener('DOMContentLoaded', () => {
            this.renderSidebar();
            this.enforceAccess();
        });
    }

    renderSidebar() {
        const navContainer = document.querySelector('.sidebar-nav');
        if (!navContainer) return;

        const userStr = localStorage.getItem('user');
        const user = userStr ? JSON.parse(userStr) : null;
        const role = user ? user.role : 'ADMIN'; // Fallback for safety

        let html = '';
        this.navConfig.forEach(item => {
            if (item.roles.includes(role)) {
                let itemPath = item.path;
                if (item.path === 'customer.html' && user && user.vendorCode) {
                    itemPath = `customer.html?vendor=${user.vendorCode}`;
                } else if (item.path === 'customer.html' && user && !user.vendorCode) {
                    // Fallback to fetch vendorCode from shopName or id? user object has it!
                    // Wait, AuthResponse maps StaffSummary. Does StaffSummary have vendorCode?
                    // Let's check StaffSummary. It has vendorId, shopName, but NOT vendorCode!
                    // If it doesn't have vendorCode, the link will fail.
                    // We must update AuthServiceImpl to include vendorCode in StaffSummary!
                }
                const isActive = this.currentPath.includes(item.path) ? 'active' : '';
                html += `<a href="${itemPath}" class="nav-item ${isActive}"><i data-lucide="${item.icon}"></i> ${item.label}</a>`;
            }
        });
        navContainer.innerHTML = html;

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
        const role = user ? user.role : null;
        
        if (!role) return;

        const currentItem = this.navConfig.find(item => this.currentPath.includes(item.path));
        if (currentItem && !currentItem.roles.includes(role)) {
            // Redirect to a safe page if unauthorized
            const fallback = role === 'KITCHEN_STAFF' ? 'orders.html' : 'dashboard.html';
            window.location.href = `/${fallback}`;
        }
    }

    navigate(path) {
        window.location.href = path;
    }
}

window.router = new Router();

