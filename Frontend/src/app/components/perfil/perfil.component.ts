import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-perfil',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './perfil.component.html',
    styleUrls: ['./perfil.component.css']
})
export class PerfilComponent implements OnInit {
    usuario: any = null;
    emailNotificaciones = '';
    telefono = '';
    guardando = false;
    cargando = true;
    emailInvalido = false;

    private apiUrl = 'http://localhost:8080/api/usuarios';

    constructor(
        private http: HttpClient,
        private authService: AuthService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargarPerfil();
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    cargarPerfil(): void {
        const id = this.authService.getUserId();
        this.http.get<any>(`${this.apiUrl}/${id}`).subscribe({
            next: (u) => {
                this.usuario = u;
                this.emailNotificaciones = u.emailNotificaciones || '';
                this.telefono = u.telefono || '';
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.notification.error('No se pudo cargar el perfil.', 'Error');
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    validarEmail(): boolean {
        if (!this.emailNotificaciones) { this.emailInvalido = false; return true; }
        const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        this.emailInvalido = !regex.test(this.emailNotificaciones);
        return !this.emailInvalido;
    }

    guardar(): void {
        if (!this.validarEmail()) {
            this.notification.error('Ingresa un correo electrónico válido.', 'Correo inválido');
            return;
        }
        this.guardando = true;
        const id = this.authService.getUserId();
        this.http.patch(`${this.apiUrl}/${id}/perfil`, {
            emailNotificaciones: this.emailNotificaciones,
            telefono: this.telefono
        }).subscribe({
            next: () => {
                this.authService.marcarEmailNotiConfigurado();
                this.notification.success('Perfil actualizado. Las notificaciones llegarán a: ' + this.emailNotificaciones, '✓ Guardado');
                this.guardando = false;
                this.cdr.markForCheck();
            },
            error: (err) => {
                const msg = err?.error?.error || 'No se pudo actualizar el perfil.';
                this.notification.error(msg, 'Error');
                this.guardando = false;
                this.cdr.markForCheck();
            }
        });
    }

    get rolLabel(): string {
        const map: Record<string, string> = {
            ADMIN: 'Coordinador',
            DOCENTE: 'Docente / Jurado',
            ESTUDIANTE: 'Estudiante'
        };
        return map[this.authService.getRole()] || this.authService.getRole();
    }
}
