import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { DocenteService } from '../../../../services/docente.service';

/**
 * Combobox con búsqueda (typeahead) para elegir un docente.
 *
 * ERR-02: la tabla docente tiene ~9,807 filas. Un <select> nativo con un <option> por
 * docente congelaba el navegador al abrirlo. No hay ninguna librería de combobox
 * instalada en el proyecto (sin ng-select/primeng/Angular Material), así que este
 * componente construye el typeahead a mano: un <input> con debounce de 300ms que
 * consulta GET /api/docentes/paginado?q=... (máximo 10 resultados por búsqueda) en vez
 * de traer la tabla completa.
 */
@Component({
    selector: 'app-docente-buscador',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './docente-buscador.component.html',
    styleUrls: ['./docente-buscador.component.css']
})
export class DocenteBuscadorComponent implements OnInit, OnDestroy {
    @Input() placeholder = 'Buscar docente por nombre...';
    @Input() disabled = false;
    /** IDs de docentes a excluir de los resultados (ej. jurados ya asignados a esta solicitud) */
    @Input() excludeIds: number[] = [];
    @Output() seleccionado = new EventEmitter<any | null>();

    query = '';
    resultados: any[] = [];
    mostrarLista = false;
    cargando = false;
    docenteElegido: any = null;

    private busquedaSub = new Subject<string>();
    private subs = new Subscription();

    constructor(private docenteService: DocenteService, private cdr: ChangeDetectorRef) {}

    ngOnInit(): void {
        this.subs.add(
            this.busquedaSub.pipe(debounceTime(300)).subscribe((q) => this.buscar(q))
        );
    }

    ngOnDestroy(): void { this.subs.unsubscribe(); }

    onInput(): void {
        // Escribir invalida la selección previa hasta que se elija un resultado de la lista
        if (this.docenteElegido) {
            this.docenteElegido = null;
            this.seleccionado.emit(null);
        }
        this.mostrarLista = true;
        if (this.query.trim().length < 2) {
            this.resultados = [];
            return;
        }
        this.busquedaSub.next(this.query.trim());
    }

    private buscar(q: string): void {
        this.cargando = true;
        this.docenteService.buscarPaginado(0, 10, q).subscribe({
            next: (res) => {
                this.resultados = (res.content || []).filter(d => !this.excludeIds.includes(d.id));
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => { this.resultados = []; this.cargando = false; this.cdr.markForCheck(); }
        });
    }

    elegir(d: any): void {
        this.docenteElegido = d;
        this.query = this.nombreCompleto(d);
        this.mostrarLista = false;
        this.seleccionado.emit(d);
        this.cdr.markForCheck();
    }

    /** Cierra la lista con un pequeño retraso para que el click en un resultado registre antes del blur */
    onBlur(): void {
        setTimeout(() => { this.mostrarLista = false; this.cdr.markForCheck(); }, 150);
    }

    onFocus(): void {
        if (this.query.trim().length >= 2) this.mostrarLista = true;
    }

    nombreCompleto(d: any): string {
        const u = d?.usuario;
        return u ? `${u.nombre} ${u.apellido}` : `Docente #${d?.id}`;
    }

    /** Permite al componente padre preseleccionar un docente (ej. desde la lista de "Sugeridos") */
    seleccionarExterno(d: any): void {
        this.elegir(d);
    }

    /** Permite al componente padre limpiar la selección (ej. después de enviar el formulario) */
    limpiar(): void {
        this.query = '';
        this.resultados = [];
        this.mostrarLista = false;
        this.docenteElegido = null;
        this.cdr.markForCheck();
    }
}
