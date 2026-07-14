import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JuradoService } from '../../../services/jurado.service';
import { DocenteService } from '../../../services/docente.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';
import { TutoriaService } from '../../../services/tutoria.service';
import { AuthService } from '../../../services/auth.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-asignar-jurados',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    templateUrl: './asignar-jurados.component.html',
    styleUrls: ['./asignar-jurados.component.css']
})
export class AsignarJuradosComponent implements OnInit {
    solicitudId!: number;
    solicitud: any = null;
    jurados: any[] = [];
    tutor: any = null;
    docentesSugeridos: any[] = [];
    todosDocentes: any[] = [];
    cargando = true;
    procesando = false;
    tutoriaCompletada: boolean | null = null;
    tutoriaTieneAsignacion = false;
    tutoriaFasesAprobadas = 0;

    formJurado!: FormGroup;
    formTutor!: FormGroup;

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
        private docenteService: DocenteService,
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
                this.cdr.markForCheck();
            },
            error: () => { this.tutoriaCompletada = false; this.cdr.markForCheck(); }
        });
        this.docenteService.listar().subscribe({
            next: (d) => { this.todosDocentes = d; this.cargando = false; this.cdr.markForCheck(); },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
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

    get docentesDisponiblesParaJurado(): any[] {
        const asignadosIds = this.jurados.map(j => j.docente?.id);
        return this.todosDocentes.filter(d => !asignadosIds.includes(d.id));
    }

    get docentesDisponiblesParaTutor(): any[] {
        const tutorId = this.tutor?.docente?.id;
        return this.todosDocentes.filter(d => d.id !== tutorId);
    }

    get rolesDisponibles() {
        const rolesOcupados = this.jurados.map(j => j.rol);
        return this.ROLES.filter(r => !rolesOcupados.includes(r.value));
    }

    get tribunalCompleto(): boolean {
        return this.jurados.length >= 3;
    }

    getNombreDocente2(d: any): string {
        const u = d?.usuario;
        return u ? `${u.nombre} ${u.apellido}` : `Docente #${d.id}`;
    }

    asignarJurado(): void {
        if (this.formJurado.invalid) return;
        this.procesando = true;
        const { docenteId, rol } = this.formJurado.value;
        this.juradoService.asignarJurado(this.solicitudId, docenteId, rol).subscribe({
            next: () => {
                this.notification.success(`Jurado asignado como ${rol}`, '✓ Asignado');
                this.formJurado.reset();
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

    seleccionarParaJurado(docenteId: number): void {
        this.formJurado.patchValue({ docenteId });
    }

    seleccionarParaTutor(docenteId: number): void {
        this.formTutor.patchValue({ docenteId });
    }

    getRolLabel(rol: string): string {
        return this.ROLES.find(r => r.value === rol)?.label || rol;
    }

    getNombreDocente(j: any): string {
        const u = j?.docente?.usuario;
        return u ? `${u.nombre} ${u.apellido}` : '—';
    }
}
