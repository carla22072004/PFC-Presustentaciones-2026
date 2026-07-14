import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CronogramaService {
  private api = 'http://localhost:8080/api/cronogramas';

  constructor(private http: HttpClient) {}

  crearCronograma(solicitudId: number, salaId: number, fecha: string, hora: string): Observable<any> {
    const params = new HttpParams()
      .set('solicitudId', solicitudId).set('salaId', salaId)
      .set('fecha', fecha).set('hora', hora);
    return this.http.post(`${this.api}/crear`, {}, { params });
  }

  /** RF-04: Asignación automática sin conflictos */
  asignarAutomatico(solicitudId: number): Observable<any> {
    return this.http.post(`${this.api}/auto/${solicitudId}`, {});
  }

  /** RF-04: Franjas disponibles en una fecha */
  disponibilidad(fecha: string, duracion: number = 45): Observable<any> {
    const params = new HttpParams().set('fecha', fecha).set('duracion', duracion);
    return this.http.get(`${this.api}/disponibilidad`, { params });
  }

  porSolicitud(id: number): Observable<any> {
    return this.http.get(`${this.api}/solicitud/${id}`);
  }

  listar(): Observable<any[]> {
    return this.http.get<any[]>(this.api);
  }

  listarPorUsuario(usuarioId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/usuario/${usuarioId}`);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
