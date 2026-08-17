import { Component, ViewEncapsulation, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { UsuarioService, Usuario } from '../../../services/usuario.service';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-gestion-usuarios',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './gestion-usuarios.component.html',
    styleUrls: ['./gestion-usuarios.component.css']
})
export class GestionUsuariosComponent implements OnInit, OnDestroy {
    usuarios: Usuario[] = [];
    cargando = true;
    filtroTexto = '';
    modalAbierto = false;
    guardando = false;

    // La tabla de usuarios reales llega a decenas de miles de filas (datos de carga k6) — siempre pagina server-side.
    paginaActual = 0;
    tamanioPagina = 20;
    totalElementos = 0;
    totalPaginas = 0;
    private busquedaSub = new Subject<string>();
    private subs = new Subscription();

    readonly roles: Usuario['rol'][] = ['ADMIN', 'COORDINADOR', 'DOCENTE', 'ESTUDIANTE'];

    nuevoUsuario: Usuario = this.usuarioVacio();

    constructor(
        private usuarioService: UsuarioService,
        private authService: AuthService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargar();
        this.subs.add(
            this.busquedaSub.pipe(debounceTime(350)).subscribe(() => {
                this.paginaActual = 0;
                this.cargar();
            })
        );
    }

    ngOnDestroy(): void { this.subs.unsubscribe(); }

    // sweetalert2 se carga en un chunk aparte (no en el bundle inicial): solo se
    // necesita cuando el usuario abre un dialogo de confirmacion.
    private async swal() {
        return (await import('sweetalert2')).default;
    }

    onBuscarChange(): void { this.busquedaSub.next(this.filtroTexto); }

    private usuarioVacio(): Usuario {
        return { nombre: '', apellido: '', email: '', password: '', rol: 'ESTUDIANTE' };
    }

    cargar(): void {
        this.cargando = true;
        this.usuarioService.listarPaginado(this.paginaActual, this.tamanioPagina, this.filtroTexto || undefined).subscribe({
            next: (data) => {
                this.usuarios = data.content;
                this.totalElementos = data.totalElements;
                this.totalPaginas = data.totalPages;
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.notification.error('No se pudo cargar la lista de usuarios.', 'Error');
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

    esUsuarioActual(u: Usuario): boolean {
        return u.id === this.authService.getUserId();
    }

    abrirModalNuevo(): void {
        this.nuevoUsuario = this.usuarioVacio();
        this.modalAbierto = true;
    }

    cerrarModal(): void {
        this.modalAbierto = false;
    }

    crearUsuario(): void {
        if (!this.nuevoUsuario.nombre || !this.nuevoUsuario.apellido || !this.nuevoUsuario.email || !this.nuevoUsuario.password) {
            this.notification.error('Completa todos los campos obligatorios.', 'Campos requeridos');
            return;
        }
        this.guardando = true;
        this.usuarioService.crear(this.nuevoUsuario).subscribe({
            next: () => {
                this.guardando = false;
                this.modalAbierto = false;
                this.notification.success(`Usuario ${this.nuevoUsuario.email} creado correctamente.`, '✓ Usuario creado');
                this.cargar();
            },
            error: (err) => {
                this.guardando = false;
                const msg = err?.error?.mensaje || err?.error?.error || 'No se pudo crear el usuario.';
                this.notification.error(msg, 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    async cambiarRol(usuario: Usuario, nuevoRol: Usuario['rol']): Promise<void> {
        if (usuario.rol === nuevoRol || !usuario.id) return;
        const rolAnterior = usuario.rol;
        const Swal = await this.swal();
        Swal.fire({
            title: '¿Cambiar rol de usuario?',
            text: `${usuario.nombre} ${usuario.apellido} pasará de ${rolAnterior} a ${nuevoRol}.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, cambiar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#2563eb'
        }).then(result => {
            if (!result.isConfirmed) { this.cdr.markForCheck(); return; }
            this.usuarioService.actualizar(usuario.id!, { ...usuario, rol: nuevoRol }).subscribe({
                next: () => {
                    usuario.rol = nuevoRol;
                    this.notification.success('Rol actualizado correctamente.', '✓ Actualizado');
                    this.cdr.markForCheck();
                },
                error: () => {
                    this.notification.error('No se pudo actualizar el rol.', 'Error');
                    this.cdr.markForCheck();
                }
            });
        });
    }

    toggleActivo(usuario: Usuario): void {
        if (!usuario.id) return;
        const accion = usuario.activo ? this.usuarioService.desactivar(usuario.id) : this.usuarioService.activar(usuario.id);
        accion.subscribe({
            next: () => {
                usuario.activo = !usuario.activo;
                this.notification.success(
                    usuario.activo ? 'Usuario activado.' : 'Usuario desactivado.',
                    '✓ Listo'
                );
                this.cdr.markForCheck();
            },
            error: () => this.notification.error('No se pudo cambiar el estado del usuario.', 'Error')
        });
    }

    async eliminar(usuario: Usuario): Promise<void> {
        if (!usuario.id) return;
        const Swal = await this.swal();
        Swal.fire({
            title: '¿Eliminar usuario?',
            text: `Esta acción no se puede deshacer: se eliminará a ${usuario.nombre} ${usuario.apellido} (${usuario.email}).`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc2626'
        }).then(result => {
            if (!result.isConfirmed) { this.cdr.markForCheck(); return; }
            this.usuarioService.eliminar(usuario.id!).subscribe({
                next: () => {
                    this.notification.success('Usuario eliminado.', '✓ Eliminado');
                    this.cargar();
                },
                error: () => {
                    this.notification.error('No se pudo eliminar el usuario. Puede tener registros asociados.', 'Error');
                    this.cdr.markForCheck();
                }
            });
        });
    }
}
