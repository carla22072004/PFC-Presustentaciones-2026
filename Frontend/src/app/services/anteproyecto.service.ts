import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AnteproyectoService {
  private apiUrl = '/api/anteproyectos';

  constructor(private http: HttpClient) {}

  /**
   * ERR-04: el visor y la descarga de PDF usaban fetch() nativo apuntando a esta misma
   * URL sin pasar por el interceptor (que reescribe /api/ -> /api/v1/), así que le
   * pegaban a una ruta que el backend ya no expone y siempre fallaban. Usar HttpClient
   * aquí para que la reescritura y el header Authorization se apliquen igual que en el
   * resto del servicio. observe: 'response' expone el status code para poder distinguir
   * un PDF real de una respuesta de error antes de descargarlo.
   */
  obtenerPdfBlob(solicitudId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.apiUrl}/ver/${solicitudId}`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  enviarAnteproyecto(solicitudId: number, archivo: File): Observable<any> {
    const formData = new FormData();
    formData.append('archivo', archivo, archivo.name);
    return this.http.post(`${this.apiUrl}/enviar/${solicitudId}`, formData);
  }

  obtenerPorSolicitud(solicitudId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/solicitud/${solicitudId}`);
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
