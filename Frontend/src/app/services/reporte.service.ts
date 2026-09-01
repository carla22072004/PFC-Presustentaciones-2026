import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConteoReporte { etiqueta: string; cantidad: number; }

export interface ResumenReporte {
  totalSolicitudes: number;
  solicitudesCompletadas: number;
  solicitudesEnProceso: number;
  solicitudesRechazadas: number;
  totalActas: number;
  actasGeneradas: number;
  actasRevisadas: number;
  actasObservadas: number;
  actasFinalizadas: number;
  actasAnuladas: number;
  actasPendientesFirma: number;
  solicitudesPorEstado: ConteoReporte[];
  sustentacionesPorPeriodo: ConteoReporte[];
}

export interface ActividadDocente {
  docenteId: number;
  docente: string;
  comoJurado: number;
  comoTutor: number;
  actasFirmadas: number;
}

export interface FiltroReporte { desde?: string; hasta?: string; carrera?: string; }

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private api = '/api/reportes';

  constructor(private http: HttpClient) {}

  // ── PDF existentes (sin cambios) ─────────────────────────────────────────
  cronogramaPdf(): Observable<Blob> {
    return this.http.get(`${this.api}/cronograma/pdf`, { responseType: 'blob' });
  }
  estadisticasPdf(): Observable<Blob> {
    return this.http.get(`${this.api}/estadisticas/pdf`, { responseType: 'blob' });
  }
  estadisticasJson(): Observable<any> {
    return this.http.get(`${this.api}/estadisticas/json`);
  }

  // ── Módulo de reportes JSON (COORDINADOR / ADMINISTRADOR) ────────────────
  private params(f: FiltroReporte = {}): HttpParams {
    let p = new HttpParams();
    if (f.desde)   p = p.set('desde', f.desde);
    if (f.hasta)   p = p.set('hasta', f.hasta);
    if (f.carrera) p = p.set('carrera', f.carrera);
    return p;
  }

  resumen(f: FiltroReporte = {}): Observable<ResumenReporte> {
    return this.http.get<ResumenReporte>(`${this.api}/resumen`, { params: this.params(f) });
  }
  solicitudesPorEstado(f: FiltroReporte = {}): Observable<ConteoReporte[]> {
    return this.http.get<ConteoReporte[]>(`${this.api}/solicitudes-por-estado`, { params: this.params(f) });
  }
  sustentacionesPorPeriodo(f: FiltroReporte = {}): Observable<ConteoReporte[]> {
    return this.http.get<ConteoReporte[]>(`${this.api}/sustentaciones-por-periodo`, { params: this.params(f) });
  }
  resumenActas(f: FiltroReporte = {}): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.api}/actas`, { params: this.params(f) });
  }
  actividadDocente(): Observable<ActividadDocente[]> {
    return this.http.get<ActividadDocente[]>(`${this.api}/actividad-docente`);
  }
  porCarrera(): Observable<Array<Record<string, any>>> {
    return this.http.get<Array<Record<string, any>>>(`${this.api}/por-carrera`);
  }
}
