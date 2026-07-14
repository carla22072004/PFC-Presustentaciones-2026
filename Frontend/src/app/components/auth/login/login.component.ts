import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './login.component.html',
    styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
    loginForm!: FormGroup;
    cargando = false;

    constructor(
        private fb: FormBuilder,
        private authService: AuthService,
        private router: Router,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        if (this.authService.isLoggedIn()) {
            this.router.navigate(['/dashboard']);
            return;
        }

        this.loginForm = this.fb.group({
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(4)]]
        });
    }

    onSubmit(): void {
        if (this.loginForm.valid) {
            this.cargando = true;
            this.authService.login(this.loginForm.value).subscribe({
                next: () => {
                    this.cargando = false;
                    this.cdr.markForCheck();
                    this.router.navigate(['/dashboard']);
                },
                error: (err: any) => {
                    this.cargando = false;
                    this.cdr.markForCheck();
                    if (err.status === 403) {
                        this.notification.error('El servidor bloqueó la petición (CORS)', 'Error 403');
                    } else if (err.status === 401) {
                        this.notification.error('Correo o contraseña incorrectos.', 'Acceso Denegado');
                    } else {
                        this.notification.error('No se pudo conectar con el servidor. Verifica que Spring Boot esté activo.', 'Error de Conexión');
                    }
                }
            });
        }
    }
}
