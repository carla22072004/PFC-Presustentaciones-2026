import { Component, ViewEncapsulation, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { EstudianteService, EstudianteDTO, EstadoAcademico, CrearEstudianteRequest, ActualizarEstudianteRequest } from '../../../services/estudiante.service';
import { CatalogoService, Carrera, PeriodoAcademico } from '../../../services/catalogo.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-gestionar-estudiantes',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './gestionar-estudiantes.component.html',
    styleUrls: ['./gestionar-estudiantes.component.css']
})
export class GestionarEstudiantesComponent implements OnInit, OnDestroy {
    estudiantes: EstudianteDTO[] = [];
    carreras: Carrera[] = [];
    periodos: PeriodoAcademico[] = [];
    estadosAcademicos: EstadoAcademico[] = [];

    cargando = true;
    filtroTexto = '';

    paginaActual = 0;
    tamanioPagina = 20;
    totalElementos = 0;
    totalPaginas = 0;
    private busquedaSub = new Subject<string>();
    private subs = new Subscription();

    modalNuevoAbierto = false;
    guardando = false;
    nuevoEstudiante: CrearEstudianteRequest = this.estudianteVacio();

    modalEditarAbierto = false;
    estudianteEditandoId: number | null = null;
    edicion: ActualizarEstudianteRequest = {};
    guardandoEdicion = false;

    constructor(
        private estudianteService: EstudianteService,
        private catalogoService: CatalogoService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.catalogoService.listarCarreras().subscribe({
            next: (c) => { this.carreras = c; this.cdr.markForCheck(); },
            error: () => {}
        });
        this.catalogoService.listarPeriodosAcademicos().subscribe({
            next: (p) => { this.periodos = p; this.cdr.markForCheck(); },
            error: () => {}
        });
        this.estudianteService.listarEstadosAcademicos().subscribe({
            next: (e) => { this.estadosAcademicos = e; this.cdr.markForCheck(); },
            error: () => {}
        });
        this.cargar();
        this.subs.add(
            this.busquedaSub.pipe(debounceTime(350)).subscribe(() => {
                this.paginaActual = 0;
                this.cargar();
            })
        );
    }

    ngOnDestroy(): void { this.subs.unsubscribe(); }

    onBuscarChange(): void { this.busquedaSub.next(this.filtroTexto); }

    private estudianteVacio(): CrearEstudianteRequest {
        return { nombre: '', apellido: '', email: '', password: '', telefono: '', carreraId: 0, periodoIngresoId: undefined, semestreActual: 1 };
    }

    cargar(): void {
        this.cargando = true;
        this.estudianteService.listarPaginado(this.paginaActual, this.tamanioPagina, this.filtroTexto || undefined).subscribe({
            next: (data) => {
                this.estudiantes = data.content;
                this.totalElementos = data.totalElements;
                this.totalPaginas = data.totalPages;
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.notification.error('No se pudo cargar la lista de estudiantes.', 'Error');
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    get paginasVisibles(): number[] {
        const total = this.totalPaginas;
        const actual = this.paginaActual;
        const ventana = 2;
        let inicio = Math.max(0, actual - ventana);
        let fin = Math.min(total - 1, actual + ventana);
        if (fin - inicio < ventana * 2) {
            inicio = Math.max(0, fin - ventana * 2);
            fin = Math.min(total - 1, inicio + ventana * 2);
        }
        const paginas: number[] = [];
        for (let i = inicio; i <= fin; i++) paginas.push(i);
        return paginas;
    }

    irAPagina(pagina: number): void {
        if (pagina < 0 || pagina >= this.totalPaginas || pagina === this.paginaActual) return;
        this.paginaActual = pagina;
        this.cargar();
    }

    paginaAnterior(): void { this.irAPagina(this.paginaActual - 1); }
    paginaSiguiente(): void { this.irAPagina(this.paginaActual + 1); }

    abrirModalNuevo(): void {
        this.nuevoEstudiante = this.estudianteVacio();
        this.modalNuevoAbierto = true;
    }

    cerrarModalNuevo(): void { this.modalNuevoAbierto = false; }

    crearEstudiante(): void {
        const e = this.nuevoEstudiante;
        if (!e.nombre || !e.apellido || !e.email || !e.password || !e.carreraId) {
            this.notification.error('Completa nombre, apellido, correo, contraseña y carrera.', 'Campos requeridos');
            return;
        }
        this.guardando = true;
        this.estudianteService.crear(e).subscribe({
            next: () => {
                this.guardando = false;
                this.modalNuevoAbierto = false;
                this.notification.success(`Estudiante ${e.email} registrado correctamente.`, '✓ Estudiante registrado');
                this.cargar();
            },
            error: (err) => {
                this.guardando = false;
                const msg = err?.error?.error || err?.error?.mensaje || 'No se pudo registrar el estudiante.';
                this.notification.error(msg, 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    abrirModalEditar(est: EstudianteDTO): void {
        this.estudianteEditandoId = est.id;
        this.edicion = {
            carreraId: est.carreraId ?? undefined,
            periodoIngresoId: est.periodoIngresoId ?? undefined,
            semestreActual: est.semestreActual,
            telefono: est.telefono ?? undefined,
            estadoAcademicoCodigo: est.estadoAcademicoCodigo
        };
        this.modalEditarAbierto = true;
    }

    cerrarModalEditar(): void {
        this.modalEditarAbierto = false;
        this.estudianteEditandoId = null;
    }

    guardarEdicion(): void {
        if (!this.estudianteEditandoId) return;
        this.guardandoEdicion = true;
        this.estudianteService.actualizar(this.estudianteEditandoId, this.edicion).subscribe({
            next: (actualizado) => {
                this.guardandoEdicion = false;
                this.modalEditarAbierto = false;
                const original = this.estudiantes.find(x => x.id === actualizado.id);
                if (original) {
                    original.carreraId = actualizado.carreraId;
                    original.carreraNombre = actualizado.carreraNombre;
                    original.periodoIngresoId = actualizado.periodoIngresoId;
                    original.periodoIngresoNombre = actualizado.periodoIngresoNombre;
                    original.semestreActual = actualizado.semestreActual;
                    original.telefono = actualizado.telefono;
                    original.estadoAcademicoCodigo = actualizado.estadoAcademicoCodigo;
                    original.estadoAcademicoNombre = actualizado.estadoAcademicoNombre;
                }
                this.estudianteEditandoId = null;
                this.notification.success('Estudiante actualizado correctamente.', '✓ Actualizado');
                this.cdr.markForCheck();
            },
            error: (err) => {
                this.guardandoEdicion = false;
                const msg = err?.error?.error || err?.error?.mensaje || 'No se pudo actualizar el estudiante.';
                this.notification.error(msg, 'Error');
                this.cdr.markForCheck();
            }
        });
    }
}
