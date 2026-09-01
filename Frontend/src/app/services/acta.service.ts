import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Página de Spring Data ya desempaquetada del ResponseWrapper por el authInterceptor. */
export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ActaResumen {
  id: number;
  solicitudId: number;
  estudianteNombre: string;
  carrera: string;
  tituloTema: string;
  estado: string;
  estadoNombre: string;
  fechaGeneracion: string;
  firmada: boolean;
  firmantesPendientes: string;
}

export interface MiembroTribunal { docente: string; rol: string; confirmado: boolean; }

export interface ActaDetalle extends ActaResumen {
  observacionesActa: string | null;
  archivoPdf: string | null;
  firmadaPresidente: boolean;
  firmadaVocal1: boolean;
  firmadaVocal2: boolean;
  firmadaTutor: boolean;
  fechaFirmaPresidente: string | null;
  fechaFirmaVocal1: string | null;
  fechaFirmaVocal2: string | null;
  fechaFirmaTutor: string | null;
  tribunal: MiembroTribunal[];
}

export interface HistorialActaEntrada {
  id: number;
  actaId: number;
  accion: string;
  estadoAnterior: string | null;
  estadoNuevo: string | null;
  usuarioEmail: string | null;
  usuarioNombre: string;
  rolUsuario: string | null;
  comentario: string | null;
  fecha: string;
}

export interface FiltroActas {
  estado?: string;
  carrera?: string;
  desde?: string;   // yyyy-MM-dd
  hasta?: string;   // yyyy-MM-dd
  q?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ActaService {
  private api = '/api/actas';

  constructor(private http: HttpClient) {}

  // ── Flujo existente (sin cambios) ─────────────────────────────────────────
  generarActa(solicitudId: number): Observable<any> {
    return this.http.post(`${this.api}/generar/${solicitudId}`, {});
  }

  firmarActa(actaId: number, rol: string): Observable<any> {
    const params = new HttpParams().set('rol', rol);
    return this.http.post(`${this.api}/firmar/${actaId}`, {}, { params });
  }

  descargarPdf(actaId: number): Observable<Blob> {
    return this.http.get(`${this.api}/descargar/${actaId}`, { responseType: 'blob' });
  }

  verPdfUrl(actaId: number): string {
    return `${this.api}/ver/${actaId}`;
  }

  listar(): Observable<any[]> {
    return this.http.get<any[]>(this.api);
  }

  porSolicitud(solicitudId: number): Observable<any> {
    return this.http.get(`${this.api}/solicitud/${solicitudId}`);
  }

  // ── Módulo 2: gestión e historial de actas ────────────────────────────────

  /** DOCENTE: actas de las pre-sustentaciones donde es tutor o jurado. */
  misActas(page = 0, size = 10): Observable<Pagina<ActaResumen>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Pagina<ActaResumen>>(`${this.api}/mis-actas`, { params });
  }

  /** ADMINISTRADOR: búsqueda/filtrado administrativo. */
  buscar(f: FiltroActas): Observable<Pagina<ActaResumen>> {
    let params = new HttpParams()
      .set('page', f.page ?? 0)
      .set('size', f.size ?? 10);
    if (f.estado)  params = params.set('estado', f.estado);
    if (f.carrera) params = params.set('carrera', f.carrera);
    if (f.desde)   params = params.set('desde', f.desde);
    if (f.hasta)   params = params.set('hasta', f.hasta);
    if (f.q)       params = params.set('q', f.q);
    return this.http.get<Pagina<ActaResumen>>(`${this.api}/buscar`, { params });
  }

  detalle(actaId: number): Observable<ActaDetalle> {
    return this.http.get<ActaDetalle>(`${this.api}/${actaId}`);
  }

  historial(actaId: number): Observable<HistorialActaEntrada[]> {
    return this.http.get<HistorialActaEntrada[]>(`${this.api}/${actaId}/historial`);
  }

  /** COORDINADOR / ADMINISTRADOR: cambia el estado del acta. */
  cambiarEstado(actaId: number, nuevoEstado: string, motivo?: string): Observable<any> {
    return this.http.patch(`${this.api}/${actaId}/estado`, { nuevoEstado, motivo });
  }
}
