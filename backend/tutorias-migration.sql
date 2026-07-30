-- ============================================================
-- MIGRACIÓN: Módulo de Tutorías
-- Sistema de Pre-Sustentaciones UTEQ
-- Ejecutar sobre el schema presus DESPUÉS del schema principal
-- ============================================================

-- ── 1. tutoria_fases ─────────────────────────────────────────────────────────

CREATE TABLE presus.tutoria_fases (
    id                    BIGSERIAL       PRIMARY KEY,
    tutor_id              BIGINT          NOT NULL,
    numero_fase           INTEGER         NOT NULL,
    estado                VARCHAR(30)     NOT NULL DEFAULT 'PENDIENTE_ESTUDIANTE',
    fecha_inicio          TIMESTAMP       NOT NULL,
    fecha_aprobacion      TIMESTAMP,
    archivo_pdf_estudiante VARCHAR(255),
    sha256_pdf            VARCHAR(64),
    tamano_pdf_bytes      BIGINT,

    -- Integridad referencial
    CONSTRAINT fk_tutoria_fases_tutor
        FOREIGN KEY (tutor_id) REFERENCES presus.tutores(id)
        ON DELETE CASCADE,

    -- Una fase por número de revisión dentro del mismo tutor
    CONSTRAINT uq_tutoria_fases_tutor_numero
        UNIQUE (tutor_id, numero_fase),

    -- Solo se permiten las fases 1, 2 y 3
    CONSTRAINT chk_tutoria_fases_numero
        CHECK (numero_fase BETWEEN 1 AND 3),

    -- Estados válidos
    CONSTRAINT chk_tutoria_fases_estado
        CHECK (estado IN ('PENDIENTE_ESTUDIANTE', 'PENDIENTE_TUTOR', 'APROBADA'))
);

CREATE INDEX idx_tutoria_fases_tutor_id
    ON presus.tutoria_fases (tutor_id);

COMMENT ON TABLE  presus.tutoria_fases                  IS 'Ciclos de revisión (máx. 3) dentro de una tutoría';
COMMENT ON COLUMN presus.tutoria_fases.numero_fase      IS 'Número de revisión: 1, 2 o 3';
COMMENT ON COLUMN presus.tutoria_fases.estado           IS 'PENDIENTE_ESTUDIANTE | PENDIENTE_TUTOR | APROBADA';
COMMENT ON COLUMN presus.tutoria_fases.sha256_pdf       IS 'Hash SHA-256 del PDF corregido para verificación de integridad';
COMMENT ON COLUMN presus.tutoria_fases.tamano_pdf_bytes IS 'Tamaño del PDF en bytes';

-- ── 2. tutoria_mensajes ───────────────────────────────────────────────────────

CREATE TABLE presus.tutoria_mensajes (
    id           BIGSERIAL    PRIMARY KEY,
    fase_id      BIGINT       NOT NULL,
    remitente_id BIGINT       NOT NULL,
    contenido    TEXT         NOT NULL,
    fecha_envio  TIMESTAMP    NOT NULL,
    tipo         VARCHAR(20)  NOT NULL,
    leido        BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Integridad referencial
    CONSTRAINT fk_tutoria_mensajes_fase
        FOREIGN KEY (fase_id) REFERENCES presus.tutoria_fases(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tutoria_mensajes_remitente
        FOREIGN KEY (remitente_id) REFERENCES presus.usuarios(id)
        ON DELETE RESTRICT,

    -- Tipos válidos
    CONSTRAINT chk_tutoria_mensajes_tipo
        CHECK (tipo IN ('OBSERVACION', 'RESPUESTA', 'APROBACION'))
);

CREATE INDEX idx_tutoria_mensajes_fase_id
    ON presus.tutoria_mensajes (fase_id);

CREATE INDEX idx_tutoria_mensajes_remitente_id
    ON presus.tutoria_mensajes (remitente_id);

-- Índice parcial para consultar mensajes no leídos eficientemente
CREATE INDEX idx_tutoria_mensajes_no_leidos
    ON presus.tutoria_mensajes (fase_id, remitente_id)
    WHERE leido = FALSE;

COMMENT ON TABLE  presus.tutoria_mensajes             IS 'Hilo de mensajes de observaciones y respuestas por fase de tutoría';
COMMENT ON COLUMN presus.tutoria_mensajes.tipo        IS 'OBSERVACION (tutor) | RESPUESTA (estudiante) | APROBACION (tutor)';
COMMENT ON COLUMN presus.tutoria_mensajes.leido       IS 'TRUE cuando el destinatario ha abierto el mensaje';
