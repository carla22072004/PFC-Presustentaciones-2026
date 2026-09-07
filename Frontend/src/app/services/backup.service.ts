import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Metadatos de un archivo de respaldo (dump) de la base de datos. */
export interface BackupInfo {
  nombre: string;
  tamanoBytes: number;
  tamanoLegible: string;
  fechaCreacion: string; // ISO date-time
}

/**
 * Apartado "Gestión de Respaldos de Base de Datos" (ADMIN, permiso BACKUPS_GESTIONAR).
 * El interceptor reescribe /api/ -> /api/v1/ y desempaqueta el ResponseWrapper.
 */
@Injectable({ providedIn: 'root' })
export class BackupService {
  private api = '/api/backups';

  constructor(private http: HttpClient) {}

  /** Lista los respaldos existentes, del más reciente al más antiguo. */
  listar(): Observable<BackupInfo[]> {
    return this.http.get<BackupInfo[]>(this.api);
  }

  /** Genera un respaldo nuevo con la fecha y hora actuales. */
  generar(): Observable<BackupInfo> {
    return this.http.post<BackupInfo>(this.api, {});
  }

  /** Descarga el archivo .dump de un respaldo. */
  descargar(nombre: string): Observable<Blob> {
    return this.http.get(`${this.api}/${encodeURIComponent(nombre)}/descargar`, { responseType: 'blob' });
  }

  /** Restaura la base de datos a partir de un respaldo (operación destructiva). */
  restaurar(nombre: string): Observable<any> {
    return this.http.post(`${this.api}/${encodeURIComponent(nombre)}/restaurar`, {});
  }

  /** Elimina permanentemente un archivo de respaldo. */
  eliminar(nombre: string): Observable<any> {
    return this.http.delete(`${this.api}/${encodeURIComponent(nombre)}`);
  }
}
