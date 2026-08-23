-- =============================================================================
-- V13__permisos_roles_dinamicos.sql
-- Sistema de permisos dinámico real: reemplaza los @PreAuthorize("hasRole(...)")
-- fijos en código por una verificación en tiempo de ejecución contra estas tablas,
-- para que "Gestionar Roles" y "Gestionar Permisos" en el admin sean funcionales
-- de verdad (asignar/quitar un permiso a un rol cambia el acceso sin recompilar).
--
-- roles_usuario.id no es autogenerado y ninguna migración lo siembra (lo hace
-- PreSustentacionesApplication en el arranque, DESPUÉS de Flyway) -- mismo problema
-- que V9 con facultades. Se siembra aquí también, idéntico y con ON CONFLICT DO
-- NOTHING, para que el FK de rol_permisos tenga algo a qué apuntar de inmediato.
--
-- El catálogo de 18 permisos y la asignación inicial por rol reproducen EXACTAMENTE
-- el mapa de @PreAuthorize que existía en el código antes de este cambio (ver commit
-- que reemplaza cada anotación por @permisoService.tienePermiso(...)), para que
-- migrar a este sistema no cambie el comportamiento de nadie el día 1.
-- =============================================================================

INSERT INTO presus.roles_usuario (id, codigo, nombre) VALUES
    (1, 'ADMIN', 'Administrador'), (2, 'DOCENTE', 'Docente'),
    (3, 'COORDINADOR', 'Coordinador'), (4, 'ESTUDIANTE', 'Estudiante')
ON CONFLICT (id) DO NOTHING;

CREATE TABLE presus.permisos (
    id SMALLINT NOT NULL,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(60) NOT NULL,
    descripcion TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE presus.rol_permisos (
    rol_id SMALLINT NOT NULL REFERENCES presus.roles_usuario (id) ON DELETE CASCADE,
    permiso_id SMALLINT NOT NULL REFERENCES presus.permisos (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES
    (1,  'USUARIOS_GESTIONAR',           'Gestionar usuarios',              'Administración', 'Crear, editar, activar/desactivar, eliminar usuarios y asignar sus roles'),
    (2,  'ROLES_PERMISOS_GESTIONAR',     'Gestionar roles y permisos',      'Administración', 'Crear/renombrar/eliminar roles y asignar permisos a cada rol'),
    (3,  'NOTIFICACIONES_GLOBAL_VER',    'Ver notificaciones globales',     'Administración', 'Ver el listado completo de notificaciones de todo el sistema'),
    (4,  'NOTIFICACIONES_ENVIAR',        'Enviar notificaciones',           'Administración', 'Crear notificaciones manuales dirigidas a otros usuarios'),
    (5,  'SOLICITUDES_REVISAR',          'Revisar solicitudes',             'Solicitudes',     'Aprobar, rechazar y listar solicitudes de pre-sustentación'),
    (6,  'SOLICITUDES_SUSPENDER',        'Suspender solicitudes',           'Solicitudes',     'Suspender una solicitud en curso'),
    (7,  'TRIBUNAL_TUTOR_ASIGNAR',       'Asignar tutor y tribunal',        'Tribunal',        'Asignar o eliminar el tutor y los jurados de una solicitud'),
    (8,  'CRONOGRAMA_GESTIONAR',         'Gestionar cronograma',            'Cronograma',      'Programar, reprogramar y eliminar fechas de defensa'),
    (9,  'SALA_GESTIONAR',               'Gestionar salas',                 'Cronograma',      'Crear y eliminar salas de sustentación'),
    (10, 'RUBRICA_GESTIONAR',            'Gestionar rúbricas',              'Evaluación',      'Crear, editar y eliminar rúbricas y sus criterios'),
    (11, 'EVALUACION_CALIFICAR',         'Calificar solicitudes',           'Evaluación',      'Registrar la nota final ponderada de una solicitud'),
    (12, 'EVALUACION_RUBRICA_REGISTRAR', 'Registrar evaluación de rúbrica', 'Evaluación',      'Registrar la evaluación de un jurado/instructor por criterio'),
    (13, 'ACTA_GENERAR',                 'Generar acta',                    'Actas',           'Generar el acta de pre-sustentación'),
    (14, 'ACTA_FIRMAR',                  'Firmar acta',                     'Actas',           'Firmar digitalmente el acta de pre-sustentación'),
    (15, 'ANTEPROYECTO_REVISAR',         'Revisar anteproyecto',            'Tutoría',         'Aprobar o rechazar el anteproyecto de un estudiante'),
    (16, 'TUTORIA_GESTIONAR',            'Gestionar tutoría',               'Tutoría',         'Crear y aprobar fases de tutoría como tutor'),
    (17, 'TUTORIA_AVANCE_ESTUDIANTE',    'Registrar avance de tutoría',     'Tutoría',         'Subir correcciones/avances como estudiante en una fase de tutoría'),
    (18, 'REPORTES_VER',                 'Ver reportes',                    'Reportes',        'Generar reportes en PDF y estadísticas del sistema')
ON CONFLICT (id) DO NOTHING;

-- ADMIN: todos los permisos
INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 1, p.id FROM presus.permisos p
ON CONFLICT DO NOTHING;

-- COORDINADOR: todos menos administración de usuarios/roles y ver notificaciones globales
INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 3, p.id FROM presus.permisos p
WHERE p.codigo NOT IN ('USUARIOS_GESTIONAR', 'ROLES_PERMISOS_GESTIONAR', 'NOTIFICACIONES_GLOBAL_VER')
ON CONFLICT DO NOTHING;

-- DOCENTE
INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 2, p.id FROM presus.permisos p
WHERE p.codigo IN ('SOLICITUDES_REVISAR', 'ACTA_FIRMAR', 'ANTEPROYECTO_REVISAR', 'EVALUACION_RUBRICA_REGISTRAR', 'TUTORIA_GESTIONAR')
ON CONFLICT DO NOTHING;

-- ESTUDIANTE
INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 4, p.id FROM presus.permisos p
WHERE p.codigo IN ('TUTORIA_AVANCE_ESTUDIANTE')
ON CONFLICT DO NOTHING;
