import { Component, ViewEncapsulation, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JuradoService } from '../../../services/jurado.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';
import { TutoriaService } from '../../../services/tutoria.service';
import { AuthService } from '../../../services/auth.service';
import { DocenteBuscadorComponent } from './docente-buscador/docente-buscador.component';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-asignar-jurados',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule, DocenteBuscadorComponent],
    templateUrl: './asignar-jurados.component.html',
    styleUrls: ['./asignar-jurados.component.css']
})
export class AsignarJuradosComponent implements OnInit {
    solicitudId!: number;
    solicitud: any = null;
    jurados: any[] = [];
    tutor: any = null;
    docentesSugeridos: any[] = [];
    cargando = true;
    procesando = false;
    tutoriaCompletada: boolean | null = null;
    tutoriaTieneAsignacion = false;
    tutoriaFasesAprobadas = 0;

    formJurado!: FormGroup;
    formTutor!: FormGroup;

    @ViewChild('buscadorJurado') buscadorJuradoRef?: DocenteBuscadorComponent;
    @ViewChild('buscadorTutor') buscadorTutorRef?: DocenteBuscadorComponent;

    readonly ROLES = [
        { value: 'PRESIDENTE', label: 'Presidente del Tribunal' },
        { value: 'VOCAL_1',    label: 'Vocal 1' },
        { value: 'VOCAL_2',    label: 'Vocal 2' },
    ];

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private fb: FormBuilder,
        private juradoService: JuradoService,
        private solicitudService: SolicitudService,
        private notification: NotificationService,
        private tutoriaService: TutoriaService,
        private authService: AuthService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
        this.formJurado = this.fb.group({
            docenteId: ['', Validators.required],
            rol:       ['', Validators.required],
        });
        this.formTutor = this.fb.group({
            docenteId: ['', Validators.required],
        });
        this.cargarDatos();
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    cargarDatos(): void {
        this.cargando = true;
        this.solicitudService.obtenerPorId(this.solicitudId).subscribe({
            next: (s) => {
                this.solicitud = s;
                this.verificarTutoria(this.solicitudId);
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => { this.tutoriaCompletada = false; this.cargando = false; this.cdr.markForCheck(); }
        });
        this.cargarJurados();
        this.cargarSugerencias();
    }

    verificarTutoria(solicitudId: number): void {
        this.tutoriaService.obtenerTutorPorSolicitud(solicitudId).subscribe({
            next: (tutor) => {
                this.tutor = tutor;
                const tutorId: number = tutor?.id;
                if (!tutorId) {
                    this.tutoriaTieneAsignacion = false;
                    this.tutoriaCompletada = false;
                    this.cdr.markForCheck();
                    return;
                }
                this.tutoriaService.obtenerResumen(tutorId, this.authService.getUserId()).subscribe({
                    next: (resumen) => {
                        this.tutoriaTieneAsignacion = true;
                        this.tutoriaFasesAprobadas = resumen.fasesAprobadas;
                        this.tutoriaCompletada = resumen.estadoTutoria === 'COMPLETADA';
                        this.cdr.markForCheck();
                    },
                    error: () => {
                        this.tutoriaTieneAsignacion = false;
                        this.tutoriaCompletada = false;
                        this.cdr.markForCheck();
                    }
                });
            },
            error: () => {
                this.tutor = null;
                this.tutoriaTieneAsignacion = false;
                this.tutoriaCompletada = false;
                this.cdr.markForCheck();
            }
        });
    }

    cargarJurados(): void {
        this.juradoService.listarPorSolicitud(this.solicitudId).subscribe({
            next: (j) => { this.jurados = j; this.cdr.markForCheck(); },
            error: () => { this.jurados = []; this.cdr.markForCheck(); }
        });
    }

    cargarTutor(): void {
        this.juradoService.obtenerTutor(this.solicitudId).subscribe({
            next: (t) => { this.tutor = t; this.cdr.markForCheck(); },
            error: () => { this.tutor = null; this.cdr.markForCheck(); }
        });
    }

    cargarSugerencias(): void {
        this.juradoService.sugerirDocentes(this.solicitudId, 8).subscribe({
            next: (d) => { this.docentesSugeridos = d; this.cdr.markForCheck(); },
            error: () => { this.docentesSugeridos = []; this.cdr.markForCheck(); }
        });
    }

    /** IDs a excluir de los resultados del buscador de jurado (ya asignados a esta solicitud) */
    get idsExcluidosJurado(): number[] {
        return this.jurados.map(j => j.docente?.id).filter((id): id is number => id != null);
    }

    /** IDs a excluir de los resultados del buscador de tutor (el tutor actual, si hay uno) */
    get idsExcluidosTutor(): number[] {
        const tutorId = this.tutor?.docente?.id;
        return tutorId != null ? [tutorId] : [];
    }

    get rolesDisponibles() {
        const rolesOcupados = this.jurados.map(j => j.rol);
        return this.ROLES.filter(r => !rolesOcupados.includes(r.value));
    }

    get tribunalCompleto(): boolean {
        return this.jurados.length >= 3;
    }

    asignarJurado(): void {
        if (this.formJurado.invalid) return;
        this.procesando = true;
        const { docenteId, rol } = this.formJurado.value;
        this.juradoService.asignarJurado(this.solicitudId, docenteId, rol).subscribe({
            next: () => {
                this.notification.success(`Jurado asignado como ${rol}`, '✓ Asignado');
                this.formJurado.reset();
                this.buscadorJuradoRef?.limpiar();
                this.cargarJurados();
                this.cargarSugerencias();
                this.procesando = false;
                this.cdr.markForCheck();
            },
            error: (err) => {
                const msg = err.error?.error || 'No se pudo asignar el jurado.';
                this.notification.error(msg, 'Error');
                this.procesando = false;
                this.cdr.markForCheck();
            }
        });
    }

    asignarAutomaticamente(): void {
        this.procesando = true;
        this.juradoService.asignarAutomaticamente(this.solicitudId).subscribe({
            next: (j) => {
                this.jurados = j;
                this.notification.success('Jurados asignados automáticamente según disponibilidad.', '✓ Éxito');
                this.cargarSugerencias();
                this.procesando = false;
                this.cdr.markForCheck();
            },
            error: (err) => {
                const msg = err.error?.error || 'No se pudo asignar automáticamente.';
                this.notification.error(msg, 'Error');
                this.procesando = false;
                this.cdr.markForCheck();
            }
        });
    }

    eliminarJurado(juradoId: number): void {
        if (!confirm('¿Eliminar este jurado?')) return;
        this.juradoService.eliminarJurado(juradoId).subscribe({
            next: () => {
                this.notification.success('Jurado eliminado.', '');
                this.cargarJurados();
                this.cargarSugerencias();
            },
            error: () => this.notification.error('No se pudo eliminar.', 'Error')
        });
    }

    asignarTutor(): void {
        if (this.formTutor.invalid) return;
        this.procesando = true;
        this.juradoService.asignarTutor(this.solicitudId, this.formTutor.value.docenteId).subscribe({
            next: () => {
                this.notification.success('Tutor asignado correctamente.', '✓ Asignado');
                this.formTutor.reset();
                this.buscadorTutorRef?.limpiar();
                this.cargarSugerencias();
                this.verificarTutoria(this.solicitudId);
                this.procesando = false;
                this.cdr.markForCheck();
            },
            error: (err) => {
                const msg = err.error?.error || 'No se pudo asignar tutor.';
                this.notification.error(msg, 'Error');
                this.procesando = false;
                this.cdr.markForCheck();
            }
        });
    }

    onDocenteJuradoSeleccionado(d: any): void {
        this.formJurado.patchValue({ docenteId: d?.id ?? '' });
    }

    onDocenteTutorSeleccionado(d: any): void {
        this.formTutor.patchValue({ docenteId: d?.id ?? '' });
    }

    seleccionarParaJurado(d: any): void {
        this.formJurado.patchValue({ docenteId: d.id });
        this.buscadorJuradoRef?.seleccionarExterno(d);
    }

    seleccionarParaTutor(d: any): void {
        this.formTutor.patchValue({ docenteId: d.id });
        this.buscadorTutorRef?.seleccionarExterno(d);
    }

    getRolLabel(rol: string): string {
        return this.ROLES.find(r => r.value === rol)?.label || rol;
    }

    getNombreDocente(j: any): string {
        const u = j?.docente?.usuario;
        return u ? `${u.nombre} ${u.apellido}` : '—';
    }
}
