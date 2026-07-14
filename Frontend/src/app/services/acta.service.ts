import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ActaService {
  private api = 'http://localhost:8080/api/actas';

  constructor(private http: HttpClient) {}

  /** RF-11: Genera el acta con PDF real */
  generarActa(solicitudId: number): Observable<any> {
    return this.http.post(`${this.api}/generar/${solicitudId}`, {});
  }

  /** RF-08: Firma por un actor (PRESIDENTE, VOCAL_1, VOCAL_2, TUTOR) */
  firmarActa(actaId: number, rol: string): Observable<any> {
    const params = new HttpParams().set('rol', rol);
    return this.http.post(`${this.api}/firmar/${actaId}`, {}, { params });
  }

  /** RF-11: Descargar PDF del acta */
  descargarPdf(actaId: number): Observable<Blob> {
    return this.http.get(`${this.api}/descargar/${actaId}`, { responseType: 'blob' });
  }

  /** RF-11: Ver PDF en línea */
  verPdfUrl(actaId: number): string {
    return `${this.api}/ver/${actaId}`;
  }

  listar(): Observable<any[]> {
    return this.http.get<any[]>(this.api);
  }

  porSolicitud(solicitudId: number): Observable<any> {
    return this.http.get(`${this.api}/solicitud/${solicitudId}`);
  }
}
