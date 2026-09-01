import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ActaService, ActaResumen, Pagina } from '../../../services/acta.service';
import { NotificationService } from '../../../services/notification.service';

/**
 * "Mis actas" del DOCENTE (permiso ACTAS_VER_PROPIAS). El backend solo devuelve las actas
 * de pre-sustentaciones donde el docente autenticado es tutor o jurado -- no puede ver ni
 * el ID de un acta ajena. Solo lectura: el docente consulta estado/detalle/historial pero
 * NO cambia el estado (eso es de coordinador/administrador).
 */
@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-mis-actas',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mis-actas.component.html',
  styleUrls: ['./mis-actas.component.css']
})
export class MisActasComponent implements OnInit {
  pagina: Pagina<ActaResumen> | null = null;
  cargando = true;
  page = 0;
  readonly size = 10;

  constructor(
    private actaService: ActaService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando = true;
    this.actaService.misActas(this.page, this.size).subscribe({
      next: (p) => { this.pagina = p; this.cargando = false; this.cdr.markForCheck(); },
      error: () => {
        this.notification.error('No se pudieron cargar tus actas.', 'Error');
        this.cargando = false; this.cdr.markForCheck();
      }
    });
  }

  irAPagina(p: number): void {
    if (!this.pagina || p < 0 || p >= this.pagina.totalPages || p === this.page) return;
    this.page = p; this.cargar();
  }

  estadoClase(estado: string): string {
    const m: Record<string, string> = {
      GENERADA: 'est-generada', REVISADA: 'est-revisada', OBSERVADA: 'est-observada',
      FINALIZADA: 'est-finalizada', ANULADA: 'est-anulada'
    };
    return m[estado] || 'est-generada';
  }
}
