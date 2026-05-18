import { Component, OnInit, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { AuthService } from '../../../services/auth/AuthService';
import { AdminService } from '../../../services/admin/admin.service';
import { Chart, registerables } from 'chart.js';
import { ChangeDetectorRef } from '@angular/core';
import { AlertService, Alert } from '../../../services/alert.service';
import { VendorPerformanceService, VendorPerformance } from '../../../services/vendor-performance.service';

Chart.register(...registerables);
interface InventoryItem {
  sku: string;
  name: string;
  quantity: number;
  price: number;
}
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboardComponent implements OnInit {
  private alertService = inject(AlertService); // Using inject() for clean Angular 19 style
  alerts: Alert[] = [];
  unreadCount: number = 0;
  isNotificationOpen: boolean = false;
  adminAlerts: any[] = [];
  // Inventory pie chart
  @ViewChild('healthChart') chartCanvas!: ElementRef;

  @ViewChild('forecastChart') forecastCanvas!: ElementRef;

  @ViewChild('trendCanvas') trendCanvas!: ElementRef;
  trendChart: any;
inventory: InventoryItem[] =[] ;

 private chart: Chart | null = null;
private aiChart: Chart | null = null;

  userRole: string = 'USER';
  userEmail: string = '';
  loggedUserWarehouseId: number | null = null;

  vendors: any[] = [];
  allUsers: any[] = [];
  categories: string[] = [];
  products: any[] = [];
  filteredProducts: any[] = [];
  auditLogs: any[] = [];
  private perfService = inject(VendorPerformanceService);
  vendorStats = signal<VendorPerformance[]>([]);

  // Update this line to include 'alerts'
  activeTab: 'inventory' | 'audit' | 'reports' | 'permissions' | 'ai-forecast' | 'alerts' | 'vendor-performance' = 'inventory';

  showProductModal = false;
  isEditMode = false;
  searchTerm: string = '';
  filterCategory: string = '';
  filterVendorId: string = '';
  filterStockStatus: string = '';

  totalInventoryValue: number = 0;
  lowStockCount: number = 0;
  totalItems: number = 0;
  forecastData: any = null;
  productForm = this.resetForm();
  notificationService: any;
  constructor(
    public authService: AuthService,
    private adminService: AdminService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {
    this.userRole = this.authService.getUserRole() || 'USER';
    this.userEmail = this.authService.getUserEmail() || 'User';
    this.loggedUserWarehouseId = this.authService.getWarehouseId();
  }

  ngOnInit(): void {
    this.loadDashboardData();
    this.initFormOptions();
    // this.loadInventory();
    this.fetchNotifications(); // 3. Initial fetch
  
    setInterval(() => this.fetchNotifications(), 60000);
    this.loadAdminAlerts();

    this.loadPerformanceData();
  }
  loadPerformanceData() {
    this.perfService.getPerformanceReport().subscribe({
      next: (data) => this.vendorStats.set(data),
      error: (err) => console.error("Could not load performance stats", err)
    });
  }
  
refreshAlerts() {
  console.log('Refreshing alerts...');
  this.loadAdminAlerts(); 
}

  

  /* ---------------- INVENTORY PIE CHART ---------------- */
  initChart() {
    if (!this.chartCanvas) return;

    const healthy = this.products.filter(p => p.currentStock > p.reorderLevel).length;
    const low = this.products.filter(p => p.currentStock <= p.reorderLevel && p.currentStock > 0).length;
    const outOfStock = this.products.filter(p => p.currentStock <= 0).length;

    if (this.chart) this.chart.destroy();

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'pie',
      data: {
        labels: ['Healthy', 'Low Stock', 'Out of Stock'],
        datasets: [{
          data: [healthy, low, outOfStock],
          backgroundColor: ['#22c55e', '#f59e0b', '#ef4444'],
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  /* ---------------- RESET PRODUCT FORM ---------------- */
  resetForm() {
    return { name: '', sku: '', category: '', reorderLevel: 10, price: 0, vendor: { id: null } };
  }

  /* ---------------- LOAD INITIAL DATA ---------------- */
  initFormOptions() {
    this.adminService.getCategories().subscribe(data => this.categories = data);
    this.adminService.getAllVendors().subscribe(data => this.vendors = data);
  }

  loadDashboardData() {
  this.adminService.getAuditLogs().subscribe(logs => this.auditLogs = logs);

  this.adminService.getProducts().subscribe(data => {
    this.products = data;
    
    // FIX: Map your existing products to the InventoryItem format for the AI
    this.inventory = data.map((p: any) => ({
      sku: p.sku,
      name: p.name,
      quantity: p.currentStock, // Ensure this matches your Product model's stock field
      price: p.price
    }));

    this.updateSummaries();
    this.applyFilters();
    setTimeout(() => this.initChart(), 0);
  });
}

  /* ---------------- SUMMARY COUNTS ---------------- */
  updateSummaries() {
    this.totalItems = this.products.length;
    this.totalInventoryValue = this.products.reduce((acc, p) => acc + (p.price * p.currentStock), 0);
    this.lowStockCount = this.products.filter(p => p.currentStock <= p.reorderLevel).length;
  }


  applyFilters() {
  this.filteredProducts = this.products.filter(p => {
    const searchMatch = (p.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
                         p.sku.toLowerCase().includes(this.searchTerm.toLowerCase()));
    
    const categoryMatch = this.filterCategory ? p.category === this.filterCategory : true;
    const vendorMatch = this.filterVendorId ? p.vendor?.id == this.filterVendorId : true;

    // Fixed Stock Status Logic
    let statusMatch = true;
    if (this.filterStockStatus && this.filterStockStatus !== 'ALL') {
      if (this.filterStockStatus === 'OUT') {
        statusMatch = (p.currentStock === 0);
      } else if (this.filterStockStatus === 'LOW') {
        // Only show items with stock between 1 and the reorder level
        statusMatch = (p.currentStock > 0 && p.currentStock <= p.reorderLevel);
      } else if (this.filterStockStatus === 'INSTOCK') {
        statusMatch = (p.currentStock > p.reorderLevel);
      }
    }

    return searchMatch && categoryMatch && vendorMatch && statusMatch;
  });
}


  loadAllUsersList() {
    this.adminService.getAllUsers().subscribe(data => this.allUsers = data);
  }


  openAddModal() {
    this.isEditMode = false;
    this.productForm = this.resetForm();
    this.showProductModal = true;
  }

  openEditModal(product: any) {
    this.isEditMode = true;
    this.productForm = JSON.parse(JSON.stringify(product));
    if (!this.productForm.vendor) this.productForm.vendor = { id: null };
    this.showProductModal = true;
  }

  closeModal() { this.showProductModal = false; }

  onSaveProduct() {
    const isDuplicate = this.products.some(p =>
      p.sku.toLowerCase() === this.productForm.sku.toLowerCase() &&
      (!this.isEditMode || p.id !== (this.productForm as any).id)
    );

    if (isDuplicate) {
      alert(`Error: SKU "${this.productForm.sku}" already exists.`);
      return;
    }
    
  const payload = JSON.parse(JSON.stringify(this.productForm));


  if (payload.vendor && payload.vendor.id) {
    payload.vendor.id = Number(payload.vendor.id);
  } else {
    alert("Please select a vendor!");
    return;
  }

  const request = this.isEditMode
    ? this.adminService.updateProduct(payload.id, payload)
    : this.adminService.addProduct(payload);

    request.subscribe({
      next: () => {
        alert(this.isEditMode ? "Product updated!" : "Product added!");
        this.loadDashboardData();
        this.closeModal();
      },
      error: (err) => alert("Failed: " + (err.error?.message || "Server error"))
    });
  }

  onDeleteProduct(id: number) {
    if (confirm('Delete this product permanently?')) {
      this.adminService.deleteProduct(id).subscribe({
        next: () => {
          alert('Product removed.');
          this.loadDashboardData();
        },
        error: () => alert('Error deleting product.')
      });
    }
  }


onImportCSV(event: any) {
  const file = event.target.files[0];
  if (file) {
    const formData = new FormData();
    formData.append('file', file);
    
    this.adminService.importCSV(formData).subscribe({
      next: (res: any) => {
        alert(res.message); 
        this.loadDashboardData();
        event.target.value = ''; 
      },
      error: (err) => {
        alert("Import Error: " + err.error?.message);
        event.target.value = '';
      }
    });
  }
}

  exportToCSV() {
    const headers = 'SKU,Name,Category,Vendor,Price\n';
    const csvRows = this.products.map(p => {
      const vId = p.vendor ? p.vendor.id : 'N/A';
      return `"${p.sku}","${p.name}","${p.category}","${vId}",${p.price}`;
    }).join('\n');

    const blob = new Blob([headers + csvRows], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'Product_Catalog.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  }


switchTab(tab: 'inventory' | 'audit' | 'reports' | 'permissions' | 'ai-forecast' | 'alerts' | 'vendor-performance') {
  this.activeTab = tab;

  
  if (this.chart) this.chart.destroy(); 
  if (this.trendChart) this.trendChart.destroy();
  if (this.comparisonChart) this.comparisonChart.destroy();
  if (this.aiChart) this.aiChart.destroy();

  if (tab === 'permissions') this.loadAllUsersList();
  if (tab === 'ai-forecast') this.getAiForecast();

  
  setTimeout(() => {
    if (tab === 'reports') {
      this.initChart();              
      this.loadTrends();             
      this.loadMonthlyComparison();  
    }
    
    if (tab === 'vendor-performance') {
      this.initVendorPerformanceChart();
    }
    
    if (tab === 'ai-forecast') {
      this.initForecastChart();
    }
  }, 150); 
}

multiForecastData: any[] = [];

getAiForecast() {
  if (this.inventory.length === 0 && this.products.length > 0) {
    this.inventory = this.products.map(p => ({
      sku: p.sku, name: p.name, quantity: p.currentStock, price: p.price
    }));
  }

  if (this.inventory.length === 0) {
    alert("No products found to analyze.");
    return;
  }

  const payload = {
    items: this.inventory.map(item => ({
      sku: item.sku,
      name: item.name,
      quantity: item.quantity 
    }))
  };

  this.http.post('http://127.0.0.1:5001/api/forecast_all', payload).subscribe({
    next: (res: any) => {
      this.multiForecastData = res.predictions;
      
      // Select first item by default to populate the chart immediately
      if (this.multiForecastData.length > 0) {
        this.updateActiveChart(this.multiForecastData[0]);
      }
      this.cdr.detectChanges(); // Force UI update
    },
    error: (err) => console.error("AI Sync Error:", err)
  });
}
initForecastChart() {
  const canvas = document.getElementById('forecastChart') as HTMLCanvasElement;
  if (!canvas || !this.forecastData) return;

  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  if (this.aiChart) this.aiChart.destroy();

  this.aiChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: this.forecastData.chart_labels, // Use the dates from Python
      datasets: [{
        label: 'Predicted Units',
        data: this.forecastData.chart_data, // The array of numbers
        borderColor: '#6366f1',
        backgroundColor: 'rgba(99, 102, 241, 0.1)',
        fill: true,
        tension: 0.4,
        pointBackgroundColor: '#4f46e5',
        pointRadius: 4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true,
          grid: { color: '#f1f5f9' }
        },
        x: {
          grid: { display: false }
        }
      },
      plugins: {
        legend: { display: false }
      }
    }
  });
}

reorderFromVendor(item: any) {
  const quantityToOrder = item.forecast - item.current_stock;
  const confirmAction = confirm(`AI suggests ordering ${quantityToOrder} units for ${item.sku}. Proceed?`);
  
  if (confirmAction) {
    const updatePayload = {
      sku: item.sku,
      quantityToAdd: quantityToOrder
    };
    this.http.post('https://smartshelfx-backend.onrender.com/api/admin/products', updatePayload).subscribe({
      next: (res) => {
        alert(`Purchase Order for ${item.sku} dispatched and stock updated!`);
        this.loadDashboardData(); 
        this.getAiForecast();    
      },
      error: (err) => {
        console.error("Dispatch Failed:", err);
        alert("Server Error: Could not dispatch the order. Please check if the backend is running.");
      }
    });
  }
}

selectedForecastTotal: number = 0;

updateActiveChart(item: any) {
    this.selectedForecastTotal = item.forecast;
  
    const chartLabels = [];
    for (let i = 1; i <= 7; i++) {
        const d = new Date();
        d.setDate(d.getDate() + i);
        chartLabels.push(d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
    }

   
    const canvas = document.getElementById('forecastChart') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (this.aiChart) this.aiChart.destroy();

    this.aiChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: chartLabels,
            datasets: [{
                label: `${item.name} Demand`,
                data: item.chart_data, // Using the individual item's data from the backend
                borderColor: '#4f46e5',
                backgroundColor: 'rgba(79, 70, 229, 0.1)',
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

  onLogout() { this.authService.logout(); }
  onOpenPermissions() { this.switchTab('permissions'); }

  onChangeRole(userId: number, event: any) {
    this.adminService.updateUserRole(userId, event.target.value).subscribe(() => this.loadAllUsersList());
  }

  onAssignWarehouse(userId: number) {
    const whId = prompt("Enter Warehouse ID:");
    if (whId) {
      this.adminService.assignWarehouse(userId, Number(whId)).subscribe(() => this.loadAllUsersList());
    }
  }



  toggleDropdown() {
    this.isNotificationOpen = !this.isNotificationOpen;
  }


exportReport() {
  console.log('Generating PDF Report...');
  
  this.adminService.downloadInventoryPdf().subscribe({
    next: (blob: Blob) => {
      // Create a URL for the binary data
      const url = window.URL.createObjectURL(blob);
      // Create a hidden anchor element to trigger download
      const link = document.createElement('a');
      link.href = url;
      link.download = `SmartShelfX_Inventory_Report_${new Date().toISOString().split('T')[0]}.pdf`;
      link.click();
      
      // Clean up the URL object
      window.URL.revokeObjectURL(url);
    },
    error: (err) => {
      console.error('Error downloading the PDF report:', err);
      alert('Failed to generate PDF. Please check if the backend service is running.');
    }
  });
}

dismissAlert(id: number, event: Event) {
  event.stopPropagation(); 

  this.alertService.dismissAlert(id).subscribe({
    next: () => {
      this.alerts = this.alerts.filter(a => a.id !== id);
      this.adminAlerts = this.adminAlerts.filter(a => a.id !== id);
      this.unreadCount = Math.max(0, this.unreadCount - 1);
      
      console.log(`Alert ${id} dismissed and removed from UI.`);
    },
    error: (err) => console.error("Backend failed to dismiss alert:", err)
  });
  }

  initVendorPerformanceChart() {
    const ctx = document.getElementById('perfChart') as HTMLCanvasElement;
    if (!ctx) return;

    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.vendorStats().map(v => v.vendorName),
        datasets: [{
          label: 'Fulfillment Rate (%)',
          data: this.vendorStats().map(v => v.fulfillmentRate),
          backgroundColor: '#1a73e8',
          borderRadius: 5
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } }
      }
    });
  }
fetchNotifications() {
  
  this.alertService.getUnreadAlerts(this.userRole).subscribe({
    next: (data) => {
      // For the Bell, we ONLY show unread
      this.alerts = data.filter(a => !a.isRead); 
      this.unreadCount = this.alerts.length;
      this.cdr.detectChanges();
    },
    error: (err) => console.error("Bell Fetch Error:", err)
  });
}

loadAdminAlerts() {
  
  this.alertService.getAlertsByRole(this.userRole).subscribe({
    next: (data) => {
      this.adminAlerts = data; 
    },
    error: (err) => console.error('Error fetching alert history', err)
  });
}


  markAsRead(id: number) {
    this.alertService.markAsRead(id).subscribe({
      next: () => {
        
        const alert = this.adminAlerts.find(a => a.id === id);
        if (alert) {
          alert.isRead = true; 
        }

        // Also remove it from the Bell Icon list immediately
        this.alerts = this.alerts.filter(a => a.id !== id);
        this.unreadCount = this.alerts.length;

        this.cdr.detectChanges();
      },
      error: (err) => console.error("Could not update alert", err)
    });
  }

loadTrends() {
  // Ensure the canvas exists before trying to draw
  if (!this.trendCanvas) {
    console.error("Trend Canvas not found in DOM");
    return;
  }

  this.adminService.getInventoryTrends().subscribe({
    next: (data) => {
      const labels = data.map(item => item.date);
      const values = data.map(item => item.totalStock);
      this.createChart(labels, values);
    },
    error: (err) => console.error('Error fetching trends', err)
  });
}

  createChart(labels: string[], values: number[]) {
  const ctx = this.trendCanvas.nativeElement.getContext('2d');

  // DESTROY existing chart instance if it exists
  if (this.trendChart) {
    this.trendChart.destroy();
  }

  this.trendChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels,
      datasets: [{
        label: 'Total Stock Level',
        data: values,
        borderColor: '#4f46e5', // Modern Indigo
        backgroundColor: 'rgba(79, 70, 229, 0.1)',
        fill: true,
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'top' },
        tooltip: { mode: 'index', intersect: false }
      },
      scales: {
        y: { beginAtZero: true, grid: { color: '#f1f5f9' } },
        x: { grid: { display: false } }
      }
    }
  });
}

@ViewChild('comparisonCanvas') comparisonCanvas!: ElementRef;
private comparisonChart: any;

loadMonthlyComparison() {
  this.adminService.getMonthlyComparison().subscribe({
    next: (data: any[]) => { 
      if (!this.comparisonCanvas) return;

      // Clean up old chart to prevent "Canvas already in use" error
      if (this.comparisonChart) {
        this.comparisonChart.destroy();
      }

      this.comparisonChart = new Chart(this.comparisonCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels: data.map(d => d.month),
          datasets: [
            {
              label: 'Purchases (Stock In)',
              data: data.map(d => d.purchases),
              backgroundColor: '#4f46e5' 
            },
            {
              label: 'Sales (Stock Out)',
              data: data.map(d => d.sales),
              backgroundColor: '#ef4444'
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: { 
              beginAtZero: true,
              title: { display: true, text: 'Units' }
            }
          }
        }
      });
    },
    error: (err) => console.error("Monthly Comparison Load Failed:", err)
  });
}

get topRestockedItems() {
  const restockMap = new Map<string, any>();
  
  // Create a set of active SKUs for quick lookup
  const activeSkus = new Set(this.products.map(p => p.sku));

  this.auditLogs.forEach(log => {
    // Only process if it's a restock AND the product is still in your active list
    if ((log.type === 'RESTOCK' || log.type === 'IN') && log.product) {
      const sku = log.product.sku;
      
      if (activeSkus.has(sku)) {
        const existing = restockMap.get(sku) || { 
          sku: sku, 
          name: log.product.name, 
          totalQuantity: 0, 
          lastRestock: log.timestamp 
        };

        existing.totalQuantity += (log.quantity || 0);
        
        if (new Date(log.timestamp) > new Date(existing.lastRestock)) {
          existing.lastRestock = log.timestamp;
        }
        restockMap.set(sku, existing);
      }
    }
  });

  return Array.from(restockMap.values())
    .sort((a, b) => b.totalQuantity - a.totalQuantity)
    .slice(0, 5);
}
}