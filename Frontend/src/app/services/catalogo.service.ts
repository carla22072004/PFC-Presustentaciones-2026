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

@Injectable({ providedIn: 'root' })
export class CatalogoService {
    private apiUrl = 'http://localhost:8080/api/catalogos';
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
}
