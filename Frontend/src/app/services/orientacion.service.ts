import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TemaPropuesto {
  id: number;
  titulo: string;
  problema?: string;
  objetivoGeneral?: string;
  objetivosEspecificos?: string;
  justificacion?: string;
  beneficiarios?: string;
  nivelDificultad?: string;
  carreraId?: number;
  carreraNombre?: string;
  lineaInvestigacionId?: number;
  lineaInvestigacionNombre?: string;
  areaId?: number;
  areaNombre?: string;
  guardado?: boolean;
}

export interface FiltroTemas {
  carreraId?: number;
  lineaInvestigacionId?: number;
  areaId?: number;
  nivelDificultad?: string;
}

export interface RecursoTitulacion {
  id: number;
  titulo: string;
  categoria: string;
  urlArchivo: string;
  carreraId?: number;
  carreraNombre?: string;
}

export interface PasoProgreso {
  clave: string;
  orden: number;
  titulo: string;
  descripcion: string;
  completado: boolean;
}

export interface ProgresoTitulacion {
  porcentaje: number;
  completados: number;
  total: number;
  pasos: PasoProgreso[];
}

/**
 * Centro de Orientación y Titulación. Las rutas se escriben sin el prefijo /v1:
 * el authInterceptor lo agrega de forma centralizada.
 */
@Injectable({ providedIn: 'root' })
export class OrientacionService {
  private api = '/api/orientacion/temas';

  constructor(private http: HttpClient) {}

  private toParams(f: FiltroTemas = {}): HttpParams {
    let p = new HttpParams();
    if (f.carreraId != null)            p = p.set('carreraId', f.carreraId);
    if (f.lineaInvestigacionId != null) p = p.set('lineaInvestigacionId', f.lineaInvestigacionId);
    if (f.areaId != null)               p = p.set('areaId', f.areaId);
    if (f.nivelDificultad)              p = p.set('nivelDificultad', f.nivelDificultad);
    return p;
  }

  explorar(filtro: FiltroTemas = {}): Observable<TemaPropuesto[]> {
    return this.http.get<TemaPropuesto[]>(this.api, { params: this.toParams(filtro) });
  }

  detalle(temaId: number): Observable<TemaPropuesto> {
    return this.http.get<TemaPropuesto>(`${this.api}/${temaId}`);
  }

  misGuardados(): Observable<TemaPropuesto[]> {
    return this.http.get<TemaPropuesto[]>(`${this.api}/guardados`);
  }

  guardar(temaId: number): Observable<void> {
    return this.http.post<void>(`${this.api}/${temaId}/guardar`, {});
  }

  quitarGuardado(temaId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${temaId}/guardar`);
  }

  // ── Recursos de titulación ────────────────────────────────────────────
  recursos(carreraId?: number): Observable<RecursoTitulacion[]> {
    let p = new HttpParams();
    if (carreraId != null) { p = p.set('carreraId', carreraId); }
    return this.http.get<RecursoTitulacion[]>('/api/orientacion/recursos', { params: p });
  }

  // ── Progreso / ruta de titulación (solo estudiante) ───────────────────
  miProgreso(): Observable<ProgresoTitulacion> {
    return this.http.get<ProgresoTitulacion>('/api/orientacion/progreso');
  }

  actualizarProgreso(pasos: Record<string, boolean>): Observable<ProgresoTitulacion> {
    return this.http.put<ProgresoTitulacion>('/api/orientacion/progreso', { pasos });
  }
}
