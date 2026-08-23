-- =============================================================================
-- V7__agregar_jurado_id_evaluaciones_criterio.sql
-- Mismo tipo de gap que V5/V6, pero aquí no hay columna "espejo" previa: el commit
-- b941b12 ("fix(backend): relacion jurado faltante en EvaluacionCriterio y cache Redis
-- sin fechas") agregó la relación @ManyToOne Jurado a EvaluacionCriterio.java (columna
-- real "jurado_id", FK NOT NULL a miembros_tribunal) pero la migración que crea esa
-- columna nunca se agregó -- Hibernate fallaba el arranque con "Schema-validation:
-- missing column [jurado_id] in table [presus.evaluaciones_criterio]".
-- =============================================================================

ALTER TABLE presus.evaluaciones_criterio ADD COLUMN IF NOT EXISTS jurado_id BIGINT;
ALTER TABLE presus.evaluaciones_criterio ALTER COLUMN jurado_id SET NOT NULL;
ALTER TABLE presus.evaluaciones_criterio
    ADD CONSTRAINT fk_evaluaciones_criterio_jurado FOREIGN KEY (jurado_id)
    REFERENCES presus.miembros_tribunal (id);
