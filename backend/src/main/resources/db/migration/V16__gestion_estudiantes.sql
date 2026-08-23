-- =============================================================================
-- V16__gestion_estudiantes.sql
-- Gestión de estudiantes: agrega un estado académico real (Estudiante no tenía
-- ningún campo de estado -- solo se creaba implícitamente, con la primera carrera
-- del catálogo y semestre fijo en 1, como efecto secundario de la primera
-- solicitud de un estudiante; ver SolicitudServiceImpl.crearPerfilEstudiante).
-- =============================================================================

CREATE TABLE presus.estados_academicos (
    id SMALLINT NOT NULL,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO presus.estados_academicos (id, codigo, nombre) VALUES
    (1, 'ACTIVO', 'Activo'),
    (2, 'EGRESADO', 'Egresado'),
    (3, 'GRADUADO', 'Graduado'),
    (4, 'RETIRADO', 'Retirado'),
    (5, 'SUSPENDIDO', 'Suspendido')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE presus.estudiante ADD COLUMN IF NOT EXISTS estado_academico_id SMALLINT;
UPDATE presus.estudiante SET estado_academico_id = 1 WHERE estado_academico_id IS NULL;
ALTER TABLE presus.estudiante ALTER COLUMN estado_academico_id SET DEFAULT 1;
ALTER TABLE presus.estudiante ALTER COLUMN estado_academico_id SET NOT NULL;
ALTER TABLE presus.estudiante
    ADD CONSTRAINT fk_estudiante_estado_academico FOREIGN KEY (estado_academico_id)
    REFERENCES presus.estados_academicos (id);

-- Nuevo permiso, mismo catálogo dinámico de V13 -- ADMIN y COORDINADOR administran estudiantes.
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES
    (20, 'ESTUDIANTES_GESTIONAR', 'Gestionar estudiantes', 'Estudiantes',
     'Registrar estudiantes y editar carrera, semestre, período de ingreso y estado académico')
ON CONFLICT (id) DO NOTHING;

INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 1, p.id FROM presus.permisos p WHERE p.codigo = 'ESTUDIANTES_GESTIONAR'
ON CONFLICT DO NOTHING;

INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 3, p.id FROM presus.permisos p WHERE p.codigo = 'ESTUDIANTES_GESTIONAR'
ON CONFLICT DO NOTHING;

-- Auditoría (misma función genérica de V15) -- quién registró/modificó un estudiante y cuándo.
CREATE TRIGGER trg_auditoria_estudiante
    AFTER INSERT OR UPDATE OR DELETE ON presus.estudiante
    FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();
