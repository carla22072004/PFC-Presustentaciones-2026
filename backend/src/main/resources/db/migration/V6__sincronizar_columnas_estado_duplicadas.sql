-- =============================================================================
-- V6__sincronizar_columnas_estado_duplicadas.sql
-- Mismo problema que V5, en las 4 tablas restantes que Hibernate reportó al validar
-- el esquema al arrancar tras el merge del equipo (54 commits): cada entidad tiene un
-- método @PrePersist/@PreUpdate que sincroniza una columna "espejo" (estado_id <-> estado
-- de texto, o tipo_mensaje_id <-> tipo de texto) que ninguna migración crea nunca.
--   - solicitud:         tiene estado_id (FK), falta "estado" (texto)
--   - cronograma:        tiene estado_id (FK), falta "estado" (texto)
--   - tutores:           tiene "estado" (texto), falta estado_id (FK a estados_proceso)
--   - tutoria_fases:     tiene "estado" (texto), falta estado_id (FK a estados_proceso)
--   - tutoria_mensajes:  tiene "tipo" (texto), falta tipo_mensaje_id (FK a tipos_mensaje)
-- Se agregan las columnas, se siembran los catálogos que TutoriaMensaje necesita con
-- IDs fijos (el código no los busca por findByCodigo como en JuradoService/Cronograma,
-- los asigna directo por switch), se rellenan filas existentes reproduciendo la misma
-- lógica de mapeo que cada entidad usa en su método de sincronización, y se añaden
-- las FKs correspondientes.
-- =============================================================================

-- ── solicitud.estado (texto), reflejo de solicitud.estado_id ──────────────────
ALTER TABLE presus.solicitud ADD COLUMN IF NOT EXISTS estado VARCHAR(30);
UPDATE presus.solicitud s SET estado = es.codigo
    FROM presus.estados_solicitud es WHERE es.id = s.estado_id AND s.estado IS NULL;
ALTER TABLE presus.solicitud ALTER COLUMN estado SET NOT NULL;

-- ── cronograma.estado (texto), reflejo de cronograma.estado_id ────────────────
ALTER TABLE presus.cronograma ADD COLUMN IF NOT EXISTS estado VARCHAR(30);
UPDATE presus.cronograma c SET estado = ec.codigo
    FROM presus.estados_cronograma ec WHERE ec.id = c.estado_id AND c.estado IS NULL;
ALTER TABLE presus.cronograma ALTER COLUMN estado SET NOT NULL;

-- ── tutores.estado_id (FK), reflejo de tutores.estado (texto) ─────────────────
ALTER TABLE presus.tutores ADD COLUMN IF NOT EXISTS estado_id SMALLINT;
UPDATE presus.tutores SET estado_id = CASE estado
    WHEN 'ACTIVO' THEN 2
    WHEN 'COMPLETADA' THEN 3
    WHEN 'FINALIZADO' THEN 3
    WHEN 'REEMPLAZADO' THEN 5
    ELSE 1
END WHERE estado_id IS NULL;
ALTER TABLE presus.tutores ALTER COLUMN estado_id SET DEFAULT 1;
ALTER TABLE presus.tutores ALTER COLUMN estado_id SET NOT NULL;
ALTER TABLE presus.tutores
    ADD CONSTRAINT fk_tutores_estado_proceso FOREIGN KEY (estado_id)
    REFERENCES presus.estados_proceso (id);

-- ── tutoria_fases.estado_id (FK), reflejo de tutoria_fases.estado (texto) ─────
ALTER TABLE presus.tutoria_fases ADD COLUMN IF NOT EXISTS estado_id SMALLINT;
UPDATE presus.tutoria_fases SET estado_id = CASE estado
    WHEN 'PENDIENTE_TUTOR' THEN 2
    WHEN 'APROBADA' THEN 3
    ELSE 1
END WHERE estado_id IS NULL;
ALTER TABLE presus.tutoria_fases ALTER COLUMN estado_id SET DEFAULT 1;
ALTER TABLE presus.tutoria_fases ALTER COLUMN estado_id SET NOT NULL;
ALTER TABLE presus.tutoria_fases
    ADD CONSTRAINT fk_tutoria_fases_estado_proceso FOREIGN KEY (estado_id)
    REFERENCES presus.estados_proceso (id);

-- ── tipos_mensaje: catálogo fijo que TutoriaMensajeServiceImpl asigna por switch,
--    no por findByCodigo, así que necesita los IDs exactos que el switch usa ──────
INSERT INTO presus.tipos_mensaje (id, codigo, nombre) VALUES
    (1, 'TEXTO', 'Texto'),
    (2, 'ARCHIVO', 'Archivo'),
    (3, 'SISTEMA', 'Sistema')
ON CONFLICT (id) DO NOTHING;

-- ── tutoria_mensajes.tipo_mensaje_id (FK), reflejo de tutoria_mensajes.tipo (texto) ──
ALTER TABLE presus.tutoria_mensajes ADD COLUMN IF NOT EXISTS tipo_mensaje_id SMALLINT;
UPDATE presus.tutoria_mensajes SET tipo_mensaje_id = CASE tipo
    WHEN 'RESPUESTA' THEN 2
    WHEN 'APROBACION' THEN 3
    ELSE 1
END WHERE tipo_mensaje_id IS NULL;
ALTER TABLE presus.tutoria_mensajes ALTER COLUMN tipo_mensaje_id SET NOT NULL;
ALTER TABLE presus.tutoria_mensajes
    ADD CONSTRAINT fk_tutoria_mensajes_tipo_mensaje FOREIGN KEY (tipo_mensaje_id)
    REFERENCES presus.tipos_mensaje (id);
