import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SolicitudService {
    private apiUrl = 'http://localhost:8080/api/solicitudes';
    constructor(private http: HttpClient) {}

    registrarSolicitud(usuarioId: number, datos: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/crear-por-usuario/${usuarioId}`, datos);
    }
    listarSolicitudes(): Observable<any[]> {
        return this.http.get<any[]>(this.apiUrl);
    }
    listarSolicitudesPaginado(page: number, size: number, estado?: string, q?: string, fechaDesde?: string, fechaHasta?: string): Observable<{
        content: any[]; totalElements: number; totalPages: number; page: number; size: number;
    }> {
        let params = `page=${page}&size=${size}`;
        if (estado) params += `&estado=${encodeURIComponent(estado)}`;
        if (q) params += `&q=${encodeURIComponent(q)}`;
        if (fechaDesde) params += `&fechaDesde=${fechaDesde}`;
        if (fechaHasta) params += `&fechaHasta=${fechaHasta}`;
        return this.http.get<{ content: any[]; totalElements: number; totalPages: number; page: number; size: number; }>(
            `${this.apiUrl}/paginado?${params}`
        );
    }
    contarPorEstado(): Observable<Record<string, number>> {
        return this.http.get<Record<string, number>>(`${this.apiUrl}/contar-por-estado`);
    }
    listarMisSolicitudes(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/mis-solicitudes`);
    }
    listarPorEstudiante(estudianteId: number): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/estudiante/${estudianteId}`);
    }
    listarPorUsuario(usuarioId: number): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/usuario/${usuarioId}`);
    }
    obtenerPorId(id: number): Observable<any> {
        return this.http.get(`${this.apiUrl}/${id}`);
    }
    enviarSolicitud(id: number): Observable<any> {
        return this.http.post(`${this.apiUrl}/enviar/${id}`, {});
    }
    aprobarSolicitud(id: number): Observable<any> {
        return this.http.post(`${this.apiUrl}/aprobar/${id}`, {});
    }
    rechazarSolicitud(id: number): Observable<any> {
        return this.http.post(`${this.apiUrl}/rechazar/${id}`, {});
    }
    rechazarConObservacion(id: number, observacion: string): Observable<any> {
        return this.http.post(`${this.apiUrl}/rechazar-con-observacion/${id}`, { observacion });
    }
    suspenderSolicitud(id: number, motivo: string): Observable<any> {
        return this.http.post(`${this.apiUrl}/suspender/${id}`, { motivo });
    }
}