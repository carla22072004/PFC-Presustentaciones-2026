import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BackupService, BackupInfo } from '../../../services/backup.service';
import { NotificationService } from '../../../services/notification.service';

/**
 * Apartado de administrador para generar y gestionar respaldos completos de la base de
 * datos. Cada respaldo es un dump `pg_dump -Fc` que el backend guarda en el servidor;
 * desde aquí se puede generar uno nuevo, descargarlo, restaurarlo o eliminarlo.
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-gestionar-respaldos',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './gestionar-respaldos.component.html',
    styleUrls: ['./gestionar-respaldos.component.css']
})
export class GestionarRespaldosComponent implements OnInit {
    respaldos: BackupInfo[] = [];
    cargando = true;
    generando = false;
    /** nombre del respaldo con una operación (restaurar/eliminar) en curso */
    ocupado: string | null = null;

    constructor(
        private backupService: BackupService,
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
        this.backupService.listar().subscribe({
            next: (data) => {
                this.respaldos = data || [];
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.notification.error('No se pudo cargar la lista de respaldos.', 'Error');
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    generar(): void {
        if (this.generando) return;
        this.generando = true;
        this.cdr.markForCheck();
        this.backupService.generar().subscribe({
            next: (info) => {
                this.generando = false;
                this.notification.success(
                    `Respaldo "${info.nombre}" generado (${info.tamanoLegible}).`, 'Respaldo creado');
                this.cargar();
            },
            error: (err) => {
                this.generando = false;
                this.notification.error(
                    err?.error?.message || 'No se pudo generar el respaldo.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    descargar(r: BackupInfo): void {
        this.backupService.descargar(r.nombre).subscribe({
            next: (blob) => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = r.nombre;
                a.click();
                URL.revokeObjectURL(url);
            },
            error: () => this.notification.error('No se pudo descargar el respaldo.', 'Error')
        });
    }

    async restaurar(r: BackupInfo): Promise<void> {
        const Swal = await this.swal();
        const res = await Swal.fire({
            title: '¿Restaurar la base de datos?',
            html: `Se reemplazará <b>todo</b> el contenido actual de la base por el del respaldo `
                + `<b>${r.nombre}</b> (${this.fecha(r.fechaCreacion)}).<br><br>`
                + `Esta acción no se puede deshacer. Después conviene reiniciar el backend.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, restaurar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc2626'
        });
        if (!res.isConfirmed) return;

        this.ocupado = r.nombre;
        this.cdr.markForCheck();
        this.backupService.restaurar(r.nombre).subscribe({
            next: (resp: any) => {
                this.ocupado = null;
                this.notification.success(
                    resp?.message || resp || 'Base de datos restaurada desde el respaldo.', 'Restauración completa');
                this.cdr.markForCheck();
            },
            error: (err) => {
                this.ocupado = null;
                this.notification.error(
                    err?.error?.message || 'No se pudo restaurar la base de datos.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    async eliminar(r: BackupInfo): Promise<void> {
        const Swal = await this.swal();
        const res = await Swal.fire({
            title: '¿Eliminar este respaldo?',
            text: `Se eliminará permanentemente el archivo "${r.nombre}". Esta acción no se puede deshacer.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc2626'
        });
        if (!res.isConfirmed) return;

        this.ocupado = r.nombre;
        this.cdr.markForCheck();
        this.backupService.eliminar(r.nombre).subscribe({
            next: () => {
                this.ocupado = null;
                this.notification.success('Respaldo eliminado.', 'Listo');
                this.cargar();
            },
            error: (err) => {
                this.ocupado = null;
                this.notification.error(
                    err?.error?.message || 'No se pudo eliminar el respaldo.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    fecha(iso: string): string {
        const d = new Date(iso);
        return isNaN(d.getTime()) ? iso : d.toLocaleString('es-EC');
    }
}
