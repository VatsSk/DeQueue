class Settings {
  constructor() {
    this.init();
  }

  init() {
    console.log('Settings initialized');
    this.setupTabs();
  }

  setupTabs() {
    const tabs = document.querySelectorAll('.settings-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            tabs.forEach(t => t.classList.remove('active'));
            e.target.classList.add('active');
        });
    });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.settingsApp = new Settings();
});
