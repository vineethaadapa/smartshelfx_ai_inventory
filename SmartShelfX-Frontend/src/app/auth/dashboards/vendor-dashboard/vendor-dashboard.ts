import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertService, Alert } from '../../../services/alert.service';
import { AuthService } from '../../../services/auth/AuthService';

@Component({
  selector: 'app-vendor-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vendor-dashboard.html',
  styleUrls: ['./vendor-dashboard.css']
})
export class VendorDashboardComponent implements OnInit {
  private http = inject(HttpClient);
  activeTab = 'inventory'; 
  username = 'Vendor';
  products = signal<any[]>([]);
  reorderRequests = signal<any[]>([]);
  salesReport = signal<any[]>([]);
  totalRevenue = signal<number>(0);
  selectedProduct = signal<any>(null);
  selectedFile: File | null = null;
  showReorderPopup: boolean = true; 
  selectedFilter = signal<string>('ALL');
  predictions = signal<any[]>([]); 
  vendorAlerts: any[] = [];
  unreadCount: number = 0;

  private alertService = inject(AlertService);
  private authService = inject(AuthService);
  cdr: any;
  userEmail: string = '';
  userRole: string = '';

  constructor() {
    this.userEmail = this.authService.getUserEmail() || 'VENDOR';
    this.userRole = this.authService.getUserRole() || 'Vendor';
  }
fetchPredictions() {
  const token = localStorage.getItem('token');

  const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });

  this.http.get<any[]>('https://smartshelfx-backend.onrender.com/api/predictions/demand-data', { headers })
    .subscribe({
      next: (data) => {
        console.log('Success! Data from Spring Boot (via Flask):', data);
        this.predictions.set(data);
      },
      error: (err) => {
        console.error('Connection failed. Make sure Flask (5001) and Spring Boot (8080) are both running.', err);
      }
    });
}
  filteredRequests = computed(() => {
    let list = [...this.reorderRequests()]; 

    if (this.selectedFilter() !== 'ALL') {
      list = list.filter(req => req.status === this.selectedFilter());
    }

    return list.sort((a, b) => 
      new Date(b.requestDate).getTime() - new Date(a.requestDate).getTime()
    );
  });

  pendingRequestCount = computed(() => 
    this.reorderRequests().filter(req => req.status === 'PENDING').length
  );

  ngOnInit() {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.username = payload.sub || payload.name || 'Vendor';
      } catch (e) {
        console.error("Token decode failed", e);
      }
    }
    this.refreshDashboard();

const vId = this.authService.getVendorIdFromToken(); 
  if (vId) {
    this.loadVendorNotifications(vId);
  } else {
    console.warn("No Vendor ID found in token.");
  }
  }

  // Inside your VendorDashboardComponent class
showNotifications = false;

toggleNotifications() {
  this.showNotifications = !this.showNotifications;
}
 markAsRead(id: number) {
    this.alertService.markAsRead(id).subscribe({
      next: () => {
        const alert = this.vendorAlerts.find(a => a.id === id);
        if (alert) {
          alert.isRead = true; 
        }
        this.vendorAlerts = this.vendorAlerts.filter(a => a.id !== id);
        this.unreadCount = this.vendorAlerts.length;

        this.cdr.detectChanges();
      },
      error: (err) => console.error("Could not update alert", err)
    });
  }

dismissAlert(id: number, event: Event) {
  event.stopPropagation(); 

  this.alertService.markAsRead(id).subscribe({
    next: () => {
      this.vendorAlerts = this.vendorAlerts.filter(a => a.id !== id);
      this.unreadCount = Math.max(0, this.unreadCount - 1);
      
      console.log(`Alert ${id} dismissed and removed from UI.`);
    },
    error: (err) => {
      console.error("Backend failed to dismiss alert:", err);
      alert("Session expired or unauthorized. Please login again.");
    }
  });
}
  loadVendorNotifications(vendorId: number) {
  this.alertService.getAlertsForVendor(vendorId).subscribe({
    next: (data: any[]) => { 
      this.vendorAlerts = data;
      this.unreadCount = data.filter((a: any) => !a.isRead).length;
    },
    error: (err) => console.error("Could not load vendor alerts", err)
  });
}
setActiveTab(tab: string) {
  this.activeTab = tab;
  
  if (tab === 'reports') {
    this.fetchSalesReport();
  } 
  else if (tab === 'ai-demands') {
    this.fetchPredictions();
  }
}

  goToReorders() {
    this.activeTab = 'requests';
    this.showReorderPopup = false;
  }

  refreshDashboard() {
    this.fetchProducts();
    this.fetchReorders();
  }

  fetchProducts() {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    this.http.get<any[]>('https://smartshelfx-backend.onrender.com/api/vendor/my-products', { headers }).subscribe({
      next: (data) => this.products.set(data),
      error: (err) => console.error("Product fetch error", err)
    });
  }

  fetchReorders() {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    this.http.get<any[]>('https://smartshelfx-backend.onrender.com/api/vendor/reorders', { headers }).subscribe({
      next: (data) => this.reorderRequests.set(data),
      error: (err) => console.error("Error loading reorders", err)
    });
  }

  updateRequestStatus(requestId: number, newStatus: 'APPROVED' | 'REJECTED') {
    const action = newStatus === 'APPROVED' ? 'Approve and Ship' : 'Reject';
    if (!confirm(`Are you sure you want to ${action} this request?`)) return;

    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    this.http.post(`https://smartshelfx-backend.onrender.com/api/vendor/reorders/${requestId}/status`, { status: newStatus }, { headers })
      .subscribe({
        next: () => {
          alert(`Order successfully ${newStatus.toLowerCase()}!`);
          this.fetchReorders(); 
        },
        error: (err) => console.error("Status update failed", err)
      });
  }

  openEditModal(product: any) {
    this.selectedProduct.set({ ...product });
  }

  saveProduct() {
    const p = this.selectedProduct();
    if (!p) return;
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    this.http.put(`https://smartshelfx-backend.onrender.com/api/vendor/products/${p.id}`, p, { headers })
      .subscribe({
        next: () => {
          this.fetchProducts();
          this.selectedProduct.set(null);
        },
        error: (err) => console.error("Update failed", err)
      });
  }

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  uploadCSV() {
    if (!this.selectedFile) return;
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    this.http.post('https://smartshelfx-backend.onrender.com/api/vendor/products/upload-csv', formData, { headers, responseType: 'text' })
      .subscribe({
        next: () => {
          alert("Catalog updated!");
          this.fetchProducts();
          this.selectedFile = null;
        },
        error: (err) => console.error("Upload failed", err)
      });
  }
fetchSalesReport() {
  const token = localStorage.getItem('token');
  const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  this.http.get<any>('https://smartshelfx-backend.onrender.com/api/vendor/sales-report', { headers }).subscribe({
    next: (data) => {
      const sortedItems = (data.items || []).sort((a: any, b: any) => 
        new Date(b.saleDate).getTime() - new Date(a.saleDate).getTime()
      );
      
      this.salesReport.set(sortedItems);
      this.totalRevenue.set(data.totalRevenue || 0);
    },
    error: (err) => console.error("Error fetching sales reports", err)
  });
}

  onLogout() {
    localStorage.removeItem('token');
    window.location.href = '/login'; 
  }
}