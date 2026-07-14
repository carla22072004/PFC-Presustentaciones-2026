import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class JuradoService {
  private api = 'http://localhost:8080/api/jurados';

  constructor(private http: HttpClient) {}

  // ── Jurados ────────────────────────────────────────────────────────────────
  asignarJurado(solicitudId: number, docenteId: number, rol: string): Observable<any> {
    const params = new HttpParams()
      .set('solicitudId', solicitudId)
      .set('docenteId', docenteId)
      .set('rol', rol);
    return this.http.post(`${this.api}/asignar`, {}, { params });
  }

  asignarAutomaticamente(solicitudId: number): Observable<any[]> {
    return this.http.post<any[]>(`${this.api}/asignar-automatico/${solicitudId}`, {});
  }

  listarPorSolicitud(solicitudId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/solicitud/${solicitudId}`);
  }

  sugerirDocentes(solicitudId: number, cantidad: number = 5): Observable<any[]> {
    const params = new HttpParams().set('cantidad', cantidad);
    return this.http.get<any[]>(`${this.api}/sugerencias/${solicitudId}`, { params });
  }

  eliminarJurado(juradoId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${juradoId}`);
  }

  // ── Tutor ──────────────────────────────────────────────────────────────────
  asignarTutor(solicitudId: number, docenteId: number): Observable<any> {
    const params = new HttpParams()
      .set('solicitudId', solicitudId)
      .set('docenteId', docenteId);
    return this.http.post(`${this.api}/tutor/asignar`, {}, { params });
  }

  obtenerTutor(solicitudId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/tutor/solicitud/${solicitudId}`);
  }

  eliminarTutor(tutorId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/tutor/${tutorId}`);
  }

  // ── Consultas por docente (para vista del jurado) ──────────────────────
  listarPorDocente(docenteId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/docente/${docenteId}`);
  }

  listarTutoriasPorDocente(docenteId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/tutor/docente/${docenteId}`);
  }

  obtenerInfoJurado(solicitudId: number, usuarioId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/info/${solicitudId}/${usuarioId}`);
  }
}
