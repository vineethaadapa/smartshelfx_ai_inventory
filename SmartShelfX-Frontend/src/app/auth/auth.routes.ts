

import { Routes } from '@angular/router';
import { WelcomePageComponent } from './welcome-page/welcome-page';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
// import { DashboardComponent } from './dashboard/dashboard';
import { roleGuard } from './guards/role.guard';
import { AdminDashboardComponent } from './dashboards/admin-dashboard/admin-dashboard';
import { ManagerDashboardComponent } from './dashboards/manager-dashboard/manager-dashboard';
import { VendorDashboardComponent } from './dashboards/vendor-dashboard/vendor-dashboard';


export const AUTH_ROUTES: Routes = [
  { 
    path: '', 
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent },
      { 
        path: 'admin-dashboard', 
        component: AdminDashboardComponent, 
        canActivate: [roleGuard(['ADMIN'])] 
      },
      { 
        path: 'manager-dashboard', 
        component: ManagerDashboardComponent, 
        canActivate: [roleGuard(['MANAGER'])] 
      },
      { 
        path: 'vendor-dashboard', 
        component: VendorDashboardComponent, 
        canActivate: [roleGuard(['VENDOR'])] 
      },

      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  }
];