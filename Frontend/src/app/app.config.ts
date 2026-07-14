import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http'; // Importación corregida
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor'; // Importamos tu interceptor

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])) // Aquí configuramos el uso del token JWT
  ]
};
