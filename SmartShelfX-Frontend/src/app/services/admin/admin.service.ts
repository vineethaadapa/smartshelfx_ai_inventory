import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';


export interface MonthlyComparison {
  month: string;
  purchases: number;
  sales: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  
  private apiUrl = 'http://localhost:8080/api/admin';

  constructor(private http: HttpClient) {}

  // Maps to @GetMapping("/products")
  getProducts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/products`);
  }

  // Maps to @PostMapping("/products")
  addProduct(product: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/products`, product);
  }

  // Maps to @DeleteMapping("/products/{id}")
  deleteProduct(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/products/${id}`);
  }

  // Maps to @GetMapping("/audit-logs")
  getAuditLogs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/audit-logs`);
  }

  // Maps to @GetMapping("/low-stock-alerts")
  getLowStockAlerts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/low-stock-alerts`);
  }

  getAllVendors() {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }
  
  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/categories`);
  }

  updateProduct(id: number, product: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/products/${id}`, product);
  }

  importCSV(formData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/products/import`, formData);
  }

  // Update this method to get ALL users
  getAllUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users-all`); 
  }

  updateUserRole(userId: number, role: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${userId}/role`, role);
  }

  importStockCsv(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file); // 'file' must match @RequestParam("file") in Java
    
    const token = localStorage.getItem('token');
    
    return this.http.post('http://localhost:8080/api/stock/import-csv', formData, {
      headers: { 
        'Authorization': `Bearer ${token}`
        // Note: Do NOT add 'Content-Type'. The browser handles it for FormData automatically.
      }
    });
  }

  /**
   * Fetches all reorder requests (Pending, Approved, Rejected) from all warehouses
   */
  getReorderRequests(): Observable<any[]> {
    const token = localStorage.getItem('token');
    // Use this.apiUrl to match your existing variable
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    return this.http.get<any[]>(`${this.apiUrl}/reorder-requests`, { headers });
  }

  /**
   * Updates the status of a specific request
   */
  updateReorderStatus(requestId: number, status: string): Observable<any> {
    const token = localStorage.getItem('token');
    // Use this.apiUrl to match your existing variable
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    return this.http.post(`${this.apiUrl}/reorder-requests/${requestId}/status?status=${status}`, {}, { headers });
  }

// Inside AdminService class

// 1. Method to assign a warehouse
assignWarehouse(userId: number, warehouseId: number): Observable<any> {
  const token = localStorage.getItem('token');
  
  // Create headers and ensure they are attached
  const headers = new HttpHeaders({ 
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json' 
  });

  return this.http.put(
    `${this.apiUrl}/assign-warehouse/${userId}/${warehouseId}`, 
    {}, // Empty body
    { headers }
  );
}

// 2. Method to get global health stats
getOverallHealth(): Observable<any> {
  const token = localStorage.getItem('token');
  const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  return this.http.get<any>(`http://localhost:8080/api/admin/overall-health`, { headers });
}

// Add this to your AdminService class
getProductForecast(payload: { product_name: string, current_stock: number }): Observable<any> {
  // If you are calling Python directly:
  return this.http.post('http://localhost:5000/api/forecast', payload);
  
  // OR if you are routing through your Spring Boot backend (Recommended):
  // return this.http.post(`${this.apiUrl}/forecast`, payload);
}

getDemandPredictions(): Observable<any[]> {
    return this.http.get<any[]>('http://localhost:8080/api/predictions/demand-data');
}

downloadInventoryPdf() {
  return this.http.get(`${this.apiUrl}/export-pdf`, {
    responseType: 'blob'
  });
}

getInventoryTrends(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/inventory-trends`);
  }

getMonthlyComparison(): Observable<MonthlyComparison[]> {
    return this.http.get<MonthlyComparison[]>(`${this.apiUrl}/monthly-comparison`);
  }
}