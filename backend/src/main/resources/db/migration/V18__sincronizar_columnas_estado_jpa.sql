-- V5: Sincronización de columnas requeridas por entidades JPA
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
END $$;
