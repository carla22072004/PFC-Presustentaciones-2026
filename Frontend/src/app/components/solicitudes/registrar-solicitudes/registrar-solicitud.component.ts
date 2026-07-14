import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';
import { CatalogoService, ModalidadTitulacion } from '../../../services/catalogo.service';

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
    cargandoModalidades = true;
    modalidades: ModalidadTitulacion[] = [];
    errorModalidades = false;

    constructor(
        private fb: FormBuilder,
        private solicitudService: SolicitudService,
        private authService: AuthService,
        private router: Router,
        private notification: NotificationService,
        private catalogoService: CatalogoService
    ) {}

    ngOnInit(): void {
        this.solicitudForm = this.fb.group({
            tituloTema: ['', [Validators.required, Validators.minLength(10)]],
            modalidadTitulacion: [null, Validators.required]
        });

        this.catalogoService.listarModalidades().subscribe({
            next: (data) => {
                this.modalidades = data;
                this.cargandoModalidades = false;
            },
            error: () => {
                this.errorModalidades = true;
                this.cargandoModalidades = false;
                this.notification.error('No se pudieron cargar las modalidades. Verifica la conexión.', 'Error');
            }
        });
    }

    enviarFormulario(): void {
        if (this.solicitudForm.valid) {
            this.enviando = true;
            const usuarioId = this.authService.getUserId();
            const rawValue = this.solicitudForm.value;

            // Send the modalidad as { id: <number> } so the backend can resolve the entity
            const payload = {
                tituloTema: rawValue.tituloTema,
                modalidadTitulacion: { id: Number(rawValue.modalidadTitulacion) }
            };

            this.solicitudService.registrarSolicitud(usuarioId, payload).subscribe({
                next: () => {
                    this.enviando = false;
                    this.notification.success('Tu tema de tesis ha sido registrado con éxito.', 'Registro Completado');
                    this.router.navigate(['/dashboard/solicitudes/mis-tramites']);
                },
                error: (err) => {
                    this.enviando = false;
                    const msg = err?.error?.error ?? 'No se pudo guardar la solicitud. Verifica que Spring Boot esté activo.';
                    this.notification.error(msg, 'Error de Conexión');
                }
            });
        } else {
            this.notification.error('Por favor, llena todos los campos correctamente.', 'Formulario Incompleto');
        }
    }
}