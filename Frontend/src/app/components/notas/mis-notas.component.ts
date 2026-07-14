import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { EvaluacionService } from '../../services/evaluacion.service';
import { AuthService } from '../../services/auth.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-mis-notas',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './mis-notas.component.html',
    styleUrls: ['./mis-notas.component.css']
})
export class MisNotasComponent implements OnInit {
    evaluaciones: any[] = [];
    cargando = true;
    estudianteId = 0;

    constructor(
        private evalService: EvaluacionService,
        private authService: AuthService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.estudianteId = this.authService.getUserId();
        this.cargar();
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    cargar(): void {
        this.cargando = true;
        this.evalService.listarPorUsuario(this.estudianteId).subscribe({
            next: (data) => { this.evaluaciones = data; this.cargando = false; this.cdr.markForCheck(); },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
        });
    }

    getBadge(resultado: string): string {
        return resultado === 'APROBADO' ? 'badge-aprobado' : 'badge-reprobado';
    }

    promedio(): number {
        if (!this.evaluaciones.length) return 0;
        const sum = this.evaluaciones.reduce((a, e) => a + (e.notaFinal || 0), 0);
        return Math.round((sum / this.evaluaciones.length) * 10) / 10;
    }
}
