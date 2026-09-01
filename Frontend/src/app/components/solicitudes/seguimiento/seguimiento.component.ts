import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { SolicitudService } from '../../../services/solicitud.service';
import { SeguimientoDTO } from '../../../models/seguimiento.dto';

@Component({
  selector: 'app-seguimiento',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seguimiento.component.html',
  styleUrls: ['./seguimiento.component.css']
})
export class SeguimientoComponent implements OnInit {
  solicitudId: number | null = null;
  seguimiento: SeguimientoDTO | null = null;
  loading: boolean = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private solicitudService: SolicitudService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.solicitudId = +idParam;
      this.cargarSeguimiento();
    } else {
      this.error = 'No se proporcionó un ID de solicitud válido.';
      this.loading = false;
    }
  }

  cargarSeguimiento(): void {
    if (!this.solicitudId) return;

    this.solicitudService.obtenerSeguimiento(this.solicitudId).subscribe({
      next: (response: any) => {
        // El authInterceptor ya desempaqueta el ResponseWrapper -> `response` ES el
        // SeguimientoDTO. Se acepta también `response.data` por si el interceptor no
        // estuviera activo (tests / llamadas directas).
        const data: SeguimientoDTO | null = response?.etapas ? response : (response?.data ?? null);
        if (data && Array.isArray(data.etapas)) {
          this.seguimiento = data;
        } else {
          this.error = 'No hay información de seguimiento para esta solicitud todavía.';
        }
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'No se pudo cargar el seguimiento. ' + (err?.error?.message || '');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  volver(): void {
    this.router.navigate(['/dashboard/solicitudes/mis-tramites']);
  }
}
