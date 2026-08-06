class Profile {
  constructor() {
    this.init();
  }

  init() {
    console.log('Profile initialized');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.profileApp = new Profile();
});
