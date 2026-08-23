import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class RubricaService {
  private api = 'http://localhost:8080/api/rubricas';
  constructor(private http: HttpClient) {}

  // GET /api/rubricas devuelve una Page<Rubrica> (Spring Data), no un arreglo plano --
  // sin extraer "content" aquí, el <select> de rúbrica quedaba siempre vacío.
  listar(): Observable<any[]> {
    return this.http.get<any>(`${this.api}?size=100`).pipe(map(page => page?.content ?? []));
  }
}
