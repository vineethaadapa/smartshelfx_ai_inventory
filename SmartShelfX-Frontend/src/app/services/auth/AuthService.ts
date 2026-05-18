import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';

interface LoginRequest { email: string; password: string; }
interface RegisterRequest { name: string; email: string; password: string; role: string; }
interface AuthResponse { token: string; email: string; message: string; }

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'https://smartshelfx-backend.onrender.com/api/auth'; 

  constructor(private http: HttpClient) {}

  register(credentials: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, credentials);
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('email', credentials.email);
        }
      })
    );
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }
  getUserEmail(): string | null {
    return localStorage.getItem('email');
  }

  getUserRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const decoded: any = jwtDecode(token);
      return decoded.role;
    } catch (error) {
      return null;
    }
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const decoded: any = jwtDecode(token);
      return Date.now() < decoded.exp * 1000;
    } catch {
      return false;
    }
  }
  
  logout(): void {
    localStorage.clear(); 
    window.location.href = '/login'; 
  }

getWarehouseId(): number | null {
  const token = this.getToken();
  if (!token) return null;
  try {
    const decoded: any = jwtDecode(token);
    console.log("Decoded Token Payload:", decoded); 
    return decoded.warehouseId || null; 
  } catch (error) {
    console.error("Token decoding failed", error);
    return null;
  }
}
getVendorIdFromToken(): number | null {
  const token = localStorage.getItem('token');
  if (!token) return null;

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    console.log("Full Token Payload:", payload); 
    return payload.id || payload.userId || payload.vendorId || null;
  } catch (e) {
    return null;
  }
}
}