
import { Routes } from '@angular/router';
import { ManagerDashboardComponent } from './auth/dashboards/manager-dashboard/manager-dashboard';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'manager-dashboard', component: ManagerDashboardComponent
  },

  
];