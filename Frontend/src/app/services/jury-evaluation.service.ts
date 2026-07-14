import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EvaluacionJuradoRequest {
    solicitudId: number;
    juradoId: number;
    notaJurado: number;
    observaciones: string;
}

export interface EvaluacionJuradoResponse {
    id: number;
    solicitudId: number;
    juradoId: number;
    notaJurado: number;
    observaciones: string;
    resultado: string;
    comentarioPreestablecido: string | null;
    nombreJurado: string;
    rolJurado: string;
}

@Injectable({ providedIn: 'root' })
export class JuryEvaluationService {
    private api = 'http://localhost:8080/api/evaluaciones-jurado';

    constructor(private http: HttpClient) {}

    guardarEvaluacion(request: EvaluacionJuradoRequest): Observable<EvaluacionJuradoResponse> {
        return this.http.post<EvaluacionJuradoResponse>(`${this.api}/guardar`, request);
    }

    obtenerEvaluacion(solicitudId: number, juradoId: number): Observable<EvaluacionJuradoResponse | null> {
        return this.http.get<EvaluacionJuradoResponse | null>(`${this.api}/${solicitudId}/${juradoId}`);
    }

    obtenerTribunal(solicitudId: number): Observable<EvaluacionJuradoResponse[]> {
        return this.http.get<EvaluacionJuradoResponse[]>(`${this.api}/tribunal/${solicitudId}`);
    }
}
