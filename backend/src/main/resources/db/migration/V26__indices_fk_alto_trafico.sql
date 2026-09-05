-- =============================================================================
-- V26__indices_fk_alto_trafico.sql
-- Mismo método que V12 (EXPLAIN sin índice -> Seq Scan -> CREATE INDEX -> Index Scan),
-- aplicado a un hallazgo de la auditoría de 2026-09-04: 62 columnas FK sin índice de
-- soporte en la base real. De esas 62, se seleccionaron solo las que participan en
-- endpoints de alto tráfico (verificado con EXPLAIN contra la base real, no supuesto):
-- todas forzaban un Seq Scan completo de tablas de decenas de miles de filas para
-- devolver 1-6 filas.
--
-- solicitud.estudiante_id (44,010 filas): "mis solicitudes" -- cada carga del
-- dashboard de un estudiante. EXPLAIN antes: Seq Scan, cost=0.00..1459.09.
--
-- historial_estados_solicitud.solicitud_id (52,806 filas): timeline de una
-- solicitud, se consulta en cada vista de detalle. EXPLAIN antes: Seq Scan,
-- cost=0.00..1268.08.
--
-- evaluaciones_criterio.solicitud_id y .jurado_id (92,409 filas, la tabla más
-- grande de las 6 revisadas): RubricaEvaluacionServiceImpl la consulta repetidamente
-- por jurado dentro de un bucle (obtenerEvaluacionesSolicitud/buildResponse) en cada
-- revisión de evaluación -- el patrón de acceso más sensible a un Seq Scan de los
-- seis. EXPLAIN antes: Seq Scan, cost=0.00..2296.11 (ambas columnas).
--
-- cronograma.solicitud_id (24,203 filas): resolver el cronograma de una solicitud.
-- EXPLAIN antes: Seq Scan, cost=0.00..601.54.
--
-- tutoria_fases.tutor_id (49,302 filas): "mis tutorías" -- carga frecuente del
-- dashboard de un docente-tutor. EXPLAIN antes: Seq Scan, cost=0.00..1722.24.
--
-- Se deja fuera deliberadamente historial_cronograma.cronograma_id (la séptima
-- columna del mismo hallazgo): solo 3,630 filas y cost=108.38 incluso con Seq Scan
-- -- bajo tráfico real (historial de reprogramaciones, consulta ocasional), no
-- justifica un índice nuevo. No se crean los otros ~55 FK restantes del hallazgo por
-- el mismo criterio: sin evidencia de tráfico alto no se agrega el índice.
-- =============================================================================

CREATE INDEX IF NOT EXISTS ix_solicitud_estudiante
    ON presus.solicitud (estudiante_id);
CREATE INDEX IF NOT EXISTS ix_historial_estados_solicitud_solicitud
    ON presus.historial_estados_solicitud (solicitud_id);
CREATE INDEX IF NOT EXISTS ix_evaluaciones_criterio_solicitud
    ON presus.evaluaciones_criterio (solicitud_id);
CREATE INDEX IF NOT EXISTS ix_evaluaciones_criterio_jurado
    ON presus.evaluaciones_criterio (jurado_id);
CREATE INDEX IF NOT EXISTS ix_cronograma_solicitud
    ON presus.cronograma (solicitud_id);
CREATE INDEX IF NOT EXISTS ix_tutoria_fases_tutor
    ON presus.tutoria_fases (tutor_id);
