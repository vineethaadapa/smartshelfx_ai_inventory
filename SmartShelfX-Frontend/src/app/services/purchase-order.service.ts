import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PurchaseOrderService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/orders';
sendPO(orderData: any): Observable<any> {
  return this.http.post('http://localhost:8080/api/orders/generate', orderData);
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