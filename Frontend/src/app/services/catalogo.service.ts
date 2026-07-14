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
}
