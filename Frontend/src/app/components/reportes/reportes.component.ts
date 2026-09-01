import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  ReporteService, ResumenReporte, ConteoReporte, ActividadDocente, FiltroReporte
} from '../../services/reporte.service';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

/**
 * Dashboard de reportes para COORDINADOR y ADMINISTRADOR (permiso REPORTES_VER).
 * El COORDINADOR consulta la gestión académica de las pre-sustentaciones; el
 * ADMINISTRADOR además ve el estado global de las actas y la actividad por docente.
 * Todas las cifras las calcula el backend con COUNT/GROUP BY (ReporteServiceImpl).
 */
@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.css']
})
export class ReportesComponent implements OnInit {
  cargando = true;
  esAdmin = false;

  resumen: ResumenReporte | null = null;
  actividad: ActividadDocente[] = [];
  porCarrera: Array<Record<string, any>> = [];

  filtro: FiltroReporte = {};

  constructor(
    private reporteService: ReporteService,
    private auth: AuthService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.esAdmin = this.auth.getRole() === 'ADMIN';
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    forkJoin({
      resumen: this.reporteService.resumen(this.filtro),
      actividad: this.reporteService.actividadDocente(),
      porCarrera: this.reporteService.porCarrera()
    }).subscribe({
      next: (r) => {
        this.resumen = r.resumen;
        this.actividad = (r.actividad || []).slice(0, 15);
        this.porCarrera = r.porCarrera || [];
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notification.error('No se pudieron cargar los reportes.', 'Error');
        this.cargando = false;
        this.cdr.markForCheck();
      }
    });
  }

  aplicarFiltro(): void { this.cargar(); }
  limpiarFiltro(): void { this.filtro = {}; this.cargar(); }

  /** Ancho de barra relativo al máximo de la serie (gráfico de barras CSS puro). */
  barra(valor: number, serie: ConteoReporte[]): number {
    const max = Math.max(1, ...serie.map(s => s.cantidad));
    return Math.round((valor / max) * 100);
  }

  descargarEstadisticasPdf(): void {
    this.reporteService.estadisticasPdf().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = 'estadisticas_evaluaciones.pdf'; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.notification.error('No se pudo generar el PDF.', 'Error')
    });
  }

  descargarCronogramaPdf(): void {
    this.reporteService.cronogramaPdf().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = 'cronograma_presustentaciones.pdf'; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.notification.error('No se pudo generar el PDF.', 'Error')
    });
  }
}
