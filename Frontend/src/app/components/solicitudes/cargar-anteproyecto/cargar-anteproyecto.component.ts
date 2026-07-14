import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AnteproyectoService } from '../../../services/anteproyecto.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-cargar-anteproyecto',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './cargar-anteproyecto.component.html',
    styleUrls: ['./cargar-anteproyecto.component.css']
})
export class CargarAnteproyectoComponent implements OnInit {
    solicitudId!: number;
    archivoSeleccionado: File | null = null;
    subiendo = false;
    intentoEnvio = false;

    anteproyectoExistente: any = null;
    cargando = true;
    bloqueado = false;

    solicitudSuspendida = false;
    solicitudMotivoSuspension = '';

    constructor(
        private route: ActivatedRoute,
        private anteproyectoService: AnteproyectoService,
        private solicitudService: SolicitudService,
        private notification: NotificationService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
        this.verificarEstado();
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    verificarEstado(): void {
        this.cargando = true;
        
        this.solicitudService.obtenerPorId(this.solicitudId).subscribe({
            next: (solicitud) => {
                this.solicitudSuspendida = solicitud.estado === 'SUSPENDIDA';
                this.solicitudMotivoSuspension = solicitud.motivoSuspension || '';
                this.cdr.markForCheck();
            },
            error: () => {
                this.solicitudSuspendida = false;
                this.cdr.markForCheck();
            }
        });

        this.anteproyectoService.obtenerPorSolicitud(this.solicitudId).subscribe({
            next: (ap) => {
                this.anteproyectoExistente = ap;
                this.bloqueado = ap && ap.archivoPdf && ap.estado !== 'RECHAZADO';
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.anteproyectoExistente = null;
                this.bloqueado = false;
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    get estadoBadge(): string {
        const e = this.anteproyectoExistente?.estado;
        if (e === 'ENVIADO')   return 'badge-enviado';
        if (e === 'APROBADO')  return 'badge-aprobado';
        if (e === 'RECHAZADO') return 'badge-rechazado';
        return '';
    }

    onFileSelected(event: any): void {
        const file: File = event.target.files[0];
        if (!file) return;
        if (file.type !== 'application/pdf') {
            this.notification.error('Solo se permiten archivos en formato PDF.', 'Formato inválido');
            return;
        }
        if (file.size > 10 * 1024 * 1024) {
            this.notification.error('El archivo supera los 10 MB permitidos.', 'Archivo muy grande');
            return;
        }
        this.archivoSeleccionado = file;
        this.intentoEnvio = false;
    }

    quitarArchivo(): void {
        this.archivoSeleccionado = null;
    }

    subirArchivo(): void {
        if (this.solicitudSuspendida) {
            this.notification.error(
                'Tu solicitud ha sido suspendida. No puedes subir archivos hasta que se liftingue la suspensión.',
                'Solicitud suspendida'
            );
            return;
        }
        if (this.bloqueado) {
            this.notification.error(
                'No puedes reemplazar el PDF. El coordinador debe rechazar el anteproyecto para permitir una nueva carga.',
                'Acción no permitida'
            );
            return;
        }
        if (!this.archivoSeleccionado) {
            this.intentoEnvio = true;
            this.notification.error('Debes seleccionar un archivo PDF antes de enviar.', 'Sin archivo');
            return;
        }

        this.subiendo = true;
        this.anteproyectoService.enviarAnteproyecto(this.solicitudId, this.archivoSeleccionado).subscribe({
            next: () => {
                this.subiendo = false;
                this.notification.success('Anteproyecto enviado correctamente.', '¡Enviado!');
                this.router.navigate(['/dashboard/solicitudes/mis-tramites']);
            },
            error: (err) => {
                this.subiendo = false;
                const msg = err?.error?.mensaje || 'No se pudo enviar el anteproyecto.';
                this.notification.error(msg, 'Error');
                this.cdr.markForCheck();
            }
        });
    }
}
