import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SolicitudService } from '../../../services/solicitud.service';
import { EvaluacionService } from '../../../services/evaluacion.service';
import { ActaService } from '../../../services/acta.service';
import { CronogramaService } from '../../../services/cronograma.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-reportes',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './reportes.component.html',
    styleUrls: ['./reportes.component.css']
})
export class ReportesComponent implements OnInit {
    solicitudes: any[] = [];
    evaluaciones: any[] = [];
    actas: any[] = [];
    cronogramas: any[] = [];
    cargando = true;
    generandoId: number | null = null;
    solicitudesAprobadas: any[] = [];

    constructor(
        private solicitudService: SolicitudService,
        private evalService: EvaluacionService,
        private actaService: ActaService,
        private cronogramaService: CronogramaService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargarTodo();
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    cargarTodo(): void {
        this.cargando = true;
        this._done = 0;
        this.solicitudService.listarSolicitudes().subscribe(s => {
            this.solicitudes = s;
            this.solicitudesAprobadas = s.filter((x: any) => x.estado === 'APROBADA');
            this.checkDone();
            this.cdr.markForCheck();
        });
        this.evalService.listar().subscribe(e => { this.evaluaciones = e; this.checkDone(); this.cdr.markForCheck(); });
        this.actaService.listar().subscribe(a => { this.actas = a; this.checkDone(); this.cdr.markForCheck(); });
        this.cronogramaService.listar().subscribe(c => { this.cronogramas = c; this.checkDone(); this.cdr.markForCheck(); });
    }

    private _done = 0;
    checkDone(): void { if (++this._done >= 4) { this.cargando = false; this._done = 0; this.cdr.markForCheck(); } }

    get totalSolicitudes(): number { return this.solicitudes.length; }
    get aprobadas(): number        { return this.solicitudes.filter(s => s.estado === 'APROBADA').length; }
    get rechazadas(): number       { return this.solicitudes.filter(s => s.estado === 'RECHAZADA').length; }
    get pendientes(): number       { return this.solicitudes.filter(s => s.estado === 'ENVIADA').length; }

    get totalEvaluaciones(): number { return this.evaluaciones.length; }
    get aprobadosEval(): number     { return this.evaluaciones.filter(e => e.resultado === 'APROBADO').length; }
    get reprobadosEval(): number    { return this.evaluaciones.filter(e => e.resultado === 'REPROBADO').length; }

    get promedioNotas(): string {
        if (!this.evaluaciones.length) return '—';
        const sum = this.evaluaciones.reduce((a, e) => a + (e.notaFinal || 0), 0);
        return (sum / this.evaluaciones.length).toFixed(2);
    }

    get actasFirmadas(): number   { return this.actas.filter(a => a.firmada).length; }
    get actasPendientes(): number { return this.actas.filter(a => !a.firmada).length; }

    generarActa(solicitudId: number): void {
        this.generandoId = solicitudId;
        this.actaService.generarActa(solicitudId).subscribe({
            next: () => {
                this.notification.success('Acta generada.', '✓');
                this.generandoId = null;
                this.cargarTodo();
            },
            error: (err) => {
                const msg = err.error?.error || 'Error al generar acta.';
                this.notification.error(msg, 'Error');
                this.generandoId = null;
                this.cdr.markForCheck();
            }
        });
    }

    descargarActa(actaId: number | undefined): void {
        if (!actaId) return;
        this.actaService.descargarPdf(actaId).subscribe({
            next: (blob) => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `acta_${actaId}.pdf`;
                a.click();
                URL.revokeObjectURL(url);
            },
            error: () => this.notification.error('No se pudo descargar el PDF.', 'Error')
        });
    }

    tieneActa(solicitudId: number): boolean {
        return this.actas.some(a => a.solicitud?.id === solicitudId);
    }

    getActa(solicitudId: number): any {
        return this.actas.find(a => a.solicitud?.id === solicitudId);
    }

    getEval(solicitudId: number): any {
        return this.evaluaciones.find(e => e.solicitud?.id === solicitudId);
    }

    nombreEstudiante(s: any): string {
        const u = s.estudiante?.usuario;
        return u ? `${u.nombre} ${u.apellido}` : '—';
    }
}
