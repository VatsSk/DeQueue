class Departments {
  constructor() {
    this.init();
  }

  init() {
    console.log('Departments initialized');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.departmentsApp = new Departments();
});
