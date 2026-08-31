import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DocenteService {
  private api = 'http://127.0.0.1:8080/api/docentes';

  constructor(private http: HttpClient) {}

  listar(): Observable<any[]> {
    return this.http.get<any[]>(this.api);
  }

  /**
   * ERR-02: la tabla docente tiene ~9,807 filas -- listar() (GET /api/docentes, sin
   * paginar) traía todo de una vez y colapsaba cualquier <select> con un <option> por
   * fila. Usar esto en su lugar para comboboxes con búsqueda (typeahead).
   */
  buscarPaginado(page: number, size: number, q?: string): Observable<{
    content: any[]; totalElements: number; totalPages: number; page: number; size: number;
  }> {
    let params = `page=${page}&size=${size}`;
    if (q) params += `&q=${encodeURIComponent(q)}`;
    return this.http.get<{ content: any[]; totalElements: number; totalPages: number; page: number; size: number; }>(
      `${this.api}/paginado?${params}`
    );
  }

  disponibles(): Observable<any[]> {
    return this.http.get<any[]>(this.api + '/disponibles');
  }

  obtenerPorUsuario(usuarioId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/usuario/${usuarioId}`);
  }
}
