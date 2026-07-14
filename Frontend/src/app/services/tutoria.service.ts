import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TutoriaFase, TutoriaMensaje, TutoriaResumen } from '../models/tutoria.model';

export interface NuevoMensajeRequest {
  contenido: string;
  tipo: 'OBSERVACION' | 'RESPUESTA' | 'APROBACION';
}

@Injectable({ providedIn: 'root' })
export class TutoriaService {
  private readonly base = 'http://localhost:8080/api/tutorias';

  constructor(private http: HttpClient) {}

  obtenerTutoriasEstudiante(usuarioId: number): Observable<TutoriaResumen[]> {
    return this.http.get<TutoriaResumen[]>(`${this.base}/estudiante/${usuarioId}`);
  }

  obtenerTutoriasDocente(usuarioId: number): Observable<TutoriaResumen[]> {
    return this.http.get<TutoriaResumen[]>(`${this.base}/docente/${usuarioId}`);
  }

  obtenerResumen(tutorId: number, usuarioId: number): Observable<TutoriaResumen> {
    const params = new HttpParams().set('usuarioId', usuarioId);
    return this.http.get<TutoriaResumen>(`${this.base}/${tutorId}/resumen`, { params });
  }

  obtenerFases(tutorId: number): Observable<TutoriaFase[]> {
    return this.http.get<TutoriaFase[]>(`${this.base}/${tutorId}/fases`);
  }

  crearFaseConObservacion(
    tutorId: number,
    tutorUsuarioId: number,
    observacion: string
  ): Observable<TutoriaFase> {
    const params = new HttpParams()
      .set('tutorUsuarioId', tutorUsuarioId)
      .set('observacion', observacion);
    return this.http.post<TutoriaFase>(`${this.base}/${tutorId}/nueva-fase`, null, { params });
  }

  subirPdfCorregido(
    faseId: number,
    archivo: File,
    estudianteUsuarioId: number
  ): Observable<TutoriaFase> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    const params = new HttpParams().set('estudianteUsuarioId', estudianteUsuarioId);
    return this.http.post<TutoriaFase>(`${this.base}/fases/${faseId}/subir-pdf`, formData, { params });
  }

  aprobarFase(
    faseId: number,
    tutorUsuarioId: number,
    comentario: string
  ): Observable<TutoriaFase> {
    const params = new HttpParams()
      .set('tutorUsuarioId', tutorUsuarioId)
      .set('comentario', comentario);
    return this.http.post<TutoriaFase>(`${this.base}/fases/${faseId}/aprobar`, null, { params });
  }

  enviarMensaje(
    faseId: number,
    remitenteId: number,
    contenido: string,
    tipo: 'OBSERVACION' | 'RESPUESTA' | 'APROBACION'
  ): Observable<TutoriaMensaje> {
    const body: NuevoMensajeRequest = { contenido, tipo };
    const params = new HttpParams().set('remitenteId', remitenteId);
    return this.http.post<TutoriaMensaje>(`${this.base}/fases/${faseId}/mensaje`, body, { params });
  }

  marcarMensajesLeidos(faseId: number, usuarioId: number): Observable<void> {
    const params = new HttpParams().set('usuarioId', usuarioId);
    return this.http.put<void>(`${this.base}/fases/${faseId}/leer`, null, { params });
  }

  verPdfFase(faseId: number): Observable<Blob> {
    return this.http.get(`${this.base}/fases/${faseId}/pdf`, { responseType: 'blob' });
  }

  /** Obtiene el Tutor (entidad Tutor, no JuradoAsignacion) asignado a una solicitud.
   *  Devuelve { id, ... } donde id es el tutorId que usan los endpoints de tutoría. */
  obtenerTutorPorSolicitud(solicitudId: number): Observable<any> {
    return this.http.get<any>(`http://localhost:8080/api/tutores/solicitud/${solicitudId}`);
  }
}
