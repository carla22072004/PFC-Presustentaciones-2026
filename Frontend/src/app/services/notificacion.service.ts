import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NotificacionService {
    private api = 'http://localhost:8080/api/notificaciones';

    // Badge reactivo — cualquier componente puede suscribirse
    private _badge = new BehaviorSubject<number>(0);
    badge$ = this._badge.asObservable();

    constructor(private http: HttpClient) {}

    listarPorUsuario(id: number): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/usuario/${id}`);
    }

    contarNoLeidas(id: number): Observable<{total: number}> {
        return this.http.get<any>(`${this.api}/usuario/${id}/no-leidas`).pipe(
            tap((r: any) => this._badge.next(r.total ?? 0))
        );
    }

    marcarLeida(id: number): Observable<any> {
        return this.http.patch(`${this.api}/${id}/marcar-leida`, {}).pipe(
            tap(() => { const v = this._badge.value; if (v > 0) this._badge.next(v - 1); })
        );
    }

    marcarTodasLeidas(usuarioId: number): Observable<any> {
        return this.http.patch(`${this.api}/usuario/${usuarioId}/marcar-todas-leidas`, {}).pipe(
            tap(() => this._badge.next(0))
        );
    }

    /** Fuerza recarga del badge desde el servidor */
    refrescarBadge(usuarioId: number): void {
        this.contarNoLeidas(usuarioId).subscribe({ error: () => {} });
    }
}