import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { RolService, Rol } from '../../../services/rol.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-gestionar-roles',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './gestionar-roles.component.html',
    styleUrls: ['./gestionar-roles.component.css']
})
export class GestionarRolesComponent implements OnInit {
    roles: Rol[] = [];
    cargando = true;

    modalNuevoAbierto = false;
    nuevoCodigo = '';
    nuevoNombre = '';
    guardandoNuevo = false;

    modalEditarAbierto = false;
    rolEditando: Rol | null = null;
    guardandoEdicion = false;

    constructor(
        private rolService: RolService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargar();
    }

    private async swal() {
        return (await import('sweetalert2')).default;
    }

    cargar(): void {
        this.cargando = true;
        this.rolService.listarRoles().subscribe({
            next: (data) => {
                this.roles = data;
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.notification.error('No se pudo cargar la lista de roles.', 'Error');
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    esRolProtegido(rol: Rol): boolean {
        return ['ADMIN', 'COORDINADOR', 'DOCENTE', 'ESTUDIANTE'].includes(rol.codigo);
    }

    abrirModalNuevo(): void {
        this.nuevoCodigo = '';
        this.nuevoNombre = '';
        this.modalNuevoAbierto = true;
    }

    cerrarModalNuevo(): void {
        this.modalNuevoAbierto = false;
    }

    crearRol(): void {
        if (!this.nuevoCodigo.trim() || !this.nuevoNombre.trim()) {
            this.notification.error('Completa el código y el nombre del rol.', 'Campos requeridos');
            return;
        }
        this.guardandoNuevo = true;
        this.rolService.crearRol({ codigo: this.nuevoCodigo, nombre: this.nuevoNombre }).subscribe({
            next: () => {
                this.guardandoNuevo = false;
                this.modalNuevoAbierto = false;
                this.notification.success(`Rol "${this.nuevoNombre}" creado. Asígnale permisos en Gestionar Permisos.`, '✓ Rol creado');
                this.cargar();
            },
            error: (err) => {
                this.guardandoNuevo = false;
                const msg = err?.error?.error || 'No se pudo crear el rol.';
                this.notification.error(msg, 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    abrirModalEditar(rol: Rol): void {
        this.rolEditando = { ...rol };
        this.modalEditarAbierto = true;
    }

    cerrarModalEditar(): void {
        this.modalEditarAbierto = false;
        this.rolEditando = null;
    }

    guardarEdicion(): void {
        const r = this.rolEditando;
        if (!r || !r.id || !r.nombre.trim()) return;
        this.guardandoEdicion = true;
        this.rolService.renombrarRol(r.id, r.nombre).subscribe({
            next: () => {
                this.guardandoEdicion = false;
                this.modalEditarAbierto = false;
                this.notification.success('Rol actualizado.', '✓ Actualizado');
                this.cargar();
            },
            error: (err) => {
                this.guardandoEdicion = false;
                const msg = err?.error?.error || 'No se pudo actualizar el rol.';
                this.notification.error(msg, 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    async eliminar(rol: Rol): Promise<void> {
        if (!rol.id) return;
        const Swal = await this.swal();
        Swal.fire({
            title: '¿Eliminar rol?',
            text: `Se eliminará el rol "${rol.nombre}" (${rol.codigo}). Esta acción no se puede deshacer.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc2626'
        }).then(result => {
            if (!result.isConfirmed) { this.cdr.markForCheck(); return; }
            this.rolService.eliminarRol(rol.id!).subscribe({
                next: () => {
                    this.notification.success('Rol eliminado.', '✓ Eliminado');
                    this.cargar();
                },
                error: (err) => {
                    const msg = err?.error?.error || 'No se pudo eliminar el rol.';
                    this.notification.error(msg, 'Error');
                    this.cdr.markForCheck();
                }
            });
        });
    }
}
