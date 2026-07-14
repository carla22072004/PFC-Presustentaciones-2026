import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TutoriaService } from '../../../services/tutoria.service';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { TutoriaResumen } from '../../../models/tutoria.model';

@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-mis-tutorias',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mis-tutorias.component.html',
  styleUrls: ['./mis-tutorias.component.css']
})
export class MisTutoriasComponent implements OnInit {
  tutorias: TutoriaResumen[] = [];
  cargando = true;
  rol = '';

  constructor(
    private tutoriaService: TutoriaService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.rol = this.authService.getRole();
    const usuarioId = this.authService.getUserId();

    const peticion = this.rol === 'ESTUDIANTE'
      ? this.tutoriaService.obtenerTutoriasEstudiante(usuarioId)
      : this.tutoriaService.obtenerTutoriasDocente(usuarioId);

    peticion.subscribe({
      next: (data) => { this.tutorias = data; this.cargando = false; this.cdr.markForCheck(); },
      error: () => {
        this.notificationService.error('No se pudieron cargar las tutorías.', 'Error');
        this.cargando = false;
        this.cdr.markForCheck();
      }
    });
  }

  verDetalle(tutorId: number): void {
    this.router.navigate(['/dashboard/tutorias/detalle', tutorId]);
  }

  segmentos(total: number): number[] {
    return Array.from({ length: total }, (_, i) => i);
  }
}
