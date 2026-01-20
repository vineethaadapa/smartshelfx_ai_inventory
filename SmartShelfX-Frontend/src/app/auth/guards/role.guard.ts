import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../services/auth/AuthService';
export const roleGuard = (expectedRoles: string[]): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const userRole = authService.getUserRole();

    if (authService.isLoggedIn() && expectedRoles.includes(userRole!)) {
      return true;
    }

    alert('Access Denied: You do not have permission for this page.');
    router.navigate(['/login']);
    return false;
  };
};