import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
    CatalogoService, Facultad, Carrera, ModalidadTitulacion, PeriodoAcademico,
    GuardarFacultadRequest, GuardarCarreraRequest, GuardarModalidadRequest, GuardarPeriodoRequest
} from '../../../services/catalogo.service';
import { NotificationService } from '../../../services/notification.service';

type Tipo = 'facultad' | 'carrera' | 'modalidad' | 'periodo';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-gestionar-carreras',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './gestionar-carreras.component.html',
    styleUrls: ['./gestionar-carreras.component.css']
})
export class GestionarCarrerasComponent implements OnInit {
    tabActiva: Tipo = 'facultad';

    facultades: Facultad[] = [];
    carreras: Carrera[] = [];
    modalidades: ModalidadTitulacion[] = [];
    periodos: PeriodoAcademico[] = [];
    cargando = true;

    modalAbierto = false;
    modalTipo: Tipo = 'facultad';
    modalModo: 'crear' | 'editar' = 'crear';
    modalId: number | null = null;
    guardando = false;

    formFacultad: GuardarFacultadRequest = { codigo: '', nombre: '' };
    formCarrera: GuardarCarreraRequest = { codigo: '', nombre: '', facultadId: 0, modalidadEstudio: '' };
    formModalidad: GuardarModalidadRequest = { codigo: '', nombre: '' };
    formPeriodo: GuardarPeriodoRequest = { codigo: '', nombre: '', fechaInicio: '', fechaFin: '', activo: false };

    constructor(
        private catalogoService: CatalogoService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargarTodo();
    }

    private async swal() {
        return (await import('sweetalert2')).default;
    }

    cambiarTab(t: Tipo): void {
        this.tabActiva = t;
    }

    cargarTodo(): void {
        this.cargando = true;
        this.catalogoService.listarFacultades().subscribe({
            next: (f) => { this.facultades = f; this.terminarCargaParcial(); },
            error: () => this.terminarCargaParcial()
        });
        this.catalogoService.listarCarreras().subscribe({
            next: (c) => { this.carreras = c; this.terminarCargaParcial(); },
            error: () => this.terminarCargaParcial()
        });
        this.catalogoService.listarModalidades().subscribe({
            next: (m) => { this.modalidades = m; this.terminarCargaParcial(); },
            error: () => this.terminarCargaParcial()
        });
        this.catalogoService.listarPeriodosAcademicos().subscribe({
            next: (p) => { this.periodos = p; this.terminarCargaParcial(); },
            error: () => this.terminarCargaParcial()
        });
    }

    private cargasPendientes = 0;
    private terminarCargaParcial(): void {
        this.cargasPendientes++;
        if (this.cargasPendientes >= 4) {
            this.cargando = false;
        }
        this.cdr.markForCheck();
    }

    cargar(): void {
        this.cargasPendientes = 0;
        this.cargarTodo();
    }

    // ── Modal genérico ──────────────────────────────────────────────

    abrirNuevo(tipo: Tipo): void {
        this.modalTipo = tipo;
        this.modalModo = 'crear';
        this.modalId = null;
        if (tipo === 'facultad') this.formFacultad = { codigo: '', nombre: '' };
        if (tipo === 'carrera') this.formCarrera = { codigo: '', nombre: '', facultadId: this.facultades[0]?.id || 0, modalidadEstudio: '' };
        if (tipo === 'modalidad') this.formModalidad = { codigo: '', nombre: '' };
        if (tipo === 'periodo') this.formPeriodo = { codigo: '', nombre: '', fechaInicio: '', fechaFin: '', activo: false };
        this.modalAbierto = true;
    }

    abrirEditar(tipo: Tipo, item: any): void {
        this.modalTipo = tipo;
        this.modalModo = 'editar';
        this.modalId = item.id;
        if (tipo === 'facultad') this.formFacultad = { codigo: item.codigo, nombre: item.nombre };
        if (tipo === 'carrera') this.formCarrera = { codigo: item.codigo, nombre: item.nombre, facultadId: item.facultad?.id || 0, modalidadEstudio: item.modalidadEstudio || '' };
        if (tipo === 'modalidad') this.formModalidad = { codigo: item.codigo, nombre: item.nombre };
        if (tipo === 'periodo') this.formPeriodo = { codigo: item.codigo, nombre: item.nombre, fechaInicio: item.fechaInicio, fechaFin: item.fechaFin, activo: item.activo };
        this.modalAbierto = true;
    }

    cerrarModal(): void {
        this.modalAbierto = false;
        this.modalId = null;
    }

    guardar(): void {
        this.guardando = true;
        const exito = (mensaje: string) => {
            this.guardando = false;
            this.modalAbierto = false;
            this.notification.success(mensaje, '✓ Guardado');
            this.cargar();
        };
        const error = (err: any) => {
            this.guardando = false;
            const msg = err?.error?.error || err?.error?.mensaje || 'No se pudo guardar.';
            this.notification.error(msg, 'Error');
            this.cdr.markForCheck();
        };

        if (this.modalTipo === 'facultad') {
            if (!this.formFacultad.codigo || !this.formFacultad.nombre) {
                this.guardando = false;
                this.notification.error('Completa código y nombre.', 'Campos requeridos');
                return;
            }
            const obs = this.modalModo === 'crear'
                ? this.catalogoService.crearFacultad(this.formFacultad)
                : this.catalogoService.actualizarFacultad(this.modalId!, this.formFacultad);
            obs.subscribe({ next: () => exito('Facultad guardada correctamente.'), error });
        } else if (this.modalTipo === 'carrera') {
            if (!this.formCarrera.codigo || !this.formCarrera.nombre || !this.formCarrera.facultadId) {
                this.guardando = false;
                this.notification.error('Completa código, nombre y facultad.', 'Campos requeridos');
                return;
            }
            const obs = this.modalModo === 'crear'
                ? this.catalogoService.crearCarrera(this.formCarrera)
                : this.catalogoService.actualizarCarrera(this.modalId!, this.formCarrera);
            obs.subscribe({ next: () => exito('Carrera guardada correctamente.'), error });
        } else if (this.modalTipo === 'modalidad') {
            if (!this.formModalidad.codigo || !this.formModalidad.nombre) {
                this.guardando = false;
                this.notification.error('Completa código y nombre.', 'Campos requeridos');
                return;
            }
            const obs = this.modalModo === 'crear'
                ? this.catalogoService.crearModalidad(this.formModalidad)
                : this.catalogoService.actualizarModalidad(this.modalId!, this.formModalidad);
            obs.subscribe({ next: () => exito('Modalidad guardada correctamente.'), error });
        } else if (this.modalTipo === 'periodo') {
            if (!this.formPeriodo.codigo || !this.formPeriodo.nombre || !this.formPeriodo.fechaInicio || !this.formPeriodo.fechaFin) {
                this.guardando = false;
                this.notification.error('Completa código, nombre y ambas fechas.', 'Campos requeridos');
                return;
            }
            const obs = this.modalModo === 'crear'
                ? this.catalogoService.crearPeriodo(this.formPeriodo)
                : this.catalogoService.actualizarPeriodo(this.modalId!, this.formPeriodo);
            obs.subscribe({ next: () => exito('Período académico guardado correctamente.'), error });
        }
    }

    async eliminar(tipo: Tipo, item: any): Promise<void> {
        const Swal = await this.swal();
        Swal.fire({
            title: '¿Eliminar registro?',
            text: `Se eliminará "${item.nombre}". Esta acción no se puede deshacer.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc2626'
        }).then(result => {
            if (!result.isConfirmed) { this.cdr.markForCheck(); return; }
            const obs = tipo === 'facultad' ? this.catalogoService.eliminarFacultad(item.id)
                : tipo === 'carrera' ? this.catalogoService.eliminarCarrera(item.id)
                : tipo === 'modalidad' ? this.catalogoService.eliminarModalidad(item.id)
                : this.catalogoService.eliminarPeriodo(item.id);
            obs.subscribe({
                next: () => {
                    this.notification.success('Registro eliminado.', '✓ Eliminado');
                    this.cargar();
                },
                error: (err) => {
                    const msg = err?.error?.error || 'No se pudo eliminar el registro.';
                    this.notification.error(msg, 'Error');
                    this.cdr.markForCheck();
                }
            });
        });
    }
}
