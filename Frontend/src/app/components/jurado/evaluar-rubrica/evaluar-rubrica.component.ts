import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { RubricaEvaluacionService } from '../../../services/rubrica-evaluacion.service';
import { JuryEvaluationService } from '../../../services/jury-evaluation.service';
import { JuradoService } from '../../../services/jurado.service';
import { AuthService } from '../../../services/auth.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';

interface EvaluacionJuradoSimple {
  notaJurado: number | null;
  observaciones: string | null;
  resultado: string | null;
  rolJurado: string;
  nombreJurado: string;
}

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-evaluar-rubrica',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './evaluar-rubrica.component.html',
    styleUrls: ['./evaluar-rubrica.component.css']
})
export class EvaluarRubricaComponent implements OnInit {

    solicitudId!: number;
    solicitud: any = null;
    juradoInfo: any = null;
    docenteId: number | null = null;
    rubrica: any = null;

    notaJurado: number | null = null;
    observaciones: string = '';
    evaluacionPrevia: EvaluacionJuradoSimple | null = null;
    evaluacionesTribunal: any[] = [];
    
    cargando = true;
    enviando = false;

    constructor(
        private route: ActivatedRoute,
        private rubricaEvalService: RubricaEvaluacionService,
        private juryEvalService: JuryEvaluationService,
        private juradoService: JuradoService,
        private authService: AuthService,
        private solicitudService: SolicitudService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
        this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
        const userId = this.authService.getUserId();

        this.solicitudService.obtenerPorId(this.solicitudId).subscribe({
            next: (s) => { this.solicitud = s; this.cdr.markForCheck(); },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
        });

        this.juradoService.obtenerInfoJurado(this.solicitudId, userId).subscribe({
            next: (info) => {
                this.juradoInfo = info;
                this.cdr.markForCheck();
                if (info?.id) {
                    this.cargarRubrica();
                    this.cargarEvaluacionPrevia();
                    this.cargarTribunal();
                } else {
                    this.cargando = false;
                    this.cdr.markForCheck();
                }
            },
            error: () => {
                this.cargando = false;
                this.notification.error('No estás asignado como jurado en esta solicitud.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    cargarRubrica(): void {
        this.rubricaEvalService.listarRubricas().subscribe({
            next: (rubricas) => {
                if (rubricas.length > 0) {
                    this.rubrica = rubricas[0];
                    this.cdr.markForCheck();
                }
            }
        });
    }

    cargarEvaluacionPrevia(): void {
        if (!this.juradoInfo?.id) return;
        
        this.juryEvalService.obtenerEvaluacion(this.solicitudId, this.juradoInfo.id).subscribe({
            next: (eval_) => {
                if (eval_) {
                    this.evaluacionPrevia = {
                        notaJurado: eval_.notaJurado,
                        observaciones: eval_.observaciones,
                        resultado: eval_.resultado,
                        rolJurado: this.juradoInfo.rol,
                        nombreJurado: this.juradoInfo.nombreDocente || 'Jurado'
                    };
                }
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    cargarTribunal(): void {
        this.juryEvalService.obtenerTribunal(this.solicitudId).subscribe({
            next: (evals) => {
                this.evaluacionesTribunal = evals;
                this.cdr.markForCheck();
            }
        });
    }

    esValido(): boolean {
        return this.notaJurado !== null 
            && this.notaJurado >= 1 
            && this.notaJurado <= 10
            && this.observaciones.trim().length > 0;
    }

    get promedioTribunal(): number | null {
        const evaluados = this.evaluacionesTribunal.filter(e => e.notaJurado);
        if (evaluados.length === 0) return null;
        const suma = evaluados.reduce((acc, e) => acc + e.notaJurado, 0);
        return suma / evaluados.length;
    }

    guardar(): void {
        if (!this.esValido() || !this.juradoInfo?.id || this.notaJurado === null) return;
        this.enviando = true;

        const request = {
            solicitudId: this.solicitudId,
            juradoId: this.juradoInfo.id,
            notaJurado: this.notaJurado!,
            observaciones: this.observaciones.trim()
        };

        this.juryEvalService.guardarEvaluacion(request).subscribe({
            next: (resp) => {
                this.evaluacionPrevia = {
                    notaJurado: this.notaJurado,
                    observaciones: this.observaciones.trim(),
                    resultado: this.notaJurado! >= 7 ? 'APROBADO' : 'REPROBADO',
                    rolJurado: this.juradoInfo.rol,
                    nombreJurado: this.juradoInfo.nombreDocente || 'Jurado'
                };
                this.notification.success(
                    `Evaluación registrada exitosamente.`,
                    '✓ Guardado'
                );
                this.enviando = false;
                this.cargarTribunal();
                this.cdr.markForCheck();
            },
            error: (err) => {
                const msg = err.error?.error || 'Error al guardar la evaluación.';
                this.notification.error(msg, 'Error');
                this.enviando = false;
                this.cdr.markForCheck();
            }
        });
    }

    editarEvaluacion(): void {
        if (this.evaluacionPrevia) {
            this.notaJurado = this.evaluacionPrevia.notaJurado;
            this.observaciones = this.evaluacionPrevia.observaciones || '';
            this.evaluacionPrevia = null;
        }
    }
}
