import { Component, OnInit } from '@angular/core';
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
    private solicitudService: SolicitudService
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
      next: (response) => {
        if (response && response.data) {
          this.seguimiento = response.data;
        } else {
          this.error = 'Error al cargar los datos del seguimiento.';
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'No se pudo cargar el seguimiento. ' + (err.error?.message || '');
        this.loading = false;
      }
    });
  }

  volver(): void {
    this.router.navigate(['/solicitudes/listar']);
  }
}
