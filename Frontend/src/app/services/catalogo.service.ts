import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ModalidadTitulacion {
    id: number;
    codigo: string;
    nombre: string;
}

export interface ConvocatoriaTitulacion {
    id: number;
    codigo: string;
    nombre: string;
    activa: boolean;
}

export interface LineaInvestigacion {
    id: number;
    codigo: string;
    nombre: string;
    descripcion?: string;
}

export interface AreaTematica {
    id: number;
    nombre: string;
    descripcion?: string;
    lineaInvestigacion: LineaInvestigacion;
}

export interface Facultad {
    id: number;
    codigo: string;
    nombre: string;
}

export interface Carrera {
    id: number;
    codigo: string;
    nombre: string;
    modalidadEstudio?: string;
    facultad?: Facultad;
}

export interface PeriodoAcademico {
    id: number;
    codigo: string;
    nombre: string;
    fechaInicio?: string;
    fechaFin?: string;
    activo: boolean;
}

export interface GuardarFacultadRequest {
    codigo: string;
    nombre: string;
}

export interface GuardarCarreraRequest {
    codigo: string;
    nombre: string;
    facultadId: number;
    modalidadEstudio?: string;
}

export interface GuardarModalidadRequest {
    codigo: string;
    nombre: string;
}

export interface GuardarPeriodoRequest {
    codigo: string;
    nombre: string;
    fechaInicio: string;
    fechaFin: string;
    activo: boolean;
}

@Injectable({ providedIn: 'root' })
export class CatalogoService {
    private apiUrl = 'http://127.0.0.1:8080/api/catalogos';
    constructor(private http: HttpClient) {}

    listarModalidades(): Observable<ModalidadTitulacion[]> {
        return this.http.get<ModalidadTitulacion[]>(`${this.apiUrl}/modalidades`);
    }

    listarConvocatoriasActivas(): Observable<ConvocatoriaTitulacion[]> {
        return this.http.get<ConvocatoriaTitulacion[]>(`${this.apiUrl}/convocatorias`);
    }

    listarLineasInvestigacion(): Observable<LineaInvestigacion[]> {
        return this.http.get<LineaInvestigacion[]>(`${this.apiUrl}/lineas-investigacion`);
    }

    listarAreasTematicas(lineaId?: number): Observable<AreaTematica[]> {
        const url = lineaId
            ? `${this.apiUrl}/areas-tematicas?lineaId=${lineaId}`
            : `${this.apiUrl}/areas-tematicas`;
        return this.http.get<AreaTematica[]>(url);
    }

    listarCarreras(): Observable<Carrera[]> {
        return this.http.get<Carrera[]>(`${this.apiUrl}/carreras`);
    }

    listarPeriodosAcademicos(): Observable<PeriodoAcademico[]> {
        return this.http.get<PeriodoAcademico[]>(`${this.apiUrl}/periodos-academicos`);
    }

    // ── Gestión de Carreras (CRUD) ─────────────────────────────────────

    listarFacultades(): Observable<Facultad[]> {
        return this.http.get<Facultad[]>(`${this.apiUrl}/facultades`);
    }

    crearFacultad(req: GuardarFacultadRequest): Observable<Facultad> {
        return this.http.post<Facultad>(`${this.apiUrl}/facultades`, req);
    }

    actualizarFacultad(id: number, req: GuardarFacultadRequest): Observable<Facultad> {
        return this.http.put<Facultad>(`${this.apiUrl}/facultades/${id}`, req);
    }

    eliminarFacultad(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/facultades/${id}`);
    }

    crearCarrera(req: GuardarCarreraRequest): Observable<Carrera> {
        return this.http.post<Carrera>(`${this.apiUrl}/carreras`, req);
    }

    actualizarCarrera(id: number, req: Partial<GuardarCarreraRequest>): Observable<Carrera> {
        return this.http.put<Carrera>(`${this.apiUrl}/carreras/${id}`, req);
    }

    eliminarCarrera(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/carreras/${id}`);
    }

    crearModalidad(req: GuardarModalidadRequest): Observable<ModalidadTitulacion> {
        return this.http.post<ModalidadTitulacion>(`${this.apiUrl}/modalidades`, req);
    }

    actualizarModalidad(id: number, req: GuardarModalidadRequest): Observable<ModalidadTitulacion> {
        return this.http.put<ModalidadTitulacion>(`${this.apiUrl}/modalidades/${id}`, req);
    }

    eliminarModalidad(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/modalidades/${id}`);
    }

    crearPeriodo(req: GuardarPeriodoRequest): Observable<PeriodoAcademico> {
        return this.http.post<PeriodoAcademico>(`${this.apiUrl}/periodos-academicos`, req);
    }

    actualizarPeriodo(id: number, req: Partial<GuardarPeriodoRequest>): Observable<PeriodoAcademico> {
        return this.http.put<PeriodoAcademico>(`${this.apiUrl}/periodos-academicos/${id}`, req);
    }

    eliminarPeriodo(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/periodos-academicos/${id}`);
    }
}
