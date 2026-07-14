import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AnteproyectoService {
  private apiUrl = 'http://localhost:8080/api/anteproyectos';

  constructor(private http: HttpClient) {}

  enviarAnteproyecto(solicitudId: number, archivo: File): Observable<any> {
    const formData = new FormData();
    formData.append('archivo', archivo, archivo.name);
    return this.http.post(`${this.apiUrl}/enviar/${solicitudId}`, formData);
  }

  obtenerPorSolicitud(solicitudId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/solicitud/${solicitudId}`);
  }

  getUrlVisualizacion(solicitudId: number): string {
    return `${this.apiUrl}/ver/${solicitudId}`;
  }

  /** RF-02: Verificar integridad SHA-256 del archivo */
  verificarIntegridad(solicitudId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/verificar/${solicitudId}`);
  }

  aprobarAnteproyecto(id: number, observaciones: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/aprobar/${id}`, {}, { params: new HttpParams().set('observaciones', observaciones) });
  }

  rechazarAnteproyecto(id: number, observaciones: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/rechazar/${id}`, {}, { params: new HttpParams().set('observaciones', observaciones) });
  }
}
