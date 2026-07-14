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
        const rol = this.authService.getRole()?.toUpperCase();
        const obs$ = rol === 'ESTUDIANTE'
            ? this.solicitudService.listarMisSolicitudes()
            : this.solicitudService.listarSolicitudes();

        obs$.subscribe({
            next: (data) => { this.solicitudes = data; this.iniciarPolling(); this.cargando = false; this.cdr.markForCheck(); },
            error: () => { this.notification.error('No se pudieron cargar las solicitudes.', 'Error'); this.cargando = false; this.cdr.markForCheck(); }
        });
    }

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