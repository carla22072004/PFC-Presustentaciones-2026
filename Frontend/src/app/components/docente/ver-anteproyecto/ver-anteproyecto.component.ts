import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AnteproyectoService } from '../../../services/anteproyecto.service';
import { AuthService } from '../../../services/auth.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-ver-anteproyecto',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './ver-anteproyecto.component.html',
    styleUrls: ['./ver-anteproyecto.component.css']
})
export class VerAnteproyectoComponent implements OnInit {
    solicitudId!: number;
    anteproyecto: any = null;
    cargando = true;
    error = '';
    pdfUrl: SafeResourceUrl | null = null;
    mostrarPdf = false;

    get backRoute(): string {
        const rol = this.authService.getRole();
        if (rol === 'ESTUDIANTE') return '/dashboard/solicitudes/mis-tramites';
        if (rol === 'ADMIN' || rol === 'COORDINADOR') return '/dashboard/admin/revisar-solicitudes';
        return '/dashboard/jurado/mis-asignaciones';
    }

    constructor(
        private route: ActivatedRoute,
        private anteproyectoService: AnteproyectoService,
        private authService: AuthService,
        private sanitizer: DomSanitizer,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
        this.cargarAnteproyecto();
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    cargarAnteproyecto(): void {
        this.cargando = true;
        this.error = '';
        this.anteproyectoService.obtenerPorSolicitud(this.solicitudId).subscribe({
            next: (data) => { this.anteproyecto = data; this.cargando = false; this.cdr.markForCheck(); },
            error: (err) => {
                this.error = err.status === 404
                    ? 'El estudiante aún no ha subido el anteproyecto.'
                    : 'Error al cargar el anteproyecto.';
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    abrirPdf(): void {
        this.cargando = true;
        const token = this.authService.getToken();
        fetch(this.anteproyectoService.getUrlVisualizacion(this.solicitudId), {
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .then(res => { if (!res.ok) throw new Error(); return res.blob(); })
            .then(blob => {
                this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(URL.createObjectURL(blob));
                this.mostrarPdf = true;
                this.cargando = false;
                this.cdr.markForCheck();
            })
            .catch(() => {
                this.error = 'No se pudo cargar el PDF. Verifica que el archivo exista.';
                this.cargando = false;
                this.cdr.markForCheck();
            });
    }

    descargarPdf(): void {
        const token = this.authService.getToken();
        fetch(this.anteproyectoService.getUrlVisualizacion(this.solicitudId), {
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .then(res => res.blob())
            .then(blob => {
                const a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = `anteproyecto_solicitud_${this.solicitudId}.pdf`;
                a.click();
            });
    }

    getBadgeClass(estado: string): string {
        const map: Record<string, string> = {
            'ENVIADO': 'badge-enviado', 'APROBADO': 'badge-aprobado', 'RECHAZADO': 'badge-rechazado',
        };
        return map[estado] || 'badge-default';
    }
}
