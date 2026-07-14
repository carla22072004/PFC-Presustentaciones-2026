import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private api = 'http://localhost:8080/api/reportes';

  constructor(private http: HttpClient) {}

  /** RF-11: Descargar PDF del cronograma */
  cronogramaPdf(): Observable<Blob> {
    return this.http.get(`${this.api}/cronograma/pdf`, { responseType: 'blob' });
  }

  /** RF-11: Descargar PDF de estadísticas */
  estadisticasPdf(): Observable<Blob> {
    return this.http.get(`${this.api}/estadisticas/pdf`, { responseType: 'blob' });
  }

  /** RF-11: Estadísticas en JSON para mostrar en dashboard */
  estadisticasJson(): Observable<any> {
    return this.http.get(`${this.api}/estadisticas/json`);
  }
}
