import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http'; // Importación corregida
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor'; // Importamos tu interceptor

export const appConfig: ApplicationConfig = {
  providers: [
    // Angular 21 usa detección de cambios "zoneless" por defecto. Los componentes de este
    // proyecto asumen Zone.js (asignan campos dentro de callbacks de subscribe sin llamar a
    // ChangeDetectorRef), así que se activa explícitamente la detección de cambios con Zone.
    // Sin esto, pantallas como "Seguimiento" recibían los datos pero la vista se quedaba en
    // "Cargando..." porque la detección de cambios no se disparaba tras la respuesta HTTP.
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])) // Aquí configuramos el uso del token JWT
  ]
};
