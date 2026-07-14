import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = 'http://localhost:8080/api/auth';

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

    logout(): void {
        localStorage.removeItem('presus_token');
        localStorage.removeItem('user_id');
        localStorage.removeItem('user_role');
        localStorage.removeItem('user_name');
        localStorage.removeItem('email_noti_configurado');
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