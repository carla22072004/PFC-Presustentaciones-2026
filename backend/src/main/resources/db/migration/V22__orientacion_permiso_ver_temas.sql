-- =============================================================================
-- V22__orientacion_permiso_ver_temas.sql
-- Corrige un choque de ids en V21: el permiso ORIENTACION_TEMAS_VER se insertó con
-- id 26, que V19 ya había usado para ACTAS_GESTIONAR -> el ON CONFLICT (id) DO
-- NOTHING lo descartó silenciosamente y el permiso nunca quedó en el catálogo, así
-- que /api/v1/orientacion/temas respondía 403 para todos.
--
-- Se vuelve a insertar con un id calculado (MAX+1) para no volver a chocar, y la
-- guarda pasa a ser por 'codigo' (único) en vez de por id. Idempotente.
-- =============================================================================

INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.permisos),
       'ORIENTACION_TEMAS_VER', 'Ver temas propuestos', 'Orientación',
       'Explorar el catálogo de temas de titulación propuestos y su detalle'
WHERE NOT EXISTS (
    SELECT 1 FROM presus.permisos WHERE codigo = 'ORIENTACION_TEMAS_VER'
);

-- ADMIN (1), DOCENTE (2), COORDINADOR (3) y ESTUDIANTE (4) pueden explorar el catálogo.
INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT r.rol_id, p.id
FROM presus.permisos p
CROSS JOIN (VALUES (1), (2), (3), (4)) AS r(rol_id)
WHERE p.codigo = 'ORIENTACION_TEMAS_VER'
ON CONFLICT DO NOTHING;
