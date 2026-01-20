import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Alert {
  id: number;
  userRole: string;
  type: string;
  priority: string;
  message: string;
  productSku: string;
  isRead: boolean;
  createdAt: string;
  minThreshold: number; // Add this line!
  warehouseId: number;
  status: string;
  productName: string;   // Add this
  currentStock: number;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/alerts';
  getAlertsForVendor(vendorId: number): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.API_URL}/vendor/${vendorId}`);
  }


  dismissAlert(id: number): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${id}/dismiss`, {});
  }

  getAlerts(): Observable<Alert[]> {
  return this.http.get<Alert[]>(`${this.API_URL}/all`);
}
getUnreadAlerts(role: string): Observable<Alert[]> {
  const token = localStorage.getItem('token');
  const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });

  return this.http.get<Alert[]>(`${this.API_URL}/unread/${role}`, { headers });
}

getAlertsByRole(role: string): Observable<Alert[]> {

  const token = localStorage.getItem('token');
  const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  
  return this.http.get<Alert[]>(`${this.API_URL}/role/${role}`, { headers });
}

markAsRead(id: number): Observable<any> {
  const token = localStorage.getItem('token');
  const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  return this.http.put(`${this.API_URL}/mark-read/${id}`, {}, { headers });
}

getManagerAlerts(warehouseId: number): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.API_URL}/manager/${warehouseId}`);
  }
}