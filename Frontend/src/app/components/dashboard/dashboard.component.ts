import { Component, ViewEncapsulation, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { NotificacionService } from '../../services/notificacion.service';
import { filter, Subscription } from 'rxjs';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
    userName = '';
    userRole = '';
    isMenuOpen = false;
    urlActual = '';
    notiBadge = 0;
    darkMode = false;
    mostrarAlertaEmail = false;
    private sub!: Subscription;
    private badgeSub!: Subscription;

    // ── MÓDULOS POR ROL ─────────────────────────────────────────────────────

    modulosEstudiante = [
        { titulo: 'Mis Trámites',        icon: 'bi-journal-text',   image: 'img/MisTramites.png',    route: '/dashboard/solicitudes/mis-tramites',  desc: 'Seguimiento de tus solicitudes de pre-sustentación' },
        { titulo: 'Nueva Solicitud',     icon: 'bi-plus-circle',    image: 'img/NuevaSolicitud.png', route: '/dashboard/solicitudes/registrar',     desc: 'Crear una nueva solicitud de pre-sustentación' },
        { titulo: 'Mis Notas',           icon: 'bi-mortarboard',    image: 'img/MisNotas.png',       route: '/dashboard/notas',                     desc: 'Consulta tus calificaciones y resultados' },
        { titulo: 'Centro de Orientación', icon: 'bi-compass',       image: 'img/MisTramites.png',    route: '/dashboard/orientacion/centro',        desc: 'Explora ideas de tema de titulación y guarda las que te interesen' },
        { titulo: 'Mi Horario',          icon: 'bi-calendar3',      image: 'img/MiHorario.png',      route: '/dashboard/horario',                   desc: 'Fecha y hora de tu pre-sustentación' },
        { titulo: 'Tutorías',            icon: 'bi-journal-bookmark-fill', image: 'img/MisTramites.png', route: '/dashboard/tutorias/mis-tutorias', desc: 'Seguimiento de las fases de tutoría de tu anteproyecto' },
        { titulo: 'Notificaciones',      icon: 'bi-bell',           image: 'img/MisNotas.png',       route: '/dashboard/notificaciones',            desc: 'Mensajes y alertas del sistema' },
    ];

    // El COORDINADOR gestiona TODO el flujo: revisa → asigna tribunal → programa → evalúa
    modulosCoordinador = [
        { titulo: 'Gestionar Solicitudes', icon: 'bi-clipboard2-check', image: 'img/MisTramites.png', route: '/dashboard/admin/revisar-solicitudes',
            desc: 'Aprobar, rechazar y hacer seguimiento de todas las solicitudes' },
        { titulo: 'Gestionar Estudiantes', icon: 'bi-mortarboard-fill', image: 'img/MisTramites.png', route: '/dashboard/admin/estudiantes',
            desc: 'Registrar estudiantes y administrar carrera, semestre y estado académico' },
        { titulo: 'Centro de Orientación', icon: 'bi-compass', image: 'img/MisTramites.png', route: '/dashboard/orientacion/centro',
            desc: 'Consultar el catálogo de temas de titulación propuestos' },
        { titulo: 'Gestionar Temas Propuestos', icon: 'bi-journal-plus', image: 'img/MisTramites.png', route: '/dashboard/orientacion/gestionar-temas',
            desc: 'Crear, editar y eliminar los temas del catálogo de orientación' },
        { titulo: 'Gestionar Recursos de Titulación', icon: 'bi-folder2-open', image: 'img/MisTramites.png', route: '/dashboard/orientacion/gestionar-recursos',
            desc: 'Crear, editar y eliminar guías, plantillas y reglamentos de titulación' },
        { titulo: 'Reportes',              icon: 'bi-graph-up-arrow',   image: 'img/MisNotas.png',    route: '/dashboard/reportes',
            desc: 'Solicitudes por estado, sustentaciones por período, actividad por docente y por carrera' },
        { titulo: 'Actas',                 icon: 'bi-folder2-open',     image: 'img/MisTramites.png', route: '/dashboard/actas/gestion',
            desc: 'Consultar, buscar y cambiar el estado de las actas; ver su historial' },
        { titulo: 'Notificaciones',        icon: 'bi-bell',             image: 'img/MisNotas.png',    route: '/dashboard/notificaciones',
            desc: 'Mensajes y alertas del sistema' },
    ];

    // El ADMIN es el administrador del sistema: controla usuarios y roles, no el flujo de solicitudes
    modulosAdministrador = [
        { titulo: 'Gestionar Usuarios y Roles', icon: 'bi-people-fill', image: 'img/MisTramites.png', route: '/dashboard/admin/usuarios',
            desc: 'Crear, activar, desactivar y asignar roles a los usuarios del sistema' },
        { titulo: 'Gestionar Roles',            icon: 'bi-shield-lock-fill', image: 'img/MisTramites.png', route: '/dashboard/admin/roles',
            desc: 'Crear, renombrar y eliminar los roles del sistema' },
        { titulo: 'Gestionar Permisos',         icon: 'bi-key-fill',    image: 'img/MisTramites.png', route: '/dashboard/admin/permisos',
            desc: 'Asignar qué puede hacer cada rol dentro del sistema' },
        { titulo: 'Auditoría',                  icon: 'bi-clock-history', image: 'img/MisTramites.png', route: '/dashboard/admin/auditoria',
            desc: 'Quién creó, modificó, aprobó o eliminó información, y cuándo' },
        { titulo: 'Gestionar Estudiantes',       icon: 'bi-mortarboard-fill', image: 'img/MisTramites.png', route: '/dashboard/admin/estudiantes',
            desc: 'Registrar estudiantes y administrar carrera, semestre y estado académico' },
        { titulo: 'Gestión de Carreras',         icon: 'bi-mortarboard',  image: 'img/MisTramites.png', route: '/dashboard/admin/carreras',
            desc: 'Administrar facultades, carreras, modalidades de titulación y períodos académicos' },
        { titulo: 'Centro de Orientación',       icon: 'bi-compass',      image: 'img/MisTramites.png', route: '/dashboard/orientacion/centro',
            desc: 'Consultar el catálogo de temas de titulación propuestos' },
        { titulo: 'Gestionar Temas Propuestos',  icon: 'bi-journal-plus', image: 'img/MisTramites.png', route: '/dashboard/orientacion/gestionar-temas',
            desc: 'Crear, editar y eliminar los temas del catálogo de orientación' },
        { titulo: 'Gestionar Recursos de Titulación', icon: 'bi-folder2-open', image: 'img/MisTramites.png', route: '/dashboard/orientacion/gestionar-recursos',
            desc: 'Crear, editar y eliminar guías, plantillas y reglamentos de titulación' },
        { titulo: 'Reportes',                    icon: 'bi-graph-up-arrow', image: 'img/MisNotas.png',  route: '/dashboard/reportes',
            desc: 'Reportes generales del sistema y de las actas: solicitudes, períodos, docentes y carreras' },
        { titulo: 'Gestión de Actas',            icon: 'bi-folder2-open', image: 'img/MisTramites.png', route: '/dashboard/actas/gestion',
            desc: 'Listar, buscar, filtrar actas; cambiar su estado y consultar el historial completo' },
        { titulo: 'Notificaciones',             icon: 'bi-bell',        image: 'img/MisNotas.png',    route: '/dashboard/notificaciones',
            desc: 'Mensajes y alertas del sistema' },
    ];

    // El DOCENTE solo ve sus asignaciones como jurado/tutor y puede firmar actas
    modulosDocente = [
        { titulo: 'Mis Asignaciones',   icon: 'bi-person-badge',    image: 'img/MisTramites.png',  route: '/dashboard/jurado/mis-asignaciones',
            desc: 'Ver las pre-sustentaciones donde eres jurado o tutor' },
        { titulo: 'Mis Actas',          icon: 'bi-file-earmark-text', image: 'img/MisTramites.png', route: '/dashboard/actas/mis-actas',
            desc: 'Consultar el estado, el detalle y el historial de las actas de tus pre-sustentaciones' },
        { titulo: 'Mis Estudiantes',    icon: 'bi-mortarboard-fill', image: 'img/MisTramites.png', route: '/dashboard/docente/mis-estudiantes',
            desc: 'Consultar los estudiantes que tienes asignados como tutor' },
        { titulo: 'Tutorías',           icon: 'bi-journal-bookmark-fill', image: 'img/MisTramites.png', route: '/dashboard/tutorias/mis-tutorias',
            desc: 'Gestiona las fases de tutoría de los estudiantes asignados' },
        { titulo: 'Notificaciones',     icon: 'bi-bell',            image: 'img/MisNotas.png',     route: '/dashboard/notificaciones',
            desc: 'Mensajes y alertas del sistema' },
    ];

    private titulos: Record<string, string> = {
        '/dashboard/perfil':                      'Mi Perfil',
        '/dashboard/solicitudes/mis-tramites':   'Mis Trámites',
        '/dashboard/solicitudes/registrar':       'Nueva Solicitud',
        '/dashboard/solicitudes/cargar-pdf':     'Cargar Anteproyecto',
        '/dashboard/notas':                       'Mis Notas',
        '/dashboard/orientacion/centro':          'Centro de Orientación y Titulación',
        '/dashboard/orientacion/gestionar-temas': 'Gestionar Temas Propuestos',
        '/dashboard/orientacion/gestionar-recursos': 'Gestionar Recursos de Titulación',
        '/dashboard/horario':                     'Mi Horario',
        '/dashboard/notificaciones':              'Notificaciones',
        '/dashboard/reportes':                    'Reportes',
        '/dashboard/actas/mis-actas':            'Mis Actas',
        '/dashboard/actas/gestion':              'Gestión de Actas',
        '/dashboard/actas':                       'Historial del Acta',
        '/dashboard/admin/usuarios':               'Gestionar Usuarios y Roles',
        '/dashboard/admin/roles':                  'Gestionar Roles',
        '/dashboard/admin/permisos':                'Gestionar Permisos',
        '/dashboard/admin/auditoria':               'Auditoría',
        '/dashboard/admin/estudiantes':             'Gestionar Estudiantes',
        '/dashboard/admin/carreras':                 'Gestión de Carreras',
        '/dashboard/admin/revisar-solicitudes':  'Gestionar Solicitudes',
        '/dashboard/admin/cronograma':            'Programar Presentación',
        '/dashboard/admin/asignar-jurados':      'Asignar Tribunal y Tutor',
        '/dashboard/admin/evaluar':              'Registrar Evaluación',
        '/dashboard/jurado/mis-asignaciones':    'Mis Asignaciones como Jurado',
        '/dashboard/docente/mis-estudiantes':    'Mis Estudiantes',
        '/dashboard/docente/anteproyecto':       'Ver Anteproyecto',
        '/dashboard/docente/firmar-acta':        'Firmar Acta',
        '/dashboard/tutorias/mis-tutorias':      'Mis Tutorías',
        '/dashboard/tutorias/detalle':           'Detalle de Tutoría',
    };

    constructor(
        public router: Router,
        private authService: AuthService,
        private notiService: NotificacionService
    ) {
        // Set immediately so esInicio() works before ngOnInit
        this.urlActual = this.router.url;
    }

    ngOnInit(): void {
        this.userName = this.authService.getUserName();
        this.userRole = this.authService.getRole();
        this.urlActual = this.router.url;

        // Restaurar modo oscuro
        this.darkMode = localStorage.getItem('dark_mode') === 'true';
        this.applyDarkMode();

        // Mostrar alerta si no ha configurado correo de notificaciones
        this.mostrarAlertaEmail = !this.authService.hasEmailNotiConfigurado();

        this.sub = this.router.events
            .pipe(filter(e => e instanceof NavigationEnd))
            .subscribe((e: any) => { this.urlActual = e.urlAfterRedirects; });

        this.cargarBadgeNoti();
        // Suscribirse al badge reactivo — se actualiza automáticamente cuando se marcan leídas
        this.badgeSub = this.notiService.badge$.subscribe(n => this.notiBadge = n);
    }

    ngOnDestroy(): void { this.sub?.unsubscribe(); this.badgeSub?.unsubscribe(); }

    cargarBadgeNoti(): void {
        const uid = this.authService.getUserId();
        this.notiService.contarNoLeidas(uid).subscribe({
            next: r => this.notiBadge = r.total,
            error: () => {}
        });
    }

    get modulosPermitidos() {
        if (this.userRole === 'ESTUDIANTE')  return this.modulosEstudiante;
        if (this.userRole === 'DOCENTE')     return this.modulosDocente;
        if (this.userRole === 'ADMIN')       return this.modulosAdministrador;
        if (this.userRole === 'COORDINADOR') return this.modulosCoordinador;
        return [];
    }

    get rolLabel(): string {
        const map: Record<string, string> = {
            ADMIN: 'Administrador',
            COORDINADOR: 'Coordinador',
            DOCENTE: 'Docente / Jurado',
            ESTUDIANTE: 'Estudiante'
        };
        return map[this.userRole] || this.userRole;
    }

    get rolColor(): string {
        const map: Record<string, string> = {
            ADMIN: 'chip-admin',
            COORDINADOR: 'chip-coordinador',
            DOCENTE: 'chip-docente',
            ESTUDIANTE: 'chip-estudiante'
        };
        return map[this.userRole] || '';
    }

    esInicio(): boolean {
        return this.urlActual === '/dashboard' || this.urlActual === '/dashboard/';
    }

    tituloPagina(): string {
        const exacta = this.titulos[this.urlActual];
        if (exacta) return exacta;
        const parcial = Object.keys(this.titulos).find(k => this.urlActual.startsWith(k));
        return parcial ? this.titulos[parcial] : 'Detalle';
    }

    irAInicio(): void { this.router.navigate(['/dashboard']); }
    toggleMenu(e: Event): void { e.stopPropagation(); this.isMenuOpen = !this.isMenuOpen; }

    toggleDarkMode(): void {
        // Añadir clase de transición para animación suave
        document.documentElement.classList.add('theme-transitioning');
        this.darkMode = !this.darkMode;
        localStorage.setItem('dark_mode', String(this.darkMode));
        this.applyDarkMode();
        // Quitar la clase después de la transición
        setTimeout(() => {
            document.documentElement.classList.remove('theme-transitioning');
        }, 500);
    }

    applyDarkMode(): void {
        document.body.classList.toggle('dark-mode', this.darkMode);
    }

    cerrarAlertaEmail(): void { this.mostrarAlertaEmail = false; }

    irAPerfil(): void {
        this.mostrarAlertaEmail = false;
        this.router.navigate(['/dashboard/perfil']);
    }

    @HostListener('document:click')
    onDocumentClick(): void { this.isMenuOpen = false; }

    logout(): void { this.authService.logout(); }
}
