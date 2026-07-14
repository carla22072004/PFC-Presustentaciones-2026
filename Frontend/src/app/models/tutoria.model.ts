export interface TutoriaMensaje {
  id: number;
  faseId: number;
  remitenteId: number;
  nombreRemitente: string;
  contenido: string;
  fechaEnvio: string;
  tipo: 'OBSERVACION' | 'RESPUESTA' | 'APROBACION';
  leido: boolean;
}

export interface TutoriaFase {
  id: number;
  tutorId: number;
  numeroFase: number;
  estado: 'PENDIENTE_ESTUDIANTE' | 'PENDIENTE_TUTOR' | 'APROBADA';
  fechaInicio: string;
  fechaAprobacion?: string;
  archivoPdfEstudiante?: string;
  tamanoPdfBytes?: number;
  mensajes: TutoriaMensaje[];
}

export interface TutoriaResumen {
  tutorId: number;
  solicitudId: number;
  tituloTema: string;
  nombreEstudiante: string;
  nombreTutor: string;
  totalFases: number;
  fasesAprobadas: number;
  estadoTutoria: 'ACTIVO' | 'COMPLETADA';
  mensajesNoLeidos: number;
  solicitudSuspendida?: boolean;
}
