import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ActaService } from '../../../services/acta.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { JuradoService } from '../../../services/jurado.service';
import { AuthService } from '../../../services/auth.service';
import { DocenteService } from '../../../services/docente.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-firmar-acta-docente',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './firmar-acta-docente.component.html',
    styleUrls: ['./firmar-acta-docente.component.css']
})
export class FirmarActaDocenteComponent implements OnInit {
    solicitudId!: number;
    solicitud: any = null;
    acta: any = null;
    jurados: any[] = [];
    tutor: any = null;
    cargando = true;
    firmando: string | null = null;

    docenteId: number | null = null;
    misRoles: string[] = [];

    readonly ROLES_LABEL: Record<string, string> = {
        PRESIDENTE: 'Presidente del Tribunal',
        VOCAL_1:    'Vocal 1',
        VOCAL_2:    'Vocal 2',
        TUTOR:      'Tutor del Proyecto',
    };

    constructor(
        private route: ActivatedRoute,
        private actaService: ActaService,
        private solicitudService: SolicitudService,
        private juradoService: JuradoService,
        private docenteService: DocenteService,
        private authService: AuthService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
        this.solicitudId = Number(this.route.snapshot.paramMap.get('id'));
        const userId = this.authService.getUserId();

        this.docenteService.obtenerPorUsuario(userId).subscribe({
            next: (docente) => { this.docenteId = docente.id; this.cargarDatos(); this.cdr.markForCheck(); },
            error: () => { this.cargando = false; this.notification.error('No se encontró el perfil de docente.', 'Error'); this.cdr.markForCheck(); }
        });
    }

    cargarDatos(): void {
        this.solicitudService.obtenerPorId(this.solicitudId).subscribe(s => { this.solicitud = s; this.cdr.markForCheck(); });

        this.juradoService.listarPorSolicitud(this.solicitudId).subscribe({
            next: (j) => {
                this.jurados = j;
                j.filter((x: any) => x.docente?.id === this.docenteId)
                    .forEach((x: any) => { if (!this.misRoles.includes(x.rol)) this.misRoles.push(x.rol); });
                this.cdr.markForCheck();
            },
            error: () => { this.cdr.markForCheck(); }
        });

        this.juradoService.obtenerTutor(this.solicitudId).subscribe({
            next: (t) => {
                this.tutor = t;
                if (t?.docente?.id === this.docenteId && !this.misRoles.includes('TUTOR')) {
                    this.misRoles.push('TUTOR');
                }
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
        });

        this.actaService.porSolicitud(this.solicitudId).subscribe({
            next: (a) => { this.acta = a; this.cdr.markForCheck(); },
            error: () => { this.acta = null; this.cdr.markForCheck(); }
        });
    }

    firmaEstado(rol: string): boolean {
        if (!this.acta) return false;
        const map: Record<string, boolean> = {
            PRESIDENTE: this.acta.firmadaPresidente,
            VOCAL_1:    this.acta.firmadaVocal1,
            VOCAL_2:    this.acta.firmadaVocal2,
            TUTOR:      this.acta.firmadaTutor,
        };
        return map[rol] ?? false;
    }

    get algunaFirmaHecha(): boolean {
        return this.misRoles.some(r => this.firmaEstado(r));
    }

    get todosFirmados(): boolean {
        return this.misRoles.length > 0 && this.misRoles.every(r => this.firmaEstado(r));
    }

    firmar(rol: string): void {
        if (!this.acta || this.firmaEstado(rol) || this.firmando) return;
        this.firmando = rol;
        this.actaService.firmarActa(this.acta.id, rol).subscribe({
            next: (a) => {
                this.acta = a;
                const msg = a.firmada
                    ? '¡Acta completamente firmada por todos los actores!'
                    : `Firma como ${this.ROLES_LABEL[rol]} registrada correctamente.`;
                this.notification.success(msg, '✍️ Firmado');
                this.firmando = null;
                this.cdr.markForCheck();
            },
            error: (err) => {
                this.notification.error(err.error?.error || 'No se pudo registrar la firma.', 'Error');
                this.firmando = null;
                this.cdr.markForCheck();
            }
        });
    }

    descargar(): void {
        if (!this.acta) return;
        this.actaService.descargarPdf(this.acta.id).subscribe({
            next: (blob) => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url; a.download = `acta_solicitud_${this.solicitudId}.pdf`; a.click();
                URL.revokeObjectURL(url);
            },
            error: () => this.notification.error('No se pudo descargar el PDF.', 'Error')
        });
    }

    getNombrePorRol(rol: string): string {
        if (rol === 'TUTOR') {
            const u = this.tutor?.docente?.usuario;
            return u ? `${u.nombre} ${u.apellido}` : '—';
        }
        const j = this.jurados.find((x: any) => x.rol === rol);
        const u = j?.docente?.usuario;
        return u ? `${u.nombre} ${u.apellido}` : '—';
    }

    get firmasCompletas(): string[] {
        const firmas = [];
        if (this.acta?.firmadaPresidente) firmas.push('Presidente');
        if (this.acta?.firmadaVocal1)     firmas.push('Vocal 1');
        if (this.acta?.firmadaVocal2)     firmas.push('Vocal 2');
        if (this.acta?.firmadaTutor)      firmas.push('Tutor');
        return firmas;
    }
}
