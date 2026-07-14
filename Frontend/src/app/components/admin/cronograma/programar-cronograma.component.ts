import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CronogramaService } from '../../../services/cronograma.service';
import { SalaService } from '../../../services/sala.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';
import { JuradoService } from '../../../services/jurado.service';
import { TutoriaService } from '../../../services/tutoria.service';
import { AuthService } from '../../../services/auth.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-programar-cronograma',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    templateUrl: './programar-cronograma.component.html',
    styleUrls: ['./programar-cronograma.component.css']
})
export class ProgramarCronogramaComponent implements OnInit {
    form!: FormGroup;
    salas: any[] = [];
    solicitud: any = null;
    solicitudId!: number;
    enviando = false;
    asignandoAuto = false;
    cronogramaExistente: any = null;
    errorConflicto: string | null = null;

    validacionTribunal: 'ok' | 'error' | null = null;
    validacionTutoria:  'ok' | 'error' | null = null;
    mensajeTribunal = '';
    mensajeTutoria  = '';

    constructor(
        private fb: FormBuilder,
        private route: ActivatedRoute,
        private router: Router,
        private cronogramaService: CronogramaService,
        private salaService: SalaService,
        private solicitudService: SolicitudService,
        private notification: NotificationService,
        private juradoService: JuradoService,
        private tutoriaService: TutoriaService,
        private authService: AuthService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
        this.form = this.fb.group({
            fecha: ['', Validators.required],
            hora: ['', Validators.required],
            salaId: ['', Validators.required]
        });
        this.cargarDatos();
        setTimeout(() => { if (this.validacionTribunal === null) { this.validacionTribunal = 'error'; this.cdr.markForCheck(); } }, 10000);
    }

    get formularioBloqueado(): boolean {
        return this.validacionTribunal !== 'ok' || this.validacionTutoria !== 'ok';
    }

    cargarDatos(): void {
        this.salaService.listar().subscribe(s => { this.salas = s; this.cdr.markForCheck(); });
        this.cronogramaService.porSolicitud(this.solicitudId).subscribe({
            next: (c) => { this.cronogramaExistente = c; this.cdr.markForCheck(); },
            error: () => { this.cronogramaExistente = null; this.cdr.markForCheck(); }
        });

        forkJoin({
            solicitud: this.solicitudService.obtenerPorId(this.solicitudId),
            jurados:   this.juradoService.listarPorSolicitud(this.solicitudId)
        }).subscribe({
            next: ({ solicitud, jurados }) => {
                this.solicitud = solicitud;
                this.verificarTribunal(jurados);
                this.verificarTutoria();
                this.cdr.markForCheck();
            },
            error: () => {
                this.validacionTribunal = 'error';
                this.mensajeTribunal = 'No se pudo cargar la información de la solicitud.';
                this.cdr.markForCheck();
            }
        });
    }

    private verificarTribunal(jurados: any[]): void {
        const rolesRequeridos = ['PRESIDENTE', 'VOCAL_1', 'VOCAL_2'];
        const rolesAsignados  = jurados.map(j => j.rol);
        const completo = rolesRequeridos.every(r => rolesAsignados.includes(r));
        if (completo) {
            this.validacionTribunal = 'ok';
        } else {
            this.validacionTribunal = 'error';
            const faltantes = rolesRequeridos
                .filter(r => !rolesAsignados.includes(r))
                .map(r => r.replace('_', ' '))
                .join(', ');
            this.mensajeTribunal = `El tribunal no está completo. Faltan: ${faltantes}.`;
        }
        this.cdr.markForCheck();
    }

    private verificarTutoria(): void {
        this.tutoriaService.obtenerTutorPorSolicitud(this.solicitudId).subscribe({
            next: (tutor) => {
                const tutorId = tutor?.id;
                if (!tutorId) {
                    this.validacionTutoria = 'error';
                    this.mensajeTutoria = 'El estudiante no tiene tutor asignado aún.';
                    this.cdr.markForCheck();
                    return;
                }
                this.tutoriaService.obtenerResumen(tutorId, this.authService.getUserId()).subscribe({
                    next: (resumen) => {
                        if (resumen.estadoTutoria === 'COMPLETADA') {
                            this.validacionTutoria = 'ok';
                        } else {
                            this.validacionTutoria = 'error';
                            this.mensajeTutoria =
                                `La tutoría no ha sido completada (${resumen.fasesAprobadas}/3 revisiones aprobadas).`;
                        }
                        this.cdr.markForCheck();
                    },
                    error: () => {
                        this.validacionTutoria = 'error';
                        this.mensajeTutoria = 'El tutor está asignado pero la tutoría aún no ha iniciado (0/3 revisiones).';
                        this.cdr.markForCheck();
                    }
                });
            },
            error: () => {
                this.validacionTutoria = 'error';
                this.mensajeTutoria = 'El estudiante no tiene tutor asignado aún.';
                this.cdr.markForCheck();
            }
        });
    }

    guardar(): void {
        if (this.form.invalid) return;
        this.enviando = true;
        this.errorConflicto = null;
        const { fecha, hora, salaId } = this.form.value;
        this.cronogramaService.crearCronograma(this.solicitudId, salaId, fecha, hora).subscribe({
            next: (c) => {
                this.enviando = false;
                this.cronogramaExistente = c;
                this.notification.success('Pre-sustentación programada correctamente.', '¡Programado!');
                this.cdr.markForCheck();
                setTimeout(() => this.router.navigate(['/dashboard/admin/revisar-solicitudes']), 1500);
            },
            error: (err) => {
                this.enviando = false;
                this.errorConflicto = err.error?.error || 'No se pudo programar. Verifica la disponibilidad.';
                this.notification.error(this.errorConflicto!, 'Conflicto de horario');
                this.cdr.markForCheck();
            }
        });
    }

    asignarAutomatico(): void {
        this.asignandoAuto = true;
        this.errorConflicto = null;
        this.cronogramaService.asignarAutomatico(this.solicitudId).subscribe({
            next: (c) => {
                this.asignandoAuto = false;
                this.cronogramaExistente = c;
                this.notification.success(
                    `Asignado automáticamente: ${this.formatFecha(c.fechaInicio)} — Sala: ${c.sala?.nombre}`,
                    '✓ Cronograma automático'
                );
                this.cdr.markForCheck();
            },
            error: (err) => {
                this.asignandoAuto = false;
                this.errorConflicto = err.error?.error || 'No se encontró disponibilidad automática.';
                this.notification.error(this.errorConflicto!, 'Sin disponibilidad');
                this.cdr.markForCheck();
            }
        });
    }

    eliminarCronograma(): void {
        if (!this.cronogramaExistente) return;
        this.cronogramaService.eliminar(this.cronogramaExistente.id).subscribe({
            next: () => { this.cronogramaExistente = null; this.notification.success('Cronograma eliminado.', 'Eliminado'); this.cdr.markForCheck(); }
        });
    }

    formatFecha(f: string): string {
        if (!f) return '—';
        return new Date(f).toLocaleString('es-EC', { dateStyle: 'medium', timeStyle: 'short' });
    }
}
