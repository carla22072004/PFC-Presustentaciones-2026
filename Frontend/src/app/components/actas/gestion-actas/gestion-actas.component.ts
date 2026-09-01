import { Component, ViewEncapsulation, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { ActaService, ActaResumen, Pagina, FiltroActas } from '../../../services/acta.service';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';

/**
 * Gestión de actas para ADMINISTRADOR (permiso ACTAS_GESTIONAR) y cambio de estado para
 * ADMINISTRADOR / COORDINADOR (permiso ACTA_ESTADO_CAMBIAR). Buscar/filtrar por estado,
 * carrera, rango de fechas y texto libre; ver detalle e historial; cambiar el estado del
 * acta según las reglas del flujo (el backend valida la transición y registra el cambio).
 */
@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-gestion-actas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './gestion-actas.component.html',
  styleUrls: ['./gestion-actas.component.css']
})
export class GestionActasComponent implements OnInit, OnDestroy {
  pagina: Pagina<ActaResumen> | null = null;
  cargando = true;

  readonly estados = ['GENERADA', 'REVISADA', 'OBSERVADA', 'FINALIZADA', 'ANULADA'];
  /** Transiciones permitidas desde cada estado (espejo del backend ActaServiceImpl.TRANSICIONES). */
  readonly transiciones: Record<string, string[]> = {
    GENERADA: ['REVISADA', 'OBSERVADA', 'ANULADA'],
    REVISADA: ['FINALIZADA', 'OBSERVADA', 'ANULADA'],
    OBSERVADA: ['REVISADA', 'GENERADA', 'ANULADA'],
    FINALIZADA: ['ANULADA'],
    ANULADA: []
  };

  f: FiltroActas = { page: 0, size: 10 };
  private buscar$ = new Subject<void>();
  private subs = new Subscription();

  esAdmin = false;

  // Modal de cambio de estado
  modalActa: ActaResumen | null = null;
  nuevoEstado = '';
  motivo = '';
  guardando = false;

  constructor(
    private actaService: ActaService,
    private auth: AuthService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.esAdmin = this.auth.getRole() === 'ADMIN';
    this.cargar();
    this.subs.add(this.buscar$.pipe(debounceTime(300)).subscribe(() => { this.f.page = 0; this.cargar(); }));
  }

  ngOnDestroy(): void { this.subs.unsubscribe(); }

  onFiltroTexto(): void { this.buscar$.next(); }

  aplicarFiltros(): void { this.f.page = 0; this.cargar(); }

  limpiarFiltros(): void {
    this.f = { page: 0, size: 10 };
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.actaService.buscar(this.f).subscribe({
      next: (p) => { this.pagina = p; this.cargando = false; this.cdr.markForCheck(); },
      error: () => {
        this.notification.error('No se pudieron cargar las actas.', 'Error');
        this.cargando = false; this.cdr.markForCheck();
      }
    });
  }

  irAPagina(p: number): void {
    if (!this.pagina || p < 0 || p >= this.pagina.totalPages || p === this.f.page) return;
    this.f.page = p; this.cargar();
  }

  estadoClase(estado: string): string {
    const m: Record<string, string> = {
      GENERADA: 'est-generada', REVISADA: 'est-revisada', OBSERVADA: 'est-observada',
      FINALIZADA: 'est-finalizada', ANULADA: 'est-anulada'
    };
    return m[estado] || 'est-generada';
  }

  // ── Cambio de estado ────────────────────────────────────────────────────
  abrirModal(a: ActaResumen): void {
    this.modalActa = a;
    this.nuevoEstado = '';
    this.motivo = '';
  }

  cerrarModal(): void { this.modalActa = null; }

  get opcionesEstado(): string[] {
    if (!this.modalActa) return [];
    const base = this.transiciones[this.modalActa.estado] ?? [];
    // El ADMIN puede anular en cualquier momento (coincide con la regla del backend).
    return this.esAdmin && !base.includes('ANULADA') ? [...base, 'ANULADA'] : base;
  }

  get motivoObligatorio(): boolean {
    return this.nuevoEstado === 'OBSERVADA' || this.nuevoEstado === 'ANULADA';
  }

  confirmarCambio(): void {
    if (!this.modalActa || !this.nuevoEstado) return;
    if (this.motivoObligatorio && !this.motivo.trim()) {
      this.notification.error('Debes indicar un motivo para este cambio de estado.', 'Falta el motivo');
      return;
    }
    this.guardando = true;
    this.actaService.cambiarEstado(this.modalActa.id, this.nuevoEstado, this.motivo.trim() || undefined).subscribe({
      next: () => {
        this.guardando = false;
        this.notification.success('Estado del acta actualizado. El cambio quedó en el historial.', 'Listo');
        this.cerrarModal();
        this.cargar();
      },
      error: (err) => {
        this.guardando = false;
        this.notification.error(err?.error?.message || 'No se pudo cambiar el estado.', 'Error');
        this.cdr.markForCheck();
      }
    });
  }
}
