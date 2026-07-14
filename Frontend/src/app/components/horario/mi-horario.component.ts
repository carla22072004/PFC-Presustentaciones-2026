import { Component, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CronogramaService } from '../../services/cronograma.service';
import { AuthService } from '../../services/auth.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-mi-horario',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './mi-horario.component.html',
    styleUrls: ['./mi-horario.component.css']
})
export class MiHorarioComponent implements OnInit {
    cronogramas: any[] = [];
    cargando = true;

    constructor(
        private cronogramaService: CronogramaService,
        private authService: AuthService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargar();
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    cargar(): void {
        this.cargando = true;
        const usuarioId = this.authService.getUserId();
        this.cronogramaService.listarPorUsuario(usuarioId).subscribe({
            next: (data: any[]) => { this.cronogramas = data; this.cargando = false; this.cdr.markForCheck(); },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
        });
    }

    getBadge(estado: string): string {
        const map: Record<string, string> = { ACTIVO: 'badge-activo', CANCELADO: 'badge-cancelado' };
        return map[estado] || 'badge-default';
    }

    getFechaFin(cronograma: any): string {
        if (!cronograma.fechaInicio) return '';
        const inicio = new Date(cronograma.fechaInicio);
        inicio.setMinutes(inicio.getMinutes() + (cronograma.duracionMin || 45));
        return inicio.toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' });
    }
}
