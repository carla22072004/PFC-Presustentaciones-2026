import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Metadatos de un archivo de respaldo (dump FULL o .tar.gz DIFERENCIAL). */
export interface BackupInfo {
  nombre: string;
  tipo: 'FULL' | 'DIFERENCIAL';
  origen: 'MANUAL' | 'AUTOMATICO' | 'EVENTO';
  tamanoBytes: number;
  tamanoLegible: string;
  fechaCreacion: string;
}

export interface EstadoRespaldos {
  ultimoRespaldo: BackupInfo | null;
  ultimoRespaldoHace: string;
  programacionActiva: boolean;
  proximoAutomatico: string | null;
  proximoAutomaticoTexto: string;
  totalRespaldos: number;
  conteoPorTipo: Record<string, number>;
  conteoPorOrigen: Record<string, number>;
  espacioUsadoBytes: number;
  espacioUsadoLegible: string;
  espacioLibreBytes: number;
  espacioLibreLegible: string;
  rpoEstimado: string;
  ultimaPruebaRestauracion: string | null;
  ultimaPruebaResultado: string;
  ultimaPruebaHace: string;
}

export interface RespaldoConfig {
  activo: boolean;
  cron: string;
  retenerDiarios: number;
  retenerSemanales: number;
  retenerMensuales: number;
  retenerDiasWal: number;
  diferencialActivo: boolean;
  cronDiferencial: string;
  cronDescripcion?: string;
  cronDiferencialDescripcion?: string;
}

export interface PruebaRestauracion {
  id: number;
  respaldoNombre: string;
  fecha: string;
  resultado: 'EXITOSA' | 'FALLIDA';
  responsable: string | null;
  notas: string | null;
}

export interface RegistrarPrueba {
  respaldoNombre: string;
  resultado: 'EXITOSA' | 'FALLIDA';
  responsable?: string;
  notas?: string;
}

export interface BaseFisica {
  nombre: string;
  tamanoBytes: number;
  tamanoLegible: string;
  fechaCreacion: string;
}

/** Estado del archivado continuo de WAL / PITR (Fase 2). */
export interface EstadoWal {
  archivadoActivo: boolean;
  walLevel: string;
  archiveCommand: string;
  archiveTimeoutSegundos: number;
  segmentosArchivados: number;
  ultimoSegmento: string | null;
  ultimoArchivado: string | null;
  fallos: number;
  ultimoFallo: string | null;
  segmentosEnDisco: number;
  tamanoArchivadoBytes: number;
  tamanoArchivadoLegible: string;
  segmentoMasAntiguo: string | null;
  pitrDisponibleDesde: string;
  basesFisicas: BaseFisica[];
  hayBaseFisica: boolean;
  advertencia: string | null;
}

/**
 * Apartado "Gestión de Respaldos de Base de Datos" (ADMIN, permiso BACKUPS_GESTIONAR).
 * El interceptor reescribe /api/ -> /api/v1/ y desempaqueta el ResponseWrapper.
 */
@Injectable({ providedIn: 'root' })
export class BackupService {
  private api = '/api/backups';

  constructor(private http: HttpClient) {}

  // ── Copias ────────────────────────────────────────────────────────────────
  listar(): Observable<BackupInfo[]> { return this.http.get<BackupInfo[]>(this.api); }

  generar(origen: 'MANUAL' | 'EVENTO' = 'MANUAL'): Observable<BackupInfo> {
    return this.http.post<BackupInfo>(`${this.api}?origen=${origen}`, {});
  }

  generarDiferencial(origen: 'MANUAL' | 'EVENTO' = 'MANUAL'): Observable<BackupInfo> {
    return this.http.post<BackupInfo>(`${this.api}/diferencial?origen=${origen}`, {});
  }

  descargar(nombre: string): Observable<Blob> {
    return this.http.get(`${this.api}/${encodeURIComponent(nombre)}/descargar`, { responseType: 'blob' });
  }

  restaurar(nombre: string): Observable<any> {
    return this.http.post(`${this.api}/${encodeURIComponent(nombre)}/restaurar`, {});
  }

  eliminar(nombre: string): Observable<any> {
    return this.http.delete(`${this.api}/${encodeURIComponent(nombre)}`);
  }

  // ── Panel de estado + cronograma ──────────────────────────────────────────
  estado(): Observable<EstadoRespaldos> { return this.http.get<EstadoRespaldos>(`${this.api}/estado`); }
  getConfig(): Observable<RespaldoConfig> { return this.http.get<RespaldoConfig>(`${this.api}/config`); }
  guardarConfig(cfg: RespaldoConfig): Observable<RespaldoConfig> { return this.http.put<RespaldoConfig>(`${this.api}/config`, cfg); }
  aplicarRetencion(): Observable<string[]> { return this.http.post<string[]>(`${this.api}/retencion`, {}); }

  // ── Pruebas de restauración ───────────────────────────────────────────────
  listarPruebas(): Observable<PruebaRestauracion[]> { return this.http.get<PruebaRestauracion[]>(`${this.api}/pruebas`); }
  registrarPrueba(req: RegistrarPrueba): Observable<PruebaRestauracion> { return this.http.post<PruebaRestauracion>(`${this.api}/pruebas`, req); }

  // ── Fase 2: WAL / PITR ────────────────────────────────────────────────────
  estadoWal(): Observable<EstadoWal> { return this.http.get<EstadoWal>(`${this.api}/wal`); }
  switchWal(): Observable<string> { return this.http.post<string>(`${this.api}/wal/switch`, {}); }
  limpiarWal(): Observable<number> { return this.http.post<number>(`${this.api}/wal/limpiar`, {}); }
  generarBaseFisica(): Observable<BaseFisica> { return this.http.post<BaseFisica>(`${this.api}/bases`, {}); }
  eliminarBase(nombre: string): Observable<any> { return this.http.delete(`${this.api}/bases/${encodeURIComponent(nombre)}`); }
}
