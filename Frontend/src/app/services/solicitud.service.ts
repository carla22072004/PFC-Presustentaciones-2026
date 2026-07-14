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