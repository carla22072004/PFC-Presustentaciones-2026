import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EscalaCriterioDTO {
  criterioId: number;
  escala: number;   // 1-100
  observaciones?: string;
  observacionAuto?: string;
  observacionManual?: string;
}

export interface EvaluacionRubricaRequest {
  solicitudId: number;
  juradoId: number;
  rubricaId: number;
  criterios: EscalaCriterioDTO[];
  observaciones?: string;
}

export interface CriterioResultado {
  criterioId: number;
  nombreCriterio: string;
  ponderacion: number;
  escala: number;
  rangoDescripcion?: string;
  notaObtenida: number;
  observacionAuto?: string;
  observacionManual?: string;
  observaciones?: string;
}

export interface EvaluacionRubricaResponse {
  solicitudId: number;
  juradoId: number;
  nombreJurado: string;
  rolJurado: string;
  detalles: CriterioResultado[];
  notaTotalJurado: number | null;
  notaPromedioTribunal: number | null;
  tribunalCompleto: boolean;
}

export interface CriterioRubrica {
  id: number;
  nombre: string;
  descripcion: string;
  ponderacion: number;
  orden: number;
}

export interface ObservacionesSolicitudDTO {
  solicitudId: number;
  tituloTema: string;
  nombreEstudiante: string;
  tutor: ObservacionesTutorDTO | null;
  jurados: ObservacionesJuradoDTO[];
  coordinador: ObservacionesCoordinadorDTO | null;
}

export interface ObservacionesTutorDTO {
  tutorId: number;
  nombreTutor: string;
  observaciones: string | null;
  fechaRegistro: string | null;
}

export interface ObservacionesJuradoDTO {
  juradoId: number;
  nombreJurado: string;
  rol: string;
  criterios: CriterioObservacionDTO[];
  notaJurado: number | null;
  observaciones: string | null;
  resultado: string | null;
  comentarioPreestablecido: string | null;
}

export interface CriterioObservacionDTO {
  nombreCriterio: string;
  ponderacion: number;
  escala: number;
  rangoDescripcion: string;
  notaObtenida: number;
  observacionAuto: string | null;
  observacionManual: string | null;
}

export interface ObservacionesCoordinadorDTO {
  observaciones: string | null;
  notaInstructor: number | null;
  notaFinal: number | null;
  resultado: string | null;
}

@Injectable({ providedIn: 'root' })
export class RubricaEvaluacionService {
  private api = 'http://localhost:8080/api/rubrica-evaluacion';
  private rubricaApi = 'http://localhost:8080/api/rubricas';

  constructor(private http: HttpClient) {}

  registrar(request: EvaluacionRubricaRequest): Observable<EvaluacionRubricaResponse> {
    return this.http.post<EvaluacionRubricaResponse>(`${this.api}/registrar`, request);
  }

  obtenerEvaluacionJurado(solicitudId: number, juradoId: number): Observable<EvaluacionRubricaResponse> {
    return this.http.get<EvaluacionRubricaResponse>(`${this.api}/solicitud/${solicitudId}/jurado/${juradoId}`);
  }

  obtenerEvaluacionesSolicitud(solicitudId: number): Observable<EvaluacionRubricaResponse[]> {
    return this.http.get<EvaluacionRubricaResponse[]>(`${this.api}/solicitud/${solicitudId}`);
  }

  notaTribunal(solicitudId: number): Observable<{ nota: number | null; mensaje?: string }> {
    return this.http.get<any>(`${this.api}/nota-tribunal/${solicitudId}`);
  }

  criteriosPorRubrica(rubricaId: number): Observable<CriterioRubrica[]> {
    return this.http.get<CriterioRubrica[]>(`${this.api}/criterios/${rubricaId}`);
  }

  listarRubricas(): Observable<any[]> {
    return this.http.get<any[]>(this.rubricaApi);
  }

  inicializarCriterios(rubricaId: number): Observable<any> {
    return this.http.post(`${this.rubricaApi}/${rubricaId}/inicializar-criterios`, {});
  }

  obtenerObservacionesSolicitud(solicitudId: number): Observable<ObservacionesSolicitudDTO> {
    return this.http.get<ObservacionesSolicitudDTO>(`http://localhost:8080/api/observaciones/solicitud/${solicitudId}`);
  }
}
