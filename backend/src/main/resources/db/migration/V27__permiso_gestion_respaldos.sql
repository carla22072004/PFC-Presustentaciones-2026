-- =============================================================================
-- V27__permiso_gestion_respaldos.sql
-- Nuevo permiso para el apartado "Gestión de Respaldos de Base de Datos" del
-- administrador: generar, listar, descargar, restaurar y eliminar dumps
-- completos de la base (pg_dump -Fc). Mismo catálogo dinámico de V13, así que
-- aparece automáticamente en "Gestionar Permisos" y se puede reasignar sin
-- recompilar. Solo ADMIN lo administra: es una operación de infraestructura,
-- no del flujo académico (COORDINADOR no lo recibe).
-- =============================================================================

INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES
    (29, 'BACKUPS_GESTIONAR', 'Gestionar respaldos de base de datos', 'Administración',
     'Generar, descargar, restaurar y eliminar respaldos (dumps) completos de la base de datos')
ON CONFLICT (id) DO NOTHING;

INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 1, p.id FROM presus.permisos p WHERE p.codigo = 'BACKUPS_GESTIONAR'
ON CONFLICT DO NOTHING;
