// Utils
window.formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR'
  }).format(amount);
};

window.formatDate = (dateString) => {
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: 'short'
  }).format(date);
};

window.getTimeAgo = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffInMinutes = Math.floor((now - date) / 60000);
  
  if (diffInMinutes < 1) return 'Just now';
  if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
  
  const diffInHours = Math.floor(diffInMinutes / 60);
  if (diffInHours < 24) return `${diffInHours}h ago`;
  
  return window.formatDate(dateString);
};

// Toast Notifications
window.showToast = (message, type = 'info') => {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  let icon = '';
  switch(type) {
    case 'success': icon = '<i data-lucide="check-circle"></i>'; break;
    case 'error': icon = '<i data-lucide="alert-circle"></i>'; break;
    case 'warning': icon = '<i data-lucide="alert-triangle"></i>'; break;
    default: icon = '<i data-lucide="info"></i>';
  }

  toast.innerHTML = `${icon} <span>${message}</span>`;
  container.appendChild(toast);
  
  if (window.lucide) {
    lucide.createIcons({ root: toast });
  }

  // Trigger animation
  setTimeout(() => toast.classList.add('show'), 10);

  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, 3000);
};

// Modal Handling
window.openModal = (modalId) => {
  const overlay = document.getElementById(`${modalId}-overlay`);
  if (overlay) overlay.classList.add('active');
};

window.closeModal = (modalId) => {
  const overlay = document.getElementById(`${modalId}-overlay`);
  if (overlay) overlay.classList.remove('active');
};

// Close modals when clicking outside
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.remove('active');
  }
});

// Setup Lucide Icons
document.addEventListener('DOMContentLoaded', () => {
  if (window.lucide) {
    lucide.createIcons();
  }
});

// Load Sidebar dynamically
window.loadSidebar = async () => {
    try {
        const res = await fetch('sidebar.html');
        if (!res.ok) return;
        const html = await res.text();
        const existingSidebar = document.getElementById('sidebar');
        if (existingSidebar) {
            existingSidebar.outerHTML = html;
        }
        
        const currentPath = window.location.pathname.split('/').pop() || 'dashboard.html';
        document.querySelectorAll('.sidebar-nav .nav-item').forEach(item => {
            if (item.getAttribute('href') === currentPath) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });
        
        if (window.lucide) {
            lucide.createIcons();
        }
    } catch (e) {
        console.error('Failed to load sidebar', e);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    window.loadSidebar();
});
