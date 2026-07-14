import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-registrar-solicitud',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    templateUrl: './registrar-solicitud.component.html',
    styleUrls: ['./registrar-solicitud.component.css']
})
export class RegistrarSolicitudComponent implements OnInit {
    solicitudForm!: FormGroup;
    enviando = false;

    constructor(
        private fb: FormBuilder,
        private solicitudService: SolicitudService,
        private authService: AuthService,
        private router: Router,
        private notification: NotificationService
    ) {}

    ngOnInit(): void {
        this.solicitudForm = this.fb.group({
            tituloTema: ['', [Validators.required, Validators.minLength(10)]],
            modalidad: ['', Validators.required]
        });
    }

    enviarFormulario(): void {
        if (this.solicitudForm.valid) {
            this.enviando = true;
            const estudianteId = this.authService.getUserId();

            this.solicitudService.registrarSolicitud(estudianteId, this.solicitudForm.value).subscribe({
                next: () => {
                    this.enviando = false;
                    this.notification.success('Tu tema de tesis ha sido registrado con éxito.', 'Registro Completado');
                    this.router.navigate(['/dashboard/solicitudes/mis-tramites']);
                },
                error: () => {
                    this.enviando = false;
                    this.notification.error('No se pudo guardar la solicitud. Verifica que Spring Boot esté activo.', 'Error de Conexión');
                }
            });
        } else {
            this.notification.error('Por favor, llena todos los campos correctamente.', 'Formulario Incompleto');
        }
    }
}