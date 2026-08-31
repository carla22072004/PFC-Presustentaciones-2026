import { Component, ViewEncapsulation, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';
import { EstadoService } from '../../../services/estado.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-listar-solicitudes',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './listar-solicitudes.component.html',
    styleUrls: ['./listar-solicitudes.component.css']
})
export class ListarSolicitudesComponent implements OnInit, OnDestroy {
    private pollingSubscriptions: Subscription[] = [];
    solicitudes: any[] = [];
    cargando = true;
    modalObs: string | null = null;
    modalTitulo = '';

    // Paginación server-side para la vista de admin/docente/coordinador: con el volumen real
    // (44k+ solicitudes) el endpoint sin paginar devuelve ~93 MB y la página nunca renderiza.
    // El estudiante sigue viendo sus propias solicitudes sin paginar (son pocas).
    paginaActual = 0; // 0-indexed, como Spring Pageable
    tamanioPagina = 20;
    totalElementos = 0;
    totalPaginas = 0;

    constructor(
        private solicitudService: SolicitudService,
        private notification: NotificationService,
        public authService: AuthService,
        private estadoService: EstadoService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargar();
        setTimeout(() => { if (this.cargando) this.cargando = false; }, 10000);
    }

    ngOnDestroy(): void {
        this.pollingSubscriptions.forEach(s => s.unsubscribe());
    }

    iniciarPolling(): void {
        this.pollingSubscriptions.forEach(s => s.unsubscribe());
        this.pollingSubscriptions = [];
        const estadosActivos = ['CREADA', 'ENVIADA', 'APROBADA', 'TUTORIA', 'EVALUACION', 'CALIFICADA'];
        this.solicitudes.forEach(sol => {
            if (estadosActivos.includes(sol.estado)) {
                const sub = this.estadoService.pollingEstado(sol.id, 15000).subscribe({
                    next: (est: any) => {
                        if (est.solicitudEstado && est.solicitudEstado !== sol.estado) {
                            sol.estado = est.solicitudEstado;
                            this.cdr.markForCheck();
                        }
                    }
                });
                this.pollingSubscriptions.push(sub);
            }
        });
    }

    cargar(): void {
        this.cargando = true;

        if (this.esEstudiante()) {
            this.solicitudService.listarMisSolicitudes().subscribe({
                next: (data) => {
                    this.solicitudes = data || [];
                    this.totalElementos = this.solicitudes.length;
                    this.totalPaginas = 1;
                    this.iniciarPolling();
                    this.cargando = false;
                    this.cdr.markForCheck();
                },
                error: () => { this.notification.error('No se pudieron cargar las solicitudes.', 'Error'); this.cargando = false; this.cdr.markForCheck(); }
            });
            return;
        }

        // Admin / docente / coordinador: página del servidor (nunca todas las filas de golpe).
        this.solicitudService.listarSolicitudesPaginado(this.paginaActual, this.tamanioPagina).subscribe({
            next: (data) => {
                this.solicitudes = data.content || [];
                this.totalElementos = data.totalElements;
                this.totalPaginas = data.totalPages;
                this.iniciarPolling();
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => { this.notification.error('No se pudieron cargar las solicitudes.', 'Error'); this.cargando = false; this.cdr.markForCheck(); }
        });
    }

    get paginasVisibles(): number[] {
        const total = this.totalPaginas;
        const actual = this.paginaActual;
        const maxBotones = 7;
        if (total <= maxBotones) return Array.from({ length: total }, (_, i) => i);
        let inicio = Math.max(0, actual - 3);
        let fin = Math.min(total - 1, inicio + maxBotones - 1);
        inicio = Math.max(0, fin - maxBotones + 1);
        const paginas: number[] = [];
        for (let i = inicio; i <= fin; i++) paginas.push(i);
        return paginas;
    }

    irAPagina(pagina: number): void {
        if (pagina < 0 || pagina >= this.totalPaginas || pagina === this.paginaActual) return;
        this.paginaActual = pagina;
        this.cargar();
    }
    paginaAnterior(): void { this.irAPagina(this.paginaActual - 1); }
    paginaSiguiente(): void { this.irAPagina(this.paginaActual + 1); }

    enviar(id: number): void {
        this.solicitudService.enviarSolicitud(id).subscribe({
            next: () => {
                this.notification.success('Solicitud enviada a revisión.', '✓ Enviada');
                this.cargar();
            },
            error: (err) => {
                const msg = err?.error?.mensaje || 'Debes cargar el PDF del anteproyecto antes de enviar.';
                this.notification.error(msg, 'No se pudo enviar');
                this.cdr.markForCheck();
            }
        });
    }

    aprobar(id: number): void {
        this.solicitudService.aprobarSolicitud(id).subscribe({
            next: () => { this.notification.success('Solicitud aprobada.', '✓'); this.cargar(); },
            error: () => { this.notification.error('No se pudo aprobar.', 'Error'); this.cdr.markForCheck(); }
        });
    }

    rechazar(id: number): void {
        this.solicitudService.rechazarSolicitud(id).subscribe({
            next: () => { this.notification.success('Solicitud rechazada.', 'Rechazada'); this.cargar(); },
            error: () => { this.notification.error('No se pudo rechazar.', 'Error'); this.cdr.markForCheck(); }
        });
    }

    verObservaciones(titulo: string, obs: string): void {
        this.modalTitulo = titulo;
        this.modalObs = obs;
    }

    cerrarModal(): void { this.modalObs = null; }

    esEstudiante(): boolean { return this.authService.getRole() === 'ESTUDIANTE'; }
    esAdmin(): boolean { return ['ADMIN', 'DOCENTE'].includes(this.authService.getRole()); }

    getBadge(estado: string): string {
        const m: Record<string, string> = {
            CREADA: 'badge-creada',
            ENVIADA: 'badge-enviada',
            APROBADA: 'badge-aprobada',
            RECHAZADA: 'badge-rechazada',
            SUSPENDIDA: 'badge-suspendida',
            TUTORIA: 'badge-tutoria',
            EVALUACION: 'badge-evaluacion',
            CALIFICADA: 'badge-calificada',
            COMPLETADA: 'badge-completada'
        };
        return m[estado] || 'badge-default';
    }

    mostrarBotonObservaciones(estado: string): boolean {
        const estadosConObservaciones = ['TUTORIA', 'EVALUACION', 'CALIFICADA', 'COMPLETADA'];
        return estadosConObservaciones.includes(estado);
    }
}
