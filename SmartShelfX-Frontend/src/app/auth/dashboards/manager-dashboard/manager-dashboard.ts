import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../services/admin/admin.service'; 
import { AuthService } from '../../../services/auth/AuthService'; 
import { MatDialog } from '@angular/material/dialog';
import { ReorderModalComponent } from '../reorder-modal/reorder-modal';
import { PurchaseOrderService } from '../../../services/purchase-order.service'; 
import { AlertService, Alert } from '../../../services/alert.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);


export interface WarehouseStock {
  id: number;
  warehouseId: number;
  product: {
    id: number;
    name: string;
    sku: string;
    category: string;
    price: number;
  };
  currentStock: number;
  reorderLevel: number;
}

export interface AiPrediction {
  productId: number;
  productName: string;
  sku: string;         // Added
  unitPrice: number;   // Added
  predictedDemand: number;
  vendorId: number;
  predictionDate: string;
}

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './manager-dashboard.html',
  styleUrls: ['./manager-dashboard.css']
})
export class ManagerDashboardComponent implements OnInit {
  private http = inject(HttpClient);
  private adminService = inject(AdminService); 
  private authService = inject(AuthService); 
  private dialog = inject(MatDialog);
  private poService = inject(PurchaseOrderService);
  private alertService = inject(AlertService);
  
  selectedFile: File | null = null;
  userEmail: string = '';
  userRole: string = '';
  loggedUserWarehouseId: number | null = null;

  products = signal<WarehouseStock[]>([]); 
  transactions = signal<any[]>([]);
  reorderHistory = signal<any[]>([]); 
  alerts: Alert[] = [];
  warehouseId: number = 1;

  searchQuery = signal<string>('');
  selectedCategory = signal<string>('');
  selectedStatus = signal<string>('ALL');
  selectedVendorStatus = signal<string>('ALL');
  selectedTransactionType = signal<string>('ALL');
  warehouseStocks: any[] = []; 
  chart: any;
  
  filteredReorderHistory = computed(() => {
    let list = this.reorderHistory();
    if (this.selectedVendorStatus() !== 'ALL') {
      list = list.filter(req => req.status === this.selectedVendorStatus());
    }
    return list;
  });

  filteredTransactions = computed(() => {
    let list = this.transactions();
    if (this.selectedTransactionType() !== 'ALL') {
      list = list.filter(t => t.type === this.selectedTransactionType());
    }
    return list;
  });
  filteredProducts = computed(() => {
    let list = this.products();
    if (this.searchQuery()) {
      const q = this.searchQuery().toLowerCase();
      list = list.filter(item => 
        item.product.name.toLowerCase().includes(q) || 
        item.product.sku.toLowerCase().includes(q)
      );
    }
    if (this.selectedCategory()) {
      list = list.filter(item => item.product.category === this.selectedCategory());
    }
    if (this.selectedStatus() !== 'ALL') {
      list = list.filter(item => {
        const isLow = item.currentStock <= item.reorderLevel && item.currentStock > 0;
        const isOut = item.currentStock <= 0;
        const isNormal = item.currentStock > item.reorderLevel;
        if (this.selectedStatus() === 'LOW') return isLow;
        if (this.selectedStatus() === 'OUT') return isOut;
        if (this.selectedStatus() === 'NORMAL') return isNormal;
        return true;
      });
    }
    return list;
  });

  categories = computed(() => {
    const cats = this.products().map(item => item.product.category);
    return [...new Set(cats)];
  });

  selectedProductId: any = null; 
  qty: number = 0;
  type: string = 'IN';
  reason: string = '';
  activeTab: string = 'inventory'; 
  unreadCount: number = 0;
  isNotificationOpen: boolean = false;
  cdr: any;
  stockService: any;

  constructor() {
    this.userEmail = this.authService.getUserEmail() || 'Manager';
    this.userRole = this.authService.getUserRole() || 'MANAGER';
    this.loggedUserWarehouseId = this.authService.getWarehouseId();
  }

  ngOnInit() {
    this.fetchData();
    this.loadAlerts();
    if (this.activeTab === 'inventory') {
            setTimeout(() => {
            this.prepareChartData();
        }, 50);
    }
  }

loadAlerts() {
  const idToUse = this.loggedUserWarehouseId || this.warehouseId; 
  this.alertService.getManagerAlerts(idToUse).subscribe({
    next: (data) => {
      this.alerts = data;
      this.unreadCount = this.alerts.filter(a => !a.isRead).length; 
    },
    error: (err) => console.error('Error fetching alerts', err)
  });
}

  markAsRead(id: number) {
    this.alertService.markAsRead(id).subscribe({
      next: () => {
        const alert = this.alerts.find(a => a.id === id);
        if (alert) {
          alert.isRead = true; 
        }

        this.alerts = this.alerts.filter(a => a.id !== id);
        this.unreadCount = this.alerts.length;

        this.cdr.detectChanges();
      },
      error: (err) => console.error("Could not update alert", err)
    });
  }

  fetchData() {
    const token = localStorage.getItem('token'); 
    if (!token) {
      alert("No token found! Please log in as MANAGER.");
      return;
    }

    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    
    this.http.get<WarehouseStock[]>('https://smartshelfx-backend.onrender.com/api/stock/inventory', { headers }).subscribe({
      next: (data) => this.products.set(data),
      error: (err) => {
        if(err.status === 403) alert("Security Error: MANAGER role required.");
      }
    });

    this.http.get<any[]>('https://smartshelfx-backend.onrender.com/api/stock/transactions', { headers }).subscribe({
      next: (data) => {
        const sorted = data.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
        this.transactions.set(sorted);
      }
    });

    this.http.get<any[]>('https://smartshelfx-backend.onrender.com/api/stock/reorder-history', { headers }).subscribe({
      next: (data) => {
        const sorted = data.sort((a, b) => new Date(b.requestDate).getTime() - new Date(a.requestDate).getTime());
        this.reorderHistory.set(sorted);
      }
    });
  }

  submitUpdate() {
    if (this.selectedProductId === null || this.selectedProductId === 'null' || this.qty <= 0) {
      alert("Please select a valid product and enter a quantity greater than 0.");
      return;
    }

    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });

    const payload = {
      productId: Number(this.selectedProductId), 
      quantity: Number(this.qty),
      type: this.type,
      reason: this.reason || "Manual Update"
    };

    this.http.post('https://smartshelfx-backend.onrender.com/api/stock/update', payload, { headers }).subscribe({
      next: () => {
        alert('Stock Updated Successfully!');
        this.fetchData(); 
        this.qty = 0;
        this.reason = '';
      },
      error: (err) => alert('Update failed: ' + (err.error?.message || err.message))
    });
  }

  sendVendorReorderRequest(item: any) {
    const qtyInput = prompt(`How many units of ${item.product.name} would you like to request?`, "50");
    if (qtyInput === null) return;
    
    const qty = Number(qtyInput);
    if (isNaN(qty) || qty <= 0) {
      alert("Please enter a valid quantity.");
      return;
    }

    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    
    const payload = {
      productId: item.product.id,
      quantity: qty,
      notes: `Purchase request sent to vendor for ${item.product.name}`
    };

    this.http.post('https://smartshelfx-backend.onrender.com/api/stock/reorder', payload, { headers }).subscribe({
      next: () => {
        alert('Purchase order sent successfully!');
        this.fetchData();
      },
      error: (err) => alert('Vendor request failed: ' + err.message)
    });
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    
    if (file) {
      this.selectedFile = file;

      const token = localStorage.getItem('token');
      const headers = new HttpHeaders({ 
        'Authorization': `Bearer ${token}` 
      });
      
      const formData = new FormData();
      formData.append('file', file);

      this.http.post('https://smartshelfx-backend.onrender.com/api/stock/import-csv', formData, { headers }).subscribe({
        next: () => {
          alert('Batch Stock updated successfully!');
          this.selectedFile = null; 
          this.fetchData();
        },
        error: (err) => {
          console.error('Upload failed', err);
          this.selectedFile = null;
          const errorMsg = err.error?.message || 'Ensure CSV format: SKU, Quantity, Type, Reason';
          alert('Failed to update stock: ' + errorMsg);
        }
      });
    }
}

  predictions = signal<AiPrediction[]>([]);

setActiveTab(tabName: string) {
  this.activeTab = tabName;
  if (tabName === 'ai-demand') {
    this.fetchAiDemands();
  }
}


fetchAiDemands() {
  const token = localStorage.getItem('token'); 
  if (!token) return;

  const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

  this.http.get<any[]>('https://smartshelfx-backend.onrender.com/api/predictions/demand-data', { headers })
    .subscribe({
      next: (data) => {
        console.log("Manager Data UI Update:", data);
        this.predictions.set(data);
      },
      error: (err) => console.error("Fetch Error:", err)
    });
}
  // 3. The Reorder Function
quickReorder(pred: any) {
  // Use 'productId' OR 'id' OR 'product_id' (whichever exists)
  const idToSend = pred.productId || pred.id || pred.product_id;

  if (!idToSend) {
    console.error("Data received from AI:", pred);
    alert("CRITICAL ERROR: This prediction has no ID. Check Flask output.");
    return;
  }

  const token = localStorage.getItem('token');
  const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  
  const payload = {
    productId: Number(idToSend), 
    quantity: Math.ceil(pred.predictedDemand),
    notes: `AI Suggested Replenishment for ${pred.productName}`
  };

  // We use the same endpoint as sendVendorReorderRequest to keep history consistent
  this.http.post('https://smartshelfx-backend.onrender.com/api/stock/reorder', payload, { headers })
    .subscribe({
      next: () => {
        alert(`✅ Reorder request for ${pred.productName} submitted!`);
        // Refresh the reorder history tab data automatically
        this.fetchData(); 
      },
      error: (err) => {
        console.error("Payload sent to server:", payload);
        alert('Reorder failed: ' + (err.error?.message || err.message));
      }
    });
}

toggleDropdown() {
    this.isNotificationOpen = !this.isNotificationOpen;
}

openReorderModal(pred: any) {
  const dialogRef = this.dialog.open(ReorderModalComponent, {
    width: '450px',
    data: {
      productName: pred.productName, 
      unitPrice: pred.unitPrice || 0,
      aiSuggestedQty: pred.predictedDemand,
      quantity: pred.predictedDemand,
      productId: pred.productId,
      vendorId: pred.vendorId,
      sku: pred.sku,
      // FIX: Use the variable defined in your class
      warehouseId: this.loggedUserWarehouseId 
    }
  });

  dialogRef.afterClosed().subscribe((result: any) => {
    if (result) {
      const stockRequestPayload = {
        product: { id: pred.id ||pred.productId}, 
        requestedQuantity: Math.round(result.quantity),
        // FIX: Use the variable defined in your class
        warehouseId: this.loggedUserWarehouseId, 
        status: 'PENDING',
        requestDate: new Date().toISOString(),
      };

      console.log("Creating Stock Request for Warehouse:", stockRequestPayload.warehouseId);

      // Ensure this endpoint exists in your VendorController.java
      this.http.post('https://smartshelfx-backend.onrender.com/api/vendor/reorders', stockRequestPayload).subscribe({
        next: (res) => {
          alert(`Success! Reorder request sent for Warehouse #${stockRequestPayload.warehouseId}`);
          this.fetchData(); // Refresh history
        },
        error: (err) => {
          console.error("Order failed!", err);
          alert("Could not send request. Check backend logs.");
        }
      });
    }
  });
}
onConfirm(result: any) {
    // This matches the 'sendPO' method in the service above
    this.poService.sendPO(result).subscribe({
      next: (res) => console.log('PO Saved:', res),
      error: (err) => console.error('Error:', err)
    });
  }
  onLogout() { this.authService.logout(); }
  triggerAI() { alert("AI Analysis triggered. Connecting to Python service..."); }
  setTab(tabName: string) { this.activeTab = tabName; }

loadWarehouseData() {
  // Now this.stockService will be defined
  this.stockService.getStocksByWarehouse(this.loggedUserWarehouseId).subscribe((data: WarehouseStock[]) => {
    this.warehouseStocks = data;
    if (this.activeTab === 'inventory') {
        setTimeout(() => this.prepareChartData(), 100);
      }
  });
}
  managerWarehouseId(managerWarehouseId: any) {
    throw new Error('Method not implemented.');
  }

prepareChartData() {
    const products = this.products(); 
    if (!products || products.length === 0) return;

    const lowStock = products.filter(item => item.currentStock <= item.reorderLevel).length;
    const healthy = products.filter(item => item.currentStock > item.reorderLevel).length;

    if (this.chart) {
        this.chart.destroy();
    }

  
    this.chart = new Chart('inventoryPieChart', {
        type: 'pie',
        data: {
            labels: ['Low Stock Alerts', 'Healthy Stock'],
            datasets: [{
                data: [lowStock, healthy],
                backgroundColor: ['#c78b14', '#198754'], // Matches your UI Warning/Success colors
                hoverOffset: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom' }
            }
        }
    });
}
}