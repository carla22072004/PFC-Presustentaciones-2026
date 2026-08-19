-- =============================================================================
-- V4__relajar_orden_estados_solicitud.sql
-- Bug real encontrado probando el flujo de creación de solicitud de punta a punta (Fase 3):
-- estados_solicitud.orden es NOT NULL en V1__schema_inicial.sql, pero el patrón de
-- "lazy-create" usado en varios servicios (SolicitudServiceImpl, JuradoServiceImpl,
-- EvaluacionServiceImpl, ActaServiceImpl -- ver EstadoSolicitud.builder()...build() en cada
-- uno) nunca establece ese campo al crear un estado por primera vez, así que el primer
-- estado que se necesita en una base de datos recién migrada (típicamente "CREADA") fallaba
-- con "null value in column orden violates not-null constraint". Se relaja la restricción
-- con un valor por defecto neutro (0) en vez de tocar los ~7 call-sites de lazy-create, que
-- están fuera del alcance de la Fase 3.
-- =============================================================================

ALTER TABLE presus.estados_solicitud ALTER COLUMN orden SET DEFAULT 0;
ALTER TABLE presus.estados_solicitud ALTER COLUMN orden DROP NOT NULL;
