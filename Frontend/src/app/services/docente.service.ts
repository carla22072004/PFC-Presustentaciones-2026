import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DocenteService {
  private api = 'http://localhost:8080/api/docentes';

  constructor(private http: HttpClient) {}

  listar(): Observable<any[]> {
    return this.http.get<any[]>(this.api);
  }

  disponibles(): Observable<any[]> {
    return this.http.get<any[]>(this.api + '/disponibles');
  }

  obtenerPorUsuario(usuarioId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/usuario/${usuarioId}`);
  }
}
