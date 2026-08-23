-- =============================================================================
-- V5__agregar_estado_id_anteproyectos.sql
-- Bug real encontrado al levantar el backend tras el merge del equipo (54 commits):
-- la entidad Anteproyecto.java mapea una columna "estado_id" (FK NOT NULL a
-- estados_proceso, ver el comentario en esa clase) que ninguna migración crea nunca --
-- V1__schema_inicial.sql solo define presus.anteproyectos con la columna de texto
-- "estado", y presus.estados_proceso se crea vacía (sin las filas PENDIENTE/EN_PROCESO/
-- APROBADO/OBSERVADO/RECHAZADO que Anteproyecto.sincronizarEstadoProceso() espera).
-- Hibernate falla el arranque en validación de esquema: "Schema-validation: missing
-- column [estado_id] in table [presus.anteproyectos]". Se agrega la columna, se siembra
-- el catálogo con los mismos códigos/IDs que usa el switch de la entidad, se rellena
-- cualquier fila existente a partir de su "estado" de texto, y se añade la FK.
-- =============================================================================

INSERT INTO presus.estados_proceso (id, codigo, nombre) VALUES
    (1, 'PENDIENTE', 'Pendiente'),
    (2, 'EN_PROCESO', 'En proceso'),
    (3, 'APROBADO', 'Aprobado'),
    (4, 'OBSERVADO', 'Observado'),
    (5, 'RECHAZADO', 'Rechazado')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE presus.anteproyectos ADD COLUMN IF NOT EXISTS estado_id SMALLINT;

UPDATE presus.anteproyectos SET estado_id = CASE estado
    WHEN 'ENVIADO' THEN 2
    WHEN 'APROBADO' THEN 3
    WHEN 'OBSERVADO' THEN 4
    WHEN 'RECHAZADO' THEN 5
    ELSE 1
END
WHERE estado_id IS NULL;

ALTER TABLE presus.anteproyectos ALTER COLUMN estado_id SET DEFAULT 1;
ALTER TABLE presus.anteproyectos ALTER COLUMN estado_id SET NOT NULL;

ALTER TABLE presus.anteproyectos
    ADD CONSTRAINT fk_anteproyectos_estado_proceso FOREIGN KEY (estado_id)
    REFERENCES presus.estados_proceso (id);
