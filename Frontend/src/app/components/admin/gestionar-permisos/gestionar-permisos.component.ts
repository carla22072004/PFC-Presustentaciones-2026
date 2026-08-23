import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RolService, Rol, Permiso } from '../../../services/rol.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-gestionar-permisos',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './gestionar-permisos.component.html',
    styleUrls: ['./gestionar-permisos.component.css']
})
export class GestionarPermisosComponent implements OnInit {
    roles: Rol[] = [];
    permisos: Permiso[] = [];
    categorias: string[] = [];
    cargando = true;

    /** rolId -> Set de códigos de permiso que tiene ahora mismo (estado local, reflejado del backend) */
    asignados = new Map<number, Set<string>>();
    /** celda (rolId+codigo) que está guardando ahora mismo, para deshabilitarla mientras responde el backend */
    guardando = new Set<string>();

    constructor(
        private rolService: RolService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargar();
    }

    cargar(): void {
        this.cargando = true;
        this.rolService.listarPermisos().subscribe({
            next: (permisos) => {
                this.permisos = permisos;
                this.categorias = [...new Set(permisos.map(p => p.categoria))];
                this.rolService.listarRoles().subscribe({
                    next: (roles) => {
                        this.roles = roles;
                        this.asignados.clear();
                        for (const r of roles) {
                            this.asignados.set(r.id!, new Set(r.permisos || []));
                        }
                        this.cargando = false;
                        this.cdr.markForCheck();
                    },
                    error: () => { this.cargando = false; this.cdr.markForCheck(); }
                });
            },
            error: () => {
                this.notification.error('No se pudo cargar el catálogo de permisos.', 'Error');
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    permisosDeCategoria(categoria: string): Permiso[] {
        return this.permisos.filter(p => p.categoria === categoria);
    }

    tienePermiso(rolId: number, codigo: string): boolean {
        return this.asignados.get(rolId)?.has(codigo) ?? false;
    }

    private celdaKey(rolId: number, codigo: string): string {
        return `${rolId}:${codigo}`;
    }

    celdaGuardando(rolId: number, codigo: string): boolean {
        return this.guardando.has(this.celdaKey(rolId, codigo));
    }

    toggle(rol: Rol, permiso: Permiso, event: Event): void {
        if (!rol.id) return;
        const key = this.celdaKey(rol.id, permiso.codigo);
        const checkbox = event.target as HTMLInputElement;
        if (this.guardando.has(key)) { checkbox.checked = this.tienePermiso(rol.id, permiso.codigo); return; }

        const set = this.asignados.get(rol.id) ?? new Set<string>();
        const estabaAsignado = set.has(permiso.codigo);
        const nuevoSet = new Set(set);
        if (estabaAsignado) nuevoSet.delete(permiso.codigo); else nuevoSet.add(permiso.codigo);

        this.guardando.add(key);
        this.cdr.markForCheck();

        this.rolService.actualizarPermisosDeRol(rol.id, [...nuevoSet]).subscribe({
            next: (codigosFinales) => {
                this.asignados.set(rol.id!, new Set(codigosFinales));
                this.guardando.delete(key);
                this.cdr.markForCheck();
            },
            error: (err) => {
                // El backend rechazó el cambio (p.ej. dejaría a ningún rol con
                // ROLES_PERMISOS_GESTIONAR): el checkbox nativo ya se marcó/desmarcó solo al
                // hacer click, ANTES de que Angular procese este handler, y como el valor
                // lógico (this.asignados) no cambia aquí, el binding [checked] no lo repinta
                // por sí solo -- se revierte el DOM del checkbox directamente.
                checkbox.checked = estabaAsignado;
                const msg = err?.error?.error || 'No se pudo actualizar el permiso.';
                this.notification.error(msg, 'Cambio rechazado');
                this.guardando.delete(key);
                this.cdr.markForCheck();
            }
        });
    }
}
