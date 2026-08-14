class Reports {
  constructor() {
    this.startDateInput = document.getElementById('startDate');
    this.endDateInput = document.getElementById('endDate');
    this.applyBtn = document.getElementById('applyFiltersBtn');
    this.exportBtn = document.getElementById('exportBtn');
    this.modal = document.getElementById('drilldownModal');
    
    this.charts = {};
    
    this.init();
  }

  async init() {
    const today = new Date();
    const lastWeek = new Date(today);
    lastWeek.setDate(lastWeek.getDate() - 7);
    
    this.endDateInput.value = today.toISOString().split('T')[0];
    this.startDateInput.value = lastWeek.toISOString().split('T')[0];
    
    this.applyBtn.addEventListener('click', () => this.loadData());
    this.exportBtn.addEventListener('click', () => this.exportCsv());
    
    this.modal.addEventListener('click', (e) => {
        if (e.target === this.modal || e.target.closest('.close-modal')) {
            this.modal.style.display = 'none';
        }
    });
    
    await this.loadData();
  }
  
  async loadData() {
    const start = this.startDateInput.value;
    const end = this.endDateInput.value;
    
    try {
        const res = await api.get(`/reports/summary?startDate=${start}&endDate=${end}`);
        if(res.data) {
            this.renderDashboard(res.data);
        }
    } catch(e) {
        console.error('Failed to load reports', e);
    }
  }
  
  renderDashboard(data) {
      document.getElementById('totalRevenue').textContent = `₹${data.orderReport.totalRevenue.toFixed(2)}`;
      document.getElementById('totalOrders').textContent = data.orderReport.totalOrders;
      
      this.renderStatusChart(data.orderReport);
      this.renderPeakHoursChart(data.peakHourReport);
      this.renderRevenueChart(data.peakHourReport);
      this.renderPopularItemsChart(data.popularItemReport);
  }
  
  renderStatusChart(orderReport) {
      const ctx = document.getElementById('statusChart').getContext('2d');
      if (this.charts.status) this.charts.status.destroy();
      
      const labels = Object.keys(orderReport.byStatus);
      const data = Object.values(orderReport.byStatus);
      
      this.charts.status = new Chart(ctx, {
          type: 'doughnut',
          data: {
              labels,
              datasets: [{
                  data,
                  backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#3b82f6'],
              }]
          },
          options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
      });
  }
  
  renderPeakHoursChart(peakHourReport) {
      const ctx = document.getElementById('peakHoursChart').getContext('2d');
      if (this.charts.peak) this.charts.peak.destroy();
      
      const labels = peakHourReport.hours.map(h => `${h.hour}:00`);
      const data = peakHourReport.hours.map(h => h.orderCount);
      
      this.charts.peak = new Chart(ctx, {
          type: 'bar',
          data: {
              labels,
              datasets: [{
                  label: 'Orders',
                  data,
                  backgroundColor: '#3b82f6',
                  borderRadius: 4
              }]
          },
          options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }
      });
  }
  
  renderRevenueChart(peakHourReport) {
      const ctx = document.getElementById('revenueChart').getContext('2d');
      if (this.charts.revenue) this.charts.revenue.destroy();
      
      const labels = peakHourReport.hours.map(h => `${h.hour}:00`);
      const data = peakHourReport.hours.map(h => h.revenue);
      
      this.charts.revenue = new Chart(ctx, {
          type: 'line',
          data: {
              labels,
              datasets: [{
                  label: 'Revenue (₹)',
                  data,
                  borderColor: '#10b981',
                  backgroundColor: 'rgba(16, 185, 129, 0.1)',
                  fill: true,
                  tension: 0.4
              }]
          },
          options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }
      });
  }
  
  renderPopularItemsChart(popularItemReport) {
      const ctx = document.getElementById('popularItemsChart').getContext('2d');
      if (this.charts.popular) this.charts.popular.destroy();
      
      const items = popularItemReport.items.slice(0, 10);
      const labels = items.map(i => i.menuItemName);
      const data = items.map(i => i.orderCount);
      
      this.charts.popular = new Chart(ctx, {
          type: 'bar',
          data: {
              labels,
              datasets: [{
                  label: 'Quantity Sold',
                  data,
                  backgroundColor: '#10b981',
                  borderRadius: 4
              }]
          },
          options: {
              responsive: true,
              maintainAspectRatio: false,
              scales: { y: { beginAtZero: true } },
              onClick: (event, elements) => {
                  if (elements.length > 0) {
                      const index = elements[0].index;
                      this.showDrilldown(items[index]);
                  }
              }
          }
      });
  }
  
  showDrilldown(item) {
      document.getElementById('drilldownTitle').textContent = `Details: ${item.menuItemName}`;
      document.getElementById('modalOrderCount').textContent = item.orderCount;
      document.getElementById('modalRevenue').textContent = `₹${item.totalRevenue.toFixed(2)}`;
      
      this.modal.style.display = 'flex';
  }
  
  async exportCsv() {
      const start = this.startDateInput.value;
      const end = this.endDateInput.value;
      try {
          const res = await fetch(`/api/v1/reports/export?startDate=${start}&endDate=${end}`, {
              headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
          });
          if (!res.ok) throw new Error('Export failed');
          
          const blob = await res.blob();
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `reports_${start}_to_${end}.csv`;
          document.body.appendChild(a);
          a.click();
          a.remove();
          window.URL.revokeObjectURL(url);
      } catch (e) {
          console.error(e);
          alert('Failed to export CSV');
      }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.reportsApp = new Reports();
});
