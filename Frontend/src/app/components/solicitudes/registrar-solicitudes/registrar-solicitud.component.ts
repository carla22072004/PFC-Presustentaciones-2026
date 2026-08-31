import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';
import { CatalogoService, ModalidadTitulacion, LineaInvestigacion, AreaTematica } from '../../../services/catalogo.service';

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

    lineasInvestigacion: LineaInvestigacion[] = [];
    cargandoLineas = true;
    areasTematicas: AreaTematica[] = [];
    cargandoAreas = false;

    constructor(
        private fb: FormBuilder,
        private solicitudService: SolicitudService,
        private authService: AuthService,
        private router: Router,
        private notification: NotificationService,
        private catalogoService: CatalogoService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.solicitudForm = this.fb.group({
            tituloTema: ['', [Validators.required, Validators.minLength(10)]],
            modalidadTitulacion: [null, Validators.required],
            lineaInvestigacion: [null, Validators.required],
            areaTematica: [null, Validators.required]
        });

        this.catalogoService.listarModalidades().subscribe({
            next: (data) => {
                this.modalidades = data;
                this.cargandoModalidades = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.errorModalidades = true;
                this.cargandoModalidades = false;
                this.notification.error('No se pudieron cargar las modalidades. Verifica la conexión.', 'Error');
                this.cdr.markForCheck();
            }
        });

        this.catalogoService.listarLineasInvestigacion().subscribe({
            next: (data) => {
                this.lineasInvestigacion = data;
                this.cargandoLineas = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.cargandoLineas = false;
                this.notification.error('No se pudieron cargar las líneas de investigación. Verifica la conexión.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    /** Al elegir la línea de investigación, carga solo las áreas temáticas que le pertenecen */
    onLineaChange(): void {
        const lineaId = this.solicitudForm.get('lineaInvestigacion')?.value;
        this.areasTematicas = [];
        this.solicitudForm.get('areaTematica')?.setValue(null);
        if (!lineaId) return;

        this.cargandoAreas = true;
        this.catalogoService.listarAreasTematicas(Number(lineaId)).subscribe({
            next: (data) => {
                this.areasTematicas = data;
                this.cargandoAreas = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.cargandoAreas = false;
                this.notification.error('No se pudieron cargar las áreas temáticas. Verifica la conexión.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    enviarFormulario(): void {
        if (this.solicitudForm.valid) {
            this.enviando = true;
            const usuarioId = this.authService.getUserId();
            const rawValue = this.solicitudForm.value;

            // Send the modalidad/línea/área as { id: <number> } so the backend can resolve la entidad
            const payload = {
                tituloTema: rawValue.tituloTema,
                modalidadTitulacion: { id: Number(rawValue.modalidadTitulacion) },
                lineaInvestigacion: { id: Number(rawValue.lineaInvestigacion) },
                areaTematica: { id: Number(rawValue.areaTematica) }
            };

            this.solicitudService.registrarSolicitud(usuarioId, payload).subscribe({
                next: () => {
                    this.enviando = false;
                    this.notification.success('Tu tema de tesis ha sido registrado con éxito.', 'Registro Completado');
                    this.router.navigate(['/dashboard/solicitudes/mis-tramites']);
                    this.cdr.markForCheck();
                },
                error: (err) => {
                    this.enviando = false;
                    const msg = err?.error?.error ?? 'No se pudo guardar la solicitud. Verifica que Spring Boot esté activo.';
                    this.notification.error(msg, 'Error de Conexión');
                    this.cdr.markForCheck();
                }
            });
        } else {
            this.notification.error('Por favor, llena todos los campos correctamente.', 'Formulario Incompleto');
        }
    }
}
