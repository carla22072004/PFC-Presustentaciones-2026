export interface EtapaSeguimientoDTO {
  nombre: string;
  estadoVisual: string; // COMPLETADO, EN_PROCESO, PENDIENTE, RECHAZADO
  fecha: string | null;
  descripcion: string;
}

export interface SeguimientoDTO {
  solicitudId: number;
  tituloProyecto: string;
  estadoActual: string;
  porcentajeProgreso: number;
  etapas: EtapaSeguimientoDTO[];
}
