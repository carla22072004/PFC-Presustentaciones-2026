-- =============================================================================
-- V18__sincronizar_columnas_estado_jpa.sql
-- Renombrada de un V5__add_estado_id_anteproyectos.sql que llegó por un merge con
-- el trabajo de una compañera de equipo (carla22072004) -- chocaba de número con el
-- V5__agregar_estado_id_anteproyectos.sql ya existente y aplicado. Ese mismo commit
-- también editaba V1__schema_inicial.sql directamente para hornear estas columnas
-- desde el CREATE TABLE -- se revirtió esa parte (una migración ya aplicada no se
-- edita nunca: rompe la validación de checksum de Flyway en cualquier base que ya
-- la haya corrido) y se preserva aquí, de forma aditiva, el mismo efecto real que
-- buscaba: columnas/FKs que las entidades JPA (Anteproyecto, Cronograma, Solicitud,
-- Tutor, TutoriaFase, TutoriaMensaje, EvaluacionCriterio) esperan y que V1 nunca
-- creó. Se agregan las 2 FK que solo estaban en la edición de V1 y no en el V5
-- original (evaluaciones_criterio.jurado_id, tutoria_mensajes.tipo_mensaje_id).
-- =============================================================================

ALTER TABLE presus.anteproyectos ADD COLUMN IF NOT EXISTS estado_id smallint NOT NULL DEFAULT 1;
ALTER TABLE presus.cronograma ADD COLUMN IF NOT EXISTS estado varchar(30) NOT NULL DEFAULT 'PROGRAMADO';
ALTER TABLE presus.solicitud ADD COLUMN IF NOT EXISTS estado varchar(30) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE presus.tutores ADD COLUMN IF NOT EXISTS estado_id smallint NOT NULL DEFAULT 1;
ALTER TABLE presus.tutoria_fases ADD COLUMN IF NOT EXISTS estado_id smallint NOT NULL DEFAULT 1;
ALTER TABLE presus.tutoria_mensajes ADD COLUMN IF NOT EXISTS tipo_mensaje_id smallint NOT NULL DEFAULT 1;
ALTER TABLE presus.evaluaciones_criterio ADD COLUMN IF NOT EXISTS jurado_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_anteproyectos_estado_id') THEN
        ALTER TABLE presus.anteproyectos ADD CONSTRAINT fk_anteproyectos_estado_id FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tutores_estado_id') THEN
        ALTER TABLE presus.tutores ADD CONSTRAINT fk_tutores_estado_id FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tutoria_fases_estado_id') THEN
        ALTER TABLE presus.tutoria_fases ADD CONSTRAINT fk_tutoria_fases_estado_id FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_evaluaciones_criterio_jurado') THEN
        ALTER TABLE presus.evaluaciones_criterio ADD CONSTRAINT fk_evaluaciones_criterio_jurado FOREIGN KEY (jurado_id) REFERENCES presus.miembros_tribunal(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tutoria_mensajes_tipo') THEN
        ALTER TABLE presus.tutoria_mensajes ADD CONSTRAINT fk_tutoria_mensajes_tipo FOREIGN KEY (tipo_mensaje_id) REFERENCES presus.tipos_mensaje(id);
    END IF;
END $$;
