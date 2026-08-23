import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, map, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('presus_token');
  const router = inject(Router);
  const notification = inject(NotificationService);
  let url = req.url;

  // 1. Versionado dinámico y centralizado: cambiar /api/ a /api/v1/ si no está ya versionado
  if (url.includes('/api/') && !url.includes('/api/v1/')) {
    url = url.replace('/api/', '/api/v1/');
  }

  // 2. En producción (cuando la aplicación corre servida por Nginx en puerto 80),
  // removemos la URL base 'http://localhost:8080' para utilizar llamadas relativas
  if (window.location.port !== '4200' && url.startsWith('http://localhost:8080')) {
    url = url.replace('http://localhost:8080', '');
  }

  // Clonar la petición con la nueva URL y el header de autenticación
  const clonedReq = req.clone({
    url,
    setHeaders: token ? { Authorization: `Bearer ${token}` } : {}
  });

  return next(clonedReq).pipe(
    map(event => {
      if (event instanceof HttpResponse) {
        const body = event.body;
        
        // 3. Centralizar el desempaquetado de ResponseWrapper (Requisito ResponseWrapper)
        // Si el body es un ResponseWrapper (tiene campos 'success' y 'data')
        if (body && typeof body === 'object' && 'success' in body && 'data' in body) {
          
          // Caso especial: login (debe devolver body.data.auth para no romper la lógica actual del authService)
          if (url.endsWith('/auth/login') && body.data && typeof body.data === 'object' && 'auth' in body.data) {
            return event.clone({ body: body.data.auth });
          }
          
          // Caso general: devolver el contenido interno 'data' directamente
          return event.clone({ body: body.data });
        }
      }
      return event;
    }),
    catchError((err: unknown) => {
      // Sesión expirada o token inválido: en vez de dejar que cada componente
      // muestre su propio mensaje generico ("No se pudo cargar..."), se limpia
      // la sesión y se redirige al login con un aviso claro de por qué.
      if (err instanceof HttpErrorResponse && err.status === 401 && !url.endsWith('/auth/login')) {
        const habiaSesion = !!token;
        localStorage.removeItem('presus_token');
        localStorage.removeItem('user_id');
        localStorage.removeItem('user_role');
        localStorage.removeItem('user_name');
        localStorage.removeItem('email_noti_configurado');
        if (habiaSesion && !router.url.includes('/login')) {
          notification.error('Tu sesión expiró. Por favor, inicia sesión de nuevo.', 'Sesión expirada');
          router.navigate(['/login']);
        }
      }
      return throwError(() => err);
    })
  );
};
