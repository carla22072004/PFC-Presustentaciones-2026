import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import { LoginComponent } from './components/auth/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';

// Todas las rutas hijas de /dashboard se cargan con loadComponent (lazy):
// solo Login y Dashboard (el shell inicial) van en el bundle principal.
export const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: LoginComponent },
    {
        path: 'dashboard',
        component: DashboardComponent,
        canActivate: [authGuard],
        children: [
            // ── Perfil ──────────────────────────────────────────────────────
            {
                path: 'perfil',
                loadComponent: () => import('./components/perfil/perfil.component').then(m => m.PerfilComponent),
                canActivate: [authGuard]
            },
            {
                path: 'solicitudes/registrar',
                loadComponent: () => import('./components/solicitudes/registrar-solicitudes/registrar-solicitud.component').then(m => m.RegistrarSolicitudComponent),
                canActivate: [authGuard]
            },
            {
                path: 'solicitudes/mis-tramites',
                loadComponent: () => import('./components/solicitudes/listar-solicitudes/listar-solicitudes.component').then(m => m.ListarSolicitudesComponent),
                canActivate: [authGuard]
            },
            {
                path: 'solicitudes/cargar-pdf/:id',
                loadComponent: () => import('./components/solicitudes/cargar-anteproyecto/cargar-anteproyecto.component').then(m => m.CargarAnteproyectoComponent),
                canActivate: [authGuard]
            },
            {
                path: 'solicitudes/ver-observaciones/:id',
                loadComponent: () => import('./components/solicitudes/ver-observaciones/ver-observaciones.component').then(m => m.VerObservacionesComponent),
                canActivate: [authGuard]
            },
            {
                path: 'notas',
                loadComponent: () => import('./components/notas/mis-notas.component').then(m => m.MisNotasComponent),
                canActivate: [authGuard]
            },
            {
                path: 'horario',
                loadComponent: () => import('./components/horario/mi-horario.component').then(m => m.MiHorarioComponent),
                canActivate: [authGuard]
            },
            {
                path: 'notificaciones',
                loadComponent: () => import('./components/notificaciones/notificaciones.component').then(m => m.NotificacionesComponent),
                canActivate: [authGuard]
            },
            // ── Administrador del sistema (ADMIN) ────────────────────────────
            {
                path: 'admin/usuarios',
                loadComponent: () => import('./components/admin/gestion-usuarios/gestion-usuarios.component').then(m => m.GestionUsuariosComponent),
                canActivate: [roleGuard(['ADMIN'])]
            },
            // ── Coordinador (COORDINADOR) ─────────────────────────────────────
            {
                path: 'admin/revisar-solicitudes',
                loadComponent: () => import('./components/admin/revisar-solicitudes/revisar-solicitudes.component').then(m => m.RevisarSolicitudesComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR'])]
            },
            {
                path: 'admin/cronograma/:id',
                loadComponent: () => import('./components/admin/cronograma/programar-cronograma.component').then(m => m.ProgramarCronogramaComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR'])]
            },
            {
                path: 'admin/asignar-jurados/:id',
                loadComponent: () => import('./components/admin/asignar-jurados/asignar-jurados.component').then(m => m.AsignarJuradosComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR'])]
            },
            {
                path: 'admin/evaluar/:id',
                loadComponent: () => import('./components/admin/evaluacion/evaluar-solicitud.component').then(m => m.EvaluarSolicitudComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR'])]
            },
            // ★ Nueva ruta: vista de solo lectura de la rúbrica para el coordinador
            {
                path: 'admin/ver-rubrica/:id',
                loadComponent: () => import('./components/admin/ver-rubrica-tribunal/ver-rubrica-tribunal.component').then(m => m.VerRubricaTribunalComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR'])]
            },
            // ── Jurado / Docente ────────────────────────────────────────────
            {
                path: 'jurado/mis-asignaciones',
                loadComponent: () => import('./components/jurado/mis-asignaciones.component').then(m => m.MisAsignacionesComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR', 'DOCENTE'])]
            },
            {
                path: 'jurado/evaluar-rubrica/:id',
                loadComponent: () => import('./components/jurado/evaluar-rubrica/evaluar-rubrica.component').then(m => m.EvaluarRubricaComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR', 'DOCENTE'])]
            },
            {
                path: 'docente/anteproyecto/:id',
                loadComponent: () => import('./components/docente/ver-anteproyecto/ver-anteproyecto.component').then(m => m.VerAnteproyectoComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR', 'DOCENTE'])]
            },
            {
                path: 'docente/firmar-acta/:id',
                loadComponent: () => import('./components/docente/firmar-acta/firmar-acta-docente.component').then(m => m.FirmarActaDocenteComponent),
                canActivate: [authGuard, roleGuard(['ADMIN', 'COORDINADOR', 'DOCENTE'])]
            },
            // ── Tutorías ────────────────────────────────────────────────────
            {
                path: 'tutorias/mis-tutorias',
                loadComponent: () => import('./components/tutorias/mis-tutorias/mis-tutorias.component').then(m => m.MisTutoriasComponent),
                canActivate: [authGuard]
            },
            {
                path: 'tutorias/detalle/:id',
                loadComponent: () => import('./components/tutorias/detalle-tutoria/detalle-tutoria.component').then(m => m.DetalleTutoriaComponent),
                canActivate: [authGuard]
            },
        ]
    },
    { path: '**', redirectTo: 'login' }
];
