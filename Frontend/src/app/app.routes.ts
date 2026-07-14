import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { LoginComponent } from './components/auth/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { RegistrarSolicitudComponent } from './components/solicitudes/registrar-solicitudes/registrar-solicitud.component';
import { ListarSolicitudesComponent } from './components/solicitudes/listar-solicitudes/listar-solicitudes.component';
import { CargarAnteproyectoComponent } from './components/solicitudes/cargar-anteproyecto/cargar-anteproyecto.component';
import { MisNotasComponent } from './components/notas/mis-notas.component';
import { MiHorarioComponent } from './components/horario/mi-horario.component';
import { NotificacionesComponent } from './components/notificaciones/notificaciones.component';
import { RevisarSolicitudesComponent } from './components/admin/revisar-solicitudes/revisar-solicitudes.component';
import { ProgramarCronogramaComponent } from './components/admin/cronograma/programar-cronograma.component';
import { EvaluarSolicitudComponent } from './components/admin/evaluacion/evaluar-solicitud.component';
import { AsignarJuradosComponent } from './components/admin/asignar-jurados/asignar-jurados.component';
import { VerRubricaTribunalComponent } from './components/admin/ver-rubrica-tribunal/ver-rubrica-tribunal.component';
import { VerAnteproyectoComponent } from './components/docente/ver-anteproyecto/ver-anteproyecto.component';
import { MisAsignacionesComponent } from './components/jurado/mis-asignaciones.component';
import { FirmarActaDocenteComponent } from './components/docente/firmar-acta/firmar-acta-docente.component';
import { EvaluarRubricaComponent } from './components/jurado/evaluar-rubrica/evaluar-rubrica.component';
import { VerObservacionesComponent } from './components/solicitudes/ver-observaciones/ver-observaciones.component';

import { PerfilComponent } from './components/perfil/perfil.component';
import { MisTutoriasComponent } from './components/tutorias/mis-tutorias/mis-tutorias.component';
import { DetalleTutoriaComponent } from './components/tutorias/detalle-tutoria/detalle-tutoria.component';

export const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: LoginComponent },
    {
        path: 'dashboard',
        component: DashboardComponent,
        canActivate: [authGuard],
        children: [
            // ── Perfil ──────────────────────────────────────────────────────
            { path: 'perfil',                        component: PerfilComponent,               canActivate: [authGuard] },
            { path: 'solicitudes/registrar',         component: RegistrarSolicitudComponent,   canActivate: [authGuard] },
            { path: 'solicitudes/mis-tramites',       component: ListarSolicitudesComponent,    canActivate: [authGuard] },
            { path: 'solicitudes/cargar-pdf/:id',     component: CargarAnteproyectoComponent,   canActivate: [authGuard] },
            { path: 'solicitudes/ver-observaciones/:id', component: VerObservacionesComponent,    canActivate: [authGuard] },
            { path: 'notas',                          component: MisNotasComponent,             canActivate: [authGuard] },
            { path: 'horario',                        component: MiHorarioComponent,            canActivate: [authGuard] },
            { path: 'notificaciones',                 component: NotificacionesComponent,       canActivate: [authGuard] },
            // ── Coordinador (ADMIN) ─────────────────────────────────────────
            { path: 'admin/revisar-solicitudes',      component: RevisarSolicitudesComponent,   canActivate: [authGuard] },
            { path: 'admin/cronograma/:id',           component: ProgramarCronogramaComponent,  canActivate: [authGuard] },
            { path: 'admin/asignar-jurados/:id',      component: AsignarJuradosComponent,       canActivate: [authGuard] },
            { path: 'admin/evaluar/:id',              component: EvaluarSolicitudComponent,     canActivate: [authGuard] },
            // ★ Nueva ruta: vista de solo lectura de la rúbrica para el coordinador
            { path: 'admin/ver-rubrica/:id',          component: VerRubricaTribunalComponent,   canActivate: [authGuard] },
            // ── Jurado / Docente ────────────────────────────────────────────
            { path: 'jurado/mis-asignaciones',        component: MisAsignacionesComponent,      canActivate: [authGuard] },
            { path: 'jurado/evaluar-rubrica/:id',     component: EvaluarRubricaComponent,       canActivate: [authGuard] },
            { path: 'docente/anteproyecto/:id',       component: VerAnteproyectoComponent,      canActivate: [authGuard] },
            { path: 'docente/firmar-acta/:id',        component: FirmarActaDocenteComponent,    canActivate: [authGuard] },
            // ── Tutorías ────────────────────────────────────────────────────
            { path: 'tutorias/mis-tutorias',          component: MisTutoriasComponent,          canActivate: [authGuard] },
            { path: 'tutorias/detalle/:id',           component: DetalleTutoriaComponent,       canActivate: [authGuard] },
        ]
    },
    { path: '**', redirectTo: 'login' }
];