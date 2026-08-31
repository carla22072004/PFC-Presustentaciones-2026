import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RegistroAuditoria {
    id: number;
    tabla: string;
    registroId: number | null;
    accion: string;
    usuarioId: number | null;
    usuarioNombre: string | null;
    fecha: string;
    datosAnteriores: string | null;
    datosNuevos: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
    private apiUrl = 'http://127.0.0.1:8080/api/auditoria';

    constructor(private http: HttpClient) {}

    listarPaginado(page: number, size: number, tabla?: string, accion?: string, q?: string): Observable<{
        content: RegistroAuditoria[]; totalElements: number; totalPages: number; number: number; size: number;
    }> {
        let params = `page=${page}&size=${size}`;
        if (tabla) params += `&tabla=${encodeURIComponent(tabla)}`;
        if (accion) params += `&accion=${encodeURIComponent(accion)}`;
        if (q) params += `&q=${encodeURIComponent(q)}`;
        return this.http.get<{ content: RegistroAuditoria[]; totalElements: number; totalPages: number; number: number; size: number; }>(
            `${this.apiUrl}/paginado?${params}`
        );
    }

    listarTablas(): Observable<string[]> {
        return this.http.get<string[]>(`${this.apiUrl}/tablas`);
    }
}
