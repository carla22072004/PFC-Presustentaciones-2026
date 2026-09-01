import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ActaService, ActaDetalle, HistorialActaEntrada } from '../../../services/acta.service';
import { NotificationService } from '../../../services/notification.service';

/**
 * Timeline de trazabilidad de un acta (RF-Actas-Historial). Los datos vienen de
 * presus.historial_estados_acta vía GET /api/v1/actas/{id}/historial -- persistidos en
 * PostgreSQL, no en el frontend. El backend aplica el control de acceso (un docente solo
 * ve el historial de sus propias actas); si no tiene permiso responde 403 y se muestra aviso.
 */
@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-historial-acta',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './historial-acta.component.html',
  styleUrls: ['./historial-acta.component.css']
})
export class HistorialActaComponent implements OnInit {
  actaId!: number;
  acta: ActaDetalle | null = null;
  historial: HistorialActaEntrada[] = [];
  cargando = true;
  sinAcceso = false;

  constructor(
    private route: ActivatedRoute,
    private actaService: ActaService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.actaId = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.actaService.detalle(this.actaId).subscribe({
      next: (d) => { this.acta = d; this.cdr.markForCheck(); },
      error: () => { this.cdr.markForCheck(); }
    });
    this.actaService.historial(this.actaId).subscribe({
      next: (h) => { this.historial = h; this.cargando = false; this.cdr.markForCheck(); },
      error: (err) => {
        this.cargando = false;
        if (err?.status === 403) { this.sinAcceso = true; }
        else { this.notification.error('No se pudo cargar el historial del acta.', 'Error'); }
        this.cdr.markForCheck();
      }
    });
  }

  accionLabel(a: string): string {
    const m: Record<string, string> = {
      CREAR: 'Acta creada',
      CAMBIO_ESTADO: 'Estado cambiado',
      FIRMA_COMPLETA: 'Acta finalizada (firma completa)'
    };
    return m[a] || a;
  }

  estadoClase(estado: string | null): string {
    const m: Record<string, string> = {
      GENERADA: 'est-generada', REVISADA: 'est-revisada', OBSERVADA: 'est-observada',
      FINALIZADA: 'est-finalizada', ANULADA: 'est-anulada'
    };
    return (estado && m[estado]) || 'est-generada';
  }

  rolLabel(rol: string | null): string {
    const m: Record<string, string> = {
      ADMIN: 'ADMINISTRADOR', COORDINADOR: 'COORDINADOR', DOCENTE: 'DOCENTE', ESTUDIANTE: 'ESTUDIANTE'
    };
    return (rol && (m[rol] || rol)) || 'SISTEMA';
  }
}
