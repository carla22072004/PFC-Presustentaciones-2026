import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EvaluacionService } from '../../../services/evaluacion.service';
import { RubricaService } from '../../../services/rubrica.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { JuradoService } from '../../../services/jurado.service';
import { ActaService } from '../../../services/acta.service';
import { JuryEvaluationService, EvaluacionJuradoResponse } from '../../../services/jury-evaluation.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-evaluar-solicitud',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './evaluar-solicitud.component.html',
  styleUrls: ['./evaluar-solicitud.component.css']
})
export class EvaluarSolicitudComponent implements OnInit {
  form!: FormGroup;
  rubricas: any[] = [];
  solicitud: any = null;
  solicitudId!: number;
  jurados: any[] = [];
  tutor: any = null;
  enviando = false;
  evaluacionExistente: any = null;
  acta: any = null;
  firmandoActa = false;
  modalObs: string | null = null;
  modalTitulo = '';

  evaluacionesJurado: EvaluacionJuradoResponse[] = [];
  notaTribunalPromedio: number | null = null;
  tribunalCompleto = false;

  readonly ROLES_FIRMA = [
    { value: 'PRESIDENTE', label: 'Presidente' },
    { value: 'VOCAL_1',    label: 'Vocal 1' },
    { value: 'VOCAL_2',    label: 'Vocal 2' },
    { value: 'TUTOR',      label: 'Tutor' },
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private evalService: EvaluacionService,
    private rubricaService: RubricaService,
    private solicitudService: SolicitudService,
    private juradoService: JuradoService,
    private actaService: ActaService,
    private juryEvalService: JuryEvaluationService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
    this.form = this.fb.group({
      rubricaId:      ['', Validators.required],
      notaInstructor: ['', [Validators.required, Validators.min(0), Validators.max(10)]],
      observaciones:  ['', Validators.required]
    });
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.rubricaService.listar().subscribe(r => this.rubricas = r);
    this.solicitudService.obtenerPorId(this.solicitudId).subscribe(s => this.solicitud = s);
    this.juradoService.listarPorSolicitud(this.solicitudId).subscribe({ next: j => this.jurados = j, error: () => {} });
    this.juradoService.obtenerTutor(this.solicitudId).subscribe({ next: t => this.tutor = t, error: () => {} });
    this.evalService.porSolicitud(this.solicitudId).subscribe({ next: e => this.evaluacionExistente = e, error: () => {} });
    this.actaService.porSolicitud(this.solicitudId).subscribe({ next: a => this.acta = a, error: () => {} });
    this.cargarNotaTribunal();
  }

  cargarNotaTribunal(): void {
    this.juryEvalService.obtenerTribunal(this.solicitudId).subscribe({
      next: (evals) => {
        this.evaluacionesJurado = evals;
        if (evals.length > 0) {
          const sum = evals.reduce((acc, e) => acc + e.notaJurado, 0);
          this.notaTribunalPromedio = Math.round((sum / evals.length) * 100) / 100;
          this.tribunalCompleto = evals.length === this.jurados.length;
        } else {
          this.notaTribunalPromedio = null;
          this.tribunalCompleto = false;
        }
      },
      error: () => {}
    });
  }

  getEvaluacionJuradoPorRol(rol: string): EvaluacionJuradoResponse | undefined {
    return this.evaluacionesJurado.find(e => e.rolJurado === rol);
  }

  get notaInstructor(): number { return +this.form.get('notaInstructor')?.value || 0; }
  get notaJurado(): number     { return this.notaTribunalPromedio ?? 0; }

  get notaFinalPreview(): number {
    return Math.round(((this.notaInstructor * 60 / 100)
                     + (this.notaJurado * 40 / 100)) * 100) / 100;
  }

  get resultadoPreview(): string {
    if (!this.form.get('notaInstructor')?.value) return '—';
    return this.notaFinalPreview >= 7 ? 'APROBADO' : 'REPROBADO';
  }

  getRangoLabel(nota: number): string {
    if (nota <= 3) return 'DEFICIENTE';
    if (nota <= 6) return 'REGULAR';
    return 'BUENO';
  }

  getRangoClass(nota: number): string {
    if (nota <= 3) return 'deficiente';
    if (nota <= 6) return 'regular';
    return 'bueno';
  }

  getRangoIcon(nota: number): string {
    if (nota <= 3) return 'bi-x-circle-fill';
    if (nota <= 6) return 'bi-exclamation-circle-fill';
    return 'bi-check-circle-fill';
  }

  getComentarioRango(nota: number): string {
    if (nota <= 3) {
      return 'El trabajo no cumple con los requisitos mínimos esperados. Se evidencian falencias significativas que requieren correcciones sustanciales.';
    } else if (nota <= 6) {
      return 'El trabajo presenta un nivel aceptable pero con aspectos que requieren mejoras o correcciones para alcanzar los estándares esperados.';
    } else {
      return 'El trabajo cumple satisfactoriamente con los objetivos y requisitos establecidos, demostrando un desempeño adecuado.';
    }
  }

  guardar(): void {
    if (this.form.invalid) return;

    if (!this.tribunalCompleto) {
      this.notification.error(
        'El tribunal aún no ha completado su evaluación. Todos los jurados deben evaluar antes.',
        'Tribunal incompleto'
      );
      return;
    }

    this.enviando = true;
    const { rubricaId, notaInstructor, observaciones } = this.form.value;
    const notaJurado = this.notaTribunalPromedio!;

    this.evalService.evaluarPonderado(
      this.solicitudId, rubricaId,
      notaInstructor, notaJurado,
      observaciones, 60, 40
    ).subscribe({
      next: (e) => {
        this.evaluacionExistente = e;
        this.notification.success(
          `Evaluación registrada. Nota final: ${e.notaFinal?.toFixed(2)} — ${e.resultado}`,
          '✓ Evaluado'
        );
        this.enviando = false;
        this.generarActaAutomatica();
      },
      error: (err) => {
        const msg = err.error?.error || 'No se pudo registrar la evaluación.';
        this.notification.error(msg, 'Error');
        this.enviando = false;
      }
    });
  }

  generarActaAutomatica(): void {
    this.actaService.generarActa(this.solicitudId).subscribe({
      next: (a) => {
        this.acta = a;
        this.notification.success('Acta generada automáticamente.', '📄 Acta lista');
      },
      error: () => {}
    });
  }

  firmarActa(rol: string): void {
    if (!this.acta) return;
    this.firmandoActa = true;
    this.actaService.firmarActa(this.acta.id, rol).subscribe({
      next: (a) => {
        this.acta = a;
        const msg = a.firmada ? '¡Acta completamente firmada por todos los actores!' : `Firma de ${rol} registrada.`;
        this.notification.success(msg, '✍️ Firmado');
        this.firmandoActa = false;
      },
      error: (err) => {
        const msg = err.error?.error || 'No se pudo registrar la firma.';
        this.notification.error(msg, 'Error');
        this.firmandoActa = false;
      }
    });
  }

  descargarActa(): void {
    if (!this.acta) return;
    this.actaService.descargarPdf(this.acta.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `acta_solicitud_${this.solicitudId}.pdf`; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.notification.error('No se pudo descargar el PDF.', 'Error')
    });
  }

  firmaEstado(rol: string): boolean {
    if (!this.acta) return false;
    const map: Record<string, boolean> = {
      PRESIDENTE: this.acta.firmadaPresidente,
      VOCAL_1:    this.acta.firmadaVocal1,
      VOCAL_2:    this.acta.firmadaVocal2,
      TUTOR:      this.acta.firmadaTutor,
    };
    return map[rol] ?? false;
  }

  getNombreJuradoPorRol(rol: string): string {
    const j = this.jurados.find(x => x.rol === rol);
    const u = j?.docente?.usuario;
    return u ? `${u.nombre} ${u.apellido}` : '—';
  }

  verObservaciones(titulo: string, obs: string): void {
    this.modalTitulo = titulo;
    this.modalObs = obs;
  }
  cerrarModal(): void { this.modalObs = null; }

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
