import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Usuario {
    id?: number;
    nombre: string;
    apellido: string;
    email: string;
    password?: string;
    /** Código del rol (ADMIN/DOCENTE/COORDINADOR/ESTUDIANTE o cualquier rol creado desde Gestionar Roles) */
    rol: string;
    activo?: boolean;
    telefono?: string;
    emailNotificaciones?: string;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
    private apiUrl = '/api/usuarios';
    constructor(private http: HttpClient) {}

    listarTodos(): Observable<Usuario[]> {
        return this.http.get<Usuario[]>(this.apiUrl);
    }
    listarPaginado(page: number, size: number, q?: string): Observable<{
        content: Usuario[]; totalElements: number; totalPages: number; page: number; size: number;
    }> {
        let params = `page=${page}&size=${size}`;
        if (q) params += `&q=${encodeURIComponent(q)}`;
        return this.http.get<{ content: Usuario[]; totalElements: number; totalPages: number; page: number; size: number; }>(
            `${this.apiUrl}/paginado?${params}`
        );
    }
    crear(usuario: Usuario): Observable<Usuario> {
        return this.http.post<Usuario>(this.apiUrl, usuario);
    }
    actualizar(id: number, usuario: Usuario): Observable<Usuario> {
        return this.http.put<Usuario>(`${this.apiUrl}/${id}`, usuario);
    }
    activar(id: number): Observable<void> {
        return this.http.patch<void>(`${this.apiUrl}/${id}/activar`, {});
    }
    desactivar(id: number): Observable<void> {
        return this.http.patch<void>(`${this.apiUrl}/${id}/desactivar`, {});
    }
    eliminar(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
