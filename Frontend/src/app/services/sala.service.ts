import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SalaService {
  private api = 'http://localhost:8080/api/salas';
  constructor(private http: HttpClient) {}
  listar(): Observable<any[]> { return this.http.get<any[]>(this.api); }
  crear(sala: any): Observable<any> { return this.http.post(this.api, sala); }
  eliminar(id: number): Observable<any> { return this.http.delete(`${this.api}/${id}`); }
}
