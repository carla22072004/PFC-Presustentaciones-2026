import { Component, ViewEncapsulation, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificacionService } from '../../services/notificacion.service';
import { AuthService } from '../../services/auth.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-notificaciones',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './notificaciones.component.html',
    styleUrls: ['./notificaciones.component.css']
})
export class NotificacionesComponent implements OnInit, OnDestroy {
    notificaciones: any[] = [];
    cargando = false;
    usuarioId = 0;

    // Paginación server-side — un usuario activo (p.ej. un coordinador notificado en cada
    // solicitud enviada) puede acumular miles de notificaciones.
    paginaActual = 0;
    tamanioPagina = 20;
    totalElementos = 0;
    totalPaginas = 0;

    // Total real de no leídas (vía badge$, no calculado sobre la página cargada -- con
    // paginación, "no leídas" de la página actual ya no representa el total del usuario).
    noLeidas = 0;
    private badgeSub = new Subscription();

    constructor(
        private notiService: NotificacionService,
        private authService: AuthService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.usuarioId = this.authService.getUserId();
        this.badgeSub.add(
            this.notiService.badge$.subscribe(n => { this.noLeidas = n; this.cdr.markForCheck(); })
        );
        this.cargar();
        setTimeout(() => { if (this.cargando) { this.cargando = false; this.cdr.markForCheck(); } }, 10000);
    }

    ngOnDestroy(): void { this.badgeSub.unsubscribe(); }

    cargar(): void {
        this.cargando = true;
        this.notiService.listarPorUsuario(this.usuarioId, this.paginaActual, this.tamanioPagina).subscribe({
            next: (data) => {
                this.notificaciones = data.content;
                this.totalElementos = data.totalElements;
                this.totalPaginas = data.totalPages;
                this.cargando = false;
                this.cdr.markForCheck();
                this.notiService.refrescarBadge(this.usuarioId);
            },
            error: () => { this.cargando = false; this.cdr.markForCheck(); }
        });
    }

    irAPagina(pagina: number): void {
        if (pagina < 0 || pagina >= this.totalPaginas || pagina === this.paginaActual) return;
        this.paginaActual = pagina;
        this.cargar();
    }

    paginaAnterior(): void { this.irAPagina(this.paginaActual - 1); }
    paginaSiguiente(): void { this.irAPagina(this.paginaActual + 1); }

    marcarLeida(id: number): void {
        const n = this.notificaciones.find(x => x.id === id);
        if (n && n.leida) return;
        this.notiService.marcarLeida(id).subscribe({
            next: () => { if (n) n.leida = true; this.cdr.markForCheck(); }
        });
    }

    marcarTodas(): void {
        this.notiService.marcarTodasLeidas(this.usuarioId).subscribe({
            next: () => { this.notificaciones.forEach(n => n.leida = true); this.cdr.markForCheck(); }
        });
    }
}
