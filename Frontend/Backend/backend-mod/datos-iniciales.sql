-- ============================================
-- DATOS INICIALES PARA TESTING
-- Sistema de Pre-Sustentaciones UTEQ
-- ============================================

-- Este script debe ejecutarse DESPUÉS del schema principal

-- 1. USUARIOS
INSERT INTO presus.usuario (id, cedula, nombres, apellidos, email, rol, activo, creado_en) VALUES
(gen_random_uuid(), '1234567890', 'Juan Carlos', 'Pérez López', 'jperez@uteq.edu.ec', 'ESTUDIANTE', true, now()),
(gen_random_uuid(), '0987654321', 'María Elena', 'García Torres', 'mgarcia@uteq.edu.ec', 'ESTUDIANTE', true, now()),
(gen_random_uuid(), '1122334455', 'Dr. Roberto', 'Martínez Silva', 'rmartinez@uteq.edu.ec', 'TUTOR', true, now()),
(gen_random_uuid(), '5544332211', 'Dra. Ana María', 'Rodríguez Vega', 'arodriguez@uteq.edu.ec', 'PRESIDENTE', true, now()),
(gen_random_uuid(), '9988776655', 'Mg. Carlos Alberto', 'López Castro', 'clopez@uteq.edu.ec', 'JURADO', true, now()),
(gen_random_uuid(), '6677889900', 'Ing. Laura Patricia', 'Sánchez Mora', 'lsanchez@uteq.edu.ec', 'COORDINACION', true, now()),
(gen_random_uuid(), '1111222233', 'Admin', 'Sistema', 'admin@uteq.edu.ec', 'ADMIN', true, now())
ON CONFLICT (email) DO NOTHING;

-- 2. ESTUDIANTES
INSERT INTO presus.estudiante (id, usuario_id, carrera, semestre, telefono, expediente_codigo, creado_en)
SELECT gen_random_uuid(), u.id, 'Ingeniería en Software', '8vo', '0991234567', 'SW-2024-001', now()
FROM presus.usuario u WHERE u.cedula = '1234567890'
ON CONFLICT (expediente_codigo) DO NOTHING;

INSERT INTO presus.estudiante (id, usuario_id, carrera, semestre, telefono, expediente_codigo, creado_en)
SELECT gen_random_uuid(), u.id, 'Ingeniería en Software', '8vo', '0987654321', 'SW-2024-002', now()
FROM presus.usuario u WHERE u.cedula = '0987654321'
ON CONFLICT (expediente_codigo) DO NOTHING;

-- 3. DOCENTES
INSERT INTO presus.docente (id, usuario_id, area_especialidad, carga_horaria_semanal, disponible, creado_en)
SELECT gen_random_uuid(), u.id, 'Ingeniería de Software', 20, true, now()
FROM presus.usuario u WHERE u.cedula = '1122334455';

INSERT INTO presus.docente (id, usuario_id, area_especialidad, carga_horaria_semanal, disponible, creado_en)
SELECT gen_random_uuid(), u.id, 'Gestión de Proyectos', 25, true, now()
FROM presus.usuario u WHERE u.cedula = '5544332211';

INSERT INTO presus.docente (id, usuario_id, area_especialidad, carga_horaria_semanal, disponible, creado_en)
SELECT gen_random_uuid(), u.id, 'Base de Datos', 18, true, now()
FROM presus.usuario u WHERE u.cedula = '9988776655';

-- 4. SALAS
INSERT INTO presus.sala (id, codigo, nombre, capacidad, disponible) VALUES
(gen_random_uuid(), 'A-201', 'Sala de Conferencias A', 30, true),
(gen_random_uuid(), 'B-105', 'Laboratorio de Computación 1', 25, true),
(gen_random_uuid(), 'C-301', 'Auditorio Principal', 100, true),
(gen_random_uuid(), 'D-102', 'Sala de Reuniones Virtual', 50, true)
ON CONFLICT (codigo) DO NOTHING;

-- 5. SOLICITUDES DE PRE-SUSTENTACIÓN
INSERT INTO presus.solicitud (id, estudiante_id, titulo_tema, modalidad, estado, fecha_registro, actualizado_en)
SELECT 
    gen_random_uuid(),
    e.id, 
    'Sistema de Gestión de Pre-Sustentaciones para la UTEQ',
    'Proyecto Tecnológico',
    'RECIBIDA',
    now(),
    now()
FROM presus.estudiante e WHERE e.expediente_codigo = 'SW-2024-001';

INSERT INTO presus.solicitud (id, estudiante_id, titulo_tema, modalidad, estado, fecha_registro, actualizado_en)
SELECT 
    gen_random_uuid(),
    e.id, 
    'Aplicación Móvil para Control de Asistencia Estudiantil',
    'Proyecto Tecnológico',
    'RECIBIDA',
    now(),
    now()
FROM presus.estudiante e WHERE e.expediente_codigo = 'SW-2024-002';

-- 6. RÚBRICA DE EVALUACIÓN
INSERT INTO presus.rubrica (id, nombre, version, vigente, creado_en) VALUES
(gen_random_uuid(), 'Rúbrica Pre-defensa Titulación II', '1.0', true, now());

-- 7. CRITERIOS DE LA RÚBRICA
INSERT INTO presus.rubrica_criterio (id, rubrica_id, nombre, ponderacion, puntaje_max, orden)
SELECT gen_random_uuid(), r.id, 'Estructura y organización del anteproyecto', 20, 20, 1
FROM presus.rubrica r WHERE r.nombre = 'Rúbrica Pre-defensa Titulación II';

INSERT INTO presus.rubrica_criterio (id, rubrica_id, nombre, ponderacion, puntaje_max, orden)
SELECT gen_random_uuid(), r.id, 'Fundamentación teórica y marco referencial', 25, 25, 2
FROM presus.rubrica r WHERE r.nombre = 'Rúbrica Pre-defensa Titulación II';

INSERT INTO presus.rubrica_criterio (id, rubrica_id, nombre, ponderacion, puntaje_max, orden)
SELECT gen_random_uuid(), r.id, 'Metodología propuesta', 20, 20, 3
FROM presus.rubrica r WHERE r.nombre = 'Rúbrica Pre-defensa Titulación II';

INSERT INTO presus.rubrica_criterio (id, rubrica_id, nombre, ponderacion, puntaje_max, orden)
SELECT gen_random_uuid(), r.id, 'Claridad y calidad de la exposición', 15, 15, 4
FROM presus.rubrica r WHERE r.nombre = 'Rúbrica Pre-defensa Titulación II';

INSERT INTO presus.rubrica_criterio (id, rubrica_id, nombre, ponderacion, puntaje_max, orden)
SELECT gen_random_uuid(), r.id, 'Respuesta a preguntas del tribunal', 20, 20, 5
FROM presus.rubrica r WHERE r.nombre = 'Rúbrica Pre-defensa Titulación II';

-- Verificación
SELECT 'Usuarios creados:' as info, COUNT(*) as total FROM presus.usuario
UNION ALL
SELECT 'Estudiantes creados:', COUNT(*) FROM presus.estudiante
UNION ALL
SELECT 'Docentes creados:', COUNT(*) FROM presus.docente
UNION ALL
SELECT 'Salas creadas:', COUNT(*) FROM presus.sala
UNION ALL
SELECT 'Solicitudes creadas:', COUNT(*) FROM presus.solicitud
UNION ALL
SELECT 'Rúbricas creadas:', COUNT(*) FROM presus.rubrica
UNION ALL
SELECT 'Criterios creados:', COUNT(*) FROM presus.rubrica_criterio;

-- ═══════════════════════════════════════════════════════════════════════
-- MIGRACIÓN: Alta prioridad RF-06, RF-08, RF-09, RF-11
-- ═══════════════════════════════════════════════════════════════════════

-- RF-09: Campos de ponderación en evaluaciones
ALTER TABLE evaluaciones 
    ADD COLUMN IF NOT EXISTS nota_instructor DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS nota_jurado     DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS peso_instructor DOUBLE PRECISION NOT NULL DEFAULT 60.0,
    ADD COLUMN IF NOT EXISTS peso_jurado     DOUBLE PRECISION NOT NULL DEFAULT 40.0;

-- RF-08: Campos de firma multi-actor en actas
ALTER TABLE actas
    ADD COLUMN IF NOT EXISTS firmada_presidente  BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS fecha_firma_presidente TIMESTAMP,
    ADD COLUMN IF NOT EXISTS firmada_vocal1      BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS fecha_firma_vocal1  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS firmada_vocal2      BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS fecha_firma_vocal2  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS firmada_tutor       BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS fecha_firma_tutor   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS observaciones_acta  TEXT;

-- RF-06: Queries adicionales ya están en repositorios (no requieren DDL extra)
-- La tabla jurados ya existe con: id, docente_id, solicitud_id, rol, confirmado, asignado_en

-- Índice para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_jurados_solicitud ON jurados(solicitud_id);
CREATE INDEX IF NOT EXISTS idx_evaluaciones_solicitud ON evaluaciones(solicitud_id);
CREATE INDEX IF NOT EXISTS idx_actas_solicitud ON actas(solicitud_id);

-- ═══════════════════════════════════════════════════════════════════════
-- MIGRACIÓN RF-07: Criterios de rúbrica y evaluación por criterio
-- ═══════════════════════════════════════════════════════════════════════

-- Tabla criterios_rubrica
CREATE TABLE IF NOT EXISTS criterios_rubrica (
    id               BIGSERIAL PRIMARY KEY,
    rubrica_id       BIGINT NOT NULL REFERENCES rubricas(id) ON DELETE CASCADE,
    nombre           VARCHAR(100) NOT NULL,
    descripcion      TEXT,
    ponderacion      DOUBLE PRECISION NOT NULL,
    orden            INTEGER NOT NULL DEFAULT 1
);

-- Tabla evaluaciones_criterio
CREATE TABLE IF NOT EXISTS evaluaciones_criterio (
    id              BIGSERIAL PRIMARY KEY,
    solicitud_id    BIGINT NOT NULL REFERENCES solicitudes(id),
    jurado_id       BIGINT NOT NULL REFERENCES jurados(id),
    criterio_id     BIGINT NOT NULL REFERENCES criterios_rubrica(id),
    escala          INTEGER NOT NULL CHECK (escala IN (0, 33, 67, 100)),
    nota_obtenida   DOUBLE PRECISION NOT NULL,
    observaciones   TEXT,
    registrado_en   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (solicitud_id, jurado_id, criterio_id)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_criterios_rubrica ON criterios_rubrica(rubrica_id);
CREATE INDEX IF NOT EXISTS idx_eval_criterio_solicitud ON evaluaciones_criterio(solicitud_id);
CREATE INDEX IF NOT EXISTS idx_eval_criterio_jurado ON evaluaciones_criterio(solicitud_id, jurado_id);

-- Insertar criterios institucionales en la rúbrica existente
INSERT INTO criterios_rubrica (rubrica_id, nombre, descripcion, ponderacion, orden)
SELECT r.id, 'Propuesta',
       'La propuesta (software, algoritmos, dispositivos, etc.) debe estar completamente desarrollada siguiendo buenas prácticas de Ingeniería de Software. Máx. 6 puntos.',
       6.0, 1
FROM rubricas r WHERE r.nombre = 'Rúbrica Pre-defensa Titulación II'
ON CONFLICT DO NOTHING;

INSERT INTO criterios_rubrica (rubrica_id, nombre, descripcion, ponderacion, orden)
SELECT r.id, 'Documento',
       'El contenido del documento (informe) debe ser de alta calidad, bien estructurado y redactado con claridad, cumpliendo buenas prácticas en la elaboración de informes técnicos. Máx. 3 puntos.',
       3.0, 2
FROM rubricas r WHERE r.nombre = 'Rúbrica Pre-defensa Titulación II'
ON CONFLICT DO NOTHING;

INSERT INTO criterios_rubrica (rubrica_id, nombre, descripcion, ponderacion, orden)
SELECT r.id, 'Exposición',
       'La exposición debe ser clara, bien estructurada y adecuada para una defensa de titulación, demostrando dominio del tema. Máx. 1 punto.',
       1.0, 3
FROM rubricas r WHERE r.nombre = 'Rúbrica Pre-defensa Titulación II'
ON CONFLICT DO NOTHING;

-- RF-02: Agregar columnas sha256 al anteproyecto (si no existen)
ALTER TABLE anteproyectos ADD COLUMN IF NOT EXISTS sha256_hash VARCHAR(64);
ALTER TABLE anteproyectos ADD COLUMN IF NOT EXISTS tamano_bytes BIGINT;
