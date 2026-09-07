-- =============================================================================
-- V29__respaldos_fase2_wal_diferencial.sql
-- Fase 2 del plan de respaldos (ver docs/basedatos/PLAN-RESPALDOS-RECUPERACION.md):
--   - Archivado continuo de WAL / PITR (incremental) -> se activa en docker-compose.yml;
--     aquí solo se agrega la retención de WAL y el panel es de solo lectura.
--   - Respaldo diferencial (filas cambiadas desde el último FULL) programable.
--
-- Se amplía presus.respaldo_config (de V28) con:
--   - retencion de WAL archivado (días)
--   - programación del diferencial (activo + cron), por defecto miércoles y viernes 02:30
--     (mismo criterio que la tabla del plan, sección 3).
-- =============================================================================

ALTER TABLE presus.respaldo_config
    ADD COLUMN IF NOT EXISTS retener_dias_wal   SMALLINT     NOT NULL DEFAULT 14,
    ADD COLUMN IF NOT EXISTS diferencial_activo BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cron_diferencial   VARCHAR(120) NOT NULL DEFAULT '0 30 2 * * WED,FRI';

ALTER TABLE presus.respaldo_config
    ADD CONSTRAINT respaldo_config_wal_pos CHECK (retener_dias_wal >= 0);
