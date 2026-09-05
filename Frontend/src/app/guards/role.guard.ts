import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const roleGuard = (rolesPermitidos: string[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notification = inject(NotificationService);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  if (rolesPermitidos.includes(authService.getRole())) {
    return true;
  }

  notification.error('No tienes permisos para acceder a esta sección.', 'Acceso Denegado (403)');
  router.navigate(['/dashboard']);
  return false;
};
