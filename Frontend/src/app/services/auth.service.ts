import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = '/api/auth';

    /**
     * Permisos del usuario en curso (códigos). El JWT no los lleva — se consultan a
     * /api/me/permisos. El panel se suscribe a esto para mostrar/ocultar módulos: si a un rol
     * se le quita un permiso, el módulo asociado desaparece al recargar, sin cerrar sesión.
     */
    private _permisos = new BehaviorSubject<string[]>([]);
    permisos$ = this._permisos.asObservable();

    constructor(private http: HttpClient, private router: Router) {}

    login(credentials: { email: string; password: string }): Observable<any> {
        return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
            tap((res: any) => {
                if (res.token) {
                    localStorage.setItem('presus_token', res.token);
                    localStorage.setItem('user_id', String(res.id));
                    localStorage.setItem('user_role', res.rol);
                    localStorage.setItem('user_name', res.nombre);
                    // Guardar si ya tiene correo de notificaciones configurado
                    localStorage.setItem('email_noti_configurado', res.emailNotificaciones ? 'true' : 'false');
                }
            })
        );
    }

    /** Recarga los permisos del usuario desde el backend y notifica a los suscriptores. */
    refrescarPermisos(): void {
        if (!this.isLoggedIn()) { this._permisos.next([]); return; }
        this.http.get<string[]>('/api/me/permisos').subscribe({
            next: (codigos) => this._permisos.next(Array.isArray(codigos) ? codigos : []),
            // Ante un fallo transitorio de red no se vacía la lista: mejor mantener el panel
            // como estaba que ocultar de golpe todos los módulos con permiso.
            error: () => {}
        });
    }

    tienePermiso(codigo: string): boolean {
        return this._permisos.value.includes(codigo);
    }

    get permisosActuales(): string[] {
        return this._permisos.value;
    }

    logout(): void {
        localStorage.removeItem('presus_token');
        localStorage.removeItem('user_id');
        localStorage.removeItem('user_role');
        localStorage.removeItem('user_name');
        localStorage.removeItem('email_noti_configurado');
        this._permisos.next([]);
        this.router.navigate(['/login']);
    }

    getToken(): string | null {
        return localStorage.getItem('presus_token');
    }

    isLoggedIn(): boolean {
        return !!this.getToken();
    }

    getRole(): string {
        return localStorage.getItem('user_role') || '';
    }

    getUserId(): number {
        return Number(localStorage.getItem('user_id') || 0);
    }

    getUserName(): string {
        return localStorage.getItem('user_name') || 'Usuario';
    }

    hasEmailNotiConfigurado(): boolean {
        return localStorage.getItem('email_noti_configurado') === 'true';
    }

    marcarEmailNotiConfigurado(): void {
        localStorage.setItem('email_noti_configurado', 'true');
    }
}
