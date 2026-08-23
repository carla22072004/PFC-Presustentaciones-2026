import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TutorService, MiEstudianteTutorado } from '../../../services/tutor.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-mis-estudiantes',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './mis-estudiantes.component.html',
    styleUrls: ['./mis-estudiantes.component.css']
})
export class MisEstudiantesComponent implements OnInit {
    estudiantes: MiEstudianteTutorado[] = [];
    cargando = true;
    filtroTexto = '';

    constructor(
        private tutorService: TutorService,
        private notification: NotificationService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargar();
    }

    cargar(): void {
        this.cargando = true;
        this.tutorService.misEstudiantes().subscribe({
            next: (data) => { this.estudiantes = data; this.cargando = false; this.cdr.markForCheck(); },
            error: () => {
                this.notification.error('No se pudo cargar la lista de tus estudiantes.', 'Error');
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    get estudiantesFiltrados(): MiEstudianteTutorado[] {
        const q = this.filtroTexto.trim().toLowerCase();
        if (!q) return this.estudiantes;
        return this.estudiantes.filter(e =>
            `${e.nombre} ${e.apellido}`.toLowerCase().includes(q) ||
            e.email.toLowerCase().includes(q) ||
            (e.expedienteCodigo || '').toLowerCase().includes(q) ||
            (e.carreraNombre || '').toLowerCase().includes(q) ||
            e.tituloTema.toLowerCase().includes(q)
        );
    }

    verTutoria(tutorId: number): void {
        this.router.navigate(['/dashboard/tutorias/detalle', tutorId]);
    }
}
