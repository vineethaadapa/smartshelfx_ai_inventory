import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PurchaseOrderService {
  private http = inject(HttpClient);
  private apiUrl = 'https://smartshelfx-backend.onrender.com/api/orders';
sendPO(orderData: any): Observable<any> {
  return this.http.post('https://smartshelfx-backend.onrender.com/api/orders/generate', orderData);
}
  submitPurchaseOrder(orderData: any): Observable<any> {
    return this.sendPO(orderData);
  }
  getAllPurchaseOrders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/all`);
  }
  updateOrderStatus(id: number, status: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/status`, { status });
  }
}