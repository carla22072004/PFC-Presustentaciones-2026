import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RubricaEvaluacionService, ObservacionesSolicitudDTO } from '../../../services/rubrica-evaluacion.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-ver-observaciones',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './ver-observaciones.component.html',
    styleUrls: ['./ver-observaciones.component.css']
})
export class VerObservacionesComponent implements OnInit {

    solicitudId!: number;
    observaciones: ObservacionesSolicitudDTO | null = null;
    cargando = true;
    error: string | null = null;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private rubricaService: RubricaEvaluacionService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
        this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
        this.cargarObservaciones();
    }

    cargarObservaciones(): void {
        this.rubricaService.obtenerObservacionesSolicitud(this.solicitudId).subscribe({
            next: (data) => {
                this.observaciones = data;
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: (err) => {
                this.error = err.error?.error || 'Error al cargar las observaciones.';
                this.cargando = false;
                this.notification.error(this.error || 'Error al cargar las observaciones.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    getRangoClass(rango: string | undefined): string {
        if (!rango) return '';
        return rango.toLowerCase();
    }

    getRangoIcon(rango: string | undefined): string {
        if (!rango) return '';
        switch (rango.toUpperCase()) {
            case 'ALTO': return 'bi-check-circle-fill';
            case 'MEDIO': return 'bi-exclamation-circle-fill';
            case 'BAJO': return 'bi-x-circle-fill';
            default: return 'bi-question-circle';
        }
    }

    getRangoLabelCoord(nota: number): string {
        if (nota <= 3) return 'DEFICIENTE';
        if (nota <= 6) return 'REGULAR';
        return 'BUENO';
    }

    getRangoClassCoord(nota: number): string {
        if (nota <= 3) return 'deficiente';
        if (nota <= 6) return 'regular';
        return 'bueno';
    }

    getRangoIconCoord(nota: number): string {
        if (nota <= 3) return 'bi-x-circle-fill';
        if (nota <= 6) return 'bi-exclamation-circle-fill';
        return 'bi-check-circle-fill';
    }

    getComentarioPorRango(nota: number): string {
        if (nota <= 3) {
            return 'El trabajo no cumple con los requisitos mínimos esperados. Se evidencian falencias significativas que requieren correcciones sustanciales.';
        } else if (nota <= 6) {
            return 'El trabajo presenta un nivel aceptable pero con aspectos que requieren mejoras o correcciones para alcanzar los estándares esperados.';
        } else {
            return 'El trabajo cumple satisfactoriamente con los objetivos y requisitos establecidos, demostrando un desempeño adecuado.';
        }
    }

    getNotaFinal(): number | null {
        if (!this.observaciones?.coordinador?.notaFinal) return null;
        return this.observaciones.coordinador.notaFinal;
    }

    getResultadoClass(resultado: string | null | undefined): string {
        if (!resultado) return '';
        return resultado === 'APROBADO' ? 'resultado-aprobado' : 'resultado-reprobado';
    }

    tieneObservaciones(): boolean {
        if (!this.observaciones) return false;
        
        for (const j of this.observaciones.jurados) {
            if (j.notaJurado) return true;
            if (j.observaciones) return true;
            if (j.criterios) {
                for (const c of j.criterios) {
                    if (c.observacionAuto || c.observacionManual) return true;
                }
            }
        }
        
        if (this.observaciones.coordinador?.observaciones) return true;
        
        return false;
    }

    getTotalJuradosEvaluados(): number {
        if (!this.observaciones?.jurados) return 0;
        return this.observaciones.jurados.filter(j => j.notaJurado !== null).length;
    }

    volver(): void {
        this.router.navigate(['/dashboard/solicitudes/mis-tramites']);
    }

    formatearRol(rol: string): string {
        const map: Record<string, string> = {
            'PRESIDENTE': 'Presidente',
            'VOCAL_1': 'Vocal 1',
            'VOCAL_2': 'Vocal 2',
            'TUTOR': 'Tutor'
        };
        return map[rol] || rol;
    }
}
