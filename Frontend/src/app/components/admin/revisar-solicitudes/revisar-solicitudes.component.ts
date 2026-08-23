import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ReporteService } from '../../../services/reporte.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-revisar-solicitudes',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './revisar-solicitudes.component.html',
    styleUrls: ['./revisar-solicitudes.component.css']
})
export class RevisarSolicitudesComponent implements OnInit {
    solicitudes: any[] = [];
    cargando = true;
    filtroEstado = '';
    modalObs: string | null = null;
    modalTitulo = '';

    // Paginación server-side — con miles de solicitudes no se puede cargar todo de una vez
    paginaActual = 0; // 0-indexed, como Spring Pageable
    tamanioPagina = 20;
    totalElementos = 0;
    totalPaginas = 0;

    // Conteos por estado para las pestañas de filtro (independientes de la página cargada)
    conteos: Record<string, number> = {};

    // Modal de rechazo con observación
    modalRechazo = false;
    solicitudIdRechazo: number | null = null;
    observacionRechazo = '';

    // Modal de suspensión
    modalSuspension = false;
    solicitudIdSuspension: number | null = null;
    motivoSuspension = '';

    constructor(
        private solicitudService: SolicitudService,
        private notification: NotificationService,
        private router: Router,
        private reporteService: ReporteService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargarConteos();
        this.cargar();
        // Safety: stop spinner after 10s even if backend unreachable
        setTimeout(() => { if (this.cargando) this.cargando = false; }, 10000);
    }

    cargar(): void {
        this.cargando = true;
        this.solicitudService.listarSolicitudesPaginado(this.paginaActual, this.tamanioPagina, this.filtroEstado || undefined).subscribe({
            next: (data) => {
                this.solicitudes = data.content;
                this.totalElementos = data.totalElements;
                this.totalPaginas = data.totalPages;
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => { this.cargando = false; this.notification.error('Error al cargar solicitudes.', 'Error'); this.cdr.markForCheck(); }
        });
    }

    cargarConteos(): void {
        this.solicitudService.contarPorEstado().subscribe({
            next: (data) => { this.conteos = data; this.cdr.markForCheck(); },
            error: () => { this.notification.error('Error al obtener conteos por estado.', 'Error'); }
        });
    }

    get solicitudesFiltradas(): any[] {
        return this.solicitudes;
    }

    contar(estado: string): number {
        return this.conteos[estado] || 0;
    }

    setFiltro(estado: string): void {
        if (this.filtroEstado === estado) return;
        this.filtroEstado = estado;
        this.paginaActual = 0;
        this.cargar();
    }

    /** Ventana de números de página a mostrar alrededor de la página actual */
    get paginasVisibles(): number[] {
        const total = this.totalPaginas;
        const actual = this.paginaActual;
        const ventana = 2;
        let inicio = Math.max(0, actual - ventana);
        let fin = Math.min(total - 1, actual + ventana);
        // Mantener siempre el mismo ancho de ventana cuando sea posible
        if (fin - inicio < ventana * 2) {
            inicio = Math.max(0, fin - ventana * 2);
            fin = Math.min(total - 1, inicio + ventana * 2);
        }
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

    inicialNombre(s: any): string {
        return (s.estudiante?.usuario?.nombre || 'E')[0].toUpperCase();
    }

    aprobar(id: number): void {
        this.solicitudService.aprobarSolicitud(id).subscribe({
            next: () => {
                const sol = this.solicitudes.find(s => s.id === id);
                if (sol) {
                    sol.estado = 'APROBADA';
                    this.cdr.markForCheck();
                }
                this.cargarConteos();

                this.notification.success(
                    'Solicitud aprobada. Redirigiendo para asignar tribunal...',
                    '✓ Aprobada'
                );

                setTimeout(() => {
                    this.router.navigate(['/dashboard/admin/asignar-jurados', id]);
                }, 1200);
            },
            error: () => this.notification.error('No se pudo aprobar.', 'Error')
        });
    }

    abrirModalRechazo(id: number): void {
        this.solicitudIdRechazo = id;
        this.observacionRechazo = '';
        this.modalRechazo = true;
    }

    cerrarModalRechazo(): void {
        this.modalRechazo = false;
        this.solicitudIdRechazo = null;
        this.observacionRechazo = '';
    }

    confirmarRechazo(): void {
        if (!this.solicitudIdRechazo) return;
        if (!this.observacionRechazo.trim()) {
            this.notification.error('Debes ingresar el motivo del rechazo.', 'Campo requerido');
            return;
        }
        const idArechazar = this.solicitudIdRechazo;
        const obsAGuardar = this.observacionRechazo;

        this.solicitudService.rechazarConObservacion(idArechazar, obsAGuardar).subscribe({
            next: () => {
                this.modalRechazo = false;
                this.solicitudIdRechazo = null;
                this.observacionRechazo = '';

                const sol = this.solicitudes.find(s => s.id === idArechazar);
                if (sol) {
                    sol.estado = 'RECHAZADA';
                    sol.observaciones = obsAGuardar;
                    this.cdr.markForCheck();
                }
                this.cargarConteos();

                setTimeout(() => {
                    this.notification.success('Solicitud rechazada. El estudiante fue notificado.', '✓ Rechazada');
                }, 100);
            },
            error: () => this.notification.error('No se pudo rechazar.', 'Error')
        });
    }

    abrirModalSuspension(id: number): void {
        this.solicitudIdSuspension = id;
        this.motivoSuspension = '';
        this.modalSuspension = true;
    }

    cerrarModalSuspension(): void {
        this.modalSuspension = false;
        this.solicitudIdSuspension = null;
        this.motivoSuspension = '';
    }

    confirmarSuspension(): void {
        if (!this.solicitudIdSuspension) return;
        if (!this.motivoSuspension.trim()) {
            this.notification.error('Debes ingresar el motivo de la suspensión.', 'Campo requerido');
            return;
        }
        const id = this.solicitudIdSuspension;
        const motivo = this.motivoSuspension;

        this.solicitudService.suspenderSolicitud(id, motivo).subscribe({
            next: () => {
                this.modalSuspension = false;
                this.solicitudIdSuspension = null;
                this.motivoSuspension = '';
                const sol = this.solicitudes.find(s => s.id === id);
                if (sol) {
                    sol.estado = 'SUSPENDIDA';
                    sol.motivoSuspension = motivo;
                    this.cdr.markForCheck();
                }
                this.cargarConteos();
                this.notification.success('Solicitud suspendida. El estudiante fue notificado.', '✓ Suspendida');
            },
            error: () => this.notification.error('No se pudo suspender.', 'Error')
        });
    }

    verObservaciones(titulo: string, obs: string): void {
        this.modalTitulo = titulo;
        this.modalObs = obs;
    }

    cerrarModal(): void { this.modalObs = null; }

    getBadge(estado: string): string {
        const m: Record<string,string> = {
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

    descargarCronogramaPdf(): void {
        this.reporteService.cronogramaPdf().subscribe({
            next: (blob) => {
                const a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = 'cronograma_presustentaciones.pdf';
                a.click();
            },
            error: () => this.notification.error('No se pudo generar el PDF.', 'Error')
        });
    }

    descargarEstadisticasPdf(): void {
        this.reporteService.estadisticasPdf().subscribe({
            next: (blob) => {
                const a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = 'estadisticas_evaluaciones.pdf';
                a.click();
            },
            error: () => this.notification.error('No se pudo generar el reporte.', 'Error')
        });
    }
}