import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { RubricaEvaluacionService, EvaluacionRubricaResponse } from '../../../services/rubrica-evaluacion.service';
import { SolicitudService } from '../../../services/solicitud.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-ver-rubrica-tribunal',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './ver-rubrica-tribunal.component.html',
    styleUrls: ['./ver-rubrica-tribunal.component.css']
})
export class VerRubricaTribunalComponent implements OnInit {
    solicitudId!: number;
    solicitud: any = null;
    evaluaciones: EvaluacionRubricaResponse[] = [];
    cargando = true;

    readonly ESCALAS_LABEL: Record<number, string> = {
        100: 'Completo',
        67:  'Casi completo',
        33:  'Significativamente incompleto',
        0:   'No presenta/entrega',
    };

    constructor(
        private route: ActivatedRoute,
        private rubricaEvalService: RubricaEvaluacionService,
        private solicitudService: SolicitudService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
        this.solicitudService.obtenerPorId(this.solicitudId).subscribe(s => {
            this.solicitud = s;
            this.cdr.markForCheck();
        });

        this.rubricaEvalService.obtenerEvaluacionesSolicitud(this.solicitudId).subscribe({
            next: (evals) => { this.evaluaciones = evals; this.cargando = false; this.cdr.markForCheck(); },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
        });
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    get evaluadosCount(): number {
        return this.evaluaciones.filter(e => e.notaTotalJurado !== null).length;
    }

    get notaPromedio(): number | null {
        const notas = this.evaluaciones.filter(e => e.notaTotalJurado !== null).map(e => e.notaTotalJurado!);
        if (notas.length === 0) return null;
        return Math.round(notas.reduce((a, b) => a + b, 0) / notas.length * 100) / 100;
    }

    get tribunalCompleto(): boolean {
        return this.evaluaciones.length > 0 && this.evaluaciones.every(e => e.notaTotalJurado !== null);
    }

    getLabelEscala(escala: number): string {
        return this.ESCALAS_LABEL[escala] ?? String(escala) + '%';
    }
}
