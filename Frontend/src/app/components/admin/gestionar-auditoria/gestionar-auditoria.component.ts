import { Component, ViewEncapsulation, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { AuditoriaService, RegistroAuditoria } from '../../../services/auditoria.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-gestionar-auditoria',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './gestionar-auditoria.component.html',
    styleUrls: ['./gestionar-auditoria.component.css']
})
export class GestionarAuditoriaComponent implements OnInit, OnDestroy {
    registros: RegistroAuditoria[] = [];
    tablas: string[] = [];
    cargando = true;

    readonly acciones = ['CREAR', 'MODIFICAR', 'ELIMINAR', 'ASIGNAR_PERMISO', 'QUITAR_PERMISO'];

    filtroTabla = '';
    filtroAccion = '';
    filtroTexto = '';
    private busquedaSub = new Subject<string>();
    private subs = new Subscription();

    paginaActual = 0;
    tamanioPagina = 20;
    totalElementos = 0;
    totalPaginas = 0;

    expandido: number | null = null;

    constructor(
        private auditoriaService: AuditoriaService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.auditoriaService.listarTablas().subscribe({
            next: (tablas) => { this.tablas = tablas; this.cdr.markForCheck(); },
            error: () => {}
        });
        this.cargar();
        this.subs.add(
            this.busquedaSub.pipe(debounceTime(300)).subscribe(() => {
                this.paginaActual = 0;
                this.cargar();
            })
        );
    }

    ngOnDestroy(): void { this.subs.unsubscribe(); }

    onBuscarChange(): void { this.busquedaSub.next(this.filtroTexto); }

    cargar(): void {
        this.cargando = true;
        this.auditoriaService.listarPaginado(
            this.paginaActual, this.tamanioPagina,
            this.filtroTabla || undefined, this.filtroAccion || undefined, this.filtroTexto || undefined
        ).subscribe({
            next: (data) => {
                this.registros = data.content;
                this.totalElementos = data.totalElements;
                this.totalPaginas = data.totalPages;
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.notification.error('No se pudo cargar el historial de auditoría.', 'Error');
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    setFiltroTabla(tabla: string): void {
        this.filtroTabla = this.filtroTabla === tabla ? '' : tabla;
        this.paginaActual = 0;
        this.cargar();
    }

    setFiltroAccion(accion: string): void {
        this.filtroAccion = this.filtroAccion === accion ? '' : accion;
        this.paginaActual = 0;
        this.cargar();
    }

    irAPagina(pagina: number): void {
        if (pagina < 0 || pagina >= this.totalPaginas || pagina === this.paginaActual) return;
        this.paginaActual = pagina;
        this.cargar();
    }

    paginaAnterior(): void { this.irAPagina(this.paginaActual - 1); }
    paginaSiguiente(): void { this.irAPagina(this.paginaActual + 1); }

    toggleExpandir(id: number): void {
        this.expandido = this.expandido === id ? null : id;
    }

    accionLabel(accion: string): string {
        const m: Record<string, string> = {
            CREAR: 'Creó', MODIFICAR: 'Modificó', ELIMINAR: 'Eliminó',
            ASIGNAR_PERMISO: 'Asignó permiso', QUITAR_PERMISO: 'Quitó permiso'
        };
        return m[accion] || accion;
    }

    accionBadgeClase(accion: string): string {
        const m: Record<string, string> = {
            CREAR: 'aud-badge-crear', MODIFICAR: 'aud-badge-modificar', ELIMINAR: 'aud-badge-eliminar',
            ASIGNAR_PERMISO: 'aud-badge-crear', QUITAR_PERMISO: 'aud-badge-eliminar'
        };
        return m[accion] || 'aud-badge-modificar';
    }

    /** Compara datosAnteriores vs datosNuevos campo por campo, solo los que cambiaron. */
    cambios(registro: RegistroAuditoria): { campo: string; antes: any; despues: any }[] {
        const antes = registro.datosAnteriores ? JSON.parse(registro.datosAnteriores) : {};
        const despues = registro.datosNuevos ? JSON.parse(registro.datosNuevos) : {};
        const claves = new Set([...Object.keys(antes), ...Object.keys(despues)]);
        const resultado: { campo: string; antes: any; despues: any }[] = [];
        claves.forEach(campo => {
            const a = antes[campo];
            const d = despues[campo];
            if (JSON.stringify(a) !== JSON.stringify(d)) {
                resultado.push({ campo, antes: a, despues: d });
            }
        });
        return resultado;
    }

    formatoValor(v: any): string {
        if (v === null || v === undefined) return '—';
        if (typeof v === 'object') return JSON.stringify(v);
        return String(v);
    }
}
