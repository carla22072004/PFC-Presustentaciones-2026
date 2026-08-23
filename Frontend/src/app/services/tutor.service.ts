import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MiEstudianteTutorado {
    tutorId: number;
    solicitudId: number;
    estudianteUsuarioId: number;
    nombre: string;
    apellido: string;
    email: string;
    telefono: string | null;
    expedienteCodigo: string | null;
    carreraNombre: string | null;
    semestreActual: number;
    estadoAcademicoCodigo: string | null;
    estadoAcademicoNombre: string | null;
    tituloTema: string;
    estadoSolicitudCodigo: string | null;
    estadoSolicitudNombre: string | null;
    estadoTutoria: string;
    fechaAsignacion: string;
}

@Injectable({ providedIn: 'root' })
export class TutorService {
  private api = 'http://localhost:8080/api/tutores';

  constructor(private http: HttpClient) {}

  misEstudiantes(): Observable<MiEstudianteTutorado[]> {
    return this.http.get<MiEstudianteTutorado[]>(`${this.api}/mis-estudiantes`);
  }

  asignar(solicitudId: number, docenteId: number): Observable<any> {
    const params = new HttpParams()
      .set('solicitudId', solicitudId)
      .set('docenteId', docenteId);
    return this.http.post(this.api + '/asignar', {}, { params });
  }

  porSolicitud(solicitudId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/solicitud/${solicitudId}`);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
