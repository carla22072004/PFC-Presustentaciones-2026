import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { JuradoService } from '../../services/jurado.service';
import { AuthService } from '../../services/auth.service';
import { DocenteService } from '../../services/docente.service';
import { JuryEvaluationService } from '../../services/jury-evaluation.service';
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
    /** IDs de miembro_tribunal (jurado) donde el docente autenticado YA registró su propia nota
     * para esa asignación en particular -- un mismo docente puede tener roles distintos en
     * distintas solicitudes (ej. Vocal 2 en una, Vocal 1 en otra), cada una con su propio estado. */
    juradosEvaluados = new Set<number>();

    constructor(
        private juryService: JuradoService,
        private docenteService: DocenteService,
        private authService: AuthService,
        private juryEvalService: JuryEvaluationService,
        private notificationService: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
        const userId = this.authService.getUserId();
        this.docenteService.obtenerPorUsuario(userId).subscribe({
            next: (docente) => { this.docenteId = docente.id; this.cargarAsignaciones(docente.id); this.cdr.markForCheck(); },
            error: () => {
                this.cargando = false;
                this.notificationService.error("No se pudo cargar la información del docente.", "Error");
                this.cdr.markForCheck();
            }
        });
    }

    cargarAsignaciones(docenteId: number): void {
        this.juryService.listarPorDocente(docenteId).subscribe({
            next: (data: any[]) => {
                this.asignaciones = data;
                this.verificarEvaluacionesPersonales(data);
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.cargando = false;
                this.notificationService.error("No se pudieron cargar las asignaciones de jurado.", "Error");
                this.cdr.markForCheck();
            }
        });

        this.juryService.listarTutoriasPorDocente(docenteId).subscribe({
            next: (data: any[]) => { this.asignacionesTutor = data; this.cdr.markForCheck(); },
            error: () => { this.asignacionesTutor = []; this.cdr.markForCheck(); }
        });
    }

    verificarEvaluacionesPersonales(asignaciones: any[]): void {
        const conSolicitud = asignaciones.filter(a => a?.id && a.solicitud?.id);
        if (conSolicitud.length === 0) return;

        const checks = conSolicitud.map(a =>
            this.juryEvalService.obtenerEvaluacion(a.solicitud.id, a.id).pipe(
                map(ev => ({ juradoId: a.id, existe: !!ev })),
                catchError(() => of({ juradoId: a.id, existe: false }))
            )
        );

        forkJoin(checks).subscribe(resultados => {
            resultados.forEach(r => { if (r.existe) this.juradosEvaluados.add(r.juradoId); });
            this.cdr.markForCheck();
        });
    }

    yaEvaluadaPersonal(juradoId: number): boolean {
        return this.juradosEvaluados.has(juradoId);
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
