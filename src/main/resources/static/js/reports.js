class Reports {
  constructor() {
    this.init();
  }

  init() {
    console.log('Reports initialized');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.reportsApp = new Reports();
});
