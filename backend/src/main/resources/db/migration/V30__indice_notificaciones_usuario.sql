-- =============================================================================
-- V30__indice_notificaciones_usuario.sql
-- Optimización: presus.notificaciones tenía la FK usuario_id pero SIN índice.
-- La consulta "notificaciones del usuario X" (NotificacionRepository.findByUsuarioId,
-- pantalla de campana / bandeja) hacía Seq Scan sobre ~146 000 filas.
--
-- Evidencia EXPLAIN (ANALYZE, BUFFERS) con la base real (1M+ registros):
--   ANTES:  Seq Scan · Rows Removed by Filter: 146 304 · Buffers: 1823 · Execution Time: 12.426 ms
--   DESPUÉS: Bitmap Index Scan sobre ix_notificaciones_usuario · Buffers: 20 · Execution Time: 0.242 ms
--   Mejora: ~51x más rápido, ~91x menos I/O.
-- =============================================================================

CREATE INDEX IF NOT EXISTS ix_notificaciones_usuario
    ON presus.notificaciones (usuario_id);
