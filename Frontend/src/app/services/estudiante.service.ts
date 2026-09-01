import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EstudianteDTO {
    id: number;
    usuarioId: number;
    nombre: string;
    apellido: string;
    email: string;
    activo: boolean;
    telefono: string | null;
    expedienteCodigo: string | null;
    carreraId: number | null;
    carreraNombre: string | null;
    periodoIngresoId: number | null;
    periodoIngresoNombre: string | null;
    semestreActual: number;
    estadoAcademicoCodigo: string;
    estadoAcademicoNombre: string;
    proyectoTitulo: string | null;
    proyectoEstado: string | null;
}

export interface EstadoAcademico {
    id: number;
    codigo: string;
    nombre: string;
}

export interface CrearEstudianteRequest {
    nombre: string;
    apellido: string;
    email: string;
    password: string;
    telefono?: string;
    carreraId: number;
    periodoIngresoId?: number;
    semestreActual?: number;
}

export interface ActualizarEstudianteRequest {
    carreraId?: number;
    periodoIngresoId?: number;
    semestreActual?: number;
    telefono?: string;
    estadoAcademicoCodigo?: string;
}

@Injectable({ providedIn: 'root' })
export class EstudianteService {
    private apiUrl = '/api/estudiantes';

    constructor(private http: HttpClient) {}

    listarPaginado(page: number, size: number, q?: string): Observable<{
        content: EstudianteDTO[]; totalElements: number; totalPages: number; number: number; size: number;
    }> {
        let params = `page=${page}&size=${size}`;
        if (q) params += `&q=${encodeURIComponent(q)}`;
        return this.http.get<{ content: EstudianteDTO[]; totalElements: number; totalPages: number; number: number; size: number; }>(
            `${this.apiUrl}/paginado?${params}`
        );
    }

    obtenerPorId(id: number): Observable<EstudianteDTO> {
        return this.http.get<EstudianteDTO>(`${this.apiUrl}/${id}`);
    }

    crear(req: CrearEstudianteRequest): Observable<EstudianteDTO> {
        return this.http.post<EstudianteDTO>(this.apiUrl, req);
    }

    actualizar(id: number, req: ActualizarEstudianteRequest): Observable<EstudianteDTO> {
        return this.http.put<EstudianteDTO>(`${this.apiUrl}/${id}`, req);
    }

    listarEstadosAcademicos(): Observable<EstadoAcademico[]> {
        return this.http.get<EstadoAcademico[]>(`${this.apiUrl}/estados-academicos`);
    }
}
