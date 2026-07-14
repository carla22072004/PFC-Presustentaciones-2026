import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { JuradoService } from '../../services/jurado.service';
import { AuthService } from '../../services/auth.service';
import { DocenteService } from '../../services/docente.service';
import { EvaluacionService } from '../../services/evaluacion.service';
import { NotificationService } from '../../services/notification.service';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-mis-asignaciones',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './mis-asignaciones.component.html',
    styleUrls: ['./mis-asignaciones.component.css']
})
export class MisAsignacionesComponent implements OnInit {
    asignaciones: any[] = [];
    asignacionesTutor: any[] = [];
    cargando = true;
    docenteId: number | null = null;
    solicitudesEvaluadas = new Set<number>();

    constructor(
        private juryService: JuradoService,
        private docenteService: DocenteService,
        private authService: AuthService,
        private evaluacionService: EvaluacionService,
        private notificationService: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
        const userId = this.authService.getUserId();
        this.docenteService.obtenerPorUsuario(userId).subscribe({
            next: (docente) => { this.docenteId = docente.id; this.cargarAsignaciones(docente.id); this.cdr.markForCheck(); },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
        });
    }

    cargarAsignaciones(docenteId: number): void {
        this.juryService.listarPorDocente(docenteId).subscribe({
            next: (data: any[]) => {
                this.asignaciones = data;
                this.verificarEvaluacionesFinal(data.map((j: any) => j.solicitud?.id).filter(Boolean));
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
        });

        this.juryService.listarTutoriasPorDocente(docenteId).subscribe({
            next: (data: any[]) => { this.asignacionesTutor = data; this.cdr.markForCheck(); },
            error: () => { this.asignacionesTutor = []; this.cdr.markForCheck(); }
        });
    }

    verificarEvaluacionesFinal(solicitudIds: number[]): void {
        if (solicitudIds.length === 0) return;

        const checks = solicitudIds.map(id =>
            this.evaluacionService.porSolicitud(id).pipe(
                map(eval_ => ({ id, existe: !!eval_?.notaFinal })),
                catchError(() => of({ id, existe: false }))
            )
        );

        forkJoin(checks).subscribe(resultados => {
            resultados.forEach(r => { if (r.existe) this.solicitudesEvaluadas.add(r.id); });
            this.cdr.markForCheck();
        });
    }

    yaEvaluada(solicitudId: number): boolean {
        return this.solicitudesEvaluadas.has(solicitudId);
    }

    /**
     * Verifica si una solicitud está suspendida
     */
    estaSuspendida(solicitud: any): boolean {
        return solicitud?.estado === 'SUSPENDIDA';
    }

    /**
     * Obtiene el mensaje de suspensión de una solicitud
     */
    obtenerMotivoSuspension(solicitud: any): string | null {
        if (solicitud?.estado === 'SUSPENDIDA') {
            return solicitud.motivoSuspension || 'La solicitud ha sido suspendida por el coordinador.';
        }
        return null;
    }

    /**
     * Muestra alerta de suspensión y retorna true si está suspendida
     * Usar con (click)="metodo() && $event.preventDefault()"
     */
    verificarSuspension(solicitud: any): boolean {
        const motivo = this.obtenerMotivoSuspension(solicitud);
        if (motivo) {
            this.notificationService.error(motivo, 'Solicitud Suspendida');
            return true;
        }
        return false;
    }

    getRolLabel(rol: string): string {
        const map: Record<string, string> = {
            PRESIDENTE: 'Presidente del Tribunal', VOCAL_1: 'Vocal 1', VOCAL_2: 'Vocal 2',
        };
        return map[rol] || rol;
    }

    getRolColor(rol: string): string {
        const map: Record<string, string> = {
            PRESIDENTE: 'rol-presidente', VOCAL_1: 'rol-vocal1', VOCAL_2: 'rol-vocal2',
        };
        return map[rol] || '';
    }

    getEstadoBadge(estado: string): string {
        const map: Record<string, string> = {
            APROBADA: 'badge-aprobada', ENVIADA: 'badge-enviada', CREADA: 'badge-creada',
            SUSPENDIDA: 'badge-suspendida',
        };
        return map[estado] || 'badge-default';
    }

    getNombreEstudiante(j: any): string {
        const u = j?.solicitud?.estudiante?.usuario;
        return u ? `${u.nombre} ${u.apellido}` : '—';
    }

    getTitulo(j: any): string { return j?.solicitud?.tituloTema || '—'; }
}
