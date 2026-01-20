import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VendorPerformance {
  vendorName: string;
  totalRequests: number;
  approvedRequests: number;
  fulfillmentRate: number;
  avgResponseTimeHours: number;
}

@Injectable({ providedIn: 'root' })
export class VendorPerformanceService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/admin/vendor-performance';

  getPerformanceReport(): Observable<VendorPerformance[]> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    return this.http.get<VendorPerformance[]>(this.apiUrl, { headers });
  }
}