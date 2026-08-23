-- =============================================================================
-- V17__gestion_carreras.sql
-- Gestión de carreras: CRUD de facultades, carreras, modalidades de titulación y
-- períodos académicos (hasta ahora solo existían endpoints de solo lectura en
-- CatalogoController, sembrados a mano por migración).
--
-- facultades_id_seq, carreras_id_seq y modalidades_titulacion_seq nunca avanzaron:
-- sus únicas filas (facultad FCI id=1, carrera ISW id=1, y las 3 modalidades base
-- id=1..3) se insertaron con id explícito por SQL crudo (V1/V8/V9), sin tocar la
-- secuencia. El primer nextval() de Hibernate habría devuelto 1 otra vez y violado
-- la PK -- mismo bug ya visto con roles_usuario (ver RolController). periodos_academicos
-- sí quedó sincronizado (su seed avanzó la secuencia), pero se ajusta también por
-- si acaso para que quede consistente y a prueba de futuros seeds manuales.
-- =============================================================================

SELECT setval('presus.facultades_id_seq', (SELECT COALESCE(MAX(id), 1) FROM presus.facultades), true);
SELECT setval('presus.carreras_id_seq', (SELECT COALESCE(MAX(id), 1) FROM presus.carreras), true);
SELECT setval('presus.periodos_academicos_id_seq', (SELECT COALESCE(MAX(id), 1) FROM presus.periodos_academicos), true);
SELECT setval('presus.modalidades_titulacion_seq', (SELECT COALESCE(MAX(id), 1) FROM presus.modalidades_titulacion), true);

-- Nuevo permiso, mismo catálogo dinámico de V13 -- solo ADMIN administra la estructura
-- académica base (facultades/carreras/modalidades/períodos); COORDINADOR y el resto
-- siguen viéndola de solo lectura vía /api/catalogos/* (permitAll a isAuthenticated()).
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES
    (21, 'CARRERAS_GESTIONAR', 'Gestionar carreras', 'Carreras',
     'Administrar facultades, carreras, modalidades de titulación y períodos académicos')
ON CONFLICT (id) DO NOTHING;

INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 1, p.id FROM presus.permisos p WHERE p.codigo = 'CARRERAS_GESTIONAR'
ON CONFLICT DO NOTHING;

-- Auditoría (misma función genérica de V15) -- quién creó/modificó/eliminó cada catálogo y cuándo.
CREATE TRIGGER trg_auditoria_facultades
    AFTER INSERT OR UPDATE OR DELETE ON presus.facultades
    FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();

CREATE TRIGGER trg_auditoria_carreras
    AFTER INSERT OR UPDATE OR DELETE ON presus.carreras
    FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();

CREATE TRIGGER trg_auditoria_modalidades_titulacion
    AFTER INSERT OR UPDATE OR DELETE ON presus.modalidades_titulacion
    FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();

CREATE TRIGGER trg_auditoria_periodos_academicos
    AFTER INSERT OR UPDATE OR DELETE ON presus.periodos_academicos
    FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();
