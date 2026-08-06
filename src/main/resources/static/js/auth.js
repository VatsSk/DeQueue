class Auth {
  constructor() {
    this.init();
  }

  init() {
    const isLoginPage = window.location.pathname === '/' || window.location.pathname === '/index.html' || window.location.pathname.includes('/customer.html');
    
    if (!isLoginPage && !api.token) {
      window.location.href = '/index.html';
    } else if (api.token && (window.location.pathname === '/' || window.location.pathname === '/index.html')) {
      window.location.href = '/dashboard.html';
    }
    
    // Bind logout buttons if they exist
    document.addEventListener('DOMContentLoaded', () => {
      const logoutBtns = document.querySelectorAll('.logout-btn');
      logoutBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.preventDefault();
          this.logout();
        });
      });
    });
  }

  async login(email, password) {
    try {
      // Assuming a generic login endpoint. Change as per backend.
      const response = await api.post('/auth/login', { email, password });
      if (response && response.data && response.data.accessToken) {
        api.setToken(response.data.accessToken);
        localStorage.setItem('user', JSON.stringify(response.data.user || {}));
        window.location.href = '/dashboard.html';
      }
    } catch (error) {
      console.error("Login failed", error);
      throw error;
    }
  }

  logout() {
    api.clearToken();
    localStorage.removeItem('user');
    window.location.href = '/index.html';
  }
}

const auth = new Auth();
window.auth = auth;
