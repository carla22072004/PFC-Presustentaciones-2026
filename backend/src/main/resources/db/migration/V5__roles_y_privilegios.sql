-- Roles de PostgreSQL con privilegios diferenciados según responsabilidad.
--
-- presus_app: rol de conexión que usará el backend en tiempo de ejecución.
--   Puede leer y escribir datos (DML) pero NO puede crear, alterar ni borrar
--   tablas (DDL) -- esas operaciones quedan reservadas al rol que corre las
--   migraciones de Flyway (el superusuario configurado en DB_USERNAME).
-- presus_readonly: rol de solo lectura para reportería, BI o auditoría externa
--   -- solo SELECT, sin ninguna posibilidad de modificar datos.
--
-- Las contraseñas de abajo son valores de desarrollo/demo, iguales en espíritu
-- a POSTGRES_PASSWORD en docker-compose.yml. Para un entorno real deben
-- rotarse y gestionarse como secretos (ver docs/despliegue/BACKUP.md para el
-- mismo criterio aplicado a otras credenciales).

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'presus_app') THEN
        CREATE ROLE presus_app LOGIN PASSWORD 'presusAppDemo2026';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'presus_readonly') THEN
        CREATE ROLE presus_readonly LOGIN PASSWORD 'presusReadonlyDemo2026';
    END IF;
END
$$;

ALTER ROLE presus_app SET search_path TO presus, public;
ALTER ROLE presus_readonly SET search_path TO presus, public;

REVOKE ALL ON SCHEMA presus FROM PUBLIC;
GRANT USAGE ON SCHEMA presus TO presus_app, presus_readonly;

-- presus_app: lectura/escritura de datos, sin DDL.
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA presus TO presus_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA presus TO presus_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA presus TO presus_app;
GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA presus TO presus_app;

-- presus_readonly: solo lectura, para reportes/auditoría.
GRANT SELECT ON ALL TABLES IN SCHEMA presus TO presus_readonly;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA presus TO presus_readonly;

-- Privilegios por defecto para objetos que se creen en migraciones futuras
-- (V6+), ejecutadas por el rol propietario del esquema (el usuario de
-- DB_USERNAME, con permisos de DDL).
ALTER DEFAULT PRIVILEGES IN SCHEMA presus
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO presus_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA presus
    GRANT USAGE, SELECT ON SEQUENCES TO presus_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA presus
    GRANT EXECUTE ON ROUTINES TO presus_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA presus
    GRANT SELECT ON TABLES TO presus_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA presus
    GRANT USAGE, SELECT ON SEQUENCES TO presus_readonly;
